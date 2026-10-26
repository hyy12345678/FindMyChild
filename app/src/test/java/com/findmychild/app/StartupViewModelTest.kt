package com.findmychild.app

import com.findmychild.app.domain.model.PairingInfo
import com.findmychild.app.domain.model.UserRole
import com.findmychild.app.domain.repository.SettingsRepository
import com.findmychild.app.testutil.MainDispatcherRule
import com.findmychild.app.ui.navigation.NavRoute
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * [StartupViewModel] 测试。
 *
 * 重点是 startOnce：路由只在进程启动时决定一次。配对成功会往 DataStore 写
 * sessionId，若那时重新决定路由，startDestination 就会从二维码页跳到指南针页，
 * NavHost 重建、QrGenerationViewModel 重建并换掉 sessionId。
 */
class StartupViewModelTest {

    // 用 StandardTestDispatcher 而非 UnconfinedTestDispatcher：init 里的协程要等
    // advanceUntilIdle() 才跑，这样才能区分"已决定"和"还没决定"两个状态。
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private val settingsRepository: SettingsRepository = mockk(relaxed = true)

    private val role = MutableStateFlow<UserRole?>(null)
    private val pairing = MutableStateFlow<PairingInfo?>(null)

    @Before
    fun setUp() {
        every { settingsRepository.getUserRole() } returns role
        every { settingsRepository.getPairingInfo() } returns pairing
    }

    private fun viewModel() = StartupViewModel(settingsRepository)

    private fun pairingInfo(sessionId: String) =
        PairingInfo(sessionId = sessionId, deviceName = "孩子设备", pairedAt = 1L)

    @Test
    fun `no role selected routes to role selection`() = runTest(mainDispatcherRule.dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(NavRoute.RoleSelection.route, vm.startupRoute.value)
    }

    @Test
    fun `route is null until the first read completes`() = runTest(mainDispatcherRule.dispatcher) {
        // 首帧之前 NavHost 还不能挂，否则 startDestination 永远是兜底值
        val vm = viewModel()

        assertNull(vm.startupRoute.value)
    }

    @Test
    fun `unpaired parent routes to qr generation`() = runTest(mainDispatcherRule.dispatcher) {
        role.value = UserRole.PARENT

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(NavRoute.ParentQrGeneration.route, vm.startupRoute.value)
    }

    @Test
    fun `paired parent routes straight to compass`() = runTest(mainDispatcherRule.dispatcher) {
        role.value = UserRole.PARENT
        pairing.value = pairingInfo("s-1")

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(NavRoute.Compass.route, vm.startupRoute.value)
    }

    @Test
    fun `unpaired child routes to qr scan`() = runTest(mainDispatcherRule.dispatcher) {
        role.value = UserRole.CHILD

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(NavRoute.ChildQrScan.route, vm.startupRoute.value)
    }

    @Test
    fun `paired child routes to active screen`() = runTest(mainDispatcherRule.dispatcher) {
        role.value = UserRole.CHILD
        pairing.value = pairingInfo("s-1")

        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(NavRoute.ChildActive.route, vm.startupRoute.value)
    }

    @Test
    fun `route does not change when pairing is saved mid-session`() =
        runTest(mainDispatcherRule.dispatcher) {
            role.value = UserRole.PARENT

            val vm = viewModel()
            advanceUntilIdle()
            assertEquals(NavRoute.ParentQrGeneration.route, vm.startupRoute.value)

            // 家长扫码完成：QrGenerationViewModel 写入配对信息。此时用户已经停在二维码页，
            // 路由必须保持不动，否则 NavHost 重建、sessionId 被换掉。
            pairing.value = pairingInfo("s-new")

            assertEquals(NavRoute.ParentQrGeneration.route, vm.startupRoute.value)
        }

    @Test
    fun `route does not change when pairing is cleared mid-session`() =
        runTest(mainDispatcherRule.dispatcher) {
            role.value = UserRole.PARENT
            pairing.value = pairingInfo("s-1")

            val vm = viewModel()
            advanceUntilIdle()
            assertEquals(NavRoute.Compass.route, vm.startupRoute.value)

            // 断开配对会 clearAll()，同样不能把用户弹回角色选择页
            pairing.value = null

            assertEquals(NavRoute.Compass.route, vm.startupRoute.value)
        }
}
