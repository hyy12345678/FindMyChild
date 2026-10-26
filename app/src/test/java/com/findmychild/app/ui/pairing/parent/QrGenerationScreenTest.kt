package com.findmychild.app.ui.pairing.parent

import androidx.compose.ui.test.junit4.createComposeRule
import com.findmychild.app.domain.model.PairingInfo
import com.findmychild.app.domain.model.PairingStatus
import com.findmychild.app.domain.repository.PairingRepository
import com.findmychild.app.domain.repository.SettingsRepository
import com.findmychild.app.domain.repository.SyncRepository
import com.findmychild.app.ui.pairing.QrCodeUtil
import com.findmychild.app.ui.theme.FindMyChildTheme
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [QrGenerationScreen] 的跳转时机测试。
 *
 * 这是"二维码一闪而过"那个 bug 的回归护栏：家长自己的 socket 打开时服务端回的
 * 还是 WAITING（孩子还没扫码），早期版本拿 ConnectionState.Connected 当配对成功，
 * 配一个 500ms delay 就跳走，孩子根本扫不到。跳转只能由 PAIRED 触发。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QrGenerationScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /** replay = 1：ViewModel 还没订阅时 emit 的值不会丢 */
    private val pairingStatusFlow = MutableSharedFlow<PairingStatus>(replay = 1)

    private val pairingRepository: PairingRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val syncRepository: SyncRepository = mockk(relaxed = true)

    private fun newViewModel(): QrGenerationViewModel {
        coEvery {
            pairingRepository.createPairingSession(any())
        } returns PairingInfo(
            sessionId = "session-1",
            deviceName = "家长设备",
            pairedAt = 1L
        )
        every { syncRepository.observeConnectionState() } returns emptyFlow()
        every { syncRepository.observePairingStatus() } returns pairingStatusFlow
        return QrGenerationViewModel(
            pairingRepository = pairingRepository,
            syncRepository = syncRepository,
            settingsRepository = settingsRepository,
            qrCodeUtil = QrCodeUtil()
        )
    }

    private fun setContent(viewModel: QrGenerationViewModel, onPaired: () -> Unit) {
        composeTestRule.setContent {
            FindMyChildTheme {
                QrGenerationScreen(
                    onBack = {},
                    onPaired = onPaired,
                    viewModel = viewModel
                )
            }
        }
    }

    @Test
    fun `waiting for the child does not leave the qr page`() {
        var paired = false
        setContent(newViewModel()) { paired = true }

        runBlocking { pairingStatusFlow.emit(PairingStatus.WAITING) }
        composeTestRule.waitForIdle()

        assertFalse("孩子还没扫码，不应该跳转", paired)
    }

    @Test
    fun `paired navigates to the compass`() {
        var paired = false
        setContent(newViewModel()) { paired = true }

        runBlocking { pairingStatusFlow.emit(PairingStatus.PAIRED) }
        composeTestRule.waitForIdle()

        assertTrue(paired)
    }

    @Test
    fun `waiting first then paired navigates`() {
        // 真实时序：家长先连上（WAITING），孩子扫码后才 PAIRED
        var paired = false
        setContent(newViewModel()) { paired = true }

        runBlocking {
            pairingStatusFlow.emit(PairingStatus.WAITING)
            pairingStatusFlow.emit(PairingStatus.PAIRED)
        }
        composeTestRule.waitForIdle()

        assertTrue(paired)
    }

    @Test
    fun `disconnected peer does not navigate`() {
        var paired = false
        setContent(newViewModel()) { paired = true }

        runBlocking { pairingStatusFlow.emit(PairingStatus.DISCONNECTED) }
        composeTestRule.waitForIdle()

        assertFalse(paired)
    }
}
