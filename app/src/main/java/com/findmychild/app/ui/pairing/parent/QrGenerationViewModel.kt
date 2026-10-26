package com.findmychild.app.ui.pairing.parent

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.findmychild.app.domain.model.ConnectionState
import com.findmychild.app.domain.model.PairingStatus
import com.findmychild.app.domain.model.UserRole
import com.findmychild.app.domain.repository.PairingRepository
import com.findmychild.app.domain.repository.SettingsRepository
import com.findmychild.app.domain.repository.SyncRepository
import com.findmychild.app.ui.pairing.QrCodeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QrGenerationViewModel @Inject constructor(
    private val pairingRepository: PairingRepository,
    private val syncRepository: SyncRepository,
    private val settingsRepository: SettingsRepository,
    private val qrCodeUtil: QrCodeUtil
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrGenerationUiState())
    val uiState: StateFlow<QrGenerationUiState> = _uiState.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /** null = 还没收到服务端的配对状态 */
    private val _pairingStatus = MutableStateFlow<PairingStatus?>(null)
    val pairingStatus: StateFlow<PairingStatus?> = _pairingStatus.asStateFlow()

    init {
        generateSession()
    }

    private fun generateSession() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val pairingInfo = pairingRepository.createPairingSession("家长设备")
            settingsRepository.savePairingInfo(pairingInfo)

            val bitmap = qrCodeUtil.generateQrCode(pairingInfo.sessionId)
            _uiState.value = _uiState.value.copy(
                sessionId = pairingInfo.sessionId,
                qrBitmap = bitmap,
                isLoading = false
            )

            // 建立 WebSocket 连接，等待子端加入
            syncRepository.connect(pairingInfo.sessionId, UserRole.PARENT)

            // 监听连接状态和配对状态
            launch {
                syncRepository.observeConnectionState().collect { state ->
                    _connectionState.value = state
                }
            }

            // 配对成功与否只能看 PAIRING_STATUS。连接状态只代表自己的 socket 通了，
            // 孩子还没扫码时服务端回的是 WAITING——早期版本拿"已连接"当"已配对"，
            // 家长二维码一闪而过就被带去指南针页，孩子根本扫不到。
            launch {
                syncRepository.observePairingStatus().collect { status ->
                    _pairingStatus.value = status
                }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            syncRepository.disconnect()
            settingsRepository.clearAll()
        }
    }
}

data class QrGenerationUiState(
    val sessionId: String = "",
    val isLoading: Boolean = true,
    val qrBitmap: Bitmap? = null
)
