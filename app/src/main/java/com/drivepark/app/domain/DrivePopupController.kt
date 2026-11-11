package com.drivepark.app.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** 다른 앱 위에 뜨는 주행 팝업 창. [sleepy]면 캐릭터가 커피를 들고 조는 정지 그림. */
interface PopupWindowPort {
    fun show(text: String, sleepy: Boolean)
    fun hide()
}

/**
 * 주행 중에만 팝업을 띄운다. 만난 사람 수는 표시하지 않는다.
 * 사색 중이면 문구만 바뀐다. 오래 달려 쉴 때가 되면([restDue]) 조는 모습 + 쉬어 가자는 문구가 먼저다.
 * 오버레이 권한이 없으면 아무것도 하지 않는다.
 */
class DrivePopupController(
    private val state: StateFlow<DriveState>,
    private val settings: Flow<AppSettings>,
    private val window: PopupWindowPort,
    private val restDue: Flow<Boolean> = MutableStateFlow(false),
    private val canDraw: () -> Boolean,
) {
    fun start(scope: CoroutineScope) {
        scope.launch {
            var showing = false
            combine(state, settings, restDue) { s, prefs, rest ->
                if (s != DriveState.DRIVING) null
                else if (rest) REST to true
                else popupText(prefs.pauseVisits) to false
            }.distinctUntilChanged().collect { shown ->
                if (!canDraw()) return@collect
                if (shown != null) {
                    window.show(shown.first, shown.second)
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
        const val REST = "● 쉬어 갈 때가 됐어요"
        fun popupText(paused: Boolean) = if (paused) REFLECTING else SEARCHING
    }
}
