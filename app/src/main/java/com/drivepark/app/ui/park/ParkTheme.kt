package com.drivepark.app.ui.park

import androidx.compose.ui.graphics.Color

enum class PropKind { TREE, BENCH, LAMP }

/** 공원 소품. x, y는 공원 영역 대비 비율(0~1). */
data class ParkProp(val kind: PropKind, val x: Float, val y: Float)

/**
 * 공원 꾸미기 데이터. 1차는 기본 테마 하나뿐이고, 2단계에서 사용자가 고르고 저장하게 한다.
 */
data class ParkTheme(
    val ground: Color,
    val path: Color,
    val accent: Color,
    val line: Color,
    /** 나무·벤치 속 채우기. */
    val fill: Color,
    val props: List<ParkProp>,
)

private val DEFAULT_PROPS = listOf(
        ParkProp(PropKind.TREE, 0.08f, 0.22f),
        ParkProp(PropKind.TREE, 0.90f, 0.20f),
        ParkProp(PropKind.TREE, 0.12f, 0.86f),
        ParkProp(PropKind.TREE, 0.88f, 0.84f),
        ParkProp(PropKind.BENCH, 0.32f, 0.20f),
        ParkProp(PropKind.BENCH, 0.68f, 0.88f),
        ParkProp(PropKind.LAMP, 0.22f, 0.50f),
        ParkProp(PropKind.LAMP, 0.80f, 0.52f),
)

val PleosLight = ParkTheme(
    ground = Color(0xFFF4F4F5),
    path = Color(0xFFE4E4E7),
    accent = Color(0xFFFF7A1A),
    line = Color(0xFFA1A1AA),
    fill = Color(0xFFFFFFFF),
    props = DEFAULT_PROPS,
)

/** 밤 공원: 진회색 바닥, 은은한 선, 가로등은 같은 주황. */
val PleosNight = ParkTheme(
    ground = Color(0xFF141417),
    path = Color(0xFF232328),
    accent = Color(0xFFFF7A1A),
    line = Color(0xFF52525B),
    fill = Color(0xFF1E1E22),
    props = DEFAULT_PROPS,
)
