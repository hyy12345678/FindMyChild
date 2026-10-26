package com.findmychild.app.ui.pairing.child

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.findmychild.app.domain.model.PairingStatus
import com.findmychild.app.domain.model.UserRole
import com.findmychild.app.domain.repository.PairingRepository
import com.findmychild.app.domain.repository.SettingsRepository
import com.findmychild.app.domain.repository.SyncRepository
import com.google.zxing.BinaryBitmap
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

    /** 服务端下发的配对状态；PAIRED 才算真的连上对方 */
    private val _pairingStatus = MutableStateFlow<PairingStatus?>(null)
    val pairingStatus: StateFlow<PairingStatus?> = _pairingStatus.asStateFlow()

    private var hasScanned = false
    private val qrReader = QRCodeReader()

    /** 配对状态订阅。断开后允许重新扫，那时要换一个新订阅，不能叠在上面 */
    private var pairingStatusJob: Job? = null

    fun startScanning() {
        _uiState.value = _uiState.value.copy(isScanning = true)
    }

    /**
     * 用户取消配对时调用：断开 WebSocket 连接并清除本地状态。
     */
    fun cancelPairing() {
        pairingStatusJob?.cancel()
        pairingStatusJob = null
        viewModelScope.launch {
            syncRepository.disconnect()
            settingsRepository.clearAll()
        }
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

                // 建立 WebSocket 连接，等服务端确认双方都在线
                syncRepository.connect(sessionId, UserRole.CHILD)

                pairingStatusJob?.cancel()
                pairingStatusJob = launch {
                    syncRepository.observePairingStatus().collect { status ->
                        _pairingStatus.value = status
                        when (status) {
                            PairingStatus.PAIRED ->
                                _uiState.value = _uiState.value.copy(isConnecting = false)

                            PairingStatus.DISCONNECTED -> {
                                // 家长掉线了，允许重新扫
                                _uiState.value = _uiState.value.copy(
                                    isConnecting = false,
                                    errorMessage = "家长设备已断开，请重新扫描"
                                )
                                hasScanned = false
                            }

                            // 自己注册上了、对方还没来，继续转圈
                            PairingStatus.WAITING -> Unit
                        }
                    }
                }
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
    val errorMessage: String? = null
)
