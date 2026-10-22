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
import com.findmychild.app.domain.repository.SettingsRepository
import com.findmychild.app.domain.repository.SyncRepository
import com.findmychild.app.ui.permission.LocationPermissionGate
import com.findmychild.app.ui.permission.rememberLocationPermissionState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * 孩子端配对成功后的状态页 ViewModel。
 * 处理断开连接和清理本地状态。
 */
@HiltViewModel
class ChildActiveViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val syncRepository: SyncRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

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
                text = "位置共享中",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "家长正在通过指南针查看你的位置\n你可以将应用切换到后台\n位置会持续共享",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 位置共享提示
            Text(
                text = "● 正在后台持续上报位置",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
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
