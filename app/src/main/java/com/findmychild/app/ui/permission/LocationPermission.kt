package com.findmychild.app.ui.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

/**
 * 定位权限的当前状态。
 *
 * 家长端需要它才能算出"你在孩子的哪个方向"，孩子端需要它才能上报位置。
 * 前台服务 + foregroundServiceType="location" 的组合下，ACCESS_FINE_LOCATION
 * 已足够，不必另外申请 ACCESS_BACKGROUND_LOCATION。
 */
class LocationPermissionState internal constructor() {
    var granted by mutableStateOf(false)
        internal set

    /** 是否已弹过一次系统权限框，用于区分"本次拒绝"和"需要去系统设置"。 */
    var requested by mutableStateOf(false)
        internal set

    internal var requestLauncher: (() -> Unit)? = null
    internal var settingsLauncher: (() -> Unit)? = null

    /** 已经拒绝过一次：再请求系统可能不再弹窗，需要引导去系统设置。 */
    val wasDenied: Boolean get() = !granted && requested

    fun request() {
        requestLauncher?.invoke()
    }

    fun openAppSettings() {
        settingsLauncher?.invoke()
    }
}

/** 是否已持有定位权限（粗粒度或精粒度任一即可）。 */
fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/**
 * 记住定位权限状态，并在用户授权后回调 [onGranted]。
 *
 * [granted][LocationPermissionState.granted] 由系统权限框的回写和首次组合时的
 * 真实状态共同维护——只取构造时快照的话，用户授权后状态永远不更新，门会卡死。
 */
@Composable
fun rememberLocationPermissionState(onGranted: () -> Unit = {}): LocationPermissionState {
    val context = LocalContext.current
    val currentOnGranted by rememberUpdatedState(onGranted)
    val state = remember { LocationPermissionState() }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        state.requested = true
        state.granted = granted
        if (granted) currentOnGranted()
    }

    // 权限可能在进入本页前就已授予（例如用户在设置里开过）
    LaunchedEffect(Unit) {
        state.granted = context.hasLocationPermission()
    }

    return state.apply {
        requestLauncher = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
        settingsLauncher = {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    "package:${context.packageName}".toUri()
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

/**
 * 权限门：已授权才渲染 [content]，否则渲染说明 + 重试 + 系统设置入口。
 *
 * 之所以要这道门而不是让业务界面自己降级：`LocationProvider` 在未授权时
 * 只能静默关闭 Flow，界面若不做区分，用户看到的永远是"等待位置..."，无从判断原因。
 */
@Composable
fun LocationPermissionGate(
    permission: LocationPermissionState,
    rationale: String,
    content: @Composable () -> Unit
) {
    if (permission.granted) {
        content()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LocationOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "需要定位权限",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = rationale,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = permission::request,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (permission.wasDenied) "再次请求权限" else "授予定位权限")
        }

        if (permission.wasDenied) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = permission::openAppSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("前往系统设置开启")
            }
        }
    }
}
