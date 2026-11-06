package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.Hat
import com.drivepark.app.domain.Regions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PixelSpriteTest {
    private val W = WolfSprites.CANVAS_W
    private val H = WolfSprites.CANVAS_H
    private val wolf = CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.NONE)

    /** 격자(17×23) 좌표 → 그림판 배열 위치. */
    private fun idx(x: Int, y: Int) = (y + WolfSprites.PAD_TOP) * W + x + WolfSprites.PAD_X

    @Test
    fun wolfIs17x23GridOn19x24Canvas_mascotsAre32x32_andUseKnownSlots() {
        assertEquals(17 to 23, WolfSprites.WIDTH to WolfSprites.HEIGHT)
        WolfSprites.all().forEach { assertEquals(19 to 24, it.width to it.height) }
        MascotSprites.all().forEach { assertEquals(32 to 32, it.width to it.height) }
        (WolfSprites.all() + MascotSprites.all()).forEach { sprite ->
            sprite.rows.forEach { row ->
                row.forEach { ch -> assert(ch in PixelSprite.SLOTS) { "unknown slot '$ch'" } }
            }
        }
    }

    @Test
    fun everyPartCombination_renders() {
        for (b in BodyColor.entries) for (f in Face.entries) for (h in Hat.entries) for (a in Accessory.entries)
            for (pose in Pose.entries) for (frame in 0..3) {
                val img = CharacterRenderer.render(CharacterParts(b, f, h, a), pose, frame)
                assertEquals(W, img.width)
                assertEquals(W * H, img.argb.size)
            }
        Regions.ALL.forEach { assertEquals(32 * 32, CharacterRenderer.renderMascot(it.id).argb.size) }
    }

    @Test
    fun palette_swapsDarkFur() {
        val (x, y) = WolfSprites.firstPixel('D')
        val gray = CharacterRenderer.render(wolf, Pose.IDLE, 0)
        val red = CharacterRenderer.render(wolf.copy(body = BodyColor.RED), Pose.IDLE, 0)
        assertEquals(0xFF585D69.toInt(), gray.argb[y * W + x])
        assertEquals(0xFF8A4B45.toInt(), red.argb[y * W + x])
    }

    @Test
    fun face_followsBaekhoLayout_greenEyes_blackBrowsAndNose() {
        val img = CharacterRenderer.render(wolf, Pose.IDLE, 0)
        val eye = 0xFF3DBE6B.toInt()
        val ink = 0xFF2A2C30.toInt()
        for (y in 9..11) { assertEquals(eye, img.argb[idx(3, y)]); assertEquals(eye, img.argb[idx(8, y)]) }
        // 눈썹: 눈 바로 위 가로 2칸(ㄱ자). 코: (5,12) 검은 한 칸.
        listOf(2 to 8, 3 to 8, 7 to 8, 8 to 8, 5 to 12).forEach { (x, y) -> assertEquals(ink, img.argb[idx(x, y)]) }
    }

    @Test
    fun sleepyFace_closesEyes() {
        val img = CharacterRenderer.render(wolf.copy(face = Face.SLEEPY), Pose.IDLE, 0)
        assertNotEquals(0xFF3DBE6B.toInt(), img.argb[idx(3, 9)])
        assertEquals(0xFF2A2C30.toInt(), img.argb[idx(3, 10)])
    }

    @Test
    fun walk_hasFourDifferentFrames_and_idleBobsHead() {
        val walk = (0..3).map { CharacterRenderer.render(wolf, Pose.WALK, it).argb.toList() }
        assertEquals(3, walk.toSet().size) // 0·2는 다리가 반대, 1·3은 같은 모양(몸이 1칸 올라감)
        assertNotEquals(CharacterRenderer.render(wolf, Pose.IDLE, 0).argb.toList(), CharacterRenderer.render(wolf, Pose.IDLE, 1).argb.toList())
    }

    @Test
    fun walk_keepsPixelsThatLeaveTheGrid() {
        // 격자 밖으로 나간 칸은 여백에 그려져야 한다(잘리면 안 된다). 판 밖이면 toSprite가 예외를 던진다.
        val up = WolfSprites.body(Pose.WALK, 1) // 몸이 1칸 올라감 → 귀 끝이 격자 -1줄(판 0줄)
        assert(up.rows[0].any { it != '.' }) { "걷기 1장: 귀 끝이 잘렸다" }
        val step = WolfSprites.body(Pose.WALK, 0) // 오른쪽 다리 +1 → 꼬리 끝이 격자 17칸(판 18칸)
        assert(step.rows.any { it[WolfSprites.CANVAS_W - 1] != '.' }) { "걷기 0장: 꼬리 끝이 잘렸다" }
    }

    @Test
    fun noPoseHasAnEmptyRowInsideTheBody() {
        for (pose in Pose.entries) for (f in 0..3) {
            val rows = WolfSprites.body(pose, f).rows
            val painted = rows.indices.filter { y -> rows[y].any { it != '.' } }
            val gaps = (painted.first()..painted.last()).filter { it !in painted }
            assertEquals("$pose $f 몸통이 끊긴 줄: $gaps\n${rows.joinToString("\n")}", emptyList<Int>(), gaps)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidSprite_throws() {
        PixelSprite(List(31) { ".".repeat(32) })
    }
}
