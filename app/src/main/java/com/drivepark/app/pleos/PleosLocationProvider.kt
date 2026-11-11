package com.drivepark.app.pleos

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import androidx.core.content.ContextCompat
import com.drivepark.app.domain.LatLng
import com.drivepark.app.domain.LocationProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * FusedLocation SDK로 마지막 위치를 1회 조회한다.
 * 권한(pleos.car.permission.FUSED_LOCATION, 런타임 권한)이 없으면 null,
 * 5초 안에 응답이 없거나 실패하면 null.
 */
class PleosLocationProvider(context: Context) : LocationProvider {
    private val appContext = context.applicationContext
    private val fused: FusedLocation? = runCatching {
        FusedLocation(appContext).takeIf { it.isSupported() }?.also { it.initialize() }
    }.onFailure { Log.w(TAG, "init failed", it) }.getOrNull()

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, PERMISSION) == PackageManager.PERMISSION_GRANTED

    override suspend fun currentLocation(): LatLng? {
        val f = fused ?: return null
        if (!hasPermission()) return null
        return withTimeoutOrNull(5_000) {
            suspendCancellableCoroutine { cont ->
                val listener = object : FusedLocationListener {
                    override fun onChanged(data: LocationData?) {
                        if (data == null) { if (cont.isActive) cont.resume(null); return }
                        // 원칙 2: 좌표는 로그에도 남기지 않는다(버그 리포트로 밖에 나갈 수 있음, 2026-11-09 보안 점검).
                        Log.d(TAG, "location received")
                        if (cont.isActive) cont.resume(LatLng(data.latitude, data.longitude))
                    }

                    override fun onError(e: Exception) {
                        Log.w(TAG, "location error", e)
                        if (cont.isActive) cont.resume(null)
                    }
                }
                runCatching { f.getLastKnownLocation(listener) }
                    .onFailure { if (cont.isActive) cont.resume(null) }
            }
        }
    }

    companion object {
        const val PERMISSION = "pleos.car.permission.FUSED_LOCATION"
        private const val TAG = "DrivePark/Location"
    }
}
