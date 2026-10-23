package com.findmychild.app.ui.compass

import com.findmychild.app.domain.model.CompassState
import com.findmychild.app.domain.model.ConnectionState
import com.findmychild.app.domain.model.PairingInfo
import com.findmychild.app.domain.model.UserRole
import com.findmychild.app.domain.repository.LocationRepository
import com.findmychild.app.domain.repository.SettingsRepository
import com.findmychild.app.domain.repository.SyncRepository
import com.findmychild.app.domain.usecase.CalculateBearingUseCase
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
 * [CompassViewModel] 测试。
 *
 * 覆盖 ensureConnected()：已配对后重开 App，StartupViewModel 会直接路由到指南针页，
 * 若不主动 connect 就永远收不到对端位置。孩子端 ChildActiveViewModel 有对应逻辑。
 */
class CompassViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val locationRepository: LocationRepository = mockk(relaxed = true)
    private val syncRepository: SyncRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)

    private fun viewModel() = CompassViewModel(
        locationRepository = locationRepository,
        syncRepository = syncRepository,
        settingsRepository = settingsRepository,
        calculateBearing = CalculateBearingUseCase()
    )

    @Before
    fun setUp() {
        // 三路数据流都置空，单独考察连接逻辑
        every { locationRepository.observeMyLocation() } returns emptyFlow()
        every { locationRepository.observeDeviceHeading() } returns emptyFlow()
        every { syncRepository.observePeerLocation() } returns emptyFlow()
        every { syncRepository.observeConnectionState() } returns emptyFlow()
    }

    @Test
    fun `ensureConnected connects with the stored session and role`() = runTest(mainDispatcherRule.dispatcher) {
        val info = PairingInfo(sessionId = "s-9", deviceName = "孩子设备", pairedAt = 7L)
        every { settingsRepository.getPairingInfo() } returns flowOf(info)
        every { settingsRepository.getUserRole() } returns flowOf(UserRole.PARENT)

        viewModel()
        advanceUntilIdle()

        coVerify { syncRepository.connect("s-9", UserRole.PARENT) }
    }

    @Test
    fun `ensureConnected does nothing without pairing info`() = runTest(mainDispatcherRule.dispatcher) {
        every { settingsRepository.getPairingInfo() } returns flowOf(null)

        viewModel()
        advanceUntilIdle()

        coVerify(exactly = 0) { syncRepository.connect(any(), any()) }
    }

    @Test
    fun `initial compass state is empty`() = runTest(mainDispatcherRule.dispatcher) {
        // 未 stub 的话 relaxed mock 的 Flow 不发射，ensureConnected 里的 .first() 会抛
        every { settingsRepository.getPairingInfo() } returns flowOf(null)

        val vm = viewModel()
        assertTrue(vm.compassState.value == CompassState())
        assertTrue(vm.connectionState.value == ConnectionState.Disconnected)
    }
}
