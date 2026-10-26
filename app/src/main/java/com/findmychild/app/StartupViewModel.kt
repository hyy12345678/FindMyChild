package com.findmychild.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.findmychild.app.domain.model.PairingInfo
import com.findmychild.app.domain.model.UserRole
import com.findmychild.app.domain.repository.SettingsRepository
import com.findmychild.app.ui.navigation.NavRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _startupRoute = MutableStateFlow<String?>(null)
    val startupRoute: StateFlow<String?> = _startupRoute.asStateFlow()

    init {
        viewModelScope.launch {
            // 只读一次：startDestination 是"进程启动时该落在哪页"的一次性决定。
            // 持续观察会让配对成功时写入的 sessionId 改变它，NavHost 重建、
            // QrGenerationViewModel 重新创建并生成新 sessionId——孩子扫到的码随即失效。
            _startupRoute.value = combine(
                settingsRepository.getUserRole(),
                settingsRepository.getPairingInfo()
            ) { role, pairingInfo ->
                determineStartDestination(role, pairingInfo)
            }.first()
        }
    }

    private fun determineStartDestination(
        role: UserRole?,
        pairingInfo: PairingInfo?
    ): String {
        // 没有选择角色 → 角色选择页
        if (role == null) {
            return NavRoute.RoleSelection.route
        }

        return when (role) {
            UserRole.PARENT -> {
                if (pairingInfo != null) {
                    // 已配对 → 直接进入指南针（后续自动重连）
                    NavRoute.Compass.route
                } else {
                    NavRoute.ParentQrGeneration.route
                }
            }
            UserRole.CHILD -> {
                if (pairingInfo != null) {
                    // 已配对 → 显示已连接状态（后续自动重连并启动服务）
                    NavRoute.ChildActive.route
                } else {
                    NavRoute.ChildQrScan.route
                }
            }
        }
    }
}
