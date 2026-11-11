package com.drivepark.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 인사 후 내비 자동 이동은 시동(기기 부팅) 한 번에 한 번만. */
class AutoNaviTest {
    private val boot = 1_700_000_000_000L

    @Test
    fun firstStartOfThisBoot_opensNavi() =
        assertTrue(shouldAutoOpenNavi(StartupMode.TO_NAVI, inSplit = false, bootMark = boot, lastAutoNaviBoot = null))

    @Test
    fun sameBootAgain_staysInPark() {
        // 앱 업데이트·재시작으로 같은 시동 안에서 다시 "새로 켜짐"이 되어도 내비로 끌고 가지 않는다(부팅 시각 계산은 몇 초 흔들린다).
        assertFalse(shouldAutoOpenNavi(StartupMode.TO_NAVI, false, bootMark = boot + 3_000, lastAutoNaviBoot = boot))
    }

    @Test
    fun newBoot_opensNaviAgain() =
        assertTrue(shouldAutoOpenNavi(StartupMode.TO_NAVI, false, bootMark = boot + 3_600_000, lastAutoNaviBoot = boot))

    @Test
    fun stayModeOrSplit_neverOpensNavi() {
        assertFalse(shouldAutoOpenNavi(StartupMode.STAY, false, boot, null))
        assertFalse(shouldAutoOpenNavi(StartupMode.TO_NAVI, inSplit = true, bootMark = boot, lastAutoNaviBoot = null))
    }
}
