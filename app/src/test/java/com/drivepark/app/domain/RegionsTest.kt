package com.drivepark.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionsTest {
    @Test
    fun regions_haveFiveValidBoxes() {
        assertEquals(5, Regions.ALL.size)
        Regions.ALL.forEach { assertTrue(it.id, it.south < it.north && it.west < it.east) }
        assertEquals(5, Regions.ALL.map { it.id }.toSet().size)
    }

    @Test
    fun regions_doNotOverlap() {
        val all = Regions.ALL
        for (i in all.indices) for (j in i + 1 until all.size) {
            val a = all[i]
            val b = all[j]
            val overlaps = a.south <= b.north && b.south <= a.north &&
                a.west <= b.east && b.west <= a.east
            assertTrue("${a.id} overlaps ${b.id}", !overlaps)
        }
    }
}
