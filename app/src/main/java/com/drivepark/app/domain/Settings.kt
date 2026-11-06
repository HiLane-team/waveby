package com.drivepark.app.domain

import kotlinx.coroutines.flow.Flow

/** 화면 테마. SYSTEM은 Pleos(시스템) 다크모드 설정을 따라간다. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * 시동을 걸어 앱이 뜬 뒤 인사 화면이 끝나면 어디로 갈지.
 * TO_NAVI: 순정 내비를 바로 띄운다(내비 버튼을 한 번 더 누르지 않게). STAY: 공원 화면에 머문다.
 */
enum class StartupMode { TO_NAVI, STAY }

/** 사용자 설정. 기본값: 알림·음성·글레오 켜짐, 사색 꺼짐, 테마는 시스템, 시동 후 내비로, 주차 10분 뒤 공원 띄우기 켜짐. */
data class AppSettings(
    val arrivalNotification: Boolean = true,
    val greetingVoice: Boolean = true,
    val gleoBriefing: Boolean = true,
    val pauseVisits: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val startupMode: StartupMode = StartupMode.TO_NAVI,
    /** 주차(P) 상태로 10분이 지나면 공원 화면을 앞으로 띄운다. */
    val showParkWhenIdle: Boolean = true,
    /**
     * 3분할에서 Waveby가 마지막으로 있던 칸. 켜질 때 이 칸으로 되돌린다.
     * 기억이 없으면 운전석에서 먼 맨 오른쪽(내비가 운전석 쪽 가운데)을 기본으로 한다.
     */
    val lastSplitPane: SplitPane = SplitPane.RIGHT,
)

/** Pleos 3분할에서 Waveby의 칸. NONE은 분할하지 않은 기본 화면. */
enum class SplitPane { NONE, MIDDLE, RIGHT }

interface SettingsRepository {
    fun observe(): Flow<AppSettings>
    suspend fun current(): AppSettings
    suspend fun update(transform: (AppSettings) -> AppSettings)
}
