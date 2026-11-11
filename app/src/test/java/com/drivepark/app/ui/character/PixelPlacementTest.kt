package com.drivepark.app.ui.character

import org.junit.Assert.assertEquals
import org.junit.Test

/** 화면에 그림을 놓는 계산: 같은 도트 크기(아래 가운데 맞춤) / 칸에 맞춤. */
class PixelPlacementTest {
    @Test
    fun basic_sameAsBefore() {
        // 17×23 그림판(19×24): 예전 식 floor(min(120/19, 120/24)) = 5, 95×120, 가운데.
        assertEquals(PixelPlacement(12, 0, 95, 120), pixelPlacement(120f, 120f, 19, 24, sameDot = true))
    }

    @Test
    fun wide_sameDotBottomCentered() {
        assertEquals(PixelPlacement(-12, 20, 145, 100), pixelPlacement(120f, 120f, 29, 20, sameDot = true))
    }

    @Test
    fun tall_overflowsUp() {
        assertEquals(PixelPlacement(7, -20, 105, 140), pixelPlacement(120f, 120f, 21, 28, sameDot = true))
    }

    @Test
    fun fit_whenNotSameDot() {
        // 칸에 맞춤: floor(min(120/29, 120/20)) = 4 → 116×80, 가운데.
        assertEquals(PixelPlacement(2, 20, 116, 80), pixelPlacement(120f, 120f, 29, 20, sameDot = false))
    }
}
