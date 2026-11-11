package com.drivepark.app.domain

/**
 * 몸 색(동물 털·사람 상의).
 * - PURPLE = 이호랑 기본 늑대 회색(이름은 예전 그대로).
 * - RED·ORANGE·YELLOW·GREEN·BLUE = 예전 5색(노을·여우·모래·숲·새벽). 고르는 목록에서는 빠졌고, 저장된 캐릭터를 그대로 그리려고 남긴다.
 * - C_로 시작하는 16색 = 몸 색판([BODY_PALETTE], 그리기 색판의 앞 16색)과 같은 색. [palette]가 그 색값.
 * 저장은 이름(name)으로 하므로 이름을 바꾸지 않는다.
 */
enum class BodyColor(val palette: Int? = null) {
    RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE,
    C_RED(0xFFE5484D.toInt()), C_ORANGE(0xFFF97316.toInt()), C_YELLOW(0xFFF5B301.toInt()), C_LIME(0xFF84CC16.toInt()),
    C_GREEN(0xFF2E7D32.toInt()), C_TEAL(0xFF14B8A6.toInt()), C_BLUE(0xFF3B82F6.toInt()), C_NAVY(0xFF1E3A8A.toInt()),
    C_PURPLE(0xFF8B5CF6.toInt()), C_PINK(0xFFF2A7B5.toInt()), C_BROWN(0xFF8A5A3C.toInt()), C_BEIGE(0xFFD9B48F.toInt()),
    C_WHITE(0xFFFFFFFF.toInt()), C_GRAY(0xFFA1A1AA.toInt()), C_CHARCOAL(0xFF52525B.toInt()), C_BLACK(0xFF18181B.toInt()),
    ;

    companion object {
        /** 고르는 목록: 기본 늑대 회색 + 색판 16색. */
        val CHOICES: List<BodyColor> = listOf(PURPLE) + entries.filter { it.palette != null }

        /** 예전 5색 + 기본(가짜 친구가 난수를 예전과 같은 순서로 쓰게 할 때). */
        val LEGACY: List<BodyColor> = listOf(RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE)
    }
}
enum class Face { SMILE, WINK, SURPRISED, SLEEPY }
enum class Hat { NONE, CAP, BEANIE, CROWN }
enum class Accessory { NONE, GLASSES, SCARF, BALLOON }

/** 눈 색. 모든 모습(사람·동물·내 그림)에 쓰고, 지역 캐릭터는 따르지 않는다. 기본은 초록(예전 색). */
enum class EyeColor { GREEN, BLUE, BROWN, BLACK, PURPLE, GOLD }

/** 사람 모양의 피부 톤. 화면에서 고르기·저장은 캐릭터 확장 3번에서. */
enum class SkinTone { LIGHT, MEDIUM, DEEP }

/** 사람 모양의 머리색. */
enum class HairColor { BLACK, BROWN, BLOND, GRAY }

/** 사람 모양의 하의(바지) 색. 상의는 BodyColor를 쓴다. */
enum class BottomColor { NAVY, DENIM, BLACK, BEIGE, GRAY }

/** 사람 모양에만 쓰는 외모 값. 동물·지역 캐릭터는 무시한다. */
data class PersonLook(
    val skin: SkinTone = SkinTone.LIGHT,
    val hair: HairColor = HairColor.BLACK,
    val bottom: BottomColor = BottomColor.NAVY,
)

data class CharacterParts(
    val body: BodyColor,
    val face: Face,
    val hat: Hat,
    val accessory: Accessory,
    val eye: EyeColor = EyeColor.GREEN,
)

/** 누구의 캐릭터인지. PERSON = 사용자 본인, PET = 사용자의 반려동물(다른 운전자에게도 표시). */
enum class CharacterKind { PERSON, PET }

/**
 * 다른 사용자에게 전달되는 정보: 외형(파츠·눈 색·모양·사람 외모·그림),닉네임, 인사말, 반려동물 여부.
 * 만난 시각·위치는 넣지 않는다(익명성 원칙).
 * @property shape 모양 id(예: "person", "wolf", [DRAWN_SHAPE]). 앱이 모르는 id면 그릴 때 누구에 따라 기본 모양으로 대신한다.
 * @property drawing 직접 그린 그림([Drawing] 형식). 모습이 [DRAWN_SHAPE]일 때만 그리는 데 쓰고, 다른 모습이어도 남겨 둔다.
 */
data class Character(
    val parts: CharacterParts,
    val nickname: String,
    val greeting: String,
    val kind: CharacterKind = CharacterKind.PERSON,
    val shape: String = "wolf",
    val look: PersonLook = PersonLook(),
    val drawing: String? = null,
) {
    companion object {
        const val NICKNAME_MAX = 10
        const val GREETING_MAX = 30

        /**
         * 앞뒤 공백을 제거한 뒤 길이 규칙을 어기거나 모양 id가 비어 있으면 null.
         * 모습이 [DRAWN_SHAPE]인데 그림이 없거나 형식이 틀려도 null(저장 막음).
         */
        fun create(
            parts: CharacterParts,
            nickname: String,
            greeting: String,
            kind: CharacterKind = CharacterKind.PERSON,
            shape: String = "wolf",
            look: PersonLook = PersonLook(),
            drawing: String? = null,
        ): Character? {
            val n = nickname.trim()
            val g = greeting.trim()
            if (n.length !in 1..NICKNAME_MAX) return null
            if (g.length !in 1..GREETING_MAX) return null
            if (shape.isBlank()) return null
            if (shape == DRAWN_SHAPE && Drawing.parse(drawing) == null) return null
            return Character(parts, n, g, kind, shape, look, drawing)
        }
    }
}
