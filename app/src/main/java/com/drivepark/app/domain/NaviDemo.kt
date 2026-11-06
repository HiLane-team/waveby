package com.drivepark.app.domain

import kotlinx.coroutines.flow.Flow

/** 시연용: 순정 내비가 목적지까지 모의 주행하게 한다. */
interface NaviDemo {
    /** 경로 요청과 모의 주행을 시작한다. 내비 연동을 쓸 수 없으면 false. */
    fun startSimulatedDrive(lat: Double, lng: Double, name: String): Boolean

    /** 내비가 목적지 도착을 알릴 때마다 내보낸다. */
    val arrived: Flow<Unit>
}
