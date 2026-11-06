package com.drivepark.app.domain

import kotlin.random.Random

/** 하루 시간대. 시작 시각(포함)부터 다음 시간대 시작 전까지. */
enum class DayPart { MORNING, LUNCH, AFTERNOON, EVENING, NIGHT }

/** 아침 5~11시, 점심 11~14시, 오후 14~18시, 저녁 18~22시, 밤 22~5시. */
fun dayPartOf(hour: Int): DayPart = when (hour) {
    in 5 until 11 -> DayPart.MORNING
    in 11 until 14 -> DayPart.LUNCH
    in 14 until 18 -> DayPart.AFTERNOON
    in 18 until 22 -> DayPart.EVENING
    else -> DayPart.NIGHT
}

/** 시동 인사 문구. 시간대마다 하나를 고른다. */
val GREETINGS: Map<DayPart, List<String>> = mapOf(
    DayPart.MORNING to listOf("오늘도 반가워요", "상쾌한 아침이에요", "오늘은 어디로 떠나볼까요?"),
    DayPart.LUNCH to listOf("어떤 메뉴를 생각하셨나요?", "점심 드라이브 좋죠"),
    DayPart.AFTERNOON to listOf("한가한 오후예요", "잠깐 바람 쐬러 가볼까요?"),
    DayPart.EVENING to listOf("좋은 저녁이에요", "오늘 하루도 수고했어요"),
    DayPart.NIGHT to listOf("늦은 밤이에요, 천천히 가요", "조용한 밤길이에요"),
)

/**
 * 본인 캐릭터면 "닉네임님, 문구". 반려동물 캐릭터면 닉네임은 반려동물 이름이라 "○○님"을 빼고 문구만.
 * 닉네임이 비어 있어도 문구만.
 */
fun startGreeting(
    hour: Int,
    nickname: String,
    kind: CharacterKind = CharacterKind.PERSON,
    random: Random = Random.Default,
): String {
    val phrase = GREETINGS.getValue(dayPartOf(hour)).random(random)
    return if (kind == CharacterKind.PET || nickname.isBlank()) phrase else "${nickname.trim()}님, $phrase"
}
