package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.GridSize

/** 엔진 테스트용 서 있는 그림(17×23). 적지 않은 줄은 빈 줄. */
object TestShapes {
    private fun shape(vararg rows: Pair<Int, String>): List<String> {
        val out = MutableList(23) { ".".repeat(17) }
        rows.forEach { (y, r) -> require(r.length == 17) { "$y 줄 길이 ${r.length}" }; out[y] = r }
        return out
    }

    private fun biped(feet: List<Pair<Int, String>>, eyes: Boolean = true): List<String> {
        val e = if (eyes) 'O' else 'W'
        return shape(
            4 to "....KKKKKKKK.....",
            5 to "...KWWWWWWWWK....",
            6 to "...KWWWWWWWWK....",
            7 to "...KWWWWWWWWK....",
            8 to "...KWWWWWWWWK....",
            9 to "...KW${e}WWW${e}WWK....",
            10 to "...KW${e}WWW${e}WWK....",
            11 to "...KWWWPWWWWK....",
            12 to "...KWWWWWWWWK....",
            13 to "...KWWWWWWWWK....",
            14 to "....KKKKKKKK.....",
            15 to "...KBBBBBBBBBK...",
            16 to "...KBBBBBBBBBK...",
            17 to "...KBBBBBBBBBK...",
            18 to "...KBBBBBBBBBK...",
            19 to "...KBBBBBBBBBK...",
            *feet.toTypedArray(),
        )
    }

    private val TWO_FEET = listOf(
        20 to "...KBBBK.KBBBK...",
        21 to "...KWWWK.KWWWK...",
        22 to "...KKKKK.KKKKK...",
    )

    /** 두 발 사람형: 발 3~7열·9~13열, 눈 2개(5·9열), 꼬리 없음. */
    val BIPED = StandSprite(biped(TWO_FEET))

    /** 눈 없는 사람형. */
    val NO_EYES = StandSprite(biped(TWO_FEET, eyes = false))

    /** 발 덩어리 하나, 폭 3(깡충 걷기). */
    val NARROW = StandSprite(
        biped(listOf(20 to "......KBK........", 21 to "......KWK........", 22 to "......KKK........")),
    )

    /** 발 덩어리 하나, 폭 6(4~9열) → 두 발로 나뉨. */
    val WIDE_RUN = StandSprite(
        biped(listOf(20 to "....KBBBBK.......", 21 to "....KWWWWK.......", 22 to "....KKKKKK.......")),
    )

    /** 네 발 옆모습, 키 작음(머리 8줄부터), 꼬리 12~15열. */
    val QUADRUPED = StandSprite(
        shape(
            8 to "KKKKK............",
            9 to "KWOWK............",
            10 to "KWWWKK...........",
            11 to "KPWWWK...........",
            12 to "KWWWWK...........",
            13 to ".KWWWK...........",
            14 to ".KKKKKKKKKKK.....",
            15 to ".KBBBBBBBBBKKBBK.",
            16 to ".KBBBBBBBBBK.KBK.",
            17 to ".KBBBBBBBBBKKK...",
            18 to ".KBBBBBBBBBK.....",
            19 to ".KWWWWWWWWWK.....",
            20 to ".KK.KK.KK.KK.....",
            21 to ".KK.KK.KK.KK.....",
            22 to ".KK.KK.KK.KK.....",
        ),
    )

    /** 그림판 끝에 닿은 그림: 귀 0줄, 발 0열과 16열. */
    val EDGE = StandSprite(
        shape(
            0 to "K...............K",
            1 to "KK.............KK",
            *(2..13).map { y -> y to if (y == 9) "KWWOWWWWWWWWOWWWK" else "KWWWWWWWWWWWWWWWK" }.toTypedArray(),
            14 to "KKKKKKKKKKKKKKKKK",
            *(15..19).map { it to "KBBBBBBBBBBBBBBBK" }.toTypedArray(),
            20 to "KBBK.........KBBK",
            21 to "KWWK.........KWWK",
            22 to "KKKK.........KKKK",
        ),
    )

    /** 키 작은 그림: 머리 10줄부터. */
    val SHORT = StandSprite(
        shape(
            10 to "....KKKKKKK......",
            11 to "...KWOWWWOWK.....",
            12 to "...KWWWPWWWK.....",
            13 to "...KWWWWWWWK.....",
            14 to "....KKKKKKK......",
            15 to "...KBBBBBBBK.....",
            16 to "...KBBBBBBBK.....",
            17 to "...KBBBBBBBK.....",
            18 to "...KBBBBBBBK.....",
            19 to "...KBBBBBBBK.....",
            20 to "...KBK...KBK.....",
            21 to "...KWK...KWK.....",
            22 to "...KKK...KKK.....",
        ),
    )

    /** 발바닥 줄이 비고, 떨어진 점이 있는 그림. */
    val BROKEN = StandSprite(
        biped(TWO_FEET.take(2)).toMutableList().also { it[0] = "K" + it[0].drop(1) },
    )

    /** 머리 구역이 빈 그림. */
    val NO_HEAD = StandSprite(biped(TWO_FEET).mapIndexed { y, r -> if (y <= 14) ".".repeat(17) else r })

    /** 동작 테스트에서 함께 돌리는 모양 목록(늑대 제외). */
    val ALL = listOf(BIPED, NO_EYES, NARROW, WIDE_RUN, QUADRUPED, EDGE, SHORT)

    /**
     * 크기마다 쓰는 단순한 그림(기본 몸 구역에 맞춤): 머리 상자(눈 2·코), 몸, 그리고
     * 다리 2개(legless=false) 또는 바닥에 넓게 붙은 몸통(legless=true, 폭 8 → 통통 걷기).
     */
    fun sized(size: GridSize, legless: Boolean): StandSprite {
        val lines = BodyLines.default(size)
        val w = size.w
        fun row(s: String) = s + ".".repeat(w - s.length)
        val rows = MutableList(size.h) { ".".repeat(w) }
        val top = lines.headEnd - 6
        rows[top] = row("...KKKKKKKK")
        for (y in top + 1 until lines.headEnd) rows[y] = row("...KWWWWWWK")
        rows[top + 2] = row("...KWOWWOWK")
        rows[top + 3] = row("...KWWPWWWK")
        rows[lines.headEnd] = row("...KKKKKKKK")
        val bodyEnd = if (legless) size.h - 1 else lines.legsFrom - 1
        for (y in lines.headEnd + 1..bodyEnd) rows[y] = row("..KBBBBBBBBK")
        if (legless) {
            rows[size.h - 1] = row("..KKKKKKKKKK")
        } else {
            for (y in lines.legsFrom until size.h - 1) rows[y] = row("...KBK..KBK")
            rows[size.h - 1] = row("...KKK..KKK")
        }
        return StandSprite(rows)
    }

    /** 네 크기 × 다리 있음/없음. */
    val SIZED: List<StandSprite> = GridSize.entries.flatMap { listOf(sized(it, false), sized(it, true)) }
}
