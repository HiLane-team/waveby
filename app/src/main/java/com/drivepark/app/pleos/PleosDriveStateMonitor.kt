package com.drivepark.app.pleos

import android.content.Context
import android.util.Log
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import com.drivepark.app.domain.DriveState
import com.drivepark.app.domain.DriveStateMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Vehicle SDK 기어 상태 → 주행/주차. 기어 P면 PARKED, 그 외는 DRIVING.
 * GEAR_UNKNOWN은 상태를 바꾸지 않는다. 권한: pleos.car.permission.CAR_DRIVE (normal).
 * 초기화나 구독에 실패하면 생성자가 예외를 던진다(AppContainer가 대체 구현으로 바꾼다).
 */
class PleosDriveStateMonitor(context: Context) : DriveStateMonitor {
    private val _state = MutableStateFlow(DriveState.PARKED)
    override val state: StateFlow<DriveState> = _state.asStateFlow()

    private val vehicle = Vehicle(context.applicationContext)

    private val listener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) {
            Log.d(TAG, "gear=$gear")
            _state.value = when (gear) {
                VehicleGear.GEAR_PARK -> DriveState.PARKED
                VehicleGear.GEAR_UNKNOWN, null -> return
                else -> DriveState.DRIVING
            }
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "gear listener failed", e)
        }
    }

    init {
        vehicle.initialize()
        @Suppress("DEPRECATION")
        vehicle.getGear().registerGearSelection(listener)
    }

    private companion object {
        const val TAG = "DrivePark/Gear"
    }
}
