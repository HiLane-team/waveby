package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.Drawing
import com.drivepark.app.domain.GridSize
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawingLettersTest {
    @Test
    fun letters_areKnownToEngine() {
        assertTrue(StandSprite.LETTERS.containsAll(Drawing.LETTERS))
        assertTrue(GridSize.BASIC.w == StandSprite.WIDTH && GridSize.BASIC.h == StandSprite.HEIGHT)
    }
}
