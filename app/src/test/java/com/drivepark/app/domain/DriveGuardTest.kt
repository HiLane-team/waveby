package com.drivepark.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveGuardTest {

    @Test
    fun windows_onlyWhenParked() {
        assertTrue(DriveGuard.mayShowWindow(DriveState.PARKED))
        assertFalse(DriveGuard.mayShowWindow(DriveState.DRIVING))
    }

    @Test
    fun whenParked_hidesValueWhileDriving() {
        // 2026-11-09 보안 점검: 도착 창을 닫지 않고 출발하면 주행 중에도 창이 잠금 화면 위에 남아 눌렸다.
        assertEquals("도착", DriveGuard.whenParked("도착", DriveState.PARKED))
        assertNull(DriveGuard.whenParked("도착", DriveState.DRIVING))
        assertNull(DriveGuard.whenParked<String>(null, DriveState.PARKED))
    }
}
