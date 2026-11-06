package com.drivepark.app.ui.pixel

/** 합성된 도트 그림. argb는 행 우선(y * width + x). 0은 투명. */
class PixelImage(val width: Int, val height: Int, val argb: IntArray)

/**
 * 문자 격자로 적은 도트 스프라이트. 한 글자가 한 픽셀이고 '.'은 투명이다.
 * 글자는 색이 아니라 팔레트 "슬롯"이다. 실제 색은 렌더러가 정한다.
 * 크기: 캐릭터(늑대) 17×23, 지역 캐릭터 32×32.
 *
 * 슬롯: o 외곽선, D 어두운 털, L 밝은 털, M 그늘, E 눈, H 하이라이트, P 분홍(귀 안쪽·볼),
 *       1~4 모자·마스코트 색, R 목도리, B 풍선, S 그림자
 */
class PixelSprite(val rows: List<String>) {
    val width: Int = rows.firstOrNull()?.length ?: 0
    val height: Int = rows.size

    init {
        require((width to height) in ALLOWED) { "스프라이트 크기는 19×24(늑대) 또는 32×32: ${width}×$height" }
        rows.forEachIndexed { i, r -> require(r.length == width) { "${i}번째 줄 길이 ${r.length}" } }
    }

    operator fun get(x: Int, y: Int): Char = rows[y][x]

    companion object {
        /** 늑대 그림판 19×24(17×23 격자 + 동작 여백), 지역 캐릭터 32×32. */
        val ALLOWED = setOf(WolfSprites.CANVAS_W to WolfSprites.CANVAS_H, 32 to 32)
        val SLOTS: Set<Char> = setOf('.', 'o', 'D', 'L', 'M', 'E', 'H', 'P', '1', '2', '3', '4', 'R', 'B', 'S')

        /** from~to 행을 dy칸 아래로 민다(빈 줄은 투명). 숨쉬기 프레임에 쓴다. */
        fun shiftRows(rows: List<String>, from: Int, to: Int, dy: Int): List<String> {
            val blank = ".".repeat(rows.first().length)
            val out = rows.toMutableList()
            for (y in to downTo from) {
                val target = y + dy
                if (target in rows.indices) out[target] = rows[y]
            }
            for (y in from until from + dy) out[y] = blank
            return out
        }
    }
}
