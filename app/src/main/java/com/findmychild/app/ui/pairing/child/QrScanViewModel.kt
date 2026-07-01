package com.findmychild.app.ui.pairing.child

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.findmychild.app.domain.model.UserRole
import com.findmychild.app.domain.repository.PairingRepository
import com.findmychild.app.domain.repository.SettingsRepository
import com.findmychild.app.domain.repository.SyncRepository
import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import javax.inject.Inject

@HiltViewModel
class QrScanViewModel @Inject constructor(
    private val pairingRepository: PairingRepository,
    private val syncRepository: SyncRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QrScanUiState())
    val uiState: StateFlow<QrScanUiState> = _uiState.asStateFlow()

    private var hasScanned = false
    private val qrReader = QRCodeReader()

    fun startScanning() {
        _uiState.value = _uiState.value.copy(isScanning = true)
    }

    fun analyzeQrImage(imageProxy: ImageProxy) {
        if (hasScanned) {
            imageProxy.close()
            return
        }

        try {
            val planes = imageProxy.planes
            if (planes.isNotEmpty()) {
                val buffer: ByteBuffer = planes[0].buffer
                val data = ByteArray(buffer.remaining())
                buffer.get(data)

                val source = PlanarYUVLuminanceSource(
                    data,
                    imageProxy.width, imageProxy.height,
                    0, 0,
                    imageProxy.width, imageProxy.height,
                    false
                )
                val bitmap = BinaryBitmap(HybridBinarizer(source))

                val result = qrReader.decode(bitmap)
                if (result != null) {
                    hasScanned = true
                    onQrScanned(result.text)
                }
            }
        } catch (_: Exception) {
            // QR 码尚未完全进入画面，继续等待
        } finally {
            imageProxy.close()
        }
    }

    private fun onQrScanned(sessionId: String) {
        _uiState.value = _uiState.value.copy(isConnecting = true)

        viewModelScope.launch {
            try {
                // 验证并加入配对
                val result = pairingRepository.joinPairingSession(sessionId, "孩子设备")
                if (result.isFailure) {
                    _uiState.value = _uiState.value.copy(
                        isConnecting = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "配对失败"
                    )
                    hasScanned = false
                    return@launch
                }

                val pairingInfo = result.getOrThrow()
                settingsRepository.savePairingInfo(pairingInfo)

                // 建立 WebSocket 连接
                syncRepository.connect(sessionId, UserRole.CHILD)

                _uiState.value = _uiState.value.copy(isConnected = true, isConnecting = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isConnecting = false,
                    errorMessage = e.message ?: "连接失败"
                )
                hasScanned = false
            }
        }
    }

}

data class QrScanUiState(
    val isScanning: Boolean = false,
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val errorMessage: String? = null
)
