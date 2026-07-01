package com.findmychild.app.data.remote

import com.findmychild.app.data.remote.dto.ErrorDto
import com.findmychild.app.data.remote.dto.PairingStatusDto
import com.findmychild.app.data.remote.dto.PeerLocationDto
import com.findmychild.app.data.remote.dto.RegisterMessage
import com.findmychild.app.data.remote.dto.LocationUpdateDto
import com.findmychild.app.domain.model.ConnectionState
import com.findmychild.app.domain.model.LocationData
import com.findmychild.app.domain.model.UserRole
import com.squareup.moshi.Moshi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * WebSocket 连接管理。
 * 使用 OkHttp 原生 WebSocket，通过 callbackFlow 封装为 Flow。
 */
@Singleton
class RelayWebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
    private val relayConfig: RelayConfig
) {
    private var webSocket: WebSocket? = null
    private var reconnectAttempt = 0
    private var isActive = false

    private val connectionStateChannel = Channel<ConnectionState>(Channel.CONFLATED)
    private val peerLocationChannel = Channel<LocationData>(Channel.CONFLATED)
    private val pairingStatusChannel = Channel<PairingStatusDto>(Channel.CONFLATED)

    /** 连接状态 Flow */
    val connectionState: Flow<ConnectionState> = connectionStateChannel.receiveAsFlow()

    /** 对端位置 Flow */
    val peerLocation: Flow<LocationData> = peerLocationChannel.receiveAsFlow()

    /** 配对状态 Flow */
    val pairingStatus: Flow<PairingStatusDto> = pairingStatusChannel.receiveAsFlow()

    /** 建立 WebSocket 连接 */
    suspend fun connect(sessionId: String, role: UserRole) {
        isActive = true
        reconnectAttempt = 0
        connectionStateChannel.send(ConnectionState.Connecting)

        val request = Request.Builder()
            .url(relayConfig.wsUrl)
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                connectionStateChannel.trySend(ConnectionState.Connected)
                reconnectAttempt = 0

                // 发送注册消息
                val roleStr = if (role == UserRole.PARENT) "PARENT" else "CHILD"
                val registerMsg = RegisterMessage(sessionId = sessionId, role = roleStr)
                val json = moshi.adapter(RegisterMessage::class.java).toJson(registerMsg)
                webSocket.send(json)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                connectionStateChannel.trySend(ConnectionState.Disconnected)
                attemptReconnect(sessionId, role)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                connectionStateChannel.trySend(ConnectionState.Error(t.message ?: "连接失败"))
                attemptReconnect(sessionId, role)
            }
        }

        webSocket = okHttpClient.newWebSocket(request, listener)
    }

    /** 发送位置更新 */
    fun sendLocation(location: LocationData, sessionId: String) {
        val dto = LocationUpdateDto(
            sessionId = sessionId,
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            timestamp = location.timestamp
        )
        val json = moshi.adapter(LocationUpdateDto::class.java).toJson(dto)
        webSocket?.send(json)
    }

    /** 断开连接 */
    fun disconnect() {
        isActive = false
        reconnectAttempt = 0
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        connectionStateChannel.trySend(ConnectionState.Disconnected)
    }

    // ===== Private =====

    private fun handleMessage(text: String) {
        try {
            val jsonAdapter = moshi.adapter(Map::class.java)
            val map = jsonAdapter.fromJson(text) ?: return
            val type = map["type"] as? String ?: return

            when (type) {
                "PAIRING_STATUS" -> {
                    val msg = moshi.adapter(PairingStatusDto::class.java).fromJson(text)
                    msg?.let { pairingStatusChannel.trySend(it) }
                }
                "PEER_LOCATION" -> {
                    val msg = moshi.adapter(PeerLocationDto::class.java).fromJson(text)
                    msg?.let {
                        peerLocationChannel.trySend(
                            LocationData(
                                latitude = it.latitude,
                                longitude = it.longitude,
                                accuracy = it.accuracy,
                                timestamp = it.timestamp
                            )
                        )
                    }
                }
                "ERROR" -> {
                    val msg = moshi.adapter(ErrorDto::class.java).fromJson(text)
                    msg?.let {
                        connectionStateChannel.trySend(ConnectionState.Error(it.message))
                    }
                }
            }
        } catch (e: Exception) {
            // 忽略解析错误
        }
    }

    private fun attemptReconnect(sessionId: String, role: UserRole) {
        if (!isActive) return
        val delay = min(30000L, (1L shl reconnectAttempt) * 1000L)
        reconnectAttempt++

        Thread {
            try {
                Thread.sleep(delay)
                kotlinx.coroutines.runBlocking {
                    connect(sessionId, role)
                }
            } catch (_: InterruptedException) {}
        }.start()
    }
}
