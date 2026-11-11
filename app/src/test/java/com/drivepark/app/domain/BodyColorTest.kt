package com.drivepark.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 몸 색: 기본 늑대 회색 + 몸 색판 16색(그리기 색판의 앞 16색). 예전 5색은 저장 값 호환용으로만 남는다. */
class BodyColorTest {
    @Test
    fun choices_areWolfGrayThenPalette() {
        assertEquals(17, BodyColor.CHOICES.size)
        assertEquals(BodyColor.PURPLE, BodyColor.CHOICES.first())
        assertEquals(BODY_PALETTE, BodyColor.CHOICES.drop(1).map { it.palette })
    }

    @Test
    fun legacyColors_haveNoPaletteValue() {
        listOf(BodyColor.RED, BodyColor.ORANGE, BodyColor.YELLOW, BodyColor.GREEN, BodyColor.BLUE, BodyColor.PURPLE)
            .forEach { assertNull(it.name, it.palette) }
    }
}
