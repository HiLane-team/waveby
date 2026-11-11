package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.GridSize
import kotlin.math.roundToInt

/**
 * 몸 구역: 머리 `0..headEnd`, 몸 `headEnd+1 until legsFrom`, 다리 `legsFrom..h-1`(맨 아랫줄 = 발바닥).
 * 동작(끄덕임·앉기·웅크리기)과 목도리가 이 값으로 줄을 정한다. 모양마다 다르게 정할 수 있다.
 */
data class BodyLines(val headEnd: Int, val legsFrom: Int) {
    /** 이 크기에서 쓸 수 있는 값인지(머리·몸·다리가 모두 한 줄 이상). */
    fun fits(size: GridSize): Boolean = headEnd > 0 && headEnd < legsFrom - 1 && legsFrom <= size.h - 1

    companion object {
        /** 크기별 기본값. 17×23은 이호랑(늑대)과 같은 (14, 20), 나머지는 같은 비율. */
        fun default(size: GridSize): BodyLines = BodyLines((size.h * 15 / 23.0).roundToInt() - 1, size.h - 3)
    }
}

/** 걷는 방식. LEGS = 발을 번갈아 들기, HOP = 다리 없이 몸 전체가 통통 튀기. */
enum class Gait { LEGS, HOP }
