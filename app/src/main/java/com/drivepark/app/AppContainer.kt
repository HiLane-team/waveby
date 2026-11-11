package com.drivepark.app

import com.drivepark.app.ui.pixel.Shapes
import android.content.Context
import android.util.Log
import com.drivepark.app.data.AppDatabase
import com.drivepark.app.data.RoomParkRepository
import com.drivepark.app.data.DataStoreSettingsRepository
import com.drivepark.app.domain.SettingsRepository
import com.drivepark.app.domain.ArrivalCoordinator
import com.drivepark.app.domain.DriveSession
import com.drivepark.app.domain.DriveState
import com.drivepark.app.domain.DriveStateMonitor
import com.drivepark.app.domain.FakeEncounterSource
import com.drivepark.app.domain.OverridableDriveStateMonitor
import com.drivepark.app.domain.ParkRepository
import com.drivepark.app.domain.RegionResolver
import com.drivepark.app.pleos.PleosDriveStateMonitor
import com.drivepark.app.pleos.PleosGreeter
import com.drivepark.app.pleos.GleoBriefer
import com.drivepark.app.drive.OverlayPopupWindow
import com.drivepark.app.drive.ParkBannerWindow
import com.drivepark.app.drive.AppForeground
import com.drivepark.app.domain.IdleParkBanner
import kotlinx.coroutines.flow.first
import com.drivepark.app.drive.ScreenSwitcher
import com.drivepark.app.domain.ParkedIdleWatcher
import com.drivepark.app.domain.RestReminder
import com.drivepark.app.domain.DrivePopupController
import com.drivepark.app.pleos.PleosLocationProvider
import com.drivepark.app.pleos.PleosNaviDemo
import com.drivepark.app.domain.NaviDemo
import kotlinx.coroutines.launch
import com.drivepark.app.ui.arrival.ArrivalNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import android.content.res.Configuration
import com.drivepark.app.domain.ThemeMode
import com.drivepark.app.ui.theme.resolveDark

/** 의존성을 수동으로 조립한다. */
class AppContainer(context: Context, private val scope: CoroutineScope) {
    val appContext: Context = context.applicationContext
    val repo: ParkRepository = RoomParkRepository(AppDatabase.create(context))
    val location = PleosLocationProvider(context)
    val greeter = PleosGreeter(context)
    val notifier = ArrivalNotifier(context)
    val settings: SettingsRepository = DataStoreSettingsRepository.create(context)

    // Vehicle SDK를 쓸 수 없으면 PARKED 고정으로 대체한다(시연 덮어쓰기는 계속 쓸 수 있다).
    private val vehicleMonitor: DriveStateMonitor =
        runCatching<DriveStateMonitor> { PleosDriveStateMonitor(context) }.getOrElse {
            Log.w("DrivePark", "Vehicle SDK unavailable, using fixed PARKED monitor", it)
            object : DriveStateMonitor {
                override val state: StateFlow<DriveState> = MutableStateFlow(DriveState.PARKED)
            }
        }

    /** 실제 기어 + 시연용 덮어쓰기. */
    val monitor = OverridableDriveStateMonitor(vehicleMonitor, scope)

    // 팝업에 쓸 내 캐릭터와 테마. 팝업은 메인 스레드에서 바로 그려야 하므로 최신 값을 들고 있는다.
    private val myCharacter = repo.observeMyCharacter().stateIn(scope, SharingStarted.Eagerly, null)
    private val themeMode = settings.observe().map { it.themeMode }.stateIn(scope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    private fun isDarkNow(): Boolean {
        val night = appContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        return resolveDark(themeMode.value, night)
    }

    private val overlay = OverlayPopupWindow(context, myCharacter = { myCharacter.value }, isDark = ::isDarkNow)

    /** "공원 보기"처럼 앱을 앞으로 부르면서 공원 화면으로 가야 할 때 보내는 신호(화면이 받아서 공원으로 이동). */
    val openParkRequests = kotlinx.coroutines.flow.MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** 주차 10분 뒤 순정 내비 위에 뜨는 "공원에 친구들이 놀고 있어요" 안내 박스. */
    val idleParkBanner: IdleParkBanner by lazy {
        IdleParkBanner(
            monitor.state,
            ParkBannerWindow(
                context,
                isDark = ::isDarkNow,
                onOpenPark = {
                    idleParkBanner.dismiss()
                    openParkRequests.tryEmit(Unit)
                    ScreenSwitcher.openPark(appContext)
                },
                onClose = { idleParkBanner.dismiss() },
            ),
        )
    }

    /** 공원 안내 박스를 띄운다(공원이 이미 보이면 띄우지 않는다). 친구 수는 지금 친구 목록에서 센다. */
    fun showIdleParkBanner() {
        if (AppForeground.visible) return
        scope.launch { idleParkBanner.show(scope, repo.observeFriends().first().size) }
    }

    /** 졸음 휴식 권유: 오래 달리면 팝업·잠금 화면이 조는 모습, 그 뒤 주차하면 휴식 권유 화면. */
    val restReminder = RestReminder(monitor.state, settings.observe().map { it.restReminder }) {
        java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    }.also { it.start(scope) }

    /** 주행 중 순정 내비 위에 뜨는 팝업. */
    val drivePopup = DrivePopupController(monitor.state, settings.observe(), overlay, restDue = restReminder.due, canDraw = overlay::canDraw)

    /** 시연용 순정 내비 모의 주행. 내비가 도착을 알리면 주차로 덮어쓴다. */
    val naviDemo: NaviDemo = PleosNaviDemo(context)

    init {
        scope.launch { naviDemo.arrived.collect { monitor.setOverride(DriveState.PARKED) } }
        // 주차가 10분 이어지면 내비 위에 공원 안내 박스를 띄운다(화면을 통째로 바꾸지 않는다, 설정에서 끌 수 있음).
        ParkedIdleWatcher(monitor.state, settings.observe().map { it.showParkWhenIdle }) {
            showIdleParkBanner()
        }.start(scope)
    }

    private val fake = FakeEncounterSource(animalShapes = Shapes.fakeFriendAnimalIds)

    val driveSession = DriveSession(
        monitor = monitor,
        source = fake,
        fake = fake,
        repo = repo,
        coordinator = ArrivalCoordinator(repo, location, RegionResolver(), greeter, GleoBriefer(context), settings),
        settings = settings,
        tickMillis = 30_000,
    )
}
