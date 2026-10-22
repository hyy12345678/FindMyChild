package com.findmychild.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.findmychild.app.domain.model.UserRole
import com.findmychild.app.ui.compass.CompassScreen
import com.findmychild.app.ui.pairing.child.QrScanScreen
import com.findmychild.app.ui.pairing.child.QrScanViewModel
import com.findmychild.app.ui.pairing.parent.QrGenerationScreen
import com.findmychild.app.ui.role.RoleSelectionScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 角色选择
        composable(NavRoute.RoleSelection.route) {
            RoleSelectionScreen(
                onRoleSelected = { role ->
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
            val scanViewModel: QrScanViewModel = hiltViewModel()

            QrScanScreen(
                onBack = {
                    scanViewModel.cancelPairing()
                    navController.navigate(NavRoute.RoleSelection.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onConnected = {
                    // 只负责跳转；定位服务由 ChildActiveScreen 在拿到定位权限后启动，
                    // 未授权时启动服务也不会有任何位置上报
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
