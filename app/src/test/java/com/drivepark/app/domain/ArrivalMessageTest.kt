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
        assertEquals("한우 음메롱을 얻었어요!", arrivalMessage(ArrivalSummary(emptyList(), hoengseong)))

    @Test
    fun arrivalMessage_both() = assertEquals(
        "이번 드라이브에서 3명이 놀러왔어요\n한우 음메롱을 얻었어요!",
        arrivalMessage(ArrivalSummary(three, hoengseong)),
    )

    @Test
    fun objectParticle_followsTheLastSyllable() {
        // 받침이 있으면 "을", 없으면 "를". 지역 캐릭터 이름(예: 한우 음메롱)에 붙는다.
        assertEquals("횡성 한우를", withObjectParticle("횡성 한우"))
        assertEquals("제주 감귤을", withObjectParticle("제주 감귤"))
        assertEquals("영덕 대게를", withObjectParticle("영덕 대게"))
        assertEquals("ABC를", withObjectParticle("ABC")) // 한글이 아니면 "를"
    }

    @Test
    fun arrivalMessage_noBracketParticleForAnyRegion() {
        Regions.ALL.forEach { r ->
            val msg = arrivalMessage(ArrivalSummary(emptyList(), r))
            assertEquals(false, msg.contains("(를)"))
        }
    }
}
