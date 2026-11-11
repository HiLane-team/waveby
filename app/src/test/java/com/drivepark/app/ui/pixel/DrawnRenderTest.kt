package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.CharacterKind
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.BottomColor
import com.drivepark.app.domain.DRAWN_SHAPE
import com.drivepark.app.domain.Drawing
import com.drivepark.app.domain.EyeColor
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.HairColor
import com.drivepark.app.domain.Hat
import com.drivepark.app.domain.PersonLook
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** 직접 그린 그림이 엔진을 그대로 지나는지, 이상한 그림에도 꺼지지 않는지. */
class DrawnRenderTest {
    private val parts = CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.NONE)

    /** 늑대 그림을 붓 글자만 쓰는 형식으로. 늑대는 이미 K B W L O P만 쓴다. */
    private val wolfText = Shapes.WOLF.rows.joinToString("\n")

    private fun withCell(text: String, x: Int, y: Int, c: Char): String {
        val rows = text.split("\n").toMutableList()
        rows[y] = rows[y].substring(0, x) + c + rows[y].substring(x + 1)
        return rows.joinToString("\n")
    }

    @Test
    fun drawn_usesDrawingPixels() {
        // 왼쪽 귀 끝 옆 빈칸(2,0)을 테두리로 칠한다.
        val edited = withCell(wolfText, 2, 0, 'K')
        val wolf = CharacterRenderer.render(parts, Pose.IDLE, 0, shape = "wolf")
        val drawn = CharacterRenderer.render(parts, Pose.IDLE, 0, shape = DRAWN_SHAPE, drawing = edited)
        assertFalse(wolf.argb.contentEquals(drawn.argb))
        // 고치지 않은 그림은 늑대와 똑같이 그려진다(같은 엔진, 같은 색표).
        val same = CharacterRenderer.render(parts, Pose.IDLE, 0, shape = DRAWN_SHAPE, drawing = wolfText)
        assertArrayEquals(wolf.argb, same.argb)
    }

    @Test
    fun drawn_invalidFallsBackByKind() {
        val wolf = CharacterRenderer.render(parts, Pose.WALK, 1, shape = "wolf", kind = CharacterKind.PET)
        val bad = CharacterRenderer.render(parts, Pose.WALK, 1, shape = DRAWN_SHAPE, kind = CharacterKind.PET, drawing = "KKK")
        assertArrayEquals(wolf.argb, bad.argb)
        val none = CharacterRenderer.render(parts, Pose.WALK, 1, shape = DRAWN_SHAPE, kind = CharacterKind.PET, drawing = null)
        assertArrayEquals(wolf.argb, none.argb)
    }

    @Test
    fun weirdDrawings_neverThrow() {
        val blank = List(23) { ".".repeat(17) }
        val dot = blank.toMutableList().also { it[11] = ".".repeat(8) + "K" + ".".repeat(8) }
        val full = List(23) { "B".repeat(17) }
        val oneFoot = blank.toMutableList().also { for (y in 15..22) it[y] = "......KK........." }
        val rnd = Random(7)
        val letters = "KBWLOP.".toList()
        val randoms = List(300) { List(23) { String(CharArray(17) { letters[rnd.nextInt(letters.size)] }) } }
        val all = listOf(blank, dot, full, oneFoot) + randoms
        for ((i, rows) in all.withIndex()) {
            val text = rows.joinToString("\n")
            for (pose in Pose.entries) for (frame in 0..3) for (hat in Hat.entries) for (acc in Accessory.entries) {
                try {
                    CharacterRenderer.render(
                        CharacterParts(BodyColor.BLUE, Face.WINK, hat, acc), pose, frame,
                        eyesClosed = frame == 2, shape = DRAWN_SHAPE, drawing = text,
                    )
                } catch (e: Exception) {
                    throw AssertionError("그림 #$i, $pose, frame $frame, $hat, $acc 에서 예외", e)
                }
            }
        }
    }

    @Test
    fun tallEye_glassesThatCannotFitAreSkipped() {
        // 눈(O)이 머리부터 발까지 이어진 그림: 안경 테가 그림판에 들어갈 수 없으니 안경만 빼고 그린다.
        val rows = List(23) { y -> if (y == 22) "KK.............KK" else "....O............" }
        val stand = StandSprite(rows)
        val a = Anatomy.of(stand)
        val glasses = Outfits.accessory(Accessory.GLASSES, a, stand)
        assertTrue(glasses == null || (0 until glasses.height).all { y -> (0 until glasses.width).all { x -> glasses[x, y] == '.' } })
    }

    @Test
    fun drawingColors_matchRenderedWolf() {
        // 그림판에 보이는 붓 색은 실제로 그려지는 색과 같아야 한다.
        val colors = CharacterRenderer.drawingColors(BodyColor.BLUE, EyeColor.GREEN, PersonLook(), Drawing.DEFAULT_COLORS)
        assertEquals(Drawing.LETTERS - '.', colors.keys)
        val img = CharacterRenderer.render(parts.copy(body = BodyColor.BLUE), Pose.IDLE, 0, shape = "wolf")
        val used = img.argb.toSet()
        // 늑대는 K B W L O P만 쓴다.
        "KBWLOP".forEach { c -> assertTrue("$c 색이 그림에 없음", colors.getValue(c) in used) }
        assertFalse(colors['B'] == CharacterRenderer.drawingColors(BodyColor.PURPLE, EyeColor.GREEN, PersonLook(), Drawing.DEFAULT_COLORS)['B'])
    }

    /** 그림 줄 0의 (1,0) (2,0) (3,0)에 H N 1을 칠하고, 색 줄로 1 = 빨강. */
    private val hn1Text = withCell(withCell(withCell(wolfText, 1, 0, 'H'), 2, 0, 'N'), 3, 0, '1') + "\n1=#E5484D"

    /** 서 있는 그림 칸(x, y) → 그려진 그림 칸(엔진이 여백을 두므로 옮긴다). */
    private fun px(img: PixelImage, x: Int, y: Int) = img.argb[(y + Poser.PAD_TOP) * img.width + (x + Poser.PAD_X)]

    @Test
    fun drawn_hairBottomFixed_useChosenColors() {
        val look = PersonLook(hair = HairColor.BLOND, bottom = BottomColor.DENIM)
        val img = CharacterRenderer.render(parts, Pose.IDLE, 0, shape = DRAWN_SHAPE, look = look, drawing = hn1Text)
        assertEquals(0xFFE2B85C.toInt(), px(img, 1, 0))
        assertEquals(0xFF5B7DB1.toInt(), px(img, 2, 0))
        assertEquals(0xFFE5484D.toInt(), px(img, 3, 0))
    }

    @Test
    fun drawingColors_matchRender() {
        val look = PersonLook(hair = HairColor.BLOND, bottom = BottomColor.DENIM)
        val colors = CharacterRenderer.drawingColors(BodyColor.PURPLE, EyeColor.BLUE, look, mapOf('1' to 0xFFE5484D.toInt()) + Drawing.DEFAULT_COLORS.minus('1'))
        val img = CharacterRenderer.render(parts.copy(eye = EyeColor.BLUE), Pose.IDLE, 0, shape = DRAWN_SHAPE, look = look, drawing = hn1Text)
        assertEquals(colors['H'], px(img, 1, 0))
        assertEquals(colors['N'], px(img, 2, 0))
        assertEquals(colors['1'], px(img, 3, 0))
        assertTrue(colors.getValue('O') in img.argb.toSet())
    }
}
