package com.findmychild.app.ui.pairing.parent

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.findmychild.app.domain.model.ConnectionState
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

            // 监听 WebSocket 配对状态消息
            // 配对成功后自动跳转由 Screen 层处理
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            syncRepository.disconnect()
            settingsRepository.clearAll()
        }
    }

    fun onPaired(onSuccess: () -> Unit) {
        viewModelScope.launch {
            // 监听连接变为 Connected 后通知配对成功
            syncRepository.observeConnectionState().collect { state ->
                if (state == ConnectionState.Connected) {
                    onSuccess()
                }
            }
        }
    }
}

data class QrGenerationUiState(
    val sessionId: String = "",
    val isLoading: Boolean = true,
    val qrBitmap: Bitmap? = null
)
