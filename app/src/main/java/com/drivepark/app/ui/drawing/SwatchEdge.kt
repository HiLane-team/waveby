package com.drivepark.app.ui.drawing

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * 색 칸(색판·붓)이 배경에 묻히는지 판단한다.
 * 다크 모드의 검정 칸, 라이트 모드의 흰 칸처럼 배경과 대비가 너무 낮으면 진한 테두리를 그린다.
 * 안드로이드 의존 없이 ARGB 정수만 다룬다(단위 테스트용).
 */
internal fun needsStrongEdge(swatch: Int, background: Int): Boolean =
    contrastRatio(swatch, background) < MIN_SWATCH_CONTRAST

/** 이 대비보다 낮으면 배경에 묻힌다고 본다. */
private const val MIN_SWATCH_CONTRAST = 1.5

/** WCAG 대비 비율(1~21). */
internal fun contrastRatio(a: Int, b: Int): Double {
    val la = luminance(a)
    val lb = luminance(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
}

/** WCAG 상대 밝기(0~1). 알파는 무시한다. */
private fun luminance(argb: Int): Double {
    fun ch(v: Int): Double {
        val c = v / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
    val r = ch((argb shr 16) and 0xFF)
    val g = ch((argb shr 8) and 0xFF)
    val b = ch(argb and 0xFF)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}
