package com.drivepark.app.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ParkedIdleWatcherTest {
    private val state = MutableStateFlow(DriveState.PARKED)
    private val enabled = MutableStateFlow(true)
    private var fired = 0
    private val tenMin = 10 * 60_000L

    private fun TestScope.start() {
        ParkedIdleWatcher(state, enabled, tenMin) { fired++ }.start(backgroundScope)
        runCurrent()
    }

    @Test
    fun parkedTenMinutes_firesOnce() = runTest {
        start()
        advanceTimeBy(tenMin - 1); runCurrent()
        assertEquals(0, fired)
        advanceTimeBy(2); runCurrent()
        assertEquals(1, fired)
        advanceTimeBy(tenMin * 3); runCurrent()
        assertEquals("주차가 이어지는 동안 한 번만", 1, fired)
    }

    @Test
    fun drivingResetsTheClock() = runTest {
        start()
        advanceTimeBy(tenMin / 2); runCurrent()
        state.value = DriveState.DRIVING; runCurrent()
        advanceTimeBy(tenMin * 2); runCurrent()
        assertEquals("주행 중에는 띄우지 않는다", 0, fired)
        state.value = DriveState.PARKED; runCurrent()
        advanceTimeBy(tenMin + 1); runCurrent()
        assertEquals(1, fired)
    }

    @Test
    fun disabled_neverFires() = runTest {
        enabled.value = false
        start()
        advanceTimeBy(tenMin * 2); runCurrent()
        assertEquals(0, fired)
    }
}
