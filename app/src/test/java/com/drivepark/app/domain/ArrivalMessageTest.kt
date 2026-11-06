package com.drivepark.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ArrivalMessageTest {
    private val three = listOf(testCharacter("a"), testCharacter("b"), testCharacter("c"))
    private val hoengseong = Regions.ALL.first { it.id == "hoengseong" }

    @Test
    fun arrivalMessage_friendsOnly() =
        assertEquals("이번 드라이브에서 3명이 놀러왔어요", arrivalMessage(ArrivalSummary(three, null)))

    @Test
    fun arrivalMessage_regionOnly() =
        assertEquals("횡성 한우을(를) 얻었어요!", arrivalMessage(ArrivalSummary(emptyList(), hoengseong)))

    @Test
    fun arrivalMessage_both() = assertEquals(
        "이번 드라이브에서 3명이 놀러왔어요\n횡성 한우을(를) 얻었어요!",
        arrivalMessage(ArrivalSummary(three, hoengseong)),
    )
}
