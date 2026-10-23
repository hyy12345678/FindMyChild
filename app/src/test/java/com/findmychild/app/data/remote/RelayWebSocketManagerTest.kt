package com.findmychild.app.data.remote

import app.cash.turbine.test
import com.findmychild.app.data.local.SettingsDataStore
import com.findmychild.app.data.remote.dto.ErrorDto
import com.findmychild.app.data.remote.dto.PairingStatusDto
import com.findmychild.app.data.remote.dto.PeerLocationDto
import com.findmychild.app.domain.model.ConnectionState
import com.findmychild.app.domain.model.UserRole
import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener as OkHttpListener
import org.junit.Assert.assertEquals
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
            // 初始状态（CONFLATED channel，可能已有缓存值）
            // 断开
            manager.disconnect()
            // 等待状态传播
            val state = awaitItem()
            assertTrue(state is ConnectionState.Disconnected)
        }
    }
}
