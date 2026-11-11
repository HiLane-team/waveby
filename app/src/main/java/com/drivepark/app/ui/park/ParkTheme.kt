package com.drivepark.app.ui.park

import androidx.compose.ui.graphics.Color

enum class PropKind { TREE, BENCH, LAMP }

/** 공원 소품. x, y는 공원 영역 대비 비율(0~1). */
data class ParkProp(val kind: PropKind, val x: Float, val y: Float)

/**
 * 공원 꾸미기 데이터. 1차는 기본 테마 하나뿐이고, 2단계에서 사용자가 고르고 저장하게 한다.
 * 그림체: 도트 잔디 산책길(Codex 시안 A, 2026-11-08 대표 선택). 가운데는 비우고 소품은 네 구석에 둔다.
 */
data class ParkTheme(
    /** 잔디 바닥. */
    val ground: Color,
    /** 잔디 무늬(작은 풀 점). */
    val grass: Color,
    /** 산책길. */
    val path: Color,
    /** 나무 잎. */
    val leaf: Color,
    /** 나무 잎의 밝은 쪽. */
    val leafLight: Color,
    /** 나무 기둥·벤치·가로등 기둥. */
    val wood: Color,
    /** 가로등 불빛. */
    val lamp: Color,
    /** 불빛 둘레의 은은한 빛(밤에만, 투명이면 안 그림). */
    val glow: Color,
    val props: List<ParkProp>,
)

/** 시안 A 배치: 나무 네 구석, 벤치·가로등은 위 왼쪽과 아래 오른쪽. 첫 벤치는 사색 중 내 캐릭터가 앉는 자리. */
private val DEFAULT_PROPS = listOf(
    ParkProp(PropKind.TREE, 0.07f, 0.12f),
    ParkProp(PropKind.TREE, 0.92f, 0.11f),
    ParkProp(PropKind.TREE, 0.06f, 0.88f),
    ParkProp(PropKind.TREE, 0.94f, 0.87f),
    ParkProp(PropKind.BENCH, 0.26f, 0.13f),
    ParkProp(PropKind.BENCH, 0.77f, 0.88f),
    ParkProp(PropKind.LAMP, 0.15f, 0.12f),
    ParkProp(PropKind.LAMP, 0.86f, 0.82f),
)

/** 낮: 낮은 채도의 잔디와 모래색 길. */
val PleosLight = ParkTheme(
    ground = Color(0xFFAFBDA0),
    grass = Color(0xFF9FAF90),
    path = Color(0xFFC6C3AA),
    leaf = Color(0xFF7F9973),
    leafLight = Color(0xFFA1B18F),
    wood = Color(0xFF8F9E79),
    lamp = Color(0xFFDAD6AD),
    glow = Color.Transparent,
    props = DEFAULT_PROPS,
)

/** 밤: 낮은 밝기의 짙은 초록, 가로등만 주황 강조색으로 은은하게. */
val PleosNight = ParkTheme(
    ground = Color(0xFF273632),
    grass = Color(0xFF2F3F3A),
    path = Color(0xFF455049),
    leaf = Color(0xFF354C40),
    leafLight = Color(0xFF45604F),
    wood = Color(0xFF68705D),
    lamp = Color(0xFFF2C38B),
    glow = Color(0x33FF7A1A),
    props = DEFAULT_PROPS,
)
