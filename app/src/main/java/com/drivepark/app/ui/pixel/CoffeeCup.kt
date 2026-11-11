package com.drivepark.app.ui.pixel

/**
 * 휴식 권유에 쓰는 커피잔 도트(오리지널). 캐릭터 엔진과 따로 그려 캐릭터 옆에 놓는다(이호랑 그림은 건드리지 않는다).
 * 김 · 테두리(먹색) · 커피(갈색) · 잔(흰색) · 주황 띠(Pleos 강조색).
 */
object CoffeeCup {
    private val ROWS = listOf(
        "..s..s...",
        ".s..s....",
        "..s..s...",
        "KKKKKKK..",
        "KCCCCCK..",
        "KWWWWWKKK",
        "KAAAAAK.K",
        "KWWWWWK.K",
        "KWWWWWKKK",
        ".KKKKK...",
    )
    private val COLORS = mapOf(
        'K' to 0xFF2A2C30.toInt(),
        'C' to 0xFF8A5A3C.toInt(),
        'W' to 0xFFF7F7F5.toInt(),
        'A' to 0xFFFF7A1A.toInt(),
        's' to 0xFFA1A1AA.toInt(),
    )

    val image: PixelImage by lazy {
        val w = ROWS[0].length
        val out = IntArray(w * ROWS.size)
        ROWS.forEachIndexed { y, row -> row.forEachIndexed { x, c -> out[y * w + x] = COLORS[c] ?: 0 } }
        PixelImage(w, ROWS.size, out)
    }
}
