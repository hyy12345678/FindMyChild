package com.findmychild.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.findmychild.app.domain.model.UserRole
import com.findmychild.app.domain.repository.SettingsRepository
import com.findmychild.app.ui.navigation.NavRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _startupRoute = MutableStateFlow(NavRoute.RoleSelection.route)
    val startupRoute: StateFlow<String> = _startupRoute.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.getUserRole(),
                settingsRepository.getPairingInfo()
            ) { role, pairingInfo ->
                determineStartDestination(role, pairingInfo)
            }.collect { route ->
                _startupRoute.value = route
            }
        }
    }

    private fun determineStartDestination(
        role: UserRole?,
        pairingInfo: com.findmychild.app.domain.model.PairingInfo?
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
