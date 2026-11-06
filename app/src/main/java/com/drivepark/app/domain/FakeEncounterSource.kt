package com.drivepark.app.domain

import kotlin.random.Random

/** 1단계용 가짜 만남 공급원. 무작위 캐릭터를 만든다. */
class FakeEncounterSource(private val random: Random = Random.Default) : EncounterSource {

    fun generate(count: Int): List<Character> = List(count) { randomCharacter() }

    /** 주행 1틱당 0~1명. */
    override suspend fun fetchNew(): List<Character> = generate(random.nextInt(0, 2))

    private fun randomCharacter(): Character {
        val parts = CharacterParts(
            body = BodyColor.entries.random(random),
            face = Face.entries.random(random),
            hat = Hat.entries.random(random),
            accessory = Accessory.entries.random(random),
        )
        return Character.create(parts, NICKNAMES.random(random), GREETINGS.random(random))
            ?: error("풀에 규칙을 어기는 문구가 있다")
    }

    companion object {
        val NICKNAMES = listOf(
            "드라이브왕", "출근길요정", "느긋한거북", "고속도로곰",
            "주말여행러", "신호등수호자", "라디오DJ", "휴게소미식가",
        )
        val GREETINGS = listOf(
            "좋은 하루 보내세요!", "안전운전 하세요~", "오늘도 화이팅!",
            "반가워요!", "즐거운 드라이브 되세요", "또 만나요!",
        )
    }
}
