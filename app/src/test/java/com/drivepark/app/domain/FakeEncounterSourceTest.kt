package com.drivepark.app.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FakeEncounterSourceTest {
    @Test
    fun generate_returnsRequestedCount() {
        val src = FakeEncounterSource()
        assertTrue(src.generate(0).isEmpty())
        assertEquals(5, src.generate(5).size)
    }

    @Test
    fun generate_producesValidCharacters() {
        FakeEncounterSource().generate(100).forEach {
            assertEquals(it, Character.create(it.parts, it.nickname, it.greeting))
        }
    }

    @Test
    fun fetchNew_returnsZeroOrOne() = runTest {
        val src = FakeEncounterSource(Random(42))
        repeat(50) { assertTrue(src.fetchNew().size in 0..1) }
    }
}
