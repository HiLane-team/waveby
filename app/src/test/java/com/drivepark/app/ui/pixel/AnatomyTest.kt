package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.GridSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnatomyTest {
    private val wolf = Shapes.WOLF

    @Test
    fun wolf_feetTailEyesCrown() {
        val a = Anatomy.of(wolf)
        assertEquals(listOf(3..6, 9..12), a.feet)
        assertEquals(listOf(3..6), a.pairA)
        assertEquals(listOf(9..12), a.pairB)
        assertTrue(a.tail.isNotEmpty())
        assertTrue(a.tail.all { it.x >= 13 })
        assertEquals(listOf(3, 8), a.eyes.map { e -> e.first().x })
        a.eyes.forEach { e -> assertEquals((9..11).toList(), e.map { it.y }.sorted()) }
        assertEquals(4, a.crownRow)
        assertEquals(7, a.headCenterX)
        assertTrue(a.hints.isEmpty())
    }

    @Test
    fun biped_noTail_twoEyes() {
        val a = Anatomy.of(TestShapes.BIPED)
        assertTrue(a.tail.isEmpty())
        assertEquals(2, a.feet.size)
        assertEquals(2, a.eyes.size)
    }

    @Test
    fun quadruped_pairsAlternate() {
        val a = Anatomy.of(TestShapes.QUADRUPED)
        assertEquals(4, a.feet.size)
        assertEquals(listOf(a.feet[0], a.feet[2]), a.pairA)
        assertEquals(listOf(a.feet[1], a.feet[3]), a.pairB)
        assertTrue(a.tail.isNotEmpty())
    }

    @Test
    fun edge_rightFootAtLastColumn_hasNoTail() {
        assertTrue(Anatomy.of(TestShapes.EDGE).tail.isEmpty())
    }

    @Test
    fun narrow_singleFoot() {
        val a = Anatomy.of(TestShapes.NARROW)
        assertEquals(1, a.feet.size)
        assertEquals(a.pairA, a.pairB)
    }

    @Test
    fun wideSingleRun_splitsInTwo() {
        assertEquals(listOf(4..6, 7..9), Anatomy.of(TestShapes.WIDE_RUN).feet)
    }

    @Test
    fun wolf_unchanged() {
        // 몸 구역을 값으로 바꾸기 전 늑대 몸 지도(2026-11-07 코드에서 뽑음). 이호랑은 하나도 바뀌면 안 된다.
        val a = Anatomy.of(wolf)
        assertEquals(listOf(3..6, 9..12), a.feet)
        assertEquals(
            "[(13,15), (13,16), (14,16), (13,17), (14,17), (15,17), (16,17), (13,18), (14,18), (15,18), (16,18), " +
                "(13,19), (14,19), (15,19), (16,19), (13,20), (14,20), (15,20), (16,20), (13,21), (14,21), (15,21)]",
            a.tail.sortedWith(compareBy({ it.y }, { it.x })).map { "(${it.x},${it.y})" }.toString(),
        )
        assertEquals(4, a.crownRow)
        assertEquals(7, a.headCenterX)
        assertEquals(16, a.headRight)
        assertEquals(Gait.LEGS, a.gait)
        assertEquals(BodyLines(14, 20), a.lines)
        assertEquals(GridSize.BASIC, a.size)
    }

    /** w×h 그림: 머리(테두리 상자 + 눈) 위쪽, 맨 아랫줄에 [sole]. */
    private fun sized(size: GridSize, sole: String): StandSprite {
        val rows = MutableList(size.h) { ".".repeat(size.w) }
        rows[2] = "..KKKKKK" + ".".repeat(size.w - 8)
        rows[3] = "..KOWWOK" + ".".repeat(size.w - 8)
        rows[4] = "..KKKKKK" + ".".repeat(size.w - 8)
        for (y in 5 until size.h - 1) rows[y] = "..KBBBBK" + ".".repeat(size.w - 8)
        rows[size.h - 1] = sole + ".".repeat(size.w - sole.length)
        return StandSprite(rows)
    }

    @Test
    fun hop_whenOneWideBlobOnSole() {
        val a = Anatomy.of(sized(GridSize.ROUND, "..KKKKKKKK"))
        assertEquals(Gait.HOP, a.gait)
        assertFalse(DrawingHint.NO_FEET in a.hints)
    }

    @Test
    fun legs_forFiveWideBlob() {
        val a = Anatomy.of(sized(GridSize.ROUND, "..KKKKK"))
        assertEquals(Gait.LEGS, a.gait)
        assertEquals(2, a.feet.size)
    }

    @Test
    fun wideRunFixture_isHopButKeepsSplitFeet() {
        // 폭 6 덩어리 하나: 걷는 방식은 통통, 발 계산(앉을 때 접는 다리 폭)은 예전 그대로.
        val a = Anatomy.of(TestShapes.WIDE_RUN)
        assertEquals(Gait.HOP, a.gait)
        assertEquals(listOf(4..6, 7..9), a.feet)
    }

    @Test
    fun gaitOverride() {
        assertEquals(Gait.LEGS, Anatomy.of(sized(GridSize.ROUND, "..KKKKKKKK"), gait = Gait.LEGS).gait)
    }

    @Test
    fun customHeadEnd_movesCrownSearch() {
        // 머리 끝 줄은 정수리 후보에서 빠진다(목·등 줄). 0~15줄이 비고 16줄만 넓으면: 기본(14)이면 16줄은 머리 밖, 16이면 빠짐.
        val rows = MutableList(23) { ".".repeat(17) }
        rows[15] = "..KKKKKKKKKKK...."
        rows[16] = ".KKKKKKKKKKKKKK.."
        rows[22] = "..KK..KK........."
        val s = StandSprite(rows)
        assertEquals(15, Anatomy.of(s, BodyLines(16, 20)).crownRow)
    }

    @Test
    fun wideSize_feetOnLastRow() {
        val a = Anatomy.of(sized(GridSize.WIDE, "..KK..KK"))
        assertEquals(2, a.feet.size)
        assertEquals(GridSize.WIDE, a.size)
        assertEquals(BodyLines(11, 16), a.lines)
    }

    @Test
    fun short_crownIsLow() {
        assertTrue(Anatomy.of(TestShapes.SHORT).crownRow >= 10)
    }

    @Test
    fun broken_reportsHints() {
        val hints = Anatomy.of(TestShapes.BROKEN).hints
        assertTrue(DrawingHint.NO_FEET in hints)
        assertTrue(DrawingHint.STRAY_PIXEL in hints)
        assertTrue(DrawingHint.NO_HEAD in Anatomy.of(TestShapes.NO_HEAD).hints)
    }

    @Test(expected = IllegalArgumentException::class)
    fun standSprite_rejectsWrongSize() {
        // 네 크기(17×23·23×23·27×19·19×27)가 아니면 만들 수 없다.
        StandSprite(List(22) { ".".repeat(17) })
    }

    @Test(expected = IllegalArgumentException::class)
    fun standSprite_rejectsUnknownLetter() {
        StandSprite(List(23) { if (it == 5) "X" + ".".repeat(16) else ".".repeat(17) })
    }
}
