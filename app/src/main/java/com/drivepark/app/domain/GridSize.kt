package com.drivepark.app.domain

/**
 * 서 있는 그림의 그림판 크기(가로 w칸 × 세로 h줄). 이 넷만 쓴다.
 * 기본은 이호랑(늑대)과 같은 17×23. 둥근형은 감귤·녹차, 가로형은 대게, 세로형은 커피잔처럼 긴 모양에.
 * 네 크기는 가로·세로 조합이 모두 달라, 그림 글자의 줄 수와 폭만 보고 크기를 알 수 있다.
 */
enum class GridSize(val w: Int, val h: Int) {
    BASIC(17, 23),
    ROUND(23, 23),
    WIDE(27, 19),
    TALL(19, 27),
    ;

    companion object {
        /** 네 크기 중 하나가 아니면 null. */
        fun of(w: Int, h: Int): GridSize? = entries.firstOrNull { it.w == w && it.h == h }
    }
}
