package com.drivepark.app.domain

import kotlinx.coroutines.flow.StateFlow

/** 현재 위치를 1회 조회한다. 권한이 없거나 위치를 얻지 못하면 null. */
interface LocationProvider {
    suspend fun currentLocation(): LatLng?
}

/** 인사말을 음성으로 읽는다. */
interface Greeter {
    fun speak(text: String)
}

enum class DriveState { DRIVING, PARKED }

/** 주행/주차 상태 변화를 알린다. */
interface DriveStateMonitor {
    val state: StateFlow<DriveState>
}
