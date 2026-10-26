package com.findmychild.app.data.remote

import com.findmychild.app.data.remote.dto.ErrorDto
import com.findmychild.app.data.remote.dto.PairingStatusDto
import com.findmychild.app.data.remote.dto.PeerLocationDto
import com.findmychild.app.data.remote.dto.RegisterMessage
import com.findmychild.app.data.remote.dto.LocationUpdateDto
import com.findmychild.app.domain.model.ConnectionState
import com.findmychild.app.domain.model.LocationData
import com.findmychild.app.domain.model.PairingStatus
import com.findmychild.app.domain.model.UserRole
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 * 使用 OkHttp 原生 WebSocket，通过 Flow 暴露连接状态、对端位置与配对状态。
 * 重连采用协程实现（delay + SupervisorJob），可取消、线程安全。
 */
@Singleton
class RelayWebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val moshi: Moshi,
    private val relayConfig: RelayConfig
) {
    private val mutex = Mutex()
    private var webSocket: WebSocket? = null
    private var reconnectAttempt = 0
    private var isActive = false

    /** socket 是否真的打开过。连接失败后 webSocket 仍非 null，不能拿它判断“已有活跃连接”。 */
    private var isOpen = false
    private var currentSessionId: String? = null
    private var currentRole: UserRole? = null

    private val reconnectScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var reconnectJob: Job? = null

    // 这三项都是"当前状态"，不是一次性事件。
    // 早期用 Channel(CONFLATED).receiveAsFlow()：元素被某个订阅者取走就消失，
    // 二维码页收掉 Connected 之后，指南针页再订阅只会停在初始值 Disconnected
    // ——界面显示"连接已断开"而 socket 其实活着。换成带当前值/重放的 Flow 后，
    // 每个订阅者一上来就能拿到最新状态。
    private val _connectionState =
        MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // 位置和配对状态没有"空"的初始值，用 replay=1 的 SharedFlow：
    // 迟到的订阅者能补上最近一条，但不会因为一个占位 null 触发业务逻辑。
    private val _peerLocation = MutableSharedFlow<LocationData>(replay = 1)
    val peerLocation: SharedFlow<LocationData> = _peerLocation.asSharedFlow()

    private val _pairingStatus = MutableSharedFlow<PairingStatus>(replay = 1)
    val pairingStatus: SharedFlow<PairingStatus> = _pairingStatus.asSharedFlow()

    /** 建立 WebSocket 连接 */
    suspend fun connect(sessionId: String, role: UserRole) = mutex.withLock {
        // 同一会话同一角色已有活跃连接时直接返回。
        // 否则会出现重复建连：旧 socket 不会被关闭（字段被覆盖），服务端会认为
        // 该角色已被占用，第二次 REGISTER 直接收到"该会话已有孩子设备"。
        // 典型触发路径：扫码页连过一次，进入 ChildActiveScreen 时 ensureConnected() 再连一次。
        if (isActive && isOpen && currentSessionId == sessionId && currentRole == role && webSocket != null) {
            return@withLock
        }

        isActive = true
        currentSessionId = sessionId
        currentRole = role
        reconnectAttempt = 0

        // 取消正在进行的重连
        reconnectJob?.cancel()
        reconnectJob = null

        // 上一次尝试留下的配对状态不该被这次复用：孩子端断开后重新扫码时，
        // replay 里的 DISCONNECTED 会让新的一次尝试一上来就报"家长设备已断开"。
        // 连接状态同理，只是它本来就会被下面的 Connecting 覆盖。
        _pairingStatus.resetReplayCache()

        // 从 DataStore 读取最新服务器地址
        val url = relayConfig.wsUrlFlow.first()
        relayConfig.updateCachedUrl(url)

        _connectionState.value = ConnectionState.Connecting

        val request = Request.Builder()
            .url(relayConfig.wsUrl)
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isOpen = true
                _connectionState.value = ConnectionState.Connected
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
                isOpen = false
                _connectionState.value = ConnectionState.Disconnected
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isOpen = false
                _connectionState.value = ConnectionState.Error(t.message ?: "连接失败")
                scheduleReconnect()
            }
        }

        this.webSocket = okHttpClient.newWebSocket(request, listener)
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
        reconnectJob?.cancel()
        reconnectJob = null
        currentSessionId = null
        currentRole = null

        isOpen = false
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
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
                    msg?.let { _pairingStatus.tryEmit(PairingStatus.fromWire(it.status)) }
                }
                "PEER_LOCATION" -> {
                    val msg = moshi.adapter(PeerLocationDto::class.java).fromJson(text)
                    msg?.let {
                        _peerLocation.tryEmit(
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
                        _connectionState.value = ConnectionState.Error(it.message)
                    }
                }
            }
        } catch (e: Exception) {
            // 忽略解析错误
        }
    }

    /**
     * 使用协程实现指数退避重连。
     * 替代原来的 Thread + runBlocking 方案，可取消、线程安全。
     */
    private fun scheduleReconnect() {
        if (!isActive) return
        val sid = currentSessionId ?: return
        val role = currentRole ?: return

        val delayMs = min(30000L, (1L shl reconnectAttempt) * 1000L)
        reconnectAttempt++

        reconnectJob = reconnectScope.launch {
            delay(delayMs)
            if (!isActive) return@launch
            try {
                connect(sid, role)
            } catch (_: Exception) {
                // 连接失败会在 onFailure 中触发下一次重连
            }
        }
    }
}
