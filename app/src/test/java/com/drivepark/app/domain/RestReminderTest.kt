package com.drivepark.app.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RestReminderTest {
    private val state = MutableStateFlow(DriveState.PARKED)
    private val enabled = MutableStateFlow(true)
    private var hour = 10
    private val twoHours = 120 * 60_000L
    private lateinit var reminder: RestReminder
    private var suggestions = 0

    private fun TestScope.start() {
        reminder = RestReminder(state, enabled) { hour }
        reminder.start(backgroundScope)
        backgroundScope.launch { reminder.suggestions.collect { suggestions++ } }
        runCurrent()
    }

    @Test
    fun threshold_isShorterInDrowsyHours() {
        assertEquals(120, restThresholdMinutes(10))
        assertEquals(120, restThresholdMinutes(20))
        assertEquals(90, restThresholdMinutes(14))
        assertEquals(90, restThresholdMinutes(15))
        assertEquals(120, restThresholdMinutes(16))
        assertEquals(90, restThresholdMinutes(2))
        assertEquals(90, restThresholdMinutes(5))
        assertEquals(120, restThresholdMinutes(6))
    }

    @Test
    fun drivingTwoHours_becomesDue_thenParkSuggestsOnce() = runTest {
        start()
        state.value = DriveState.DRIVING; runCurrent()
        advanceTimeBy(twoHours - 1); runCurrent()
        assertFalse(reminder.due.value)
        advanceTimeBy(2); runCurrent()
        assertTrue(reminder.due.value)
        assertEquals("주행 중에는 권유 화면을 띄우지 않는다", 0, suggestions)
        state.value = DriveState.PARKED; runCurrent()
        assertEquals(1, suggestions)
        assertFalse("P로 바꾸면 시계가 처음부터", reminder.due.value)
    }

    @Test
    fun shortDrive_parkDoesNotSuggest() = runTest {
        start()
        state.value = DriveState.DRIVING; runCurrent()
        advanceTimeBy(twoHours / 2); runCurrent()
        state.value = DriveState.PARKED; runCurrent()
        assertEquals(0, suggestions)
        // 다시 달리면 처음부터 센다.
        state.value = DriveState.DRIVING; runCurrent()
        advanceTimeBy(twoHours / 2 + 1); runCurrent()
        assertFalse(reminder.due.value)
    }

    @Test
    fun drowsyHour_usesNinetyMinutes() = runTest {
        hour = 14
        start()
        state.value = DriveState.DRIVING; runCurrent()
        advanceTimeBy(90 * 60_000L + 1); runCurrent()
        assertTrue(reminder.due.value)
    }

    @Test
    fun disabled_neverDueOrSuggests() = runTest {
        enabled.value = false
        start()
        state.value = DriveState.DRIVING; runCurrent()
        advanceTimeBy(twoHours * 2); runCurrent()
        assertFalse(reminder.due.value)
        state.value = DriveState.PARKED; runCurrent()
        assertEquals(0, suggestions)
    }

    @Test
    fun turningOffWhileDue_clears() = runTest {
        start()
        state.value = DriveState.DRIVING; runCurrent()
        advanceTimeBy(twoHours + 1); runCurrent()
        enabled.value = false; runCurrent()
        assertFalse(reminder.due.value)
        state.value = DriveState.PARKED; runCurrent()
        assertEquals(0, suggestions)
    }

    @Test
    fun demo_markDueNow_onlyWhileDriving() = runTest {
        start()
        reminder.markDueNow(); runCurrent()
        assertFalse("주차 중에는 시연 버튼이 아무것도 하지 않는다", reminder.due.value)
        state.value = DriveState.DRIVING; runCurrent()
        reminder.markDueNow(); runCurrent()
        assertTrue(reminder.due.value)
        state.value = DriveState.PARKED; runCurrent()
        assertEquals(1, suggestions)
    }
}
