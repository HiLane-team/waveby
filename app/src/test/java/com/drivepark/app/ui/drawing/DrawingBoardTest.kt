package com.drivepark.app.ui.drawing

import com.drivepark.app.domain.Drawing
import com.drivepark.app.domain.GridSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawingBoardTest {
    @Test
    fun paint_andErase() {
        val b = DrawingBoard.blank().paint(3, 4, Brush.BODY)
        assertEquals('B', b.at(3, 4))
        val e = b.paint(3, 4, Brush.ERASER)
        assertEquals('.', e.at(3, 4))
        assertTrue(e.canUndo)
    }

    @Test
    fun paint_outsideIsIgnored() {
        val b = DrawingBoard.blank()
        assertEquals(b, b.paint(-1, 0, Brush.BODY))
        assertEquals(b, b.paint(17, 0, Brush.BODY))
        assertEquals(b, b.paint(0, 23, Brush.BODY))
    }

    @Test
    fun paint_sameLetterIsNotAnUndoStep() {
        val b = DrawingBoard.blank().paint(1, 1, Brush.BODY)
        val again = b.paint(1, 1, Brush.BODY)
        assertEquals(b, again)
    }

    @Test
    fun stroke_isOneUndo() {
        var b = DrawingBoard.blank().paint(0, 0, Brush.OUTLINE).beginStroke()
        b = b.paint(1, 1, Brush.BODY).paint(2, 1, Brush.BODY).paint(1, 1, Brush.LIGHT).paint(1, 1, Brush.BODY).endStroke()
        assertEquals('B', b.at(1, 1))
        val u = b.undo()
        assertEquals('.', u.at(1, 1))
        assertEquals('.', u.at(2, 1))
        assertEquals('K', u.at(0, 0))
    }

    @Test
    fun emptyStroke_addsNoUndo() {
        val b = DrawingBoard.blank().beginStroke().endStroke()
        assertFalse(b.canUndo)
    }

    @Test
    fun undo_limitIs50() {
        var b = DrawingBoard.blank()
        for (i in 0 until 60) b = b.paint(i % 17, i / 17, Brush.BODY)
        var steps = 0
        while (b.canUndo) { b = b.undo(); steps++ }
        assertEquals(50, steps)
        // 앞의 10칸은 되돌릴 수 없어 남는다.
        assertEquals('B', b.at(0, 0))
        assertEquals('.', b.at(10 % 17, 0))
        assertEquals(b, b.undo())
    }

    @Test
    fun clear_canBeUndone() {
        val b = DrawingBoard.blank().paint(5, 5, Brush.EYE)
        val c = b.clear()
        assertEquals('.', c.at(5, 5))
        assertEquals('O', c.undo().at(5, 5))
    }

    @Test
    fun fromStand_keepsHairBottomAndFixed() {
        val rows = List(23) { y ->
            when (y) {
                0 -> "HN1234KBWLOP....."
                else -> ".".repeat(17)
            }
        }
        val b = DrawingBoard.fromStand(rows, fixed = mapOf('1' to 0xFF123456.toInt()))
        assertEquals("HN1234KBWLOP.....", b.rows[0])
        assertEquals(0xFF123456.toInt(), b.colors['1'])
        assertEquals(Drawing.DEFAULT_COLORS['2'], b.colors['2'])
        assertFalse(b.canUndo)
    }

    @Test
    fun paint_newBrushes() {
        val b = DrawingBoard.blank().paint(0, 0, Brush.HAIR).paint(1, 0, Brush.BOTTOM).paint(2, 0, Brush.FIXED3)
        assertEquals("HN3", b.rows[0].take(3))
    }

    @Test
    fun colors_startAsDefaults() {
        assertEquals(Drawing.DEFAULT_COLORS, DrawingBoard.blank().colors)
    }

    @Test
    fun setColor_isOneUndo() {
        val painted = DrawingBoard.blank().paint(0, 0, Brush.FIXED1)
        val recolored = painted.setColor('1', 0xFFE5484D.toInt())
        assertEquals(0xFFE5484D.toInt(), recolored.colors['1'])
        val back = recolored.undo()
        assertEquals(Drawing.DEFAULT_COLORS['1'], back.colors['1'])
        assertEquals('1', back.at(0, 0))
        assertEquals('.', back.undo().at(0, 0))
    }

    @Test
    fun setColor_sameColor_noHistory() {
        val b = DrawingBoard.blank()
        assertFalse(b.setColor('1', Drawing.DEFAULT_COLORS.getValue('1')).canUndo)
    }

    @Test
    fun clear_keepsColors() {
        val b = DrawingBoard.blank().paint(0, 0, Brush.FIXED2).setColor('2', 0xFF14B8A6.toInt()).clear()
        assertEquals(0xFF14B8A6.toInt(), b.colors['2'])
        assertTrue(b.isBlank)
    }

    @Test
    fun text_roundTrip_withColors() {
        val b = DrawingBoard.blank().paint(0, 0, Brush.FIXED1).setColor('1', 0xFF84CC16.toInt())
        val again = DrawingBoard.fromText(b.toText())
        assertEquals(b.rows, again.rows)
        assertEquals(0xFF84CC16.toInt(), again.colors['1'])
    }

    @Test
    fun text_dropsColorLine_whenLetterErased() {
        val b = DrawingBoard.blank().paint(0, 0, Brush.FIXED1).setColor('1', 0xFF84CC16.toInt()).paint(0, 0, Brush.ERASER)
        assertEquals(23, b.toText().split("\n").size)
    }

    @Test
    fun toText_parses() {
        val b = DrawingBoard.blank().paint(0, 22, Brush.OUTLINE)
        assertNotNull(Drawing.parse(b.toText()))
    }

    @Test
    fun isBlank_onlyWhenNothingPainted() {
        assertTrue(DrawingBoard.blank().isBlank)
        assertFalse(DrawingBoard.blank().paint(3, 3, Brush.SHADE).isBlank)
    }

    @Test
    fun resized_bottomCenter() {
        val b = DrawingBoard.blank().paint(8, 22, Brush.OUTLINE)
        val round = b.resized(GridSize.ROUND)
        assertEquals(GridSize.ROUND, round.size)
        assertEquals('K', round.at(11, 22))
        val wide = b.resized(GridSize.WIDE)
        assertEquals('K', wide.at(13, 18))
    }

    @Test
    fun cropCount_counts() {
        val b = DrawingBoard.blank().paint(0, 0, Brush.OUTLINE).paint(1, 0, Brush.OUTLINE).paint(2, 0, Brush.OUTLINE)
            .paint(5, 10, Brush.OUTLINE)
        assertEquals(3, b.cropCount(GridSize.WIDE))
        assertEquals(0, b.cropCount(GridSize.ROUND))
        assertEquals(1, b.resized(GridSize.WIDE).rows.sumOf { r -> r.count { it != '.' } }) // 4칸 중 3칸 잘림
    }

    @Test
    fun resized_undoRestores() {
        val b = DrawingBoard.blank().paint(0, 0, Brush.FIXED1).setColor('1', 0xFF84CC16.toInt())
        val back = b.resized(GridSize.WIDE).undo()
        assertEquals(GridSize.BASIC, back.size)
        assertEquals(b.rows, back.rows)
        assertEquals(b.colors, back.colors)
    }

    @Test
    fun blank_andText_followSize() {
        val b = DrawingBoard.blank(GridSize.TALL).paint(18, 26, Brush.OUTLINE)
        assertEquals(GridSize.TALL, DrawingBoard.fromText(b.toText()).size)
        assertEquals(b, b.paint(19, 0, Brush.OUTLINE)) // 판 밖은 그대로
    }

    @Test
    fun cursor_staysInside() {
        assertEquals(Cursor(0, 0), Cursor(0, 0).moved(-3, 0, GridSize.BASIC))
        assertEquals(Cursor(16, 22), Cursor(16, 22).moved(5, 5, GridSize.BASIC))
        assertEquals(Cursor(26, 18), Cursor(20, 10).moved(9, 9, GridSize.WIDE))
    }

    @Test
    fun touch_findsTheCellUnderTheFinger() {
        // 한 칸 = 10px. 누른 자리의 칸, 판 밖은 null.
        assertEquals(Cursor(0, 0), cellAt(0f, 0f, 10f, GridSize.BASIC))
        assertEquals(Cursor(3, 2), cellAt(39.9f, 25f, 10f, GridSize.BASIC))
        assertEquals(Cursor(16, 22), cellAt(169f, 229f, 10f, GridSize.BASIC))
        assertEquals(null, cellAt(-1f, 5f, 10f, GridSize.BASIC))
        assertEquals(null, cellAt(170f, 5f, 10f, GridSize.BASIC))
        assertEquals(null, cellAt(5f, 230f, 10f, GridSize.BASIC))
    }

    @Test
    fun fastSwipe_leavesNoGaps() {
        // 손가락이 빨리 움직여 칸을 건너뛰어도, 두 칸 사이를 빈틈없이 잇는다(양 끝 포함).
        assertEquals(listOf(Cursor(2, 5)), cellsBetween(Cursor(2, 5), Cursor(2, 5)))
        assertEquals((0..6).map { Cursor(it, 1) }, cellsBetween(Cursor(0, 1), Cursor(6, 1)))
        assertEquals((0..4).map { Cursor(4 - it, 4 - it) }, cellsBetween(Cursor(4, 4), Cursor(0, 0)))
        val slope = cellsBetween(Cursor(0, 0), Cursor(6, 2))
        assertEquals(Cursor(0, 0), slope.first())
        assertEquals(Cursor(6, 2), slope.last())
        slope.zipWithNext().forEach { (a, b) ->
            assertTrue("$a→$b", kotlin.math.abs(a.x - b.x) <= 1 && kotlin.math.abs(a.y - b.y) <= 1)
        }
    }

    @Test
    fun fingerStroke_paintsEveryCellAsOneUndo() {
        val cells = cellsBetween(Cursor(1, 1), Cursor(5, 1))
        val b = cells.fold(DrawingBoard.blank().beginStroke()) { acc, c -> acc.paint(c.x, c.y, Brush.OUTLINE) }.endStroke()
        assertEquals(".KKKKK", b.rows[1].substring(0, 6))
        assertTrue(b.undo().isBlank)
    }

    @Test
    fun paintWhileMoving_isOneUndo() {
        val b = DrawingBoard.blank().beginStroke()
            .paint(1, 1, Brush.OUTLINE).paint(2, 1, Brush.OUTLINE).paint(3, 1, Brush.OUTLINE).endStroke()
        assertTrue(b.undo().isBlank)
    }

    @Test
    fun pickableBrushes_hideNose() {
        // 코는 엔진이 눈 위치로 정하므로 붓 목록에 없다(예전 그림의 P는 그대로 읽는다).
        assertTrue(Brush.NOSE !in Brush.PICKABLE)
        assertEquals(Brush.entries.size - 1, Brush.PICKABLE.size)
    }
}
