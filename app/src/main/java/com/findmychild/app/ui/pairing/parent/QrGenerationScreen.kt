package com.findmychild.app.ui.pairing.parent

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.findmychild.app.domain.model.ConnectionState
import com.findmychild.app.domain.model.PairingStatus

@Composable
fun QrGenerationScreen(
    onBack: () -> Unit,
    onPaired: () -> Unit,
    viewModel: QrGenerationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val pairingStatus by viewModel.pairingStatus.collectAsState()

    // 只有双方都连上（PAIRED）才跳转。
    // 连接状态只说明家长自己的 socket 打开了，孩子还没扫码时服务端回的是 WAITING；
    // 拿它跳转会让二维码一闪而过，孩子根本来不及扫。
    LaunchedEffect(pairingStatus) {
        if (pairingStatus == PairingStatus.PAIRED) {
            onPaired()
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Text(
                    text = "配对设备",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // QR 码卡片
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else if (uiState.qrBitmap != null) {
                    Image(
                        bitmap = uiState.qrBitmap!!.asImageBitmap(),
                        contentDescription = "配对二维码",
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    CircularProgressIndicator()
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 状态文字：配对状态优先，它才是"对方来了吗"的真话；
            // 还没收到配对状态时才退回连接状态（只是自己的 socket 通没通）
            val statusText = when (pairingStatus) {
                PairingStatus.PAIRED -> "已配对"
                PairingStatus.WAITING -> "已连接，等待小朋友扫码..."
                PairingStatus.DISCONNECTED -> "对方已断开"
                null -> when (connectionState) {
                    is ConnectionState.Disconnected -> "未连接"
                    is ConnectionState.Connecting -> "正在建立连接..."
                    is ConnectionState.Connected -> "正在加入会话..."
                    is ConnectionState.Error -> "连接出错"
                }
            }
            val statusColor = when (pairingStatus) {
                PairingStatus.PAIRED -> MaterialTheme.colorScheme.primary
                PairingStatus.WAITING -> MaterialTheme.colorScheme.secondary
                PairingStatus.DISCONNECTED, null -> when (connectionState) {
                    is ConnectionState.Error -> MaterialTheme.colorScheme.error
                    is ConnectionState.Connected -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (connectionState is ConnectionState.Connecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "请让小朋友用他的手机扫码",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "扫码后会自动建立连接\n并跳转到指南针页面",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.disconnect(); onBack() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("取消配对")
            }
        }
    }
}
