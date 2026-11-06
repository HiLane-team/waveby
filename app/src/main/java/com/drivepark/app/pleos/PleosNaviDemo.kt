package com.drivepark.app.pleos

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.constants.RouteDriving
import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.data.ApplicationVisibleInfo
import ai.pleos.playground.navi.data.BookmarkInfo
import ai.pleos.playground.navi.data.CameraAlert
import ai.pleos.playground.navi.data.ChargerOperator
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.DrivingInfo
import ai.pleos.playground.navi.data.FindRouteResult
import ai.pleos.playground.navi.data.RecentDestinationInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.data.RouteStartInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.data.WaypointArrivedInfo
import ai.pleos.playground.navi.data.WaypointChangedInfo
import ai.pleos.playground.navi.data.WaypointInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import com.drivepark.app.domain.NaviDemo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * NaviHelper로 경로를 요청하고, 경로가 시작되면 순정 내비의 모의 주행을 켠다.
 * 권한: pleos.car.permission.NAVI_ROUTE / NAVI_CUSTOM_ROUTE / NAVI_ROUTE_SEARCH (normal)
 */
class PleosNaviDemo(context: Context) : NaviDemo {
    private val _arrived = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    override val arrived: Flow<Unit> = _arrived.asSharedFlow()

    @Volatile private var simulateOnRouteStart = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val listener = object : NaviHelperEventListener {
        override fun onRouteStarted(info: RouteStartInfo) {
            Log.d(TAG, "route started: ${info.name} ${info.distance}m")
            if (simulateOnRouteStart) {
                simulateOnRouteStart = false
                // 경로 시작 직후(수 ms)에 보낸 요청은 내비가 무시했다(2026-11-05). 안내가 자리 잡을 시간을 준다.
                mainHandler.postDelayed({
                    Log.d(TAG, "requesting simulated drive")
                    runCatching { helper?.getAssistant()?.requestSimulatedDrive() }
                        .onFailure { Log.w(TAG, "simulated drive failed", it) }
                }, SIMULATION_DELAY_MS)
            }
        }

        override fun onDestinationArrived(info: DestinationArrivedInfo) {
            Log.d(TAG, "destination arrived")
            _arrived.tryEmit(Unit)
        }

        override fun onError(code: NaviErrorCode) {
            Log.w(TAG, "navi error: $code")
        }

        override fun onRouteEnded() = Unit
        override fun onReRouteEnded() = Unit
        override fun onRouteOptionChanged(option: RouteOption) = Unit
        override fun onRouteCancelled() = Unit
        override fun onWaypointChanged(info: WaypointChangedInfo) = Unit
        override fun onWayPointArrived(info: WaypointArrivedInfo) = Unit
        override fun onBookMarkChanged() = Unit
        override fun onDestinationChanged() = Unit
        override fun onApplicationVisibilityChangeRequest(info: ApplicationVisibleInfo) = Unit
        override fun onCurrentLocationInfo(info: CurrentLocationInfo) = Unit
        override fun onRouteStateInfo(info: RouteStateInfo) = Unit
        override fun onDestinationInfo(info: DestinationInfo) = Unit
        override fun onWayPointInfo(list: List<WaypointInfo>) = Unit
        override fun onRecentDestinationInfo(list: List<RecentDestinationInfo>) = Unit
        override fun onBookmarkInfo(list: List<BookmarkInfo>) = Unit
        override fun onUserDefinedRoute(result: FindRouteResult) = Unit
        override fun onCurrentRoadSpeedLimit(limit: Int) = Unit
        override fun onCameraAlert(list: List<CameraAlert>) = Unit
        override fun onDrivingInfo(info: DrivingInfo) = Unit
        override fun onNavigationStatus(status: RouteDriving) = Unit
        override fun onTBTInfo(list: List<TBTInfo>) = Unit
        override fun onChargerOperatorInfo(list: List<ChargerOperator>) = Unit
    }

    private val helper: NaviHelper? = runCatching {
        NaviHelper(context.applicationContext).also {
            it.initialize()
            it.addListener(listener)
        }
    }.onFailure { Log.w(TAG, "NaviHelper unavailable", it) }.getOrNull()

    override fun startSimulatedDrive(lat: Double, lng: Double, name: String): Boolean {
        val h = helper ?: return false
        return runCatching {
            simulateOnRouteStart = true
            // 주의: 이 SDK 버전의 RouteInfo는 경도(longitude)가 먼저다.
            h.requestRoute(
                RouteInfo(
                    longitude = lng, latitude = lat, poiName = name,
                    poiId = "", address = "", poiSubId = "",
                    routeOption = RouteOption.RECOMMENDED,
                ),
            )
        }.onFailure {
            simulateOnRouteStart = false
            Log.w(TAG, "requestRoute failed", it)
        }.isSuccess
    }

    private companion object {
        const val TAG = "DrivePark/Navi"
        const val SIMULATION_DELAY_MS = 2_000L
    }
}
