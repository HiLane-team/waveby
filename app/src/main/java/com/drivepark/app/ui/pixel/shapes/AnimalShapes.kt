package com.drivepark.app.ui.pixel.shapes

import com.drivepark.app.ui.pixel.ShapeDef
import com.drivepark.app.ui.pixel.ShapeKind
import com.drivepark.app.ui.pixel.StandSprite

// 기본 동물(오리지널). 모두 늑대처럼 왼쪽을 보는 3/4 두 발 서기. B = 털 색 고르기, W = 밝은 털.
// 강아지·고양이는 Codex 시안(2026-11-08, PR #21)을 바탕으로 했다. 23×23 그림판, 몸 구역은 크기별 기본값.
// 화풍 기준: 이호랑 — 주둥이는 얼굴 테두리 밖으로 튀어나오지 않고, 꼬리는 몸 아래 뒤쪽에 작게(2~3줄).

/** 강아지: 늘어진 귀(고정 갈색), 납작한 얼굴에 코 한 칸, 몸 뒤에 짧게 붙은 꼬리(끝이 밝음). */
internal val DOG = ShapeDef(
    "dog", ShapeKind.ANIMAL,
    StandSprite(
        listOf(
            ".......................", // 0
            "........K..............", // 1
            ".......KBKKKKK.........", // 2
            "....K.KBBBBBBBK........", // 3
            "...K1KBBLLLBBBBKKK.....", // 4
            "..K111LLLLBBBB1111K....", // 5  늘어진 귀
            "..K111BLBBBBBB11B1K....", // 6
            ".K1111KBBBBKBB11131K...", // 7  눈썹
            ".K1111OWBBBOBB11131K...", // 8  눈
            "..K1KWOWBBBOBB11131K...", // 9
            "...KWWOWWWWOWWW1111K...", // 10
            "...KWWWWPWWWWWW111K....", // 11 코
            "...KWWWWWWWWWWWB1K.....", // 12
            "...KKKWWWWWWLBBKK......", // 13 턱
            "......KBBWBWWBBK.......", // 14
            "......KBBWWWWBBLK......", // 15 몸통
            ".....KBBBWWWWWBLLLK....", // 16
            ".....KBBBWWWWWLLLLKKK..", // 17 꼬리
            ".....KB1BWWWWBL1L1BBBK.", // 18
            "......KBBWWWWBBLB1BBWK.", // 19 꼬리 끝
            "......KBBBBBBBBBBBKKKK.", // 20 다리
            "......KWWWKKKWWWK......", // 21
            ".......KKK...KKK.......", // 22 발바닥
        ),
    ),
    fixed = mapOf('1' to 0xFF8C6449.toInt(), '3' to 0xFF755540.toInt()),
    label = "강아지",
)

/** 고양이: 뾰족 귀(안쪽 분홍), 이마 줄무늬, 위로 선 꼬리. */
internal val CAT = ShapeDef(
    "cat", ShapeKind.ANIMAL,
    StandSprite(
        listOf(
            ".................K.....", // 0  귀 끝
            ".....K..........KBK....", // 1
            "....KBKK.......KBBK....", // 2
            "....KB1BK.....KBB1BK...", // 3
            "....KB11BKKKKKBB11BK...", // 4
            "....KB1112BB2BBBB1BK...", // 5  이마 줄무늬
            "....KBBBB2BB2BBBB1BK...", // 6
            "...KBBBBBBBBBBBBBBBK...", // 7
            "...KBBBWBBBBBWBBBBBK...", // 8
            "..KBWWOWWWWWOWWBBBBK...", // 9  눈
            "..KBWWOWWWWWOWWWWBBBK..", // 10
            "...KLWWWPWWWWWWLWBBKBK.", // 11 코, 꼬리 끝
            "...KWWWKKKWWWWWWWBKBWBK", // 12 입
            "....KKWWWWWWWWWBKK.KWBK", // 13
            "......KKWWWWWWBBK..KBBK", // 14 턱
            "......KBBWWWWWBBBK.KBBK", // 15 몸통
            ".....KBBBWWWWWWBBBKKBBK", // 16
            ".....KBBWWWWWWWBBBKKBBK", // 17
            ".....KBBBWWWWWWBBBKKBBK", // 18
            "......KBBWWWWWWBBBBBBK.", // 19
            ".......KBBBBBBBBBBBBK..", // 20 다리
            ".......KWWBKKKWWBKKK...", // 21
            "........KKK...KKK......", // 22 발바닥
        ),
    ),
    fixed = mapOf('1' to 0xFFD8AFA5.toInt(), '2' to 0xFF78899B.toInt()),
    label = "고양이",
)

/** 토끼: 긴 귀(안쪽 분홍), 동그란 흰 꼬리. */
internal val RABBIT = ShapeDef(
    "rabbit", ShapeKind.ANIMAL,
    StandSprite(
        listOf(
            "...KK...KK.......", // 0  귀 끝
            "..KBPK.KBPK......", // 1
            "..KBPK.KBPK......", // 2
            "..KBPK.KBPK......", // 3
            "..KBPK.KBPK......", // 4
            "..KBBKKKBBK......", // 5
            ".KBBBBBBBBBK.....", // 6
            ".KBBBBBBBBBBK....", // 7
            ".KBWOWWWWOWBK....", // 8  눈
            ".KBWOWWWWOWBK....", // 9
            ".KWWWWPWWWWWK....", // 10 코
            ".KWWWWKWWWWWK....", // 11 입
            "..KWWWWWWWWK.....", // 12
            "...KKWWWWKK......", // 13
            "....KBBBBK.......", // 14
            "...KBBWWWBBK.....", // 15
            "...KBBWWWBBK.....", // 16
            "...KBBWWWBBKKK...", // 17 꼬리
            "...KBBWWWBBKWWK..", // 18
            "...KBBBBBBBKKK...", // 19
            "...KBBBK.KBBK....", // 20
            "...KWWWK.KWWK....", // 21
            "...KKKKK.KKKK....", // 22
        ),
    ),
    label = "토끼",
)

/** 햄스터: 키 작은 몸(머리 8줄부터), 통통한 볼, 아주 짧은 꼬리. */
internal val HAMSTER = ShapeDef(
    "hamster", ShapeKind.ANIMAL,
    StandSprite(
        listOf(
            ".................", // 0
            ".................", // 1
            ".................", // 2
            ".................", // 3
            ".................", // 4
            ".................", // 5
            ".................", // 6
            ".................", // 7
            "...KK.....KK.....", // 8  귀
            "..KPBKKKKKBPK....", // 9
            "..KBBBBBBBBBK....", // 10
            ".KBWOWWWWOWBK....", // 11 눈
            ".KWWOWWWWOWWK....", // 12
            "KWWWWWPWWWWWWK...", // 13 볼 주머니, 코
            ".KWWWWKWWWWWK....", // 14 입
            "..KBWWWWWWWBK....", // 15
            ".KBBWWWWWWWBBK...", // 16
            ".KBBWWWWWWWBBKK..", // 17 꼬리
            ".KBBWWWWWWWBBK...", // 18
            "..KBBBBBBBBBK....", // 19
            "...KBBK.KBBBK....", // 20
            "...KWWK.KWWWK....", // 21
            "...KKKK.KKKKK....", // 22
        ),
    ),
    label = "햄스터",
)
