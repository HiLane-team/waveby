package com.drivepark.app.drive

import com.drivepark.app.domain.SplitPane
import org.junit.Assert.assertEquals
import org.junit.Test

class SplitPlacementTest {
    private val display = 2560

    @Test
    fun paneOf_readsPleosLayout() {
        // Pleos 3분할: 차량 화면 0~853, 가운데 853~1694, 오른쪽 1718~2559. 분할 안 함: 853~2535
        assertEquals(SplitPane.MIDDLE, SplitPlacement.paneOf(853, 1694, display))
        assertEquals(SplitPane.RIGHT, SplitPlacement.paneOf(1718, 2559, display))
        assertEquals(SplitPane.NONE, SplitPlacement.paneOf(853, 2535, display))
    }

    @Test
    fun needsSwap_onlyWhenSplitAndDifferentFromMemory() {
        assertEquals(true, SplitPlacement.needsSwap(current = SplitPane.MIDDLE, remembered = SplitPane.RIGHT))
        assertEquals(true, SplitPlacement.needsSwap(current = SplitPane.RIGHT, remembered = SplitPane.MIDDLE))
        assertEquals(false, SplitPlacement.needsSwap(current = SplitPane.RIGHT, remembered = SplitPane.RIGHT))
        assertEquals("분할이 아니면 건드리지 않는다", false, SplitPlacement.needsSwap(current = SplitPane.NONE, remembered = SplitPane.RIGHT))
    }
}
