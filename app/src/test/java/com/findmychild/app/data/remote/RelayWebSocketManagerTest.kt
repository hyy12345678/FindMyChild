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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
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
