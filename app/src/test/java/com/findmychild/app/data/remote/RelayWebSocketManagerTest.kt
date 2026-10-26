package com.findmychild.app.data.remote

import app.cash.turbine.test
import com.findmychild.app.data.local.SettingsDataStore
import com.findmychild.app.data.remote.dto.ErrorDto
import com.findmychild.app.data.remote.dto.PairingStatusDto
import com.findmychild.app.data.remote.dto.PeerLocationDto
import com.findmychild.app.domain.model.ConnectionState
import com.findmychild.app.domain.model.PairingStatus
import com.findmychild.app.domain.model.UserRole
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener as OkHttpListener
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [RelayWebSocketManager] 集成测试。
 *
 * 使用 MockK mock OkHttpClient，验证：
 * - disconnect 后 connectionState 变为 Disconnected
 * - disconnect 可以安全地多次调用
 * - 连接状态 Flow 正确传播
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RelayWebSocketManagerTest {

    private lateinit var manager: RelayWebSocketManager
    private val mockClient: OkHttpClient = mockk(relaxed = true)
    private val mockWebSocket: WebSocket = mockk(relaxed = true)
    private val moshi = Moshi.Builder().build()
    private val capturedListeners = mutableListOf<OkHttpListener>()

    @Before
    fun setUp() {
        // RelayConfig 在构造函数里就取 settingsDataStore.serverUrl 赋值给 wsUrlFlow，
        // relaxed mock 给出的 Flow 不发射任何元素，会让 connect() 里的 .first() 抛
        // NoSuchElementException。因此必须先 stub 好这个 Flow 再构造 RelayConfig。
        val settingsDataStore = mockk<SettingsDataStore>(relaxed = true)
        every { settingsDataStore.serverUrl } returns flowOf(SettingsDataStore.DEFAULT_SERVER_URL)

        val relayConfig = RelayConfig(settingsDataStore)
        every { mockClient.newWebSocket(any(), any()) } returns mockWebSocket
        manager = RelayWebSocketManager(mockClient, moshi, relayConfig)
    }

    @Test
    fun `disconnect sets connection state to Disconnected`() = runTest {
        manager.disconnect()
        val state = manager.connectionState.first()
        assertTrue("Expected Disconnected but got $state", state is ConnectionState.Disconnected)
    }

    @Test
    fun `multiple disconnects are safe`() = runTest {
        manager.disconnect()
        manager.disconnect()
        manager.disconnect()
        val state = manager.connectionState.first()
        assertTrue(state is ConnectionState.Disconnected)
    }

    @Test
    fun `disconnect after connect attempt sets Disconnected`() = runTest {
        // 模拟 connect — 由于 mockClient.newWebSocket 返回 mockWebSocket，
        // connect 不会真正建立网络连接，但会设置 isActive = true 并发送 Connecting 状态
        manager.connect("test-session", UserRole.PARENT)

        // 断开
        manager.disconnect()
        val state = manager.connectionState.first()
        assertTrue(state is ConnectionState.Disconnected)
    }

    @Test
    fun `connect twice with the same session and role opens only one socket`() = runTest {
        // 扫码页连过一次后，ChildActiveScreen 的 ensureConnected() 会再调一次。
        // 若这里不幂等，旧 socket 会被覆盖而不关闭，服务端第二次 REGISTER 会报
        // "该会话已有孩子设备"，正常配对流程直接被破坏。
        captureListeners()
        manager.connect("s-1", UserRole.CHILD)
        lastListener().onOpen(mockWebSocket, mockk(relaxed = true))

        manager.connect("s-1", UserRole.CHILD)

        verify(exactly = 1) { mockClient.newWebSocket(any(), any()) }
    }

    @Test
    fun `connect again after a failure opens a new socket`() = runTest {
        // 回归：守卫曾用 webSocket != null 判断"已有连接"，但连接失败后该字段仍非 null，
        // 于是重连被自己挡住，永远不再尝试。失败后必须允许重新建连。
        captureListeners()
        manager.connect("s-1", UserRole.CHILD)
        lastListener().onFailure(mockWebSocket, RuntimeException("cleartext blocked"), null)

        manager.connect("s-1", UserRole.CHILD)

        verify(exactly = 2) { mockClient.newWebSocket(any(), any()) }
    }

    @Test
    fun `connect before the socket opens is not treated as an existing connection`() = runTest {
        // onOpen 尚未回调时不算"已连接"，允许重试
        captureListeners()
        manager.connect("s-1", UserRole.CHILD)

        manager.connect("s-1", UserRole.CHILD)

        verify(exactly = 2) { mockClient.newWebSocket(any(), any()) }
    }

    @Test
    fun `connect with a different session opens a new socket`() = runTest {
        captureListeners()
        manager.connect("s-1", UserRole.CHILD)
        lastListener().onOpen(mockWebSocket, mockk(relaxed = true))

        manager.connect("s-2", UserRole.CHILD)

        verify(exactly = 2) { mockClient.newWebSocket(any(), any()) }
    }

    @Test
    fun `connect with a different role opens a new socket`() = runTest {
        captureListeners()
        manager.connect("s-1", UserRole.CHILD)
        lastListener().onOpen(mockWebSocket, mockk(relaxed = true))

        manager.connect("s-1", UserRole.PARENT)

        verify(exactly = 2) { mockClient.newWebSocket(any(), any()) }
    }

    @Test
    fun `connect after disconnect opens a new socket`() = runTest {
        captureListeners()
        manager.connect("s-1", UserRole.CHILD)
        lastListener().onOpen(mockWebSocket, mockk(relaxed = true))

        manager.disconnect()
        manager.connect("s-1", UserRole.CHILD)

        verify(exactly = 2) { mockClient.newWebSocket(any(), any()) }
    }

    /**
     * 捕获 newWebSocket 收到的 listener。mock 的 OkHttpClient 不会自己回调，
     * 不手动驱动 onOpen / onFailure 就无法表示连接的真实状态。
     */
    private fun captureListeners() {
        every { mockClient.newWebSocket(any(), any()) } answers {
            capturedListeners += args[1] as OkHttpListener
            mockWebSocket
        }
    }

    private fun lastListener(): OkHttpListener = capturedListeners.last()

    @Test
    fun `connection state flow emits updates`() = runTest {
        manager.connectionState.test {
            // StateFlow 一开始就持有当前值
            val initial = awaitItem()
            assertTrue(initial is ConnectionState.Disconnected)

            manager.disconnect()
            // 断开后仍是 Disconnected；StateFlow 去重，不会重复发同值
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * 回归：这三项原来用 Channel(CONFLATED).receiveAsFlow()，元素被取走就消失。
     * 二维码页收掉 Connected 后，指南针页再订阅只会停在初始值 Disconnected——
     * 界面显示"连接已断开"而 socket 其实活着。后订阅者必须能拿到当前值。
     */
    @Test
    fun `a late subscriber still sees the current connection state`() = runTest {
        captureListeners()
        manager.connect("s-1", UserRole.PARENT)
        lastListener().onOpen(mockWebSocket, mockk(relaxed = true))

        // 没有任何先前的订阅者
        val state = manager.connectionState.first()
        assertTrue("Expected Connected but got $state", state is ConnectionState.Connected)
    }

    /**
     * 同上，但配对状态没有"空"的初始值，用 replay=1 的 SharedFlow 实现。
     * 孩子扫码后家长页才订阅，也必须收到这条 PAIRED。
     */
    @Test
    fun `a late subscriber still sees the latest pairing status`() = runTest {
        captureListeners()
        manager.connect("s-1", UserRole.PARENT)
        lastListener().onOpen(mockWebSocket, mockk(relaxed = true))
        lastListener().onMessage(
            mockWebSocket,
            """{"type":"PAIRING_STATUS","status":"PAIRED","peerDeviceName":"孩子设备"}"""
        )

        val status = manager.pairingStatus.first()
        assertEquals(PairingStatus.PAIRED, status)
    }

    @Test
    fun `unknown pairing status is treated as WAITING`() = runTest {
        captureListeners()
        manager.connect("s-1", UserRole.PARENT)
        lastListener().onOpen(mockWebSocket, mockk(relaxed = true))
        lastListener().onMessage(
            mockWebSocket,
            """{"type":"PAIRING_STATUS","status":"SOMETHING_NEW","peerDeviceName":""}"""
        )

        assertEquals(PairingStatus.WAITING, manager.pairingStatus.first())
    }

    /**
     * 孩子端断开后允许重新扫。若不清空 replay，上一次的 DISCONNECTED 会让
     * 新的一次尝试一上来就报"家长设备已断开"。
     */
    @Test
    fun `a new connection attempt does not inherit the previous pairing status`() = runTest {
        captureListeners()
        manager.connect("s-1", UserRole.CHILD)
        lastListener().onOpen(mockWebSocket, mockk(relaxed = true))
        lastListener().onMessage(
            mockWebSocket,
            """{"type":"PAIRING_STATUS","status":"DISCONNECTED","peerDeviceName":""}"""
        )
        assertEquals(PairingStatus.DISCONNECTED, manager.pairingStatus.first())

        manager.connect("s-2", UserRole.CHILD)

        // replay 已清空：此刻订阅什么都拿不到
        val replayed = withTimeoutOrNull(50) { manager.pairingStatus.first() }
        assertNull("新连接不应立刻收到上一次的 DISCONNECTED", replayed)

        // 服务端回了才有
        lastListener().onOpen(mockWebSocket, mockk(relaxed = true))
        lastListener().onMessage(
            mockWebSocket,
            """{"type":"PAIRING_STATUS","status":"WAITING","peerDeviceName":""}"""
        )
        assertEquals(PairingStatus.WAITING, manager.pairingStatus.first())
    }

    @Test
    fun `peer location survives a late subscriber`() = runTest {
        captureListeners()
        manager.connect("s-1", UserRole.CHILD)
        lastListener().onOpen(mockWebSocket, mockk(relaxed = true))
        lastListener().onMessage(
            mockWebSocket,
            """{"type":"PEER_LOCATION","latitude":31.2,"longitude":121.4,"accuracy":5.0,"timestamp":1}"""
        )

        val location = manager.peerLocation.first()
        assertEquals(31.2, location.latitude, 1e-9)
        assertEquals(121.4, location.longitude, 1e-9)
    }
}
