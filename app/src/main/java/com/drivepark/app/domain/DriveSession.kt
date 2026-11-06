package com.drivepark.app.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 주행 상태를 지켜보며 주행 중에는 만남을 대기함에 쌓고(화면·알림·음성 없음),
 * 주차로 바뀌면 도착 처리를 한다. 비어 있지 않은 결과만 [arrivals]로 내보낸다.
 */
class DriveSession(
    private val monitor: DriveStateMonitor,
    private val source: EncounterSource,
    private val fake: FakeEncounterSource,
    private val repo: ParkRepository,
    private val coordinator: ArrivalCoordinator,
    private val settings: SettingsRepository,
    private val tickMillis: Long = 60_000,
) {
    private val _arrivals = MutableSharedFlow<ArrivalSummary>(extraBufferCapacity = 8)
    val arrivals: SharedFlow<ArrivalSummary> = _arrivals.asSharedFlow()
    private val started = AtomicBoolean(false)

    /** 두 번째 호출부터는 아무것도 하지 않는다(서비스 재시작 등). */
    fun start(scope: CoroutineScope) {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            var previous: DriveState? = null
            monitor.state.collectLatest { state ->
                val was = previous
                previous = state
                when (state) {
                    DriveState.DRIVING -> while (isActive) {
                        delay(tickMillis)
                        // 사색 중이면 만남을 모으지 않는다(2단계: 내 캐릭터도 보내지 않음).
                        if (!settings.current().pauseVisits) repo.addPending(source.fetchNew())
                    }
                    // 앱 시작 시의 초기 PARKED(was == null)는 전환으로 보지 않는다.
                    // 도착 처리는 다음 상태 변화로 취소되지 않게 한다.
                    DriveState.PARKED -> if (was == DriveState.DRIVING) {
                        withContext(NonCancellable) { arrive() }
                    }
                }
            }
        }
    }

    suspend fun simulateDrive(encounters: Int): ArrivalSummary {
        repo.addPending(fake.generate(encounters))
        return arrive()
    }

    private suspend fun arrive(): ArrivalSummary {
        val summary = coordinator.onParked()
        if (!summary.isEmpty) _arrivals.emit(summary)
        return summary
    }
}
