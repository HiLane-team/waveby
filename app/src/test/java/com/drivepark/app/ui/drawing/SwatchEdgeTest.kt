package com.drivepark.app.ui.drawing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 색 칸이 배경에 묻히면 진한 테두리를 그린다(다크 모드 검정 칸, 라이트 모드 흰 칸). */
class SwatchEdgeTest {
    private val darkBg = 0xFF0F0F11.toInt()
    private val darkSurface = 0xFF1E1E22.toInt()
    private val lightBg = 0xFFFFFFFF.toInt()

    @Test fun `다크 배경의 검정 칸은 진한 테두리`() {
        assertTrue(needsStrongEdge(0xFF18181B.toInt(), darkBg))
        assertTrue(needsStrongEdge(0xFF18181B.toInt(), darkSurface))
    }

    @Test fun `라이트 배경의 흰 칸은 진한 테두리`() {
        assertTrue(needsStrongEdge(0xFFFFFFFF.toInt(), lightBg))
    }

    @Test fun `잘 보이는 색은 그대로`() {
        assertFalse(needsStrongEdge(0xFFE5484D.toInt(), darkBg)) // 빨강
        assertFalse(needsStrongEdge(0xFF52525B.toInt(), darkBg)) // 진회색
        assertFalse(needsStrongEdge(0xFF18181B.toInt(), lightBg)) // 라이트의 검정
        assertFalse(needsStrongEdge(0xFFFFFFFF.toInt(), darkBg)) // 다크의 흰색
    }

    @Test fun `대비 비율 계산(WCAG)`() {
        assertEquals(21.0, contrastRatio(0xFF000000.toInt(), 0xFFFFFFFF.toInt()), 0.01)
        assertEquals(1.0, contrastRatio(0xFF777777.toInt(), 0xFF777777.toInt()), 0.0001)
    }
}
