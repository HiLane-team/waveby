package com.drivepark.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DrawingTest {
    private fun blank(rows: Int = 23, cols: Int = 17) = List(rows) { ".".repeat(cols) }.joinToString("\n")
    private val parts = CharacterParts(BodyColor.RED, Face.SMILE, Hat.NONE, Accessory.NONE)

    @Test
    fun parse_acceptsValid() {
        val d = Drawing.parse(blank())
        assertNotNull(d)
        assertEquals(23, d!!.rows.size)
        assertEquals(blank(), d.text())
    }

    @Test
    fun parse_rejectsWrongSize() {
        assertNull(Drawing.parse(blank(rows = 22)))
        assertNull(Drawing.parse(blank(cols = 16)))
        assertNull(Drawing.parse(blank(rows = 24)))
    }

    @Test
    fun parse_rejectsUnknownLetter() {
        val rows = blank().split("\n").toMutableList()
        rows[3] = "X" + rows[3].drop(1)
        assertNull(Drawing.parse(rows.joinToString("\n")))
    }

    /** 0줄 앞쪽 칸들에 [letters]를 차례로 넣은 23줄 그림. */
    private fun withLetters(letters: String): List<String> {
        val rows = blank().split("\n").toMutableList()
        rows[0] = letters + rows[0].drop(letters.length)
        return rows
    }

    @Test
    fun oldFormat_23Rows_parsesWithNoColors() {
        val text = withLetters("KBWLOP").joinToString("\n")
        val d = Drawing.parse(text)!!
        assertTrue(d.colors.isEmpty())
        assertEquals(text, d.text())
    }

    @Test
    fun newLetters_allowed() {
        assertNotNull(Drawing.parse(withLetters("HN1234").joinToString("\n")))
    }

    @Test
    fun colorLine_roundTrip() {
        val text = (withLetters("13") + "1=#8A5A3C 3=#E5484D").joinToString("\n")
        val d = Drawing.parse(text)!!
        assertEquals(mapOf('1' to 0xFF8A5A3C.toInt(), '3' to 0xFFE5484D.toInt()), d.colors)
        val again = Drawing.parse(d.text())!!
        assertEquals(d.rows, again.rows)
        assertEquals(d.colors, again.colors)
        assertEquals("1=#8A5A3C 3=#E5484D", d.text().split("\n").last())
    }

    @Test
    fun colorLine_lowercaseHex_accepted() {
        val d = Drawing.parse((withLetters("1") + "1=#8a5a3c").joinToString("\n"))!!
        assertEquals(0xFF8A5A3C.toInt(), d.colors['1'])
    }

    @Test
    fun usedLetterWithoutColor_getsDefault() {
        val d = Drawing.parse(withLetters("2").joinToString("\n"))!!
        assertEquals(0xFFF2A7B5.toInt(), d.colors['2'])
    }

    @Test
    fun unusedLetterColor_dropped() {
        val d = Drawing.parse((withLetters("K") + "4=#000000").joinToString("\n"))!!
        assertFalse('4' in d.colors)
        assertEquals(23, d.text().split("\n").size)
    }

    @Test
    fun badColorLine_returnsNull() {
        listOf("5=#FFFFFF", "1=#FFF", "1=#GGGGGG", "1=#111111 1=#222222", "1#111111").forEach { line ->
            assertNull(line, Drawing.parse((withLetters("1") + line).joinToString("\n")))
        }
    }

    @Test
    fun of_matchesParse() {
        val d = Drawing.of(withLetters("1"), mapOf('1' to 0xFF123456.toInt()))!!
        val again = Drawing.parse(d.text())!!
        assertEquals(d.rows, again.rows)
        assertEquals(mapOf('1' to 0xFF123456.toInt()), again.colors)
    }

    @Test
    fun palette_has20OpaqueColors() {
        assertEquals(20, DRAWING_PALETTE.size)
        assertTrue(DRAWING_PALETTE.all { it ushr 24 == 0xFF })
        assertEquals(20, DRAWING_PALETTE.toSet().size)
        assertTrue(DRAWING_PALETTE.containsAll(Drawing.DEFAULT_COLORS.values))
    }

    @Test
    fun palette_isBodyColorsThenPersonRow() {
        // 앞 16색 = 몸 색(옷 색), 뒤 4색 = 사람 색(기본 사람 캐릭터의 피부 밝음·중간 + 구릿빛 + 검은 머리).
        assertEquals(16, BODY_PALETTE.size)
        assertEquals(BODY_PALETTE, DRAWING_PALETTE.take(16))
        assertEquals(
            listOf(0xFFF6D7C3, 0xFFD9A27E, 0xFFB57A52, 0xFF3A3132).map { it.toInt() },
            DRAWING_PALETTE.drop(16),
        )
    }

    @Test
    fun parse_rejectsNull() {
        assertNull(Drawing.parse(null))
    }

    @Test
    fun create_drawnNeedsValidDrawing() {
        assertNull(Character.create(parts, "나", "안녕", shape = DRAWN_SHAPE, drawing = null))
        assertNull(Character.create(parts, "나", "안녕", shape = DRAWN_SHAPE, drawing = "KKK"))
        val ok = Character.create(parts, "나", "안녕", shape = DRAWN_SHAPE, drawing = blank())
        assertEquals(blank(), ok?.drawing)
    }

    @Test
    fun create_otherShapeKeepsDrawing() {
        val c = Character.create(parts, "나", "안녕", shape = "wolf", drawing = blank())
        assertEquals("wolf", c?.shape)
        assertEquals(blank(), c?.drawing)
    }

    /** w×h 빈 판에 (0, h-1) 한 칸만 테두리. */
    private fun sized(w: Int, h: Int): List<String> =
        List(h) { y -> if (y == h - 1) "K" + ".".repeat(w - 1) else ".".repeat(w) }

    @Test
    fun sizes_roundTrip() {
        GridSize.entries.forEach { s ->
            val d = Drawing.parse(sized(s.w, s.h).joinToString("\n"))!!
            assertEquals(s, d.size)
            val again = Drawing.parse(d.text())!!
            assertEquals(s, again.size)
            assertEquals(d.rows, again.rows)
        }
    }

    @Test
    fun oldBasic_stillParses() {
        assertEquals(GridSize.BASIC, Drawing.parse(blank())!!.size)
    }

    @Test
    fun unknownSize_null() {
        assertNull(Drawing.parse(sized(20, 20).joinToString("\n")))
        assertNull(Drawing.parse(sized(17, 22).joinToString("\n")))
    }

    @Test
    fun mixedWidths_null() {
        val rows = blank().split("\n").toMutableList()
        rows[5] = rows[5] + "."
        assertNull(Drawing.parse(rows.joinToString("\n")))
    }

    @Test
    fun colorLine_withNonBasicSize() {
        val rows = sized(27, 19).toMutableList()
        rows[0] = "1" + rows[0].drop(1)
        val d = Drawing.parse((rows + "1=#E5484D").joinToString("\n"))!!
        assertEquals(GridSize.WIDE, d.size)
        assertEquals(0xFFE5484D.toInt(), d.colors['1'])
        assertEquals(20, d.text().split("\n").size)
    }

    @Test
    fun gridSize_of() {
        assertEquals(GridSize.WIDE, GridSize.of(27, 19))
        assertNull(GridSize.of(18, 23))
    }
}
