package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.Hat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** 색판 몸 색은 그 색 그대로 털(B)에 칠하고, 예전 6색은 예전 색 그대로. */
class BodyColorRenderTest {
    private val parts = CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.NONE)

    /** 늑대 서 있는 그림의 몸 색(B) 칸 하나 → 그려진 그림의 칸 번호. */
    private val bodyIndex: Int = Shapes.WOLF.rows.withIndex().firstNotNullOf { (y, r) ->
        r.indexOf('B').takeIf { it >= 0 }?.let { x -> (y + Poser.PAD_TOP) * Poser.CANVAS_W + (x + Poser.PAD_X) }
    }

    private fun bodyPixel(c: BodyColor) = CharacterRenderer.render(parts.copy(body = c), Pose.IDLE, 0, shape = "wolf").argb[bodyIndex]

    @Test
    fun paletteBody_isExactColor() {
        BodyColor.CHOICES.drop(1).forEach { c -> assertEquals(c.name, c.palette, bodyPixel(c)) }
    }

    @Test
    fun legacyBodies_unchanged() {
        assertEquals(0xFF585D69.toInt(), bodyPixel(BodyColor.PURPLE))
        assertEquals(0xFF8A4B45.toInt(), bodyPixel(BodyColor.RED))
        assertEquals(0xFF4A5F7E.toInt(), bodyPixel(BodyColor.BLUE))
    }

    @Test
    fun paletteBody_hasLighterBelly() {
        // 밝은 털(W)은 몸 색과 달라야 배·얼굴이 구분된다(흰색 몸 제외).
        val colors = CharacterRenderer.drawingColors(BodyColor.C_RED, com.drivepark.app.domain.EyeColor.GREEN, com.drivepark.app.domain.PersonLook(), com.drivepark.app.domain.Drawing.DEFAULT_COLORS)
        assertNotEquals(colors['B'], colors['W'])
    }
}
