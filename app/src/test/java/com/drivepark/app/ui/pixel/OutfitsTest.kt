package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.Hat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OutfitsTest {
    private val wolf = Shapes.WOLF
    private val shapes = listOf(wolf) + TestShapes.ALL + TestShapes.QUADRUPED

    private fun paintedRows(sp: PixelSprite) = sp.rows.indices.filter { y -> sp.rows[y].any { it != '.' } }
    private fun paintedCols(sp: PixelSprite, y: Int) = sp.rows[y].indices.filter { sp.rows[y][it] != '.' }

    @Test
    fun wolfCap_sitsOnCrownBetweenEars() {
        val a = Anatomy.of(wolf)
        val cap = Outfits.hat(Hat.CAP, a)!!
        val bottom = paintedRows(cap).last()
        assertEquals(4 + Poser.PAD_TOP, bottom)
        val cols = paintedCols(cap, paintedRows(cap).first())
        assertEquals((a.headCenterX + Poser.PAD_X).toDouble(), (cols.first() + cols.last()) / 2.0, 1.0)
    }

    @Test
    fun wolfOutfits_matchCurrentWolfOverlays() {
        // 엔진으로 옮기기 전 늑대 전용 덧그림(격자 좌표 그대로)과 같아야 한다.
        fun old(vararg rows: Pair<Int, String>) = MutableList(Poser.CANVAS_H) { ".".repeat(Poser.CANVAS_W) }
            .also { list -> rows.forEach { (y, r) -> list[y + Poser.PAD_TOP] = ".$r." } }
        val a = Anatomy.of(wolf)
        assertEquals(old(2 to ".....ooooo.......", 3 to "....o11111o......", 4 to "..ooo111111o....."), Outfits.hat(Hat.CAP, a)!!.rows)
        assertEquals(
            old(0 to "......33.........", 1 to ".....o33o........", 2 to "....o2222o.......", 3 to "...o222222o......", 4 to "...o333333o......"),
            Outfits.hat(Hat.BEANIE, a)!!.rows,
        )
        assertEquals(old(1 to "....4.4.4........", 2 to "....44444........", 3 to "....4H4H4........"), Outfits.hat(Hat.CROWN, a)!!.rows)
        assertEquals(
            old(14 to "...oRRRRRRRRRo...", 15 to "...oRRRRRRRRRo...", 16 to "...oRRo..........", 17 to "...oRRo.........."),
            Outfits.accessory(Accessory.SCARF, a, wolf)!!.rows,
        )
        assertEquals(
            old(0 to "..............BBB", 1 to ".............BBHB", 2 to ".............BBBB", 3 to "..............BB.", 4 to "...............o.", 5 to "...............o."),
            Outfits.accessory(Accessory.BALLOON, a, wolf)!!.rows,
        )
    }

    @Test
    fun glasses_ringEachEye() {
        for (s in listOf(wolf, TestShapes.BIPED)) {
            val a = Anatomy.of(s)
            val g = Outfits.accessory(Accessory.GLASSES, a, s)!!
            a.eyes.forEach { eye ->
                val top = eye.minBy { it.y }
                val bottom = eye.maxBy { it.y }
                assertEquals('o', g[top.x + Poser.PAD_X, top.y - 1 + Poser.PAD_TOP])
                assertEquals('o', g[bottom.x + Poser.PAD_X, bottom.y + 1 + Poser.PAD_TOP])
                eye.forEach { assertEquals('.', g[it.x + Poser.PAD_X, it.y + Poser.PAD_TOP]) }
            }
        }
    }

    @Test
    fun glasses_noEyes_null() {
        assertNull(Outfits.accessory(Accessory.GLASSES, Anatomy.of(TestShapes.NO_EYES), TestShapes.NO_EYES))
    }

    @Test
    fun allOutfitsFitCanvas() {
        for (s in shapes) {
            val a = Anatomy.of(s)
            for (h in Hat.entries) Outfits.hat(h, a)?.let { assertEquals(Poser.CANVAS_W to Poser.CANVAS_H, it.width to it.height) }
            for (acc in Accessory.entries) Outfits.accessory(acc, a, s)?.let { assertEquals(Poser.CANVAS_W to Poser.CANVAS_H, it.width to it.height) }
        }
    }

    @Test
    fun overlaysNeverLeaveCanvasWhileTheHeadMoves() {
        // 렌더러는 머리 이동(headDy)만큼 덧그림을 옮긴다. 그림판 밖으로 나가면 예외(조용히 잘리면 안 됨).
        val parts = com.drivepark.app.domain.CharacterParts(com.drivepark.app.domain.BodyColor.PURPLE, com.drivepark.app.domain.Face.SMILE, Hat.NONE, Accessory.NONE)
        for (s in shapes) for (h in Hat.entries) for (acc in Accessory.entries) for (p in Pose.entries) for (f in 0..3) {
            CharacterRenderer.renderStand(ShapeDef("test", ShapeKind.ANIMAL, s), Anatomy.of(s), parts.copy(hat = h, accessory = acc), p, f)
        }
    }

    @Test
    fun short_hatAboveItsOwnHead() {
        val a = Anatomy.of(TestShapes.SHORT)
        assertEquals(a.crownRow + Poser.PAD_TOP, paintedRows(Outfits.hat(Hat.CAP, a)!!).last())
    }

    @Test
    fun quadruped_crownIsOnTheHeadNotTheBack() {
        val a = Anatomy.of(TestShapes.QUADRUPED)
        assertTrue("crown ${a.crownRow}", a.crownRow <= 13)
        assertTrue("center ${a.headCenterX}", a.headCenterX <= 5)
    }
}
