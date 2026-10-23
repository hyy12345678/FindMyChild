package com.findmychild.app.ui.compass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.findmychild.app.domain.model.ConnectionState
import com.findmychild.app.ui.component.CompassView
import com.findmychild.app.ui.permission.LocationPermissionGate
import com.findmychild.app.ui.permission.rememberLocationPermissionState

/**
 * 指南针主页面 — 家长找孩子的核心界面。
 *
 * 布局：
 * - 顶部：连接状态指示器
 * - 中央：全屏 CompassView
 * - 底部：断开连接按钮
 */
@Composable
fun CompassScreen(
    onDisconnected: () -> Unit,
    viewModel: CompassViewModel = hiltViewModel()
) {
    val compassState by viewModel.compassState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val permission = rememberLocationPermissionState()

    // 进入即申请：没有本机位置就算不出方位角，指南针也就没有意义。
    // 授权后才开始订阅数据——未授权时 LocationProvider 会直接关闭 Flow，
    // 提前订阅会让 combine 永远少一路，且之后授权也不会自动恢复。
    LaunchedEffect(permission.granted) {
        if (permission.granted) viewModel.startObserving() else permission.request()
    }

    LocationPermissionGate(
        permission = permission,
        rationale = "指南针需要用你的位置来计算孩子所在的方位和距离。\n" +
            "位置只用于本次寻人，不会上传到任何第三方。"
    ) {
        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // === 顶部连接状态指示器 ===
                ConnectionStatusBar(connectionState)

                // === 指南针 ===
                CompassView(
                    state = compassState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                )

                // === 底部操作 ===
                Button(
                    onClick = {
                        viewModel.disconnect()
                        onDisconnected()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text("断开连接并退出", fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ConnectionStatusBar(state: ConnectionState) {
    val (text, color) = when (state) {
        is ConnectionState.Disconnected -> "连接已断开" to MaterialTheme.colorScheme.error
        is ConnectionState.Connecting -> "正在连接..." to MaterialTheme.colorScheme.secondary
        is ConnectionState.Connected -> "已连接" to MaterialTheme.colorScheme.primary
        is ConnectionState.Error -> "连接出错" to MaterialTheme.colorScheme.error
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state is ConnectionState.Connecting) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 2.dp,
                color = color
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            // 状态圆点
            Spacer(modifier = Modifier.width(4.dp))
            androidx.compose.foundation.Canvas(modifier = Modifier.size(8.dp)) {
                drawCircle(color = color)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = text,
            color = color,
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        if (state is ConnectionState.Error) {
            Text(
                text = "重连中...",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
        }
    }
}
