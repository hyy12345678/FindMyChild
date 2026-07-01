package com.findmychild.app.ui.navigation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.findmychild.app.data.sensor.LocationService
import com.findmychild.app.domain.model.UserRole
import com.findmychild.app.ui.compass.CompassScreen
import com.findmychild.app.ui.pairing.child.QrScanScreen
import com.findmychild.app.ui.pairing.parent.QrGenerationScreen
import com.findmychild.app.ui.role.RoleSelectionScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String,
    onRoleSelected: (UserRole) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 角色选择
        composable(NavRoute.RoleSelection.route) {
            RoleSelectionScreen(
                onRoleSelected = { role ->
                    onRoleSelected(role)
                    when (role) {
                        UserRole.PARENT -> {
                            navController.navigate(NavRoute.ParentQrGeneration.route) {
                                popUpTo(NavRoute.RoleSelection.route) { inclusive = true }
                            }
                        }
                        UserRole.CHILD -> {
                            navController.navigate(NavRoute.ChildQrScan.route) {
                                popUpTo(NavRoute.RoleSelection.route) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        // 家长：生成二维码
        composable(NavRoute.ParentQrGeneration.route) {
            QrGenerationScreen(
                onBack = {
                    navController.navigate(NavRoute.RoleSelection.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onPaired = {
                    navController.navigate(NavRoute.Compass.route) {
                        popUpTo(NavRoute.ParentQrGeneration.route) { inclusive = true }
                    }
                }
            )
        }

        // 孩子：扫描二维码
        composable(NavRoute.ChildQrScan.route) {
            val context = LocalContext.current

            QrScanScreen(
                onBack = {
                    navController.navigate(NavRoute.RoleSelection.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onConnected = {
                    // 启动前台定位服务
                    val intent = Intent(context, LocationService::class.java)
                    ContextCompat.startForegroundService(context, intent)

                    // 跳转到已连接状态
                    navController.navigate(NavRoute.ChildActive.route) {
                        popUpTo(NavRoute.ChildQrScan.route) { inclusive = true }
                    }
                }
            )
        }

        // 家长：指南针
        composable(NavRoute.Compass.route) {
            CompassScreen(
                onDisconnected = {
                    navController.navigate(NavRoute.RoleSelection.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // 孩子：已连接状态（简单的状态页）
        composable(NavRoute.ChildActive.route) {
            ChildActiveScreen(
                onDisconnected = {
                    navController.navigate(NavRoute.RoleSelection.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
