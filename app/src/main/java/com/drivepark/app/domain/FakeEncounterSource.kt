package com.drivepark.app.domain

import kotlin.random.Random

/**
 * 1단계용 가짜 만남 공급원. 사람 70%·반려동물 30%로 무작위 캐릭터를 만든다.
 * 시연 중 같은 이름이 자꾸 보이면 가짜 티가 나므로, 최근 [RECENT]명과 이름이 겹치지 않게 고른다.
 * @param animalShapes 반려동물에게 쓸 동물 모양 id(도메인은 그림 목록을 모르므로 밖에서 받는다).
 */
class FakeEncounterSource(
    private val random: Random = Random.Default,
    private val animalShapes: List<String> = listOf("wolf"),
) : EncounterSource {

    /** 최근에 쓴 이름(오래된 것부터). */
    private val recent = ArrayDeque<String>()

    fun generate(count: Int): List<Character> = List(count) { randomCharacter() }

    /** 주행 1틱당 0~1명. */
    override suspend fun fetchNew(): List<Character> = generate(random.nextInt(0, 2))

    private fun randomCharacter(): Character {
        val parts = CharacterParts(
            // 난수는 예전 6색 목록에서 뽑아(같은 시드 = 같은 순서) 다른 값이 바뀌지 않게 하고, 실제 몸 색은 아래에서 색판 16색으로 바꾼다.
            body = BodyColor.LEGACY.random(random),
            face = Face.entries.random(random),
            hat = Hat.entries.random(random),
            accessory = Accessory.entries.random(random),
        )
        val pet = random.nextInt(100) < PET_PERCENT && animalShapes.isNotEmpty()
        val greeting = GREETINGS.random(random)
        val character = if (pet) {
            Character.create(parts, freshName(PET_NAMES), greeting, CharacterKind.PET, animalShapes.random(random))
        } else {
            val look = PersonLook(SkinTone.entries.random(random), HairColor.entries.random(random), BottomColor.entries.random(random))
            Character.create(parts, freshName(NICKNAMES), greeting, CharacterKind.PERSON, "person", look)
        }
        character ?: error("풀에 규칙을 어기는 문구가 있다")
        // 눈 색은 난수를 쓰지 않고 이미 정한 값에서 고른다. 난수를 하나 더 쓰면 같은 시드의 다음 친구들이 모두 바뀐다.
        // 글자로 만든 해시만 쓴다(enum의 hashCode는 실행마다 달라 같은 시드에서도 눈 색이 바뀐다).
        val key = with(character) { "$nickname|$greeting|${parts.body}|${parts.face}|${parts.hat}|${parts.accessory}" }
        val eye = EyeColor.entries[Math.floorMod(key.hashCode(), EyeColor.entries.size)]
        val palette = BodyColor.CHOICES.drop(1)
        val body = palette[Math.floorMod((key + "|body").hashCode(), palette.size)]
        return character.copy(parts = character.parts.copy(eye = eye, body = body))
    }

    /** 최근에 쓰지 않은 이름을 고르고 기억한다. */
    private fun freshName(pool: List<String>): String {
        val name = pool.filterNot { it in recent }.ifEmpty { pool }.random(random)
        recent.addLast(name)
        while (recent.size > RECENT) recent.removeFirst()
        return name
    }

    companion object {
        const val PET_PERCENT = 30

        /** 이 수만큼 최근 이름은 다시 쓰지 않는다(풀 크기보다 충분히 작게). */
        const val RECENT = 20

        // 닉네임은 10자, 인사말은 30자 이하. 실존 인물·브랜드·작품 이름은 쓰지 않는다.
        val NICKNAMES = listOf(
            "드라이브왕", "출근길요정", "느긋한거북", "고속도로곰", "주말여행러", "신호등수호자", "라디오DJ", "휴게소미식가",
            "새벽운전러", "바다보러가요", "커피한잔", "노을사냥꾼", "캠핑가는길", "퇴근길토끼", "산책하는별", "초보운전중",
            "졸음쉼터", "느림보달팽이", "맑은하늘", "구름한조각", "단풍구경", "벚꽃드라이브", "달빛운전", "아침햇살",
            "국도여행자", "해안도로", "터널통과중", "바람을가르며", "콧노래운전", "오늘도무사히", "주유소단골", "길치탈출",
        )
        val PET_NAMES = listOf(
            "초코", "보리", "콩이", "두부", "뭉치", "호두", "까미", "누리",
            "모카", "쿠키", "별이", "구름", "솜이", "밤톨", "도토리", "망고",
            "치즈", "라떼", "단비", "하루", "봄이", "가을", "복실", "꼬마",
            "땅콩", "자두", "율무", "보송", "깜순", "토리", "마루", "새콤",
        )
        val GREETINGS = listOf(
            "좋은 하루 보내세요!", "안전운전 하세요~", "오늘도 화이팅!", "반가워요!", "즐거운 드라이브 되세요", "또 만나요!",
            "창밖 풍경이 예쁘네요", "커피 한잔 어때요?", "천천히 가도 괜찮아요", "노래 한 곡 추천해 주세요",
            "오늘 날씨 좋죠?", "길 막히면 쉬어 가요", "주말 잘 보내세요", "멋진 차네요!",
            "집에 잘 들어가세요", "같은 길 달렸네요!", "행복한 여행 되세요", "어디 가는 길이에요?",
            "졸리면 꼭 쉬어 가요", "다음에 또 스쳐요", "웃으며 운전해요", "오늘도 수고 많았어요",
        )
    }
}
