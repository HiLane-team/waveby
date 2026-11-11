package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.CharacterKind

/**
 * 캐릭터 모양 목록: 이름 → 서 있는 그림 한 장. 동작은 동작 엔진(Anatomy·Poser·Outfits)이 자동으로 만든다.
 * 새 모양은 17×23 서 있는 그림을 여기에 추가하면 된다(구역: 머리 0~14줄, 몸 15~19줄, 다리 20~22줄).
 */
object Shapes {
    /**
     * 기본 캐릭터: 오리지널 늑대. 왼쪽을 바라보는 3/4 구도.
     * 격자 규칙은 docs/character-ref/baekho/백호_캐릭터_참고.md 를 따른다(같은 글자 체계, 같은 줄 구간).
     * 얼굴: 눈 = 3·8열 9~11줄 초록 세로 3칸, 눈썹 = 눈 바로 위 검은 가로 2칸(ㄱ자), 코 = (5,12) 검은 한 칸.
     */
    val WOLF = StandSprite(
        listOf(
            ".K.........K.....", // 0  귀 끝
            "KBK.......KBK....", // 1
            "KLBK.....KBLBK...", // 2
            "KLLBK...KBLLBK...", // 3
            "KLLBBKKKBBBBBBK..", // 4  머리 위(정수리)
            ".KBBBBBBBBBBBBK..", // 5
            ".KBBBBBBBBBBBBBK.", // 6
            ".KBBBBBBBWWBBBBK.", // 7
            "KKKKWBWKKWWWWBBKK", // 8  눈썹(ㄱ자의 가로)
            "KWWOWWWWOWWWWWBBK", // 9  눈
            ".KWOWWWWOWWWWWBK.", // 10
            "KLWOWWWWOWWWWLBK.", // 11
            ".KWWWPWWWWWWWWBK.", // 12 코
            "..KWWWWWWWWWLBK..", // 13
            "...KWWWWWWWLLK...", // 14 턱
            "...KKWWWWLBBKK...", // 15 몸통
            "...KLWWWWWBBBBK..", // 16
            "..KLWWWWWWBBBBKKK", // 17
            "..KLWWWWWWLBBKBBK", // 18 꼬리
            "..KKWWWWWWLBKBBBK", // 19
            "...KWLWWWKBLKBWWK", // 20 다리
            "...KWLKKKLBLKKKK.", // 21
            "...KKKK..KKKK....", // 22 발바닥
        ),
    )

    private val WOLF_DEF = ShapeDef("wolf", ShapeKind.ANIMAL, WOLF, label = "늑대")

    /** 모양 id → 정의. 지역 캐릭터 id는 Region.id와 같다. */
    val ALL: Map<String, ShapeDef> = listOf(
        com.drivepark.app.ui.pixel.shapes.PERSON,
        WOLF_DEF,
        com.drivepark.app.ui.pixel.shapes.DOG,
        com.drivepark.app.ui.pixel.shapes.CAT,
        com.drivepark.app.ui.pixel.shapes.HOENGSEONG,
        com.drivepark.app.ui.pixel.shapes.BOSEONG,
        com.drivepark.app.ui.pixel.shapes.JEJU,
        com.drivepark.app.ui.pixel.shapes.GANGNEUNG,
        com.drivepark.app.ui.pixel.shapes.YEONGDEOK,
    ).associateBy { it.id }

    /**
     * 초안(앱에 안 들어감): 디자인을 다시 할 담당에게 넘길 참고용. 규칙 테스트는 계속 통과해야 한다.
     * 새 디자인을 받으면 그 모양을 [ALL]에 넣는다. 지역 id의 MASCOT 모양이 [ALL]에 들어가면
     * 도감·도착 창이 그 그림으로 바뀌고, 모은 지역 캐릭터가 공원에 놀러 오기 시작한다.
     * 2026-11-08: 강아지·고양이·지역 5종은 Codex 시안으로 [ALL]에 들어갔다. 토끼·햄스터는 시안이 없어 남겨 둔다.
     */
    val DRAFT_SHAPES: List<ShapeDef> = listOf(
        com.drivepark.app.ui.pixel.shapes.RABBIT,
        com.drivepark.app.ui.pixel.shapes.HAMSTER,
    )

    /** 지역 캐릭터 새 그림이 등록돼 있는지(없으면 예전 둥근 그림을 쓴다). */
    fun hasMascot(regionId: String): Boolean = ALL[regionId]?.kind == ShapeKind.MASCOT

    private val anatomies = HashMap<String, Anatomy>()

    /** 모양 정의. 모르는 이름이면 늑대(미리보기·테스트용). 저장된 캐릭터를 그릴 때는 [resolve]를 쓴다. */
    fun def(id: String): ShapeDef = ALL[id] ?: WOLF_DEF

    /** 엄격 조회: 앱에 등록된 모양만. */
    fun find(id: String): ShapeDef? = ALL[id]

    /** 고를 수 있는 동물 모양(등록 순서). 반려동물의 기본 모습은 첫 번째. */
    val animals: List<ShapeDef> = ALL.values.filter { it.kind == ShapeKind.ANIMAL }

    /** [animals]의 id만. 뷰모델·가짜 만남처럼 그림을 모르는 쪽에 넘긴다. */
    val animalIds: List<String> = animals.map { it.id }

    /**
     * 가짜 친구(1단계 시연용)의 반려동물 모양. 사용자가 고를 수 있는 [animalIds]와 따로 둔다.
     * 2026-11-09 대표 결정: 늑대·고양이만. 강아지는 무작위 색·소품과 겹치면 어색해 보여 뺐다.
     */
    val fakeFriendAnimalIds: List<String> = listOf("wolf", "cat").filter { it in animalIds }

    /**
     * 저장된 캐릭터를 그릴 모양. 앱이 모르는 id(지운 모양, 다른 앱 버전에서 온 친구 등)면
     * 누구에 따라 기본 모양으로 대신한다: 나 → 사람, 반려동물 → 첫 동물. 저장된 값은 바꾸지 않는다.
     */
    fun resolve(id: String, kind: CharacterKind): ShapeDef = find(id) ?: when (kind) {
        CharacterKind.PERSON -> ALL.getValue("person")
        CharacterKind.PET -> animals.firstOrNull() ?: WOLF_DEF
    }

    fun sprite(id: String): StandSprite = def(id).stand

    /** 모양의 몸 지도(한 번만 계산). 모르는 이름이면 늑대. */
    fun anatomy(id: String): Anatomy = synchronized(anatomies) {
        anatomies.getOrPut(def(id).id) { def(id).let { d -> Anatomy.of(d.stand, d.bodyLines, d.gait) } }
    }
}

/** 모양 종류. 색 입히는 규칙이 다르다(사람: 상의·피부·머리·하의, 동물: 털, 지역: 고정 색). */
enum class ShapeKind { PERSON, ANIMAL, MASCOT }

/**
 * 모양 하나: 서 있는 그림 + 고정 색(글자 1~4 → ARGB).
 * 그림에 쓴 고정 색 글자는 모두 [fixed]에 있어야 하고, 머리(H)·하의(N)는 사람만, 지역 캐릭터는 몸 색(B)을 쓰지 않는다.
 */
class ShapeDef(
    val id: String,
    val kind: ShapeKind,
    val stand: StandSprite,
    val fixed: Map<Char, Int> = emptyMap(),
    /** 고르는 화면에 보일 이름. */
    val label: String = id,
    /** false면 머리·하의 글자 검사를 건너뛴다(사용자가 직접 그린 그림 — 동물 색표에 머리·하의 색을 더해 그린다). */
    internal val strict: Boolean = true,
    /** 몸 구역. null이면 크기별 기본값([BodyLines.default]). */
    lines: BodyLines? = null,
    /** 걷는 방식. null이면 엔진이 그림을 보고 정한다(바닥에 넓게 붙은 한 덩어리 = 통통). */
    val gait: Gait? = null,
    /** 놀람 표정의 입 기준점(코 자리, 서 있는 그림 좌표). null이면 그림의 코(P) → 눈 위치로 정한다. */
    val nose: Pair<Int, Int>? = null,
    /** 지역 캐릭터의 고정 눈 색(ARGB, 캐릭터에 어울리는 퍼스널 컬러). null이면 예전 초록. 동물·사람은 고른 눈 색을 쓴다. */
    val eye: Int? = null,
) {
    val bodyLines: BodyLines = lines ?: BodyLines.default(stand.size)

    init {
        require(bodyLines.fits(stand.size)) { "$id: 몸 구역 $bodyLines 이 ${stand.size} 그림에 맞지 않음" }
        if (strict) check()
    }

    private fun check() {
        val used = stand.rows.flatMap { it.toList() }.toSet()
        (used intersect setOf('1', '2', '3', '4')).forEach { c -> require(c in fixed) { "$id: 고정 색 '$c'의 색이 없음" } }
        if (kind != ShapeKind.PERSON) {
            require('H' !in used && 'N' !in used) { "$id: 머리(H)·하의(N)는 사람 모양만" }
        }
        if (kind == ShapeKind.MASCOT) require('B' !in used) { "$id: 지역 캐릭터는 몸 색(B) 대신 고정 색을 쓴다" }
    }
}
