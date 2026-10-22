package com.findmychild.app.ui.compass

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.findmychild.app.domain.model.CompassState
import com.findmychild.app.domain.model.ConnectionState
import com.findmychild.app.domain.model.LocationData
import com.findmychild.app.domain.repository.LocationRepository
import com.findmychild.app.domain.repository.SettingsRepository
import com.findmychild.app.domain.repository.SyncRepository
import com.findmychild.app.domain.usecase.CalculateBearingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompassViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val syncRepository: SyncRepository,
    private val settingsRepository: SettingsRepository,
    private val calculateBearing: CalculateBearingUseCase
) : ViewModel() {

    private val _compassState = MutableStateFlow(CompassState())
    val compassState: StateFlow<CompassState> = _compassState.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var myLastLocation: LocationData? = null
    private var peerLastLocation: LocationData? = null

    init {
        observeData()
        ensureConnected()
    }

    /**
     * 如果已配对但 WebSocket 未连接（如重开 App），主动建立连接。
     */
    private fun ensureConnected() {
        viewModelScope.launch {
            // 读取已保存的配对信息
            val pairingInfo = settingsRepository.getPairingInfo().first() ?: return@launch
            // 读取角色
            val role = settingsRepository.getUserRole().first() ?: return@launch

            // 如果当前未连接，则主动连接
            if (_connectionState.value is ConnectionState.Disconnected) {
                syncRepository.connect(pairingInfo.sessionId, role)
            }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            // 监听连接状态
            launch {
                syncRepository.observeConnectionState().collect { state ->
                    _connectionState.value = state
                }
            }

            // 合并三路数据：我的位置 + 对端位置 + 设备朝向
            launch {
                combine(
                    locationRepository.observeMyLocation(),
                    syncRepository.observePeerLocation(),
                    locationRepository.observeDeviceHeading()
                ) { myLoc, peerLoc, heading ->
                    Triple(myLoc, peerLoc, heading)
                }.collect { (myLoc, peerLoc, heading) ->
                    myLastLocation = myLoc
                    peerLastLocation = peerLoc

                    val bearing = calculateBearing.bearing(myLoc, peerLoc)
                    val distance = calculateBearing.distance(myLoc, peerLoc)
                    val arrowRotation = calculateBearing.normalizeAngle(bearing - heading)

                    _compassState.value = CompassState(
                        bearing = bearing,
                        heading = heading,
                        arrowRotation = arrowRotation,
                        distance = distance,
                        lastUpdateTime = peerLoc.timestamp,
                        isTargetFound = true,
                        locationAccuracy = peerLoc.accuracy
                    )
                }
            }
        }
    }

    /** 断开配对 */
    fun disconnect() {
        viewModelScope.launch {
            syncRepository.disconnect()
            settingsRepository.clearAll()
            _compassState.value = CompassState()
            _connectionState.value = ConnectionState.Disconnected
        }
    }
}
