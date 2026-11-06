package com.drivepark.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GreetingTest {
    @Test
    fun dayPart_boundaries() {
        assertEquals(DayPart.NIGHT, dayPartOf(4))
        assertEquals(DayPart.MORNING, dayPartOf(5))
        assertEquals(DayPart.MORNING, dayPartOf(10))
        assertEquals(DayPart.LUNCH, dayPartOf(11))
        assertEquals(DayPart.LUNCH, dayPartOf(13))
        assertEquals(DayPart.AFTERNOON, dayPartOf(14))
        assertEquals(DayPart.AFTERNOON, dayPartOf(17))
        assertEquals(DayPart.EVENING, dayPartOf(18))
        assertEquals(DayPart.EVENING, dayPartOf(21))
        assertEquals(DayPart.NIGHT, dayPartOf(22))
        assertEquals(DayPart.NIGHT, dayPartOf(0))
    }

    @Test
    fun greeting_usesNickname_andAPhraseOfThatDayPart() {
        for (hour in 0..23) {
            val text = startGreeting(hour, "이호랑", random = Random(hour))
            assertTrue(text, text.startsWith("이호랑님, "))
            assertTrue(text, text.removePrefix("이호랑님, ") in GREETINGS.getValue(dayPartOf(hour)))
        }
    }

    @Test
    fun greeting_withoutNickname_isJustThePhrase() {
        val text = startGreeting(8, " ", random = Random(1))
        assertTrue(text, text in GREETINGS.getValue(DayPart.MORNING))
    }

    @Test
    fun petCharacter_dropsNickname() {
        for (hour in 0..23) {
            val text = startGreeting(hour, "콩이", CharacterKind.PET, Random(hour))
            assertTrue(text, "님" !in text.substringBefore(","))
            assertTrue(text, text in GREETINGS.getValue(dayPartOf(hour)))
        }
    }

    @Test
    fun everyDayPart_hasAtLeastTwoPhrases() {
        DayPart.entries.forEach { assertTrue(it.name, GREETINGS.getValue(it).size >= 2) }
    }
}
