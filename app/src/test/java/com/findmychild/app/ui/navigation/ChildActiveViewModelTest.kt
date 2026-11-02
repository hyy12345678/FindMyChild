package com.findmychild.app.ui.navigation

import com.findmychild.app.domain.model.ConnectionState
import com.findmychild.app.domain.model.PairingInfo
import com.findmychild.app.domain.model.PairingStatus
import com.findmychild.app.domain.model.UserRole
import com.findmychild.app.domain.repository.SettingsRepository
import com.findmychild.app.domain.repository.SyncRepository
import com.findmychild.app.testutil.MainDispatcherRule
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        every { syncRepository.observePairingStatus() } returns emptyFlow()
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

    @Test
    fun `pairing status is observed in init, without waiting for location permission`() =
        runTest(mainDispatcherRule.dispatcher) {
            val status = MutableSharedFlow<PairingStatus>(replay = 1)
            every { syncRepository.observePairingStatus() } returns status

            // 页面一构造就该订阅：配对状态与定位权限无关，
            // 放进 startSharing() 之前的那道权限门里会让孩子在授权前对"家长走了"毫无知觉
            val vm = viewModel()
            advanceUntilIdle()
            assertNull(vm.pairingStatus.value)

            status.emit(PairingStatus.PAIRED)
            advanceUntilIdle()
            assertEquals(PairingStatus.PAIRED, vm.pairingStatus.value)
        }

    @Test
    fun `parent leaving is surfaced as DISCONNECTED while the socket stays up`() =
        runTest(mainDispatcherRule.dispatcher) {
            // 这正是真机上复现的那一幕：家长离开时孩子这一侧的连接状态毫发无损，
            // 只有 pairingStatus 这条能看出对方走了。两者必须互不干扰。
            every { syncRepository.observeConnectionState() } returns
                flowOf(ConnectionState.Connected)
            val status = MutableSharedFlow<PairingStatus>(replay = 1)
            every { syncRepository.observePairingStatus() } returns status

            val vm = viewModel()
            status.emit(PairingStatus.PAIRED)
            advanceUntilIdle()

            status.emit(PairingStatus.DISCONNECTED)
            advanceUntilIdle()

            assertEquals(PairingStatus.DISCONNECTED, vm.pairingStatus.value)
            // 连接状态不受影响——socket 确实还连着，页面不能因此显示"连接已断开"
            assertTrue(vm.connectionState.value is ConnectionState.Connected)
        }

    @Test
    fun `pairing status stays null when the server never says anything`() =
        runTest(mainDispatcherRule.dispatcher) {
            // 还没收到服务端消息时不能预设一个状态，否则页面会凭猜测显示"对方已断开"
            val vm = viewModel()
            advanceUntilIdle()
            assertNull(vm.pairingStatus.value)
        }
}
