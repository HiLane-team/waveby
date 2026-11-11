package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.EyeColor
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.Hat
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/** 고른 눈 색이 눈 칸에만 칠해지고, 지역 캐릭터는 눈 색을 따르지 않는지. */
class EyeColorRenderTest {
    private val parts = CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.NONE)

    private val expected = mapOf(
        EyeColor.GREEN to 0xFF3DBE6B.toInt(), EyeColor.BLUE to 0xFF3B82F6.toInt(), EyeColor.BROWN to 0xFF8A5A3C.toInt(),
        EyeColor.BLACK to 0xFF111114.toInt(), EyeColor.PURPLE to 0xFF8B5CF6.toInt(), EyeColor.GOLD to 0xFFF5B301.toInt(),
    )

    /** 늑대 서 있는 그림의 눈(O) 칸 → 그려진 그림의 칸 번호. */
    private val eyeIndexes: List<Int> = Shapes.WOLF.rows.flatMapIndexed { y, r ->
        r.mapIndexedNotNull { x, c -> if (c == 'O') (y + Poser.PAD_TOP) * Poser.CANVAS_W + (x + Poser.PAD_X) else null }
    }

    @Test
    fun eyePixels_followEyeColor() {
        val green = CharacterRenderer.render(parts, Pose.IDLE, 0, shape = "wolf")
        expected.forEach { (eye, argb) ->
            val img = CharacterRenderer.render(parts.copy(eye = eye), Pose.IDLE, 0, shape = "wolf")
            eyeIndexes.forEach { i -> assertEquals("$eye", argb, img.argb[i]) }
            img.argb.indices.filter { it !in eyeIndexes }.forEach { i -> assertEquals("$eye 눈 밖 칸 $i", green.argb[i], img.argb[i]) }
        }
    }

    @Test
    fun mascot_ignoresEyeColor() {
        // 늑대 그림의 몸 색(B)을 고정 색 1로 바꾼 테스트용 지역 캐릭터.
        val stand = StandSprite(Shapes.WOLF.rows.map { it.replace('B', '1') })
        val def = ShapeDef("test_mascot", ShapeKind.MASCOT, stand, fixed = mapOf('1' to 0xFFB5653A.toInt()))
        val anatomy = Anatomy.of(stand)
        val green = CharacterRenderer.renderStand(def, anatomy, parts, Pose.IDLE, 0)
        val blue = CharacterRenderer.renderStand(def, anatomy, parts.copy(eye = EyeColor.BLUE), Pose.IDLE, 0)
        assertArrayEquals(green.argb, blue.argb)
    }
}
