package com.drivepark.app.domain

import kotlinx.coroutines.flow.Flow

/** 화면 테마. SYSTEM은 Pleos(시스템) 다크모드 설정을 따라간다. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * 시동을 걸어 앱이 뜬 뒤 인사 화면이 끝나면 어디로 갈지.
 * TO_NAVI: 순정 내비를 바로 띄운다(내비 버튼을 한 번 더 누르지 않게). STAY: 공원 화면에 머문다.
 */
enum class StartupMode { TO_NAVI, STAY }

/** 사용자 설정. 기본값: 알림·음성·글레오 켜짐, 사색 꺼짐, 테마는 시스템, 시동 후 내비로, 주차 10분 뒤 공원 안내 켜짐, 휴식 권유 켜짐. */
data class AppSettings(
    val arrivalNotification: Boolean = true,
    val greetingVoice: Boolean = true,
    val gleoBriefing: Boolean = true,
    val pauseVisits: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val startupMode: StartupMode = StartupMode.TO_NAVI,
    /** 주차(P) 상태로 10분이 지나면 내비 위에 공원 안내 박스를 띄운다([IdleParkBanner]). */
    val showParkWhenIdle: Boolean = true,
    /**
     * 3분할에서 Waveby가 마지막으로 있던 칸. 켜질 때 이 칸으로 되돌린다.
     * 기억이 없으면 운전석에서 먼 맨 오른쪽(내비가 운전석 쪽 가운데)을 기본으로 한다.
     */
    val lastSplitPane: SplitPane = SplitPane.RIGHT,
    /** 마지막으로 인사 후 내비를 자동으로 띄운 시동의 부팅 시각(ms). 같은 시동에서는 다시 띄우지 않는다. */
    val lastAutoNaviBoot: Long? = null,
    /** 오래 달리면(평소 2시간, 졸음 주의 시간대 1시간 30분) 쉬어 가자고 권한다. 끌 수 있다. */
    val restReminder: Boolean = true,
    /** 공원지기 이호랑의 처음 안내를 끝까지 봤는지. 보고 나면 구석에서 놀다가 누를 때만 도움말을 준다. */
    val keeperTutorialDone: Boolean = false,
)

/** Pleos 3분할에서 Waveby의 칸. NONE은 분할하지 않은 기본 화면. */
enum class SplitPane { NONE, MIDDLE, RIGHT }

interface SettingsRepository {
    fun observe(): Flow<AppSettings>
    suspend fun current(): AppSettings
    suspend fun update(transform: (AppSettings) -> AppSettings)
}

/** 부팅 시각을 몇 초 오차로 계산하므로 이 안이면 같은 시동으로 본다. */
private const val SAME_BOOT_TOLERANCE_MS = 60_000L

/**
 * 인사가 끝난 뒤 순정 내비를 자동으로 띄울지. 시동(기기 부팅) 한 번에 한 번만 띄운다:
 * 앱 업데이트·재시작으로 같은 시동 안에서 다시 "새로 켜짐"이 되면 공원에 머문다.
 * 3분할 중이면 내비가 이미 옆 칸에 보이므로 띄우지 않는다.
 */
fun shouldAutoOpenNavi(mode: StartupMode, inSplit: Boolean, bootMark: Long, lastAutoNaviBoot: Long?): Boolean {
    if (mode != StartupMode.TO_NAVI || inSplit) return false
    return lastAutoNaviBoot == null || kotlin.math.abs(bootMark - lastAutoNaviBoot) > SAME_BOOT_TOLERANCE_MS
}
