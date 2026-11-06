package com.drivepark.app.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** 시연용: 덮어쓰기 값이 있으면 그 값을, 없으면 실제(base) 주행 상태를 쓴다. */
class OverridableDriveStateMonitor(
    private val base: DriveStateMonitor,
    scope: CoroutineScope,
) : DriveStateMonitor {
    private val _override = MutableStateFlow<DriveState?>(null)
    val override: StateFlow<DriveState?> = _override.asStateFlow()

    override val state: StateFlow<DriveState> =
        combine(base.state, _override) { b, o -> o ?: b }
            .stateIn(scope, SharingStarted.Eagerly, base.state.value)

    fun setOverride(state: DriveState?) {
        _override.value = state
    }
}
