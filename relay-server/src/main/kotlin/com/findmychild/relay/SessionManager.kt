package com.findmychild.relay

import io.ktor.websocket.WebSocketSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * 管理配对会话。
 * sessionId → Session(parentSession?, childSession?, createdAt, lastActiveAt)
 *
 * 内置定时清理机制：超过 [SESSION_TIMEOUT_MS] 未活跃的会话自动清除，
 * 防止家长生成 QR 后不扫码导致 session 永久残留。
 */
class SessionManager {

    private data class Session(
        var parentSession: WebSocketSession? = null,
        var childSession: WebSocketSession? = null,
        val createdAt: Long = System.currentTimeMillis(),
        var lastActiveAt: Long = System.currentTimeMillis()
    )

    private val sessions = ConcurrentHashMap<String, Session>()
    private val mutex = Mutex()

    companion object {
        /** 会话超时时间：30 分钟 */
        private const val SESSION_TIMEOUT_MS = 30 * 60 * 1000L
        /** 清理间隔：5 分钟 */
        private const val CLEANUP_INTERVAL_MS = 5 * 60 * 1000L
    }

    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // 启动定期清理任务
        cleanupScope.launch {
            while (true) {
                delay(CLEANUP_INTERVAL_MS)
                cleanupExpiredSessions()
            }
        }
    }

    /**
     * 清理过期的空闲会话。
     * 如果双方都未连接且创建时间超过超时阈值，则移除。
     */
    private suspend fun cleanupExpiredSessions() {
        val now = System.currentTimeMillis()
        val expiredIds = mutableListOf<String>()

        mutex.withLock {
            for ((id, session) in sessions) {
                val isIdle = session.parentSession == null && session.childSession == null
                val isExpired = now - session.lastActiveAt > SESSION_TIMEOUT_MS
                if (isIdle && isExpired) {
                    expiredIds.add(id)
                }
            }
            for (id in expiredIds) {
                sessions.remove(id)
            }
        }
    }

    /**
     * 注册设备到会话。
     * @return PairingStatus 通知的 PairingStatusMessage 和需要被通知的对方 session
     */
    suspend fun register(
        sessionId: String,
        role: String,
        wsSession: WebSocketSession
    ): RegisterResult {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val session = sessions.getOrPut(sessionId) { Session() }
            session.lastActiveAt = now

            return when (role) {
                "PARENT" -> {
                    if (session.parentSession != null) {
                        RegisterResult.Error("该会话已有家长设备")
                    } else {
                        session.parentSession = wsSession
                        if (session.childSession != null) {
                            // 双方都已就位 → 配对成功
                            RegisterResult.Paired(
                                peerSession = session.childSession!!,
                                peerNotification = PairingStatusMessage(
                                    status = "PAIRED",
                                    peerDeviceName = "家长设备"
                                )
                            )
                        } else {
                            RegisterResult.Waiting
                        }
                    }
                }
                "CHILD" -> {
                    if (session.childSession != null) {
                        RegisterResult.Error("该会话已有孩子设备")
                    } else {
                        session.childSession = wsSession
                        if (session.parentSession != null) {
                            RegisterResult.Paired(
                                peerSession = session.parentSession!!,
                                peerNotification = PairingStatusMessage(
                                    status = "PAIRED",
                                    peerDeviceName = "孩子设备"
                                )
                            )
                        } else {
                            RegisterResult.Waiting
                        }
                    }
                }
                else -> RegisterResult.Error("无效的角色: $role")
            }
        }
    }

    /**
     * 将位置转发给同会话的对方。
     */
    suspend fun relayLocation(
        sessionId: String,
        fromSession: WebSocketSession,
        latitude: Double,
        longitude: Double,
        accuracy: Float,
        timestamp: Long
    ): WebSocketSession? {
        val session = sessions[sessionId] ?: return null
        session.lastActiveAt = System.currentTimeMillis()

        // 判断发送者是谁，转发给另一方
        return if (fromSession == session.parentSession) {
            session.childSession
        } else if (fromSession == session.childSession) {
            session.parentSession
        } else null
    }

    /**
     * 处理断开连接。
     * @return 需要通知的对端 session，如果双方都断开则返回 null
     */
    suspend fun disconnect(
        sessionId: String,
        wsSession: WebSocketSession
    ): WebSocketSession? {
        val session = sessions[sessionId] ?: return null

        return mutex.withLock {
            session.lastActiveAt = System.currentTimeMillis()
            when (wsSession) {
                session.parentSession -> {
                    session.parentSession = null
                    val peer = session.childSession
                    if (session.childSession == null) {
                        sessions.remove(sessionId)
                    }
                    peer
                }
                session.childSession -> {
                    session.childSession = null
                    val peer = session.parentSession
                    if (session.parentSession == null) {
                        sessions.remove(sessionId)
                    }
                    peer
                }
                else -> null
            }
        }
    }
}

sealed class RegisterResult {
    data object Waiting : RegisterResult()
    data class Paired(
        val peerSession: WebSocketSession,
        val peerNotification: PairingStatusMessage
    ) : RegisterResult()

    data class Error(val message: String) : RegisterResult()
}
