package com.drivepark.app.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * 쉬어 가기를 권할 연속 주행 시간(분). 평소 2시간, 졸음 주의 시간대(오후 2~4시, 새벽 2~6시)는 1시간 30분.
 * [hourOfDay]는 주행을 시작한 시각(0~23). 시각은 판단에만 쓰고 저장하지 않는다.
 */
fun restThresholdMinutes(hourOfDay: Int): Int = if (hourOfDay in 14..15 || hourOfDay in 2..5) 90 else 120

/**
 * 졸음 휴식 권유. 기어 D가 기준 시간 넘게 이어지면 [due]가 true(주행 중: 팝업·잠금 화면이 조는 모습으로).
 * 그 뒤 P로 바꾸면 [suggestions]로 한 번 알린다(휴식 권유 화면). P로 바꾸면 시계는 처음부터.
 * 끄면(설정) 아무것도 하지 않는다. 주행 시작 시각은 메모리에만 있다(저장하지 않음).
 */
class RestReminder(
    private val state: Flow<DriveState>,
    private val enabled: Flow<Boolean>,
    private val hourOfDay: () -> Int,
) {
    private val _due = MutableStateFlow(false)
    val due: StateFlow<Boolean> = _due.asStateFlow()

    private val _suggestions = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val suggestions: SharedFlow<Unit> = _suggestions.asSharedFlow()

    @Volatile private var drivingAndOn = false

    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(scope: CoroutineScope) = scope.launch {
        combine(state, enabled) { s, on -> s to on }
            .distinctUntilChanged()
            .collectLatest { (s, on) ->
                drivingAndOn = s == DriveState.DRIVING && on
                when {
                    drivingAndOn -> {
                        delay(restThresholdMinutes(hourOfDay()) * 60_000L)
                        _due.value = true
                    }
                    s == DriveState.PARKED && on && _due.value -> {
                        _due.value = false
                        _suggestions.emit(Unit)
                    }
                    else -> _due.value = false
                }
            }
    }

    /** 시연용(개발자 메뉴): 주행 중이면 바로 "쉴 때"로 건너뛴다. 주차 중에는 아무것도 하지 않는다. */
    fun markDueNow() {
        if (drivingAndOn) _due.value = true
    }
}
