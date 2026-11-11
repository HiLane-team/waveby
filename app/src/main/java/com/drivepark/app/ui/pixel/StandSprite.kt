package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.GridSize

/**
 * 서 있는 그림 한 장(크기는 [GridSize] 네 가지, 왼쪽을 바라봄). 모든 동작은 이 그림에서 자동으로 만든다.
 * 글자: K 테두리, W 밝은 털(사람은 피부), L 그늘, B 몸 색(동물 털·사람 상의), O 눈, P 코·볼,
 * H 머리카락(사람만), N 하의(사람만), 1~4 모양이 정한 고정 색, . 빈칸.
 * 몸 구역은 [BodyLines](모양마다 정할 수 있고, 17×23 기본은 머리 0~14 · 몸 15~19 · 다리 20~22). 맨 아랫줄이 발바닥.
 */
class StandSprite(val rows: List<String>) {
    /** 그림판 크기(줄 수·폭으로 정해진다). 네 크기 중 하나가 아니면 만들 수 없다. */
    val size: GridSize = requireNotNull(GridSize.of(rows.firstOrNull()?.length ?: 0, rows.size)) {
        "서 있는 그림 크기는 17×23·23×23·27×19·19×27 중 하나: ${rows.firstOrNull()?.length}×${rows.size}"
    }

    init {
        rows.forEachIndexed { y, r ->
            require(r.length == size.w) { "${y}줄 길이 ${r.length}" }
            r.forEach { c -> require(c in LETTERS) { "${y}줄에 모르는 글자 '$c'" } }
        }
    }

    /** 범위 밖은 빈칸. */
    operator fun get(x: Int, y: Int): Char = rows.getOrNull(y)?.getOrNull(x) ?: '.'

    fun filled(x: Int, y: Int): Boolean = get(x, y) != '.'

    override fun equals(other: Any?) = other is StandSprite && other.rows == rows
    override fun hashCode() = rows.hashCode()

    companion object {
        /** 기본 크기(17×23). 엔진 계산은 [size]를 쓰고, 이 값은 테스트·미리보기 도구용. */
        const val WIDTH = 17
        const val HEIGHT = 23
        val LETTERS = setOf('K', 'W', 'L', 'B', 'O', 'P', 'H', 'N', '1', '2', '3', '4', '.')
    }
}

data class Cell(val x: Int, val y: Int)
