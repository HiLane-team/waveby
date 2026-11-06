package com.drivepark.app.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BriefingPromptTest {
    private val hoengseong = Regions.ALL.first { it.id == "hoengseong" }
    private val summary = ArrivalSummary(listOf(testCharacter("드라이브왕"), testCharacter("b"), testCharacter("c")), hoengseong)

    @Test
    fun prompt_containsCountNamesRegion() {
        val p = BriefingPrompt.build(summary)
        assertTrue(p.contains("3"))
        assertTrue(p.contains("드라이브왕"))
        assertTrue(p.contains("횡성 한우"))
    }

    @Test
    fun prompt_hasNoCoordinatesOrDates() {
        val p = BriefingPrompt.build(summary)
        assertFalse(Regex("""\d{2,3}\.\d+""").containsMatchIn(p))
        assertFalse(Regex("""\d{4}-\d{2}""").containsMatchIn(p))
        assertFalse(Regex("""\d{1,2}:\d{2}""").containsMatchIn(p))
        assertFalse(Regex("""\d+시\s*\d*분?""").containsMatchIn(p))
    }
}
