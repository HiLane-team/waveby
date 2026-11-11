package com.drivepark.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CharacterTest {
    private val parts = CharacterParts(BodyColor.RED, Face.SMILE, Hat.NONE, Accessory.NONE)

    @Test
    fun create_trimsAndAccepts() {
        val c = Character.create(parts, "  뚜벅이 ", "안녕!")
        assertEquals("뚜벅이", c?.nickname)
        assertEquals("안녕!", c?.greeting)
    }

    @Test
    fun create_rejectsEmptyOrTooLong() {
        assertNull(Character.create(parts, "", "안녕"))
        assertNull(Character.create(parts, "   ", "안녕"))
        assertNull(Character.create(parts, "가".repeat(11), "안녕"))
        assertNull(Character.create(parts, "뚜벅이", ""))
        assertNull(Character.create(parts, "뚜벅이", "가".repeat(31)))
    }

    @Test
    fun create_acceptsMaxLengths() {
        assertNotNull(Character.create(parts, "가".repeat(10), "가".repeat(30)))
    }

    @Test
    fun parts_defaultEyeIsGreen() {
        assertEquals(EyeColor.GREEN, CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.NONE).eye)
    }
}
