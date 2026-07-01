package com.findmychild.app.ui.role

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.findmychild.app.domain.model.UserRole
import com.findmychild.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoleSelectionViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    fun selectRole(role: UserRole, onComplete: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setUserRole(role)
            onComplete()
        }
    }
}
