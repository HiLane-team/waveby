package com.drivepark.app.ui.pixel.shapes

import com.drivepark.app.ui.pixel.ShapeDef
import com.drivepark.app.ui.pixel.ShapeKind
import com.drivepark.app.ui.pixel.StandSprite

/**
 * 사람(오리지널). 짧은 머리(H), 얼굴·손(W 피부), 반팔 티(B 상의), 바지(N 하의), 신발(K).
 * 왼쪽을 보는 3/4 구도라 앞쪽(왼쪽) 팔만 보인다. 두 발 3~7열, 9~12열.
 */
internal val PERSON = ShapeDef(
    "person", ShapeKind.PERSON,
    StandSprite(
        listOf(
            ".................", // 0
            ".................", // 1
            ".....KKKKKK......", // 2
            "...KKHHHHHHKK....", // 3  정수리
            "..KHHHHHHHHHHK...", // 4
            ".KHHHHHHHHHHHHK..", // 5
            ".KHHHHHHHHHHHHK..", // 6
            ".KHHWWWHHHHHHHK..", // 7  앞머리
            ".KHWWWWWWWHHHHK..", // 8
            ".KWWOWWWWOWWHHK..", // 9  눈
            ".KWWOWWWWOWWWHK..", // 10
            ".KWWWWWWWWWWWWK..", // 11
            ".KW1WWWWWW1WWWK..", // 12 볼(고정 분홍)
            "..KWWWWKWWWWWK...", // 13 입
            "...KKWWWWWWWKK...", // 14 턱
            "...KBBBBBBBK.....", // 15 어깨
            "..KBBBBBBBBBK....", // 16 소매
            "..KBKBBBBBBBK....", // 17
            "..KWKBBBBBBBK....", // 18 손
            "..KKKNNNNNNNK....", // 19 바지
            "....KNNNKNNNK....", // 20
            "....KNNNKNNNK....", // 21
            "....KKKK.KKKK....", // 22 신발
        ),
    ),
    // 볼은 고정 분홍(P는 엔진에서 코로 읽히고 먹색으로 칠해지므로 쓰지 않는다).
    fixed = mapOf('1' to 0xFFF2A7B5.toInt()),
    label = "사람",
    // 코는 그리지 않고 입(7, 13) 두 줄 위를 기준점으로 둔다 → 놀람이면 입이 위로 한 칸 길어진다(턱에 붙지 않게).
    nose = 7 to 10,
)
