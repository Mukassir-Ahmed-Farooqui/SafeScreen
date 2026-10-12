package com.example.safescreen.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safescreen.AppContainer
import com.example.safescreen.monitoring.PermissionState
import com.example.safescreen.security.PinResult
import com.example.safescreen.ui.theme.PtyxisPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val palette: StateFlow<PtyxisPalette> = container.settingsRepository.palette
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PtyxisPalette.COBALT_NEON)

    private val _permissions = MutableStateFlow(container.permissions.read())
    val permissions: StateFlow<PermissionState> = _permissions.asStateFlow()

    fun refreshPermissions() {
        _permissions.value = container.permissions.read()
    }

    fun setPalette(palette: PtyxisPalette) {
        viewModelScope.launch { container.settingsRepository.setPalette(palette) }
    }

    val notificationsEnabled: StateFlow<Boolean> = container.settingsRepository.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { container.settingsRepository.setNotificationsEnabled(enabled) }
    }

    fun sendTestNotification() {
        container.notifier.sendTest()
    }

    fun lockNow() = container.sessionManager.lock()

    suspend fun changePin(current: String, new: String): PinResult =
        container.pinManager.changePin(current, new)
}
