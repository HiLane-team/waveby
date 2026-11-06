package com.drivepark.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.drivepark.app.domain.AppSettings
import com.drivepark.app.domain.ParkRepository
import com.drivepark.app.domain.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val overlayGranted: Boolean = false,
    val locationGranted: Boolean = false,
)

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val repo: ParkRepository,
    private val overlayGranted: () -> Boolean,
    private val locationGranted: () -> Boolean,
) : ViewModel() {
    private val permissions = MutableStateFlow(overlayGranted() to locationGranted())
    private var versionTaps = 0

    val state: StateFlow<SettingsUiState> =
        combine(settings.observe(), permissions) { s, (overlay, location) -> SettingsUiState(s, overlay, location) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState(overlayGranted = permissions.value.first, locationGranted = permissions.value.second))

    fun set(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settings.update(transform) }
    }

    fun resetAll() {
        viewModelScope.launch { repo.resetAll() }
    }

    fun refreshPermissions() {
        permissions.value = overlayGranted() to locationGranted()
    }

    /** 앱 정보를 5번째 누를 때 true(개발자 메뉴 열기). */
    fun onVersionTapped(): Boolean {
        versionTaps++
        if (versionTaps < 5) return false
        versionTaps = 0
        return true
    }
}
