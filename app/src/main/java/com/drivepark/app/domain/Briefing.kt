package com.drivepark.app.domain

/** 주차 시 도착 브리핑 문장을 만든다(글레오). 실패하면 null. */
interface Briefer {
    suspend fun brief(summary: ArrivalSummary): String?
}

/** 글레오 브리핑의 최대 길이. 넘으면 버리고 정해진 문장을 쓴다. */
const val MAX_BRIEFING_LENGTH = 80

/** 친구가 1명 이상이면 도착 요약, 새 지역이 있으면 지역 획득 문구. 줄바꿈으로 잇는다. */
fun arrivalMessage(summary: ArrivalSummary): String = buildList {
    if (summary.friends.isNotEmpty()) add("이번 드라이브에서 ${summary.friends.size}명이 놀러왔어요")
    summary.newRegion?.let { add("${withObjectParticle(it.mascotName)} 얻었어요!") }
}.joinToString("\n")

/** 글레오를 쓸 수 없을 때 읽는 문장: 도착 요약 + 대표 친구 인사말. */
fun fallbackBriefing(summary: ArrivalSummary): String {
    val first = summary.friends.firstOrNull() ?: return arrivalMessage(summary)
    return arrivalMessage(summary) + "\n${first.nickname}: ${first.greeting}"
}

/**
 * 글레오 프롬프트. 친구 수, 최대 3명의 닉네임·인사말, 새 지역 캐릭터 이름만 넣는다.
 * 좌표, 주소, 시각, 날짜는 넣지 않는다.
 */
object BriefingPrompt {
    fun build(summary: ArrivalSummary): String = buildString {
        appendLine("당신은 차량 앱의 다정한 안내자 글레오입니다.")
        appendLine("아래 정보로 방금 주차한 운전자에게 한국어 두 문장 이내, 80자 이내, 이모지 없이 환영 인사를 하세요.")
        appendLine("- 공원에 놀러온 친구 수: ${summary.friends.size}명")
        summary.friends.take(3).forEach { appendLine("- 친구 ${it.nickname}의 인사: ${it.greeting}") }
        summary.newRegion?.let { appendLine("- 새로 얻은 지역 캐릭터: ${it.mascotName}") }
    }.trimEnd()
}

/** 목적격 조사를 붙인다: 마지막 글자에 받침이 있으면 "을", 없으면(한글이 아니어도) "를". */
fun withObjectParticle(word: String): String {
    val last = word.lastOrNull() ?: return word
    val hasFinal = last in '가'..'힣' && (last - '가') % 28 != 0
    return word + if (hasFinal) "을" else "를"
}
