package com.drivepark.app.domain

enum class BodyColor { RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE }
enum class Face { SMILE, WINK, SURPRISED, SLEEPY }
enum class Hat { NONE, CAP, BEANIE, CROWN }
enum class Accessory { NONE, GLASSES, SCARF, BALLOON }

data class CharacterParts(
    val body: BodyColor,
    val face: Face,
    val hat: Hat,
    val accessory: Accessory,
)

/**
 * 캐릭터 종류. PERSON = 사용자 본인(사람형 17×23 늑대 등), PET = 사용자의 반려동물.
 * 지금 앱에는 PERSON만 있다. 반려동물 캐릭터를 넣을 때 저장소·전송 데이터에도 이 값을 추가한다(기획서 단계).
 */
enum class CharacterKind { PERSON, PET }

/** 다른 사용자에게 전달되는 유일한 정보: 외형, 닉네임, 인사말. */
data class Character(
    val parts: CharacterParts,
    val nickname: String,
    val greeting: String,
    val kind: CharacterKind = CharacterKind.PERSON,
) {
    companion object {
        const val NICKNAME_MAX = 10
        const val GREETING_MAX = 30

        /** 앞뒤 공백을 제거한 뒤 길이 규칙을 어기면 null. */
        fun create(parts: CharacterParts, nickname: String, greeting: String): Character? {
            val n = nickname.trim()
            val g = greeting.trim()
            if (n.length !in 1..NICKNAME_MAX) return null
            if (g.length !in 1..GREETING_MAX) return null
            return Character(parts, n, g)
        }
    }
}
