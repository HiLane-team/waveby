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
    private val W = Poser.CANVAS_W
    private val H = Poser.CANVAS_H
    private val wolf = CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.NONE)

    /** 격자(17×23) 좌표 → 그림판 배열 위치. */
    private fun idx(x: Int, y: Int) = (y + Poser.PAD_TOP) * W + x + Poser.PAD_X

    /** 늑대 몸 장면(동작 엔진). */
    private fun body(pose: Pose, frame: Int, face: Face = Face.SMILE, walk: WalkStyle = WalkStyle.DEFAULT) =
        Poser.pose(Shapes.WOLF, Shapes.anatomy("wolf"), pose, frame, face, false, walk).sprite

    /** 늑대의 모든 몸 장면 + 모자·소품 덧그림. */
    private fun wolfSprites(): List<PixelSprite> {
        val a = Shapes.anatomy("wolf")
        return Pose.entries.flatMap { p -> Face.entries.flatMap { f -> (0..3).map { body(p, it, f) } } } +
            Hat.entries.mapNotNull { Outfits.hat(it, a) } + Accessory.entries.mapNotNull { Outfits.accessory(it, a, Shapes.WOLF) }
    }

    /** 서기 0장에서 슬롯이 처음 나오는 그림판 좌표. */
    private fun firstPixel(slot: Char): Pair<Int, Int> {
        body(Pose.IDLE, 0).rows.forEachIndexed { y, row -> val x = row.indexOf(slot); if (x >= 0) return x to y }
        error("no $slot")
    }

    @Test
    fun wolfIs17x23GridOn19x24Canvas_mascotsAre32x32_andUseKnownSlots() {
        assertEquals(17 to 23, StandSprite.WIDTH to StandSprite.HEIGHT)
        wolfSprites().forEach { assertEquals(19 to 24, it.width to it.height) }
        MascotSprites.all().forEach { assertEquals(32 to 32, it.width to it.height) }
        (wolfSprites() + MascotSprites.all()).forEach { sprite ->
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
        val (x, y) = firstPixel('D')
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
        assertEquals(3, walk.toSet().size) // 0·2는 두 발이 땅에(같은 모양), 1은 앞발, 3은 뒷발을 든다
        assertEquals(walk[0], walk[2])
        assertNotEquals(walk[1], walk[3])
        assertNotEquals(CharacterRenderer.render(wolf, Pose.IDLE, 0).argb.toList(), CharacterRenderer.render(wolf, Pose.IDLE, 1).argb.toList())
    }

    @Test
    fun walk_keepsPixelsThatLeaveTheGrid() {
        // 격자 밖으로 나간 칸은 여백에 그려져야 한다(잘리면 안 된다). 판 밖이면 toSprite가 예외를 던진다.
        val up = body(Pose.WALK, 1) // 몸이 1칸 올라감 → 귀 끝이 격자 -1줄(판 0줄)
        assert(up.rows[0].any { it != '.' }) { "걷기 1장: 귀 끝이 잘렸다" }
    }

    @Test
    fun walk_keepsBodyAndTailConnected() {
        val base = body(Pose.IDLE, 0)
        // 꼬리 살랑을 끈 모양으로 본다(살랑은 꼬리를 일부러 1칸 늘린다: walkStyles 테스트).
        (0..3).map { body(Pose.WALK, it, walk = WalkStyle(lift = 2, tailWag = false)) }.forEachIndexed { f, s ->
            // 첫 칸이 있는 줄부터 발바닥까지 빈 줄이 없어야 한다(몸통·다리가 끊기지 않음).
            val filled = s.rows.map { r -> r.any { it != '.' } }
            val top = filled.indexOf(true)
            assert(filled.drop(top).all { it }) { "걷기 ${f}장: 중간에 빈 줄이 있다" }
            // 꼬리는 다리를 따라 좌우로 움직이지 않는다(오른쪽 끝 열이 서 있을 때와 같다).
            fun rightEdge(sp: PixelSprite) = sp.rows.maxOf { it.indexOfLast { c -> c != '.' } }
            assertEquals("걷기 ${f}장: 꼬리가 옆으로 움직였다", rightEdge(base), rightEdge(s))
        }
    }

    @Test
    fun walkStyles_liftHigherAndWagTail() {
        val one = body(Pose.WALK, 1, walk = WalkStyle(lift = 1))
        val two = body(Pose.WALK, 1, walk = WalkStyle(lift = 2))
        assertNotEquals(one.rows, two.rows)
        val wag = body(Pose.WALK, 1, walk = WalkStyle(tailWag = true))
        // 꼬리가 1칸 바깥으로(판의 마지막 열까지) 나가되 잘리지 않는다.
        assert(wag.rows.any { it[Poser.CANVAS_W - 1] != '.' }) { "꼬리 살랑: 꼬리 끝이 안 보인다" }
        // 발 2칸에 꼬리까지 켜도 모든 장이 그림판 안에 그려진다(밖이면 toSprite가 예외).
        (0..3).forEach { body(Pose.WALK, it, walk = WalkStyle(lift = 2, tailWag = true)) }
    }

    @Test
    fun noPoseHasAnEmptyRowInsideTheBody() {
        for (pose in Pose.entries) for (f in 0..3) {
            val rows = body(pose, f).rows
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
