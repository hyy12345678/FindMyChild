package com.findmychild.app.ui.navigation

sealed class NavRoute(val route: String) {
    data object RoleSelection : NavRoute("role_selection")
    data object ParentQrGeneration : NavRoute("parent_qr")
    data object ChildQrScan : NavRoute("child_qr_scan")
    data object Compass : NavRoute("compass")
    data object ChildActive : NavRoute("child_active")  // 孩子已连接状态页
}
