package com.drivepark.app.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OverridableDriveStateMonitorTest {
    @Test
    fun override_wins() = runTest {
        val base = FakeMonitor(DriveState.PARKED)
        val m = OverridableDriveStateMonitor(base, backgroundScope)
        m.setOverride(DriveState.DRIVING)
        runCurrent()
        assertEquals(DriveState.DRIVING, m.state.value)
    }

    @Test
    fun baseChanges_ignoredWhileOverridden() = runTest {
        val base = FakeMonitor(DriveState.PARKED)
        val m = OverridableDriveStateMonitor(base, backgroundScope)
        m.setOverride(DriveState.DRIVING)
        runCurrent()
        base.state.value = DriveState.DRIVING; runCurrent()
        base.state.value = DriveState.PARKED; runCurrent()
        assertEquals(DriveState.DRIVING, m.state.value)
    }

    @Test
    fun clearOverride_returnsToBase() = runTest {
        val repo = InMemoryParkRepository()
        val base = FakeMonitor(DriveState.PARKED)
        val m = OverridableDriveStateMonitor(base, backgroundScope)
        val coordinator = testCoordinator(repo)
        val session = DriveSession(m, OnePerTick(), FakeEncounterSource(), repo, coordinator, InMemorySettingsRepository(), tickMillis = 60_000)
        val arrivals = mutableListOf<ArrivalSummary>()
        backgroundScope.launch { session.arrivals.collect { arrivals += it } }
        session.start(backgroundScope)
        runCurrent()
        m.setOverride(DriveState.DRIVING)
        testScheduler.advanceTimeBy(60_001)
        m.setOverride(null)
        runCurrent()
        assertEquals(DriveState.PARKED, m.state.value)
        assertEquals(1, arrivals.size)
    }
}
