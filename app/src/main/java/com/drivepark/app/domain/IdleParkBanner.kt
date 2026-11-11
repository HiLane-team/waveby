package com.drivepark.app.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** 순정 내비 위에 뜨는 "공원에 친구들이 놀고 있어요" 안내 박스. 화면 쪽(안드로이드 창)이 구현한다. */
interface ParkBannerPort {
    fun show(title: String, subtitle: String)
    fun hide()
}

/**
 * 주차 10분 뒤 공원 안내(예전에는 공원 화면을 통째로 앞으로 띄웠다 → 다른 앱 사용을 끊어서 안내 박스로 바꿈).
 * 주차 중에만 띄우고, [autoHideMs] 뒤 저절로 숨기며, 출발하면(주차가 아니면) 바로 숨긴다.
 * Pleos는 일반 안드로이드 알림을 화면에 보여 주지 않아(에뮬레이터 2.1.0 확인) 주행 팝업과 같은 오버레이 창을 쓴다.
 */
class IdleParkBanner(
    private val state: StateFlow<DriveState>,
    private val port: ParkBannerPort,
    private val autoHideMs: Long = 15_000L,
) {
    private var job: Job? = null

    /** 주차 중이면 띄운다. 이미 떠 있으면 문구를 바꾸고 시간을 다시 잰다. */
    fun show(scope: CoroutineScope, friendCount: Int) {
        if (state.value != DriveState.PARKED) return
        job?.cancel()
        port.show(TITLE, subtitle(friendCount))
        job = scope.launch {
            withTimeoutOrNull(autoHideMs) { state.first { it != DriveState.PARKED } }
            port.hide()
            job = null
        }
    }

    /** 닫기(×)나 "공원 보기"를 누르면 숨긴다. */
    fun dismiss() {
        job?.cancel()
        job = null
        port.hide()
    }

    companion object {
        const val TITLE = "공원에 친구들이 놀고 있어요"

        /** 작은 글씨: 공원지기 이호랑과 친구 수(친구가 없으면 이호랑만). */
        fun subtitle(friendCount: Int): String =
            if (friendCount > 0) "이호랑과 친구 ${friendCount}명이 기다려요" else "이호랑이 공원에서 기다려요"
    }
}
