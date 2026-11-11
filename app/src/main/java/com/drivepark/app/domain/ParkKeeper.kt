package com.drivepark.app.domain

/**
 * 공원지기가 지금 하는 일. INVITE = 공원에서 폴짝이며 "나 좀 눌러 봐!"로 누르기를 유도,
 * GUIDE = 눌러서 시작한 처음 안내(대화 상자), PLAY = 구석에서 혼자 놀다가 누르면 도움말.
 */
enum class KeeperMode { INVITE, GUIDE, PLAY }

/**
 * 공원지기 이호랑: 팀의 오리지널 캐릭터가 공원에 늘 머물며 처음 쓰는 사람을 안내한다.
 * 친구가 아니므로 친구 수·친구 목록·도착 알림에 들어가지 않는다. 다른 사람에게 보내지도 않는다.
 * 모습은 대표가 2026-11-08 직접 꾸며 저장한 최종 이호랑을 그대로 고정했다(그림을 바꾸지 않는다).
 */
object ParkKeeper {
    /** 친구가 이만큼 모이면 안내를 멈추고 구석에서 논다. */
    const val GUIDE_UNTIL_FRIENDS = 3

    /** 최종 이호랑 그림(17×23, 고정 색 2 = 분홍 볼). */
    private val DRAWING = listOf(
        ".KK.......KK.....",
        "KBK.......KBK....",
        "KLBK.....KBLBK...",
        "KLLBK...KBLLBK...",
        "KWLBBKKKBBLWBBK..",
        ".KBBBBBBBBBBBBK..",
        ".KBBBBBBBBBBBBBK.",
        ".KBBBBBBBWWBBBBK.",
        "KKKKWBWKKWWWWBBKK",
        "KWWOWWWWOWWWWWBBK",
        ".KWOWWWWOWWWWWBK.",
        "KLWOWWWWOWW2WWBK.",
        ".KWWWKWWWW2WWLBK.",
        "..KWWWWWWWWWLBK..",
        "...KWWWWWWWLLK...",
        "...KKWWWWLBBKK...",
        "...KLWWWWWBBBBK..",
        "..KLWWWWWWBBBBKKK",
        "..KLWWWWWWLBBKBBK",
        "..KKWWWWWWLBKBBBK",
        "...KWLWWWKBLKBWWK",
        "...KWLKKKLBLKKKK.",
        "...KKKK..KKKK....",
    ).joinToString("\n") + "\n2=#F2A7B5"

    val CHARACTER = Character(
        CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.SCARF, EyeColor.GREEN),
        nickname = "이호랑",
        greeting = "안녕하세요.",
        kind = CharacterKind.PERSON,
        shape = DRAWN_SHAPE,
        drawing = DRAWING,
    )

    /**
     * 처음 안내(누를 때마다 한 줄씩). 마지막 줄을 넘기면 안내를 끝낸 것으로 저장한다.
     * 말투: 이웃처럼 친근한 반말(대표 예시 "안녕? 난 이호랑이야. 네 공원의 새로운 이웃이자, 공원지기이지.").
     */
    val TUTORIAL = listOf(
        "안녕? 난 이호랑이야. 네 공원의 새로운 이웃이자, 공원지기지.",
        "드라이브하다 스쳐 간 차의 캐릭터들이 네 친구가 될 거야.",
        "주차하면 그 친구들이 이 공원으로 놀러 와.",
        "친구를 누르면 인사말을 볼 수 있어. 친구 탭에선 스티커로 답례도 하고.",
        "여러 지역을 달리면 그곳 캐릭터가 도감에 모여.",
        "네 모습은 설정에서 언제든 꾸밀 수 있어. 그럼, 잘 다녀와!",
    )

    /** 놀고 있을 때 누르면 보여 주는 짧은 도움말(누를 때마다 다음, 끝나면 닫힘). */
    val HELP = listOf(
        "드라이브를 떠나면 친구들이 놀러 올 거야.",
        "친구 탭에서 스티커로 답례할 수 있어.",
        "도감에서 모은 지역 캐릭터를 볼 수 있어.",
        "사색에 잠기면 친구 방문을 잠깐 멈춰.",
    )

    /** 안내 전, 공원에 선 이호랑 머리 위 작은 말풍선. */
    const val INVITE_LINE = "나 좀 눌러 봐!"

    /**
     * 안내를 아직 끝내지 않았고 친구가 적으면: 이호랑을 누르기 전엔 유도, 누른 뒤엔 안내. 아니면 논다.
     * [tutorialStarted]는 저장하지 않는다(안내 도중 나갔다 오면 다시 유도부터).
     */
    fun mode(friendCount: Int, tutorialDone: Boolean, tutorialStarted: Boolean = false): KeeperMode = when {
        tutorialDone || friendCount >= GUIDE_UNTIL_FRIENDS -> KeeperMode.PLAY
        tutorialStarted -> KeeperMode.GUIDE
        else -> KeeperMode.INVITE
    }

    /** 안내 다음 줄. 마지막 줄 다음은 null(안내 끝). */
    fun nextTutorialStep(step: Int): Int? = (step + 1).takeIf { it <= TUTORIAL.lastIndex }

    /** 도움말 다음 줄. 닫혀 있으면(null) 첫 줄, 마지막 줄 다음은 null(닫힘). */
    fun nextHelp(index: Int?): Int? = if (index == null) 0 else (index + 1).takeIf { it <= HELP.lastIndex }
}
