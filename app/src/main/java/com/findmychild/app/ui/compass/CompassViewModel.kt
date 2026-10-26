package com.findmychild.app.ui.compass

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.findmychild.app.domain.model.CompassState
import com.findmychild.app.domain.model.ConnectionState
import com.findmychild.app.domain.model.LocationData
import com.findmychild.app.domain.model.PairingStatus
import com.findmychild.app.domain.repository.LocationRepository
import com.findmychild.app.domain.repository.SettingsRepository
import com.findmychild.app.domain.repository.SyncRepository
import com.findmychild.app.domain.usecase.CalculateBearingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

    /** null = 还没收到服务端的配对状态 */
    private val _pairingStatus = MutableStateFlow<PairingStatus?>(null)
    val pairingStatus: StateFlow<PairingStatus?> = _pairingStatus.asStateFlow()

    private var observeJob: Job? = null
    private var myLastLocation: LocationData? = null
    private var peerLastLocation: LocationData? = null

    init {
        ensureConnected()
        observePairingStatus()
    }

    /**
     * 观察配对状态。
     *
     * 连接状态只说明家长自己的 socket 通着。孩子把应用切到后台、关机、点"停止共享"时，
     * 家长这一侧毫发无损，服务端发来的 DISCONNECTED 是唯一能看出"对方走了"的信号。
     * 早先只看连接状态，孩子离开后指南针页一直显示"已连接"，家长无从判断。
     *
     * 不放在 [startObserving] 里：它由定位权限就绪触发，而配对状态与定位权限无关。
     */
    private fun observePairingStatus() {
        viewModelScope.launch {
            syncRepository.observePairingStatus().collect { status ->
                _pairingStatus.value = status
            }
        }
    }

    /**
     * 开始订阅三路数据（我的位置 + 对端位置 + 设备朝向）。
     *
     * 由 UI 在定位权限就绪后调用，而不是在 init 里自动开始：
     * [com.findmychild.app.data.sensor.LocationProvider] 未授权时会直接关闭 Flow，
     * 若那时就已订阅，combine 将永远少一路，用户后续授权也不会自动恢复。
     * 孩子端没有这个问题——它的定位服务是授权后才启动的。
     */
    fun startObserving() {
        if (observeJob?.isActive == true) return
        observeJob = viewModelScope.launch {
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
                    _compassState.value = CompassState(
                        bearing = bearing,
                        heading = heading,
                        distance = distance,
                        lastUpdateTime = peerLoc.timestamp,
                        isTargetFound = true,
                        locationAccuracy = peerLoc.accuracy
                    )
                }
            }
        }
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

    /** 断开配对 */
    fun disconnect() {
        viewModelScope.launch {
            syncRepository.disconnect()
            settingsRepository.clearAll()
            _compassState.value = CompassState()
            _connectionState.value = ConnectionState.Disconnected
            _pairingStatus.value = null
        }
    }
}
