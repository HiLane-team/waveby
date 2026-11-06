package com.drivepark.app.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** 다른 앱 위에 뜨는 주행 팝업 창. */
interface PopupWindowPort {
    fun show(text: String)
    fun hide()
}

/**
 * 주행 중에만 팝업을 띄운다. 만난 사람 수는 표시하지 않는다.
 * 사색 중이면 문구만 바뀐다. 오버레이 권한이 없으면 아무것도 하지 않는다.
 */
class DrivePopupController(
    private val state: StateFlow<DriveState>,
    private val settings: Flow<AppSettings>,
    private val window: PopupWindowPort,
    private val canDraw: () -> Boolean,
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            var showing = false
            combine(state, settings) { s, prefs ->
                if (s == DriveState.DRIVING) popupText(prefs.pauseVisits) else null
            }.distinctUntilChanged().collect { text ->
                if (!canDraw()) return@collect
                if (text != null) {
                    window.show(text)
                    showing = true
                } else if (showing) {
                    window.hide()
                    showing = false
                }
            }
        }
    }

    companion object {
        const val SEARCHING = "● 친구를 찾는 중…"
        const val REFLECTING = "● 사색 중…"
        fun popupText(paused: Boolean) = if (paused) REFLECTING else SEARCHING
    }
}
