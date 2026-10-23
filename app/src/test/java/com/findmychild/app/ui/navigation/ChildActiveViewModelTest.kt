package com.findmychild.app.ui.navigation

import com.findmychild.app.domain.model.ConnectionState
import com.findmychild.app.domain.model.PairingInfo
import com.findmychild.app.domain.model.UserRole
import com.findmychild.app.domain.repository.SettingsRepository
import com.findmychild.app.domain.repository.SyncRepository
import com.findmychild.app.testutil.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * [ChildActiveViewModel] 测试。
 *
 * 重点是 ensureConnected()：进程被杀后 StartupViewModel 会凭本地配对信息直接
 * 路由到孩子端状态页，若不在这里恢复连接，页面显示"位置共享中"但实际什么都没连。
 */
class ChildActiveViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val syncRepository: SyncRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)

    @Before
    fun setUp() {
        every { syncRepository.observeConnectionState() } returns emptyFlow()
    }

    private fun viewModel() =
        ChildActiveViewModel(mockk(relaxed = true), syncRepository, settingsRepository)

    @Test
    fun `ensureConnected connects with the stored session and role`() = runTest(mainDispatcherRule.dispatcher) {
        val info = PairingInfo(sessionId = "s-1", deviceName = "孩子设备", pairedAt = 42L)
        every { settingsRepository.getPairingInfo() } returns flowOf(info)
        every { settingsRepository.getUserRole() } returns flowOf(UserRole.CHILD)

        viewModel().ensureConnected()
        advanceUntilIdle()

        coVerify { syncRepository.connect("s-1", UserRole.CHILD) }
    }

    @Test
    fun `ensureConnected does nothing when pairing info was cleared`() = runTest(mainDispatcherRule.dispatcher) {
        // 停止共享后 clearAll() 会清空配对信息，此时不能重连
        every { settingsRepository.getPairingInfo() } returns flowOf(null)

        viewModel().ensureConnected()
        advanceUntilIdle()

        coVerify(exactly = 0) { syncRepository.connect(any(), any()) }
    }

    @Test
    fun `ensureConnected does nothing when role is missing`() = runTest(mainDispatcherRule.dispatcher) {
        every { settingsRepository.getPairingInfo() } returns flowOf(PairingInfo("s", "d", 1L))
        every { settingsRepository.getUserRole() } returns flowOf(null)

        viewModel().ensureConnected()
        advanceUntilIdle()

        coVerify(exactly = 0) { syncRepository.connect(any(), any()) }
    }

    @Test
    fun `connection state is exposed to the screen`() = runTest(mainDispatcherRule.dispatcher) {
        every { syncRepository.observeConnectionState() } returns flowOf(ConnectionState.Connected)

        val vm = viewModel()
        advanceUntilIdle()

        assertTrue(vm.connectionState.value is ConnectionState.Connected)
    }

    @Test
    fun `connection state defaults to disconnected`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = viewModel()
        assertTrue(vm.connectionState.value is ConnectionState.Disconnected)
    }
}
