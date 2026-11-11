package com.drivepark.app.domain

/**
 * 원칙 1(주행 중 무조작)을 지키는 창 규칙.
 * 주행 잠금 화면은 앱 화면만 덮고, 따로 뜨는 창(도착 요약, 확인 대화상자)은 그 위에 그려져 눌린다.
 * 그래서 이런 창은 주행 중에 띄우지 않는다. (2026-11-09 보안 점검에서 도착 창이 주행 중에 눌리는 것을 확인)
 */
object DriveGuard {
    fun mayShowWindow(state: DriveState): Boolean = state != DriveState.DRIVING

    /** 주행 중이면 null(창을 숨김), 주차 중이면 [value] 그대로. */
    fun <T> whenParked(value: T?, state: DriveState): T? = if (mayShowWindow(state)) value else null
}
