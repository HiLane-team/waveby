package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.GridSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** 크기별 기본 몸 구역, 서 있는 그림의 크기 읽기, 모양이 정한 몸 구역 검사. */
class BodyLinesTest {
    private fun blank(s: GridSize) = List(s.h) { ".".repeat(s.w) }

    @Test
    fun defaults() {
        assertEquals(BodyLines(14, 20), BodyLines.default(GridSize.BASIC))
        assertEquals(BodyLines(14, 20), BodyLines.default(GridSize.ROUND))
        assertEquals(BodyLines(11, 16), BodyLines.default(GridSize.WIDE))
        assertEquals(BodyLines(17, 24), BodyLines.default(GridSize.TALL))
    }

    @Test
    fun standSprite_readsSize() {
        GridSize.entries.forEach { s -> assertEquals(s, StandSprite(blank(s)).size) }
        assertThrows(IllegalArgumentException::class.java) { StandSprite(List(23) { ".".repeat(18) }) }
    }

    @Test
    fun shapeDef_rejectsBadLines() {
        val stand = StandSprite(blank(GridSize.BASIC))
        assertThrows(IllegalArgumentException::class.java) { ShapeDef("x", ShapeKind.ANIMAL, stand, lines = BodyLines(14, 15)) }
        assertThrows(IllegalArgumentException::class.java) { ShapeDef("x", ShapeKind.ANIMAL, stand, lines = BodyLines(0, 20)) }
        assertThrows(IllegalArgumentException::class.java) { ShapeDef("x", ShapeKind.ANIMAL, stand, lines = BodyLines(14, 23)) }
    }

    @Test
    fun shapeDef_customLines() {
        val def = ShapeDef("x", ShapeKind.ANIMAL, StandSprite(blank(GridSize.BASIC)), lines = BodyLines(16, 20), gait = Gait.HOP)
        assertEquals(BodyLines(16, 20), def.bodyLines)
        assertEquals(Gait.HOP, def.gait)
        assertEquals(BodyLines(14, 20), ShapeDef("y", ShapeKind.ANIMAL, StandSprite(blank(GridSize.BASIC))).bodyLines)
    }
}
