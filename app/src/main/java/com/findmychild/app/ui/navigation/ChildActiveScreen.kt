package com.findmychild.app.ui.navigation

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.findmychild.app.data.sensor.LocationService
import com.findmychild.app.domain.model.ConnectionState
import com.findmychild.app.domain.model.PairingStatus
import com.findmychild.app.domain.repository.SettingsRepository
import com.findmychild.app.domain.repository.SyncRepository
import com.findmychild.app.ui.permission.LocationPermissionGate
import com.findmychild.app.ui.permission.rememberLocationPermissionState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 孩子端配对成功后的状态页 ViewModel。
 * 负责连接生命周期、启动/停止位置共享、以及清理本地状态。
 */
@HiltViewModel
class ChildActiveViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val syncRepository: SyncRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    /** null = 还没收到服务端的配对状态 */
    private val _pairingStatus = MutableStateFlow<PairingStatus?>(null)
    val pairingStatus: StateFlow<PairingStatus?> = _pairingStatus.asStateFlow()

    init {
        observeConnectionState()
        observePairingStatus()
    }

    /**
     * 已配对但进程重启后，主动把 WebSocket 连接恢复回来。
     *
     * 家长端的 CompassViewModel 有同样的逻辑，孩子端一直没有：进程被杀后
     * StartupViewModel 会凭本地配对信息直接路由到本页，页面显示"位置共享中"，
     * 但 SyncRepository 的 sessionId 是空的、WebSocket 也没连，
     * 位置消息到了服务器会被 relayLocation() 当未知会话丢弃。
     */
    fun ensureConnected() {
        viewModelScope.launch {
            val pairingInfo = settingsRepository.getPairingInfo().first() ?: return@launch
            val role = settingsRepository.getUserRole().first() ?: return@launch
            syncRepository.connect(pairingInfo.sessionId, role)
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            syncRepository.observeConnectionState().collect { state ->
                _connectionState.value = state
            }
        }
    }

    /**
     * 观察配对状态。
     *
     * 家长端已经因为"只看连接状态"错过两次（`QrGenerationScreen` 拿 Connected 当已配对、
     * `CompassScreen` 在孩子离开后仍显示"已连接"），这里是同一个错的第三处。
     * 家长离开时孩子这一侧的 socket 毫发无损，连接状态一直是 Connected，
     * 服务端发来的 `DISCONNECTED` 是唯一能看出"对方走了"的信号。
     *
     * 不观察它的代价不只是文案难看：孩子会一直以为家长在看，
     * GPS 和前台服务也就一直往一个没有接收方的中继上报位置——既耗电，也没必要地传位置。
     */
    private fun observePairingStatus() {
        viewModelScope.launch {
            syncRepository.observePairingStatus().collect { status ->
                _pairingStatus.value = status
            }
        }
    }

    /**
     * 开始位置共享：启动前台定位服务。
     *
     * 只在拿到定位权限后调用——没权限时 [com.findmychild.app.data.sensor.LocationProvider]
     * 会直接关闭 Flow，服务即使跑起来也不会有任何位置上报。
     */
    fun startSharing() {
        ContextCompat.startForegroundService(
            appContext,
            Intent(appContext, LocationService::class.java)
        )
    }

    /**
     * 停止位置共享：断开 WebSocket、停止定位服务、清除本地配对数据。
     */
    fun stopSharing(onDone: () -> Unit) {
        viewModelScope.launch {
            // 1. 停止前台定位服务
            val intent = Intent(appContext, LocationService::class.java)
            appContext.stopService(intent)

            // 2. 断开 WebSocket 连接
            syncRepository.disconnect()

            // 3. 清除本地配对数据
            settingsRepository.clearAll()

            // 4. 通知 UI 跳转
            onDone()
        }
    }
}

/**
 * 孩子端配对成功后的状态页。
 * 告知用户正在共享位置，并可随时断开。
 */
@Composable
fun ChildActiveScreen(
    onDisconnected: () -> Unit,
    viewModel: ChildActiveViewModel = hiltViewModel()
) {
    val permission = rememberLocationPermissionState()
    val connectionState by viewModel.connectionState.collectAsState()
    val pairingStatus by viewModel.pairingStatus.collectAsState()

    // 冷启动时恢复连接（正常扫码进入时连接已由 QrScanViewModel 建好，重复 connect 无副作用）
    LaunchedEffect(Unit) {
        viewModel.ensureConnected()
    }

    // 拿到定位权限才启动上报：这是孩子端真正开始共享位置的前提
    LaunchedEffect(permission.granted) {
        if (permission.granted) viewModel.startSharing() else permission.request()
    }

    LocationPermissionGate(
        permission = permission,
        rationale = "上报位置需要定位权限，否则家长看不到你的位置。\n" +
            "你可以随时停止共享。"
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                // 对方走后还写"位置共享中"是假话：没有人接收，位置也不该再往外传
                text = if (pairingStatus == PairingStatus.DISCONNECTED) "对方已离开" else "位置共享中",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when (pairingStatus) {
                    PairingStatus.DISCONNECTED ->
                        "家长已停止共享\n你的位置此刻没有在共享\n可以停止共享退出，或等待对方回来"

                    PairingStatus.WAITING ->
                        "等待家长连接...\n连上之后家长就能看到你的位置"

                    else ->
                        "家长正在通过指南针查看你的位置\n你可以将应用切换到后台\n位置会持续共享"
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 配对状态优先：它才说明对方在不在。连接状态只代表自己这一侧的 socket——
            // 家长离开时它毫发无损，只看它会让孩子一直以为家长在看。
            val (statusText, statusColor) = when (pairingStatus) {
                PairingStatus.PAIRED ->
                    "● 正在后台持续上报位置" to MaterialTheme.colorScheme.primary
                PairingStatus.WAITING ->
                    "● 等待家长连接..." to MaterialTheme.colorScheme.secondary
                PairingStatus.DISCONNECTED ->
                    "● 对方已断开，位置未在共享" to MaterialTheme.colorScheme.error
                null -> when (connectionState) {
                    is ConnectionState.Connected ->
                        "● 正在后台持续上报位置" to MaterialTheme.colorScheme.primary
                    is ConnectionState.Connecting ->
                        "● 正在连接..." to MaterialTheme.colorScheme.secondary
                    is ConnectionState.Error ->
                        "● 连接出错，正在重连" to MaterialTheme.colorScheme.error
                    is ConnectionState.Disconnected ->
                        "● 连接已断开" to MaterialTheme.colorScheme.error
                }
            }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.stopSharing {
                        onDisconnected()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("停止共享并退出", fontSize = 16.sp)
            }
        }
    }
}
