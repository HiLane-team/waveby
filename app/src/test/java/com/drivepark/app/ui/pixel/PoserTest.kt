package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.Face
import com.drivepark.app.domain.GridSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PoserTest {
    private val wolf = Shapes.WOLF
    private val shapes = listOf(wolf) + TestShapes.ALL

    private fun pose(s: StandSprite, p: Pose, f: Int, face: Face = Face.SMILE, closed: Boolean = false, walk: WalkStyle = WalkStyle.DEFAULT) =
        Poser.pose(s, Anatomy.of(s), p, f, face, closed, walk)

    private fun painted(sp: PixelSprite) = sp.rows.indices.filter { y -> sp.rows[y].any { it != '.' } }

    @Test
    fun allPosesFitCanvasAndStayConnected() {
        for (s in shapes) for (p in Pose.entries) for (f in 0..3) for (closed in listOf(false, true)) for (face in Face.entries) {
            val sp = pose(s, p, f, face, closed).sprite
            assertEquals(Poser.CANVAS_W to Poser.CANVAS_H, sp.width to sp.height)
            val rows = painted(sp)
            val gaps = (rows.first()..rows.last()).filter { it !in rows }
            assertEquals("$p $f $face closed=$closed\n${sp.rows.joinToString("\n")}", emptyList<Int>(), gaps)
        }
    }

    @Test
    fun walk_liftsAlternatePairsAndRaisesBody() {
        val w = (0..3).map { pose(wolf, Pose.WALK, it).sprite.rows }
        assertEquals(w[0], w[2])
        assertNotEquals(w[1], w[3])
        assertEquals(painted(pose(wolf, Pose.WALK, 0).sprite).first() - 1, painted(pose(wolf, Pose.WALK, 1).sprite).first())
    }

    @Test
    fun walk_singleFootHops() {
        val sp = pose(TestShapes.NARROW, Pose.WALK, 1).sprite
        assertTrue(sp.rows[Poser.CANVAS_H - 1].all { it == '.' })
    }

    @Test
    fun noEyes_closeIsNoop() {
        assertEquals(pose(TestShapes.NO_EYES, Pose.IDLE, 0).sprite.rows, pose(TestShapes.NO_EYES, Pose.IDLE, 0, closed = true).sprite.rows)
    }

    @Test
    fun eyesClosed_wolfMiddleIsInk() {
        val sp = pose(wolf, Pose.IDLE, 0, closed = true).sprite
        assertEquals('o', sp[3 + Poser.PAD_X, 10 + Poser.PAD_TOP])
        assertNotEquals('E', sp[3 + Poser.PAD_X, 9 + Poser.PAD_TOP])
    }

    @Test
    fun sit_isLowerThanIdle() {
        for (s in shapes) assertTrue(painted(pose(s, Pose.SIT, 0).sprite).first() > painted(pose(s, Pose.IDLE, 0).sprite).first())
    }

    @Test
    fun doze_nodsWithEyesClosed() {
        val a = pose(wolf, Pose.DOZE, 0).sprite
        val b = pose(wolf, Pose.DOZE, 1).sprite
        assertNotEquals(a.rows, b.rows)
        listOf(a, b).forEach { sp -> assertTrue(sp.rows.none { 'E' in it }) }
    }

    @Test
    fun crouchIsOneLower_airHasNoSoleUnderFeet() {
        for (s in shapes) {
            assertEquals(painted(pose(s, Pose.IDLE, 0).sprite).first() + 1, painted(pose(s, Pose.CROUCH, 0).sprite).first())
            // 공중: 다리 있는 모양만 발을 든다(다리 없는 통통 모양은 그림 그대로, 공원이 통째로 띄운다).
            if (Anatomy.of(s).gait == Gait.LEGS) assertTrue(pose(s, Pose.AIR, 0).sprite.rows[Poser.CANVAS_H - 1].all { it == '.' })
        }
    }

    @Test
    fun headDy_table() {
        assertEquals(listOf(0, 1, 0, 1), (0..3).map { pose(wolf, Pose.IDLE, it).headDy })
        assertEquals(listOf(0, -1, 0, -1), (0..3).map { pose(wolf, Pose.WALK, it).headDy })
        assertEquals(3, pose(wolf, Pose.SIT, 0).headDy)
        assertEquals(listOf(3, 4), (0..1).map { pose(wolf, Pose.DOZE, it).headDy })
        assertEquals(1, pose(wolf, Pose.CROUCH, 0).headDy)
        assertEquals(0, pose(wolf, Pose.AIR, 0).headDy)
    }

    @Test
    fun sit_dropsLinesFromBodyLines() {
        // 늑대 기본(14, 20): 17·18·20줄을 뺀다 → 서기보다 칠한 줄이 3줄 적고 아래로 3칸.
        val s = TestShapes.BIPED
        val idle = painted(pose(s, Pose.IDLE, 0).sprite)
        val sit = painted(Poser.pose(s, Anatomy.of(s), Pose.SIT, 0).sprite)
        assertEquals(idle.last(), sit.last())
        assertEquals(idle.first() + 3, sit.first())
        // 몸 구역 (14, 19)면 16·17·19줄을 뺀다: 원래 18줄(몸)이 남아 15줄 아래로 옮겨진다.
        val custom = Poser.pose(s, Anatomy.of(s, BodyLines(14, 19)), Pose.SIT, 0).sprite
        val base = Poser.pose(s, Anatomy.of(s), Pose.SIT, 0).sprite
        assertNotEquals(base.rows, custom.rows)
    }

    @Test
    fun hopWalk_raisesWholeBodyTwo() {
        val s = TestShapes.sized(GridSize.ROUND, legless = true)
        val a = Anatomy.of(s)
        assertEquals(Gait.HOP, a.gait)
        val idle = Poser.pose(s, a, Pose.IDLE, 0).sprite
        val up = Poser.pose(s, a, Pose.WALK, 1)
        assertEquals(-2, up.headDy)
        // 1장 = 서기 그림이 통째로 2칸 위.
        assertEquals(idle.rows.drop(2), up.sprite.rows.dropLast(2))
        assertEquals(idle.rows, Poser.pose(s, a, Pose.WALK, 0).sprite.rows)
        assertEquals(idle.rows, Poser.pose(s, a, Pose.WALK, 2).sprite.rows)
    }

    @Test
    fun hopSit_lowersOne() {
        val s = TestShapes.sized(GridSize.ROUND, legless = true)
        val a = Anatomy.of(s)
        val idle = painted(Poser.pose(s, a, Pose.IDLE, 0).sprite)
        val sit = Poser.pose(s, a, Pose.SIT, 0)
        assertEquals(1, sit.headDy)
        assertEquals(idle.size - 1, painted(sit.sprite).size)
        assertEquals(idle.last(), painted(sit.sprite).last())
        assertEquals(2, Poser.pose(s, a, Pose.DOZE, 1).headDy)
        assertEquals(1, Poser.pose(s, a, Pose.CROUCH, 0).headDy)
    }

    @Test
    fun canvas_perSize() {
        val wide = Poser.pose(TestShapes.sized(GridSize.WIDE, false), Anatomy.of(TestShapes.sized(GridSize.WIDE, false)), Pose.IDLE, 0).sprite
        assertEquals(29 to 20, wide.width to wide.height)
        val tall = TestShapes.sized(GridSize.TALL, false)
        val t = Poser.pose(tall, Anatomy.of(tall), Pose.IDLE, 0).sprite
        assertEquals(21 to 28, t.width to t.height)
    }

    @Test
    fun nod_usesHeadEnd() {
        // 서기 1장(끄덕임): 머리 끝(16)까지 1칸 내려간다. 기본(14)이면 15·16줄은 그대로.
        val s = TestShapes.BIPED
        val custom = Poser.pose(s, Anatomy.of(s, BodyLines(16, 20)), Pose.IDLE, 1).sprite
        val base = Poser.pose(s, Anatomy.of(s), Pose.IDLE, 1).sprite
        assertNotEquals(base.rows, custom.rows)
        // 원래 16줄(몸) → custom에서는 17줄로 내려감(그림판 위 여백 1칸 → 화면 줄 +1).
        assertEquals(s.rows[16].replace('B', 'D').replace('K', 'o'), custom.rows[17 + Poser.PAD_TOP].substring(Poser.PAD_X, Poser.PAD_X + 17))
    }

    // 코 자동 위치: 그림에 코(P)가 없으면 두 눈 사이(왼쪽으로 내림)·눈 아래 한 줄을 코로 본다(이호랑 기준).

    @Test
    fun noseFromEyes_matchesIhorang() {
        // 이호랑의 코(5, 12) = 눈(3열·8열, 9~11줄) 사이 · 눈 아래 한 줄.
        assertEquals(5 to 12, Poser.noseAnchor(wolf, Anatomy.of(wolf)))
        val noNose = StandSprite(wolf.rows.map { it.replace('P', 'W') })
        assertEquals(5 to 12, Poser.noseAnchor(noNose, Anatomy.of(noNose)))
    }

    @Test
    fun surprised_withoutNose_opensMouthLikeIhorang() {
        val noNose = StandSprite(wolf.rows.map { it.replace('P', 'W') })
        val smile = pose(noNose, Pose.IDLE, 0, Face.SMILE).sprite
        val surprised = pose(noNose, Pose.IDLE, 0, Face.SURPRISED).sprite
        assertNotEquals(smile.rows, surprised.rows)
        // 입은 이호랑과 같은 칸(5, 14)에 생긴다.
        val wolfMouth = pose(wolf, Pose.IDLE, 0, Face.SURPRISED).sprite.rows
        val row = 14 + Poser.PAD_TOP
        assertEquals(wolfMouth[row][5 + Poser.PAD_X], surprised.rows[row][5 + Poser.PAD_X])
    }

    @Test
    fun surprised_person_opensMouth() {
        // 사람은 코 기준점을 (7, 10)으로 정해 두어, 놀람이면 입(7, 13)이 위로 한 칸 길어진다("오" 모양, 턱에 붙지 않게).
        val def = Shapes.def("person")
        val a = Shapes.anatomy("person")
        fun cell(face: Face, x: Int, y: Int) =
            Poser.pose(def.stand, a, Pose.IDLE, 0, face, nose = def.nose).sprite.rows[y + Poser.PAD_TOP][x + Poser.PAD_X]
        assertEquals('o', cell(Face.SMILE, 7, 13))
        assertNotEquals('o', cell(Face.SMILE, 7, 12))
        assertEquals('o', cell(Face.SURPRISED, 7, 13))
        assertEquals('o', cell(Face.SURPRISED, 7, 12))
        assertNotEquals('o', cell(Face.SURPRISED, 6, 13)) // 옆으로 넓어지지 않는다
    }

    @Test
    fun surprised_ihorangMouth_staysOneCell() {
        // 이호랑의 입(5, 14)은 머리 구역 마지막 줄이라 아래로 늘지 않는다(고정본과 같음).
        val sp = pose(wolf, Pose.IDLE, 0, Face.SURPRISED).sprite.rows
        assertEquals('o', sp[14 + Poser.PAD_TOP][5 + Poser.PAD_X])
        assertNotEquals('o', sp[15 + Poser.PAD_TOP][5 + Poser.PAD_X])
    }

    @Test
    fun noseFromEyes_noEyes_isNull() {
        val blank = StandSprite(wolf.rows.map { it.replace('P', 'W').replace('O', 'W') })
        assertEquals(null, Poser.noseAnchor(blank, Anatomy.of(blank)))
    }
}
