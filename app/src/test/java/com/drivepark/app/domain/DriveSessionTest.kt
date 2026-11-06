package com.drivepark.app.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeMonitor(initial: DriveState) : DriveStateMonitor {
    override val state = MutableStateFlow(initial)
}

class OnePerTick : EncounterSource {
    override suspend fun fetchNew() = listOf(testCharacter("틱"))
}

@OptIn(ExperimentalCoroutinesApi::class)
class DriveSessionTest {
    private val repo = InMemoryParkRepository()
    private val coordinator = testCoordinator(repo)

    private fun TestScope.startSession(monitor: FakeMonitor): Pair<DriveSession, MutableList<ArrivalSummary>> {
        val session = DriveSession(monitor, OnePerTick(), FakeEncounterSource(), repo, coordinator, InMemorySettingsRepository(), tickMillis = 60_000)
        val arrivals = mutableListOf<ArrivalSummary>()
        backgroundScope.launch { session.arrivals.collect { arrivals += it } }
        session.start(backgroundScope)
        runCurrent()
        return session to arrivals
    }

    @Test
    fun driving_accumulatesPending_noArrivalEmitted() = runTest {
        val monitor = FakeMonitor(DriveState.PARKED)
        val (_, arrivals) = startSession(monitor)
        monitor.state.value = DriveState.DRIVING
        advanceTimeBy(180_001)
        assertEquals(3, repo.pending.size)
        assertTrue(arrivals.isEmpty())
    }

    @Test
    fun drivingThenParked_emitsArrival() = runTest {
        val monitor = FakeMonitor(DriveState.PARKED)
        val (_, arrivals) = startSession(monitor)
        monitor.state.value = DriveState.DRIVING
        advanceTimeBy(180_001)
        monitor.state.value = DriveState.PARKED
        runCurrent()
        assertEquals(1, arrivals.size)
        assertEquals(3, arrivals[0].friends.size)
    }

    @Test
    fun initialParked_doesNotTriggerArrival() = runTest {
        val monitor = FakeMonitor(DriveState.PARKED)
        repo.addPending(listOf(testCharacter("남은")))
        val (_, arrivals) = startSession(monitor)
        advanceTimeBy(600_000)
        assertTrue(arrivals.isEmpty())
        assertEquals(1, repo.pending.size) // onParked가 불리지 않았다
    }

    @Test
    fun parkedTwice_secondSummaryEmpty() = runTest {
        val monitor = FakeMonitor(DriveState.PARKED)
        val (_, arrivals) = startSession(monitor)
        monitor.state.value = DriveState.DRIVING
        advanceTimeBy(60_001)
        monitor.state.value = DriveState.PARKED
        runCurrent()
        monitor.state.value = DriveState.DRIVING
        runCurrent()
        monitor.state.value = DriveState.PARKED
        runCurrent()
        assertEquals(1, arrivals.size)
    }

    @Test
    fun simulateDrive_deliversRequestedCount() = runTest {
        val (session, arrivals) = startSession(FakeMonitor(DriveState.PARKED))
        assertEquals(3, session.simulateDrive(3).friends.size)
        runCurrent()
        assertEquals(1, arrivals.size)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DriveSessionIdempotencyTest {
    @Test
    fun start_twice_isIdempotent() = runTest {
        val repo = InMemoryParkRepository()
        val monitor = FakeMonitor(DriveState.PARKED)
        val coordinator = testCoordinator(repo)
        val session = DriveSession(monitor, OnePerTick(), FakeEncounterSource(), repo, coordinator, InMemorySettingsRepository(), tickMillis = 60_000)
        session.start(backgroundScope)
        session.start(backgroundScope)
        runCurrent()
        monitor.state.value = DriveState.DRIVING
        advanceTimeBy(60_001)
        assertEquals(1, repo.pending.size)
    }
}

class CountingSource : EncounterSource {
    var calls = 0
    override suspend fun fetchNew(): List<Character> { calls++; return listOf(testCharacter("틱")) }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DriveSessionPauseTest {
    private val repo = InMemoryParkRepository()
    private val monitor = FakeMonitor(DriveState.PARKED)
    private val source = CountingSource()

    private fun TestScope.session(settings: InMemorySettingsRepository, location: LatLng = SEOUL): DriveSession {
        val s = DriveSession(
            monitor, source, FakeEncounterSource(), repo,
            testCoordinator(repo, FakeLocation(location)), settings, tickMillis = 60_000,
        )
        s.start(backgroundScope)
        runCurrent()
        return s
    }

    @Test
    fun paused_noFetch() = runTest {
        session(InMemorySettingsRepository(AppSettings(pauseVisits = true)))
        monitor.state.value = DriveState.DRIVING
        advanceTimeBy(180_001)
        assertEquals(0, source.calls)
        assertTrue(repo.pending.isEmpty())
    }

    @Test
    fun pauseTurnedOnWhileDriving_stopsNextTick() = runTest {
        val settings = InMemorySettingsRepository()
        session(settings)
        monitor.state.value = DriveState.DRIVING
        advanceTimeBy(60_001)
        settings.state.value = AppSettings(pauseVisits = true)
        advanceTimeBy(120_000)
        assertEquals(1, repo.pending.size)
    }

    @Test
    fun paused_parkStillCollectsRegion() = runTest {
        session(InMemorySettingsRepository(AppSettings(pauseVisits = true)), HOENGSEONG)
        monitor.state.value = DriveState.DRIVING
        advanceTimeBy(60_001)
        monitor.state.value = DriveState.PARKED
        runCurrent()
        assertEquals(setOf("hoengseong"), repo.observeCollectedRegionIds().first())
    }
}
