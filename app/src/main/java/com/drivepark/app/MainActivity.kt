package com.drivepark.app

import com.drivepark.app.ui.pixel.Shapes
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.saveable.rememberSaveable
import com.drivepark.app.domain.shouldAutoOpenNavi
import com.drivepark.app.domain.Character
import com.drivepark.app.ui.welcome.WelcomeScreen
import com.drivepark.app.domain.StartupMode
import com.drivepark.app.drive.AppForeground
import com.drivepark.app.drive.ScreenSwitcher
import com.drivepark.app.drive.SplitPlacement
import androidx.lifecycle.lifecycleScope
import com.drivepark.app.domain.SplitPane
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.drivepark.app.domain.ArrivalSummary
import com.drivepark.app.domain.DriveGuard
import com.drivepark.app.domain.DriveState
import com.drivepark.app.drive.DriveWatchService
import com.drivepark.app.ui.arrival.ArrivalDialog
import com.drivepark.app.ui.character.CreateCharacterScreen
import com.drivepark.app.ui.character.CreateCharacterViewModel
import com.drivepark.app.ui.collection.CollectionScreen
import com.drivepark.app.ui.collection.CollectionViewModel
import com.drivepark.app.ui.dev.DevMenuScreen
import com.drivepark.app.ui.onboarding.OnboardingScreen
import com.drivepark.app.ui.onboarding.OnboardingViewModel
import com.drivepark.app.ui.onboarding.startRoute
import com.drivepark.app.ui.park.FriendListScreen
import com.drivepark.app.ui.park.ParkScreen
import com.drivepark.app.ui.park.ParkViewModel
import com.drivepark.app.ui.settings.SettingsScreen
import com.drivepark.app.ui.settings.SettingsViewModel
import com.drivepark.app.ui.shell.AppShell
import com.drivepark.app.ui.shell.RailItem
import com.drivepark.app.ui.theme.DriveParkTheme
import com.drivepark.app.ui.theme.PleosMotion

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as DriveParkApp
        val container = app.container
        if (!DriveWatchService.start(this)) app.startSessionInProcess()
        setContent {
            val prefs by container.settings.observe().collectAsState(initial = com.drivepark.app.domain.AppSettings())
            // 앱이 새로 켜질 때만(화면 회전 등 재생성은 제외) 시동 인사를 보여준다.
            val fresh = savedInstanceState == null && !intent.getBooleanExtra(ScreenSwitcher.EXTRA_SKIP_WELCOME, false)
            DriveParkTheme(prefs.themeMode) { DriveParkNav(container, freshStart = fresh) }
        }
        // 새로 켜질 때만 기억한 분할 칸으로 되돌린다(칸 크기가 바뀌어 다시 만들어질 때는 제외).
        restorePending = savedInstanceState == null
    }

    override fun onStart() { super.onStart(); AppForeground.visible = true }
    override fun onStop() { AppForeground.visible = false; super.onStop() }

    // ── 3분할 칸 기억하기 ──
    // 사용자가 옮긴 칸은 그대로 두고 저장만 한다. 칸이 옮겨질 때 오는 화면 배치 신호(global layout)에서만 확인하고
    // (에뮬레이터에서 칸 이동 시 onResume·onNewIntent·onConfigurationChanged는 오지 않고 이 신호만 옴을 로그로 확인),
    // 켜진 뒤 첫 배치 때 한 번만 기억한 칸으로 되돌린다.

    private var restorePending = false
    private var lastSeenPane: SplitPane? = null

    // 분할하면 활동이 새로 만들어진다. 확인기는 이 활동 인스턴스에만 붙이고, 사라질 때 뗀다
    // (예전 인스턴스에 남은 확인기는 DESTROYED 상태라 저장 코루틴이 돌지 않았다).
    private val layoutListener = android.view.ViewTreeObserver.OnGlobalLayoutListener { onWindowLaidOut() }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    override fun onDestroy() {
        window.decorView.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
        super.onDestroy()
    }

    private fun onWindowLaidOut() {
        val pane = SplitPlacement.paneOf(this)
        if (pane == lastSeenPane) return // 칸이 그대로면 아무것도 하지 않는다(대부분의 배치 신호)
        lastSeenPane = pane
        val settings = (application as DriveParkApp).container.settings
        lifecycleScope.launch {
            if (restorePending) {
                restorePending = false
                // 켜질 때: 분할 칸에 있고 기억한 칸과 다르면 내비를 한 번 실행해 맞바꾼다(분할 중 마지막 실행 앱이 가운데로 감).
                if (SplitPlacement.needsSwap(pane, settings.current().lastSplitPane)) {
                    ScreenSwitcher.openNavi(this@MainActivity)
                    return@launch
                }
            }
            // 분할 칸이면 기억한다(분할하지 않은 화면은 마지막 분할 칸 기억을 지우지 않는다).
            if (pane != SplitPane.NONE) settings.update { if (it.lastSplitPane == pane) it else it.copy(lastSplitPane = pane) }
        }
    }
}

private object Routes {
    const val CREATE = "create"
    const val ONBOARDING = "onboarding"
    const val DEV = "dev"
}

@Composable
private fun DriveParkNav(container: AppContainer, freshStart: Boolean) {
    // 시작 화면: 내 캐릭터가 없으면 초기 설정, 있으면 공원
    var me by remember { mutableStateOf<Character?>(null) }
    val start by produceState<String?>(null) {
        me = container.repo.getMyCharacter()
        value = startRoute(hasCharacter = me != null)
    }
    // 시동 인사: 새로 켜졌고 캐릭터가 있을 때 한 번. 닫으면 다시 안 뜬다.
    var showWelcome by rememberSaveable { mutableStateOf(freshStart) }
    val startRoute = start ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    // API 33 이상에서 알림 권한 요청. 첫 실행이면 초기 설정의 권한 단계에서 묻는다.
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && startRoute != Routes.ONBOARDING) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // 앱이 열려 있으면 도착 요약을 바닥 시트로도 보여준다
    var arrival by remember { mutableStateOf<ArrivalSummary?>(null) }
    LaunchedEffect(Unit) { container.driveSession.arrivals.collect { arrival = it } }

    // 졸음 휴식 권유: 주행 중 쉴 때면 잠금 화면이 조는 모습, 그 뒤 주차하면 휴식 권유 화면(+인사 음성이 켜져 있으면 읽기).
    val restDue by container.restReminder.due.collectAsState()
    val myCharacter by container.repo.observeMyCharacter().collectAsState(initial = null)
    var showRest by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        container.restReminder.suggestions.collect {
            showRest = true
            if (container.settings.current().greetingVoice) container.greeter.speak(com.drivepark.app.ui.rest.REST_MESSAGE)
        }
    }

    val scope = rememberCoroutineScope()
    val activity = androidx.activity.compose.LocalActivity.current
    val nav = rememberNavController()
    // 내비 위 안내 박스의 "공원 보기": 어느 화면에 있었든 공원으로 간다(초기 설정 중이면 그대로).
    LaunchedEffect(Unit) {
        container.openParkRequests.collect {
            if (nav.currentDestination?.route != Routes.ONBOARDING) {
                nav.navigate(RailItem.PARK.route) { popUpTo(0) { inclusive = true } }
            }
        }
    }
    val parkVm: ParkViewModel = viewModel { ParkViewModel(container.repo) }
    val park by parkVm.state.collectAsState()
    val driveState by container.monitor.state.collectAsState()
    val demoOverride by container.monitor.override.collectAsState()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val selected = RailItem.entries.firstOrNull { it.route == route }

    // 주행 중에는 반복 애니메이션을 멈춘다(앱은 잠금 화면 뒤에 가려져 있고, CPU는 내비가 써야 한다).
    androidx.compose.runtime.CompositionLocalProvider(
        com.drivepark.app.ui.theme.LocalAnimationsEnabled provides (driveState != DriveState.DRIVING),
        com.drivepark.app.ui.theme.LocalDriving provides (driveState == DriveState.DRIVING),
    ) {
        AppShell(
            showRail = route != Routes.ONBOARDING && route != null,
            selected = selected,
            newFriends = park.newFriends.size,
            // 개발자 메뉴는 시연 중 "주차"를 눌러야 하므로 잠그지 않는다.
            driving = driveState == DriveState.DRIVING && route != Routes.DEV,
            onDemoPark = if (demoOverride != null) ({ container.monitor.setOverride(DriveState.PARKED) }) else null,
            rest = restDue,
            me = myCharacter,
            onSelect = { nav.goTo(it.route) },
        ) {
            NavHost(
                nav, startDestination = startRoute,
                enterTransition = { PleosMotion.enter }, exitTransition = { PleosMotion.exit },
                popEnterTransition = { PleosMotion.enter }, popExitTransition = { PleosMotion.exit },
            ) {
                composable(Routes.ONBOARDING) {
                    val vm: OnboardingViewModel = viewModel { OnboardingViewModel() }
                    val characterVm: CreateCharacterViewModel = viewModel { CreateCharacterViewModel(container.repo, Shapes.animalIds) }
                    OnboardingScreen(vm, characterVm, container) {
                        nav.navigate(RailItem.PARK.route) { popUpTo(0) { inclusive = true } }
                    }
                }
                composable(Routes.CREATE) {
                    val vm: CreateCharacterViewModel = viewModel { CreateCharacterViewModel(container.repo, Shapes.animalIds) }
                    CreateCharacterScreen(vm, title = "내 캐릭터 꾸미기", saveLabel = "저장", showKindRow = true) {
                        // 설정에서 수정하러 왔으면 돌아가고, 처음 만들었으면 공원을 시작점으로
                        if (nav.previousBackStackEntry != null) nav.popBackStack()
                        else nav.navigate(RailItem.PARK.route) { popUpTo(0) { inclusive = true } }
                    }
                }
                composable(RailItem.PARK.route) {
                    val prefs by container.settings.observe().collectAsState(initial = com.drivepark.app.domain.AppSettings())
                    ParkScreen(parkVm, paused = prefs.pauseVisits, onTogglePause = {
                        scope.launch { container.settings.update { it.copy(pauseVisits = !it.pauseVisits) } }
                    }, keeperTutorialDone = prefs.keeperTutorialDone, onKeeperTutorialDone = {
                        scope.launch { container.settings.update { it.copy(keeperTutorialDone = true) } }
                    })
                }
                composable(RailItem.FRIENDS.route) { FriendListScreen(parkVm) }
                composable(RailItem.COLLECTION.route) {
                    val vm: CollectionViewModel = viewModel {
                        CollectionViewModel(container.repo) { container.location.hasPermission() }
                    }
                    CollectionScreen(vm)
                }
                composable(RailItem.SETTINGS.route) {
                    val vm: SettingsViewModel = viewModel {
                        SettingsViewModel(
                            container.settings, container.repo,
                            overlayGranted = { android.provider.Settings.canDrawOverlays(container.appContext) },
                            locationGranted = { container.location.hasPermission() },
                        )
                    }
                    SettingsScreen(vm, onEditCharacter = { nav.navigate(Routes.CREATE) }, onOpenDev = { nav.navigate(Routes.DEV) })
                }
                composable(Routes.DEV) { DevMenuScreen(container) { nav.popBackStack() } }
            }
        }
    }

    // 주행이 시작되면 인사 화면은 바로 닫는다(운전 중 조작·시선 방해 방지).
    LaunchedEffect(driveState) { if (driveState == DriveState.DRIVING) showWelcome = false }
    val welcomeFor = me
    AnimatedVisibility(
        visible = showWelcome && welcomeFor != null && startRoute != Routes.ONBOARDING,
        enter = fadeIn(), exit = fadeOut(tween(600)),
    ) {
        welcomeFor?.let {
            WelcomeScreen(it) {
                showWelcome = false
                // 인사가 끝나면 설정에 따라 순정 내비를 바로 띄운다(내비 버튼을 한 번 더 누르지 않게).
                // 3분할 중이면 내비가 이미 옆 칸에 보이고, 실행하면 칸이 맞바뀌어 기억한 자리가 흐트러지므로 건너뛴다.
                val inSplit = (activity?.let(SplitPlacement::paneOf) ?: SplitPane.NONE) != SplitPane.NONE
                // 시동 한 번에 한 번만: 같은 시동 안에서 앱이 다시 "새로 켜짐"이 되면(업데이트·재시작) 공원에 머문다.
                scope.launch {
                    val bootMark = System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime()
                    val s = container.settings.current()
                    if (shouldAutoOpenNavi(s.startupMode, inSplit, bootMark, s.lastAutoNaviBoot)) {
                        container.settings.update { it.copy(lastAutoNaviBoot = bootMark) }
                        ScreenSwitcher.openNavi(container.appContext)
                    }
                }
            }
        }
    }

    // 휴식 권유가 떠 있는 동안 도착 요약 창은 기다린다(창은 화면 위에 따로 뜨므로 순서를 여기서 정한다).
    // 도착 창은 주행 잠금 화면보다 위에 그려지므로, 닫지 않고 출발하면 주행 중에도 눌렸다(2026-11-09 보안 점검).
    // 그래서 주행이 시작되면 닫고, 닫히기 전 한 프레임도 그리지 않는다.
    LaunchedEffect(driveState) { if (driveState == DriveState.DRIVING) arrival = null }
    ArrivalDialog(DriveGuard.whenParked(if (showRest) null else arrival, driveState)) {
        arrival = null
        nav.goTo(RailItem.PARK.route)
    }
    // 주행이 다시 시작되면 휴식 권유 화면은 닫는다(운전 중 큰 화면 금지).
    LaunchedEffect(driveState) { if (driveState == DriveState.DRIVING) showRest = false }
    AnimatedVisibility(visible = showRest && myCharacter != null, enter = fadeIn(), exit = fadeOut(tween(400))) {
        myCharacter?.let { com.drivepark.app.ui.rest.RestScreen(it) { showRest = false } }
    }
}

/** 메뉴 이동: 같은 화면을 쌓지 않고 공원을 바닥에 둔다. */
private fun NavHostController.goTo(route: String) = navigate(route) {
    popUpTo(graph.startDestinationId) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
