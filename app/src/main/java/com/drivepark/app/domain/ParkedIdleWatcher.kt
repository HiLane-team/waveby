package com.drivepark.app.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 주차(P) 상태가 [idleMillis] 동안 이어지면 [onIdle]을 한 번 부른다(공원 화면을 앞으로 띄우는 데 쓴다).
 * 주행하면 시계가 처음부터 다시 간다. 시동 직후(앱 시작)도 주차면 시계가 바로 간다.
 * 앱은 다른 앱의 터치를 알 수 없으므로 "동작 없음"은 "주차가 이어짐"으로 판단한다.
 */
class ParkedIdleWatcher(
    private val state: Flow<DriveState>,
    private val enabled: Flow<Boolean>,
    private val idleMillis: Long = 10 * 60_000L,
    private val onIdle: () -> Unit,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun start(scope: CoroutineScope) = scope.launch {
        combine(state, enabled) { s, on -> on && s == DriveState.PARKED }
            .distinctUntilChanged()
            .collectLatest { parkedAndOn ->
                if (!parkedAndOn) return@collectLatest
                delay(idleMillis)
                onIdle()
            }
    }
}
