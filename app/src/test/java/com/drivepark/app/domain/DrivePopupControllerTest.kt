package com.drivepark.app.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingPopup : PopupWindowPort {
    val events = mutableListOf<String>()
    override fun show(text: String, sleepy: Boolean) { events += if (sleepy) "show:$text:sleepy" else "show:$text" }
    override fun hide() { events += "hide" }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DrivePopupControllerTest {
    private val state = MutableStateFlow(DriveState.PARKED)
    private val settings = MutableStateFlow(AppSettings())
    private val popup = RecordingPopup()
    private val rest = MutableStateFlow(false)

    private fun TestScope.start(canDraw: Boolean = true) {
        DrivePopupController(state, settings, popup, restDue = rest) { canDraw }.start(backgroundScope)
        runCurrent()
    }

    @Test
    fun initialParked_noShow() = runTest {
        start()
        assertTrue(popup.events.none { it.startsWith("show") })
    }

    @Test
    fun driving_showsSearchingText() = runTest {
        start()
        state.value = DriveState.DRIVING; runCurrent()
        assertEquals("show:● 친구를 찾는 중…", popup.events.last())
    }

    @Test
    fun parked_hides() = runTest {
        start()
        state.value = DriveState.DRIVING; runCurrent()
        state.value = DriveState.PARKED; runCurrent()
        assertEquals("hide", popup.events.last())
        assertEquals(1, popup.events.count { it == "hide" })
    }

    @Test
    fun noPermission_doesNothing() = runTest {
        start(canDraw = false)
        state.value = DriveState.DRIVING; runCurrent()
        state.value = DriveState.PARKED; runCurrent()
        assertTrue(popup.events.isEmpty())
    }

    @Test
    fun pauseChangesText_whileDriving() = runTest {
        start()
        state.value = DriveState.DRIVING; runCurrent()
        settings.value = AppSettings(pauseVisits = true); runCurrent()
        assertEquals("show:● 사색 중…", popup.events.last())
    }

    @Test
    fun restDue_showsSleepyRestText_evenWhilePaused() = runTest {
        start()
        state.value = DriveState.DRIVING; runCurrent()
        rest.value = true; runCurrent()
        assertEquals("show:● 쉬어 갈 때가 됐어요:sleepy", popup.events.last())
        settings.value = AppSettings(pauseVisits = true); runCurrent()
        assertEquals("쉴 때 문구가 사색 문구보다 먼저", "show:● 쉬어 갈 때가 됐어요:sleepy", popup.events.last())
        state.value = DriveState.PARKED; runCurrent()
        assertEquals("hide", popup.events.last())
    }
}
