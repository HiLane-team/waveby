package com.drivepark.app.ui.pixel.shapes

import com.drivepark.app.ui.pixel.BodyLines
import com.drivepark.app.ui.pixel.Gait
import com.drivepark.app.ui.pixel.ShapeDef
import com.drivepark.app.ui.pixel.ShapeKind
import com.drivepark.app.ui.pixel.StandSprite

// 지역 캐릭터(오리지널, 지자체 공식 마스코트 아님): 특산물 자체에 얼굴과 짧은 다리. 색은 고정 색 1~4만 쓴다. id = Region.id.
// Codex 시안(2026-11-08, PR #21)을 바탕으로 했다. 눈 색(eye)은 캐릭터마다 어울리는 색(퍼스널 컬러)으로 고정한다.

/** 횡성 한우 "음메롱"(2026-11-09 이름 정함, 횡성한우축제 캐릭터 "한우리"와 겹치지 않게): 갈색 송아지, 뿔과 옆으로 난 귀, 크림색 주둥이(콧구멍 두 칸), 몸 뒤에 짧은 꼬리(끝 털 진한 갈색). 눈 = 짙은 밤색. */
internal val HOENGSEONG = ShapeDef(
    "hoengseong", ShapeKind.MASCOT,
    StandSprite(
        listOf(
            "......KK........K......", // 0  뿔
            ".....K22K......K2K.....", // 1
            ".....K22K.K...K22K.....", // 2
            ".....K222K1KKK222K.....", // 3
            ".KKK..K223331222K..KKK.", // 4
            "K111KK1211311112K.K111K", // 5  귀
            ".K131141111141111K131K.", // 6
            ".K1111O11111O11111111K.", // 7  눈
            "..KK11O11111O114111KK..", // 8
            "...K11122222111441K....", // 9  주둥이
            "...K12222222224411K....", // 10
            "..K222P2222P22241K.....", // 11 콧구멍
            "..K22222222222211K.....", // 12
            "..K222233322221KK......", // 13
            "...KKKK222222111K......", // 14 턱
            ".....K33112221131KK....", // 15 몸통
            ".....K331222221333K....", // 16
            ".....K333222223333KKK..", // 17 꼬리
            ".....K341222221433111K.", // 18
            "......K31111111311144K.", // 19 꼬리 끝 털
            "......K111111111111KKK.", // 20 다리
            "......K4444KK4444K.....", // 21 발굽
            ".......KKKK..KKKK......", // 22
        ),
    ),
    fixed = mapOf('1' to 0xFFAB825B.toInt(), '2' to 0xFFE6CBA3.toInt(), '3' to 0xFFC79E74.toInt(), '4' to 0xFF795F48.toInt()),
    label = "한우 음메롱",
    eye = 0xFF5A2E1A.toInt(),
)

/** 보성 녹차: 찻잎을 머리에 인 녹차 몸, 두 발. 눈 = 찻물 호박색. */
internal val BOSEONG = ShapeDef(
    "boseong", ShapeKind.MASCOT,
    StandSprite(
        listOf(
            ".......KK..............", // 0  찻잎
            ".......K4KK.......KK...", // 1
            ".......K424K....KK33K..", // 2
            ".......K424K..KK333K...", // 3
            "......KK4244KK33443K...", // 4
            ".....K11K444KK3333K....", // 5
            "....K111K44KK33333K....", // 6
            "....K1111KKK33333K.....", // 7
            "...K111KK111KK3KKK.....", // 8
            "...K1KK22KKKKKKK11K....", // 9
            "...KK2O22222O22K11K....", // 10 눈
            "...KK2O22222O222K1K....", // 11
            "...KK22222222222KK.....", // 12
            "....KKK2KK2222KK1K.....", // 13
            ".....K1KKKKKKK1KK......", // 14
            ".....KKKK1111KKK1K.....", // 15
            "....KKK11KKKKKK3KK.....", // 16 몸통
            "....K1KKK11KK3333KK....", // 17
            "...K11K33KK33333K1K....", // 18
            "....KK1K3333333K1K.....", // 19
            ".....KKKKKKKKKKKKK.....", // 20 다리
            ".....K333K...K333K.....", // 21
            ".....KKKKK...KKKKK.....", // 22
        ),
    ),
    fixed = mapOf('1' to 0xFF78965B.toInt(), '2' to 0xFFD7E2AF.toInt(), '3' to 0xFF405E42.toInt(), '4' to 0xFFA6BC7A.toInt()),
    label = "보성 녹차",
    lines = BodyLines(15, 21),
    eye = 0xFFC08A1E.toInt(),
)

/** 제주 감귤: 동그란 귤 몸, 꼭지 잎, 두 발. 눈 = 귤잎 초록. */
internal val JEJU = ShapeDef(
    "jeju", ShapeKind.MASCOT,
    StandSprite(
        listOf(
            ".............KKKKKK....", // 0  잎
            "............K333333K...", // 1
            ".........33K332223K....", // 2
            ".........3K333333K.....", // 3
            ".......KK33K333KK......", // 4  꼭지
            ".....KK11111KKKKK......", // 5
            "....K122211111111KK....", // 6
            "...K111111111111111K...", // 7
            "...K221111111111111K...", // 8
            "..K12211111111111411K..", // 9
            "..K111O11111O1111111K..", // 10 눈
            ".K1111O11111O11111111K.", // 11
            ".K1111O11111O11111411K.", // 12
            ".K1111111111111111111K.", // 13
            "..K11111111111111111K..", // 14
            "..K11111KKK111111111K..", // 15 입
            "...K141111111111411K...", // 16
            "...K111111111111111K...", // 17
            "....KK11111141111KK....", // 18
            "......KK11111KKKK......", // 19
            "......K1KKKKKK11K......", // 20 다리
            "......K11K...K11K......", // 21
            "......KKKK...KKKK......", // 22
        ),
    ),
    fixed = mapOf('1' to 0xFFE8A246.toInt(), '2' to 0xFFF5CF83.toInt(), '3' to 0xFF537846.toInt(), '4' to 0xFFC4833E.toInt()),
    label = "제주 감귤",
    eye = 0xFF3E7B3A.toInt(),
)

/** 강릉 커피: 거품 얹은 커피잔 몸, 오른쪽 손잡이, 다리 없이 통통 걷기. 눈 = 에스프레소 갈색. */
internal val GANGNEUNG = ShapeDef(
    "gangneung", ShapeKind.MASCOT,
    StandSprite(
        listOf(
            "...................", // 0
            "...................", // 1
            "...................", // 2
            "....KKKKKKK........", // 3  거품
            "...K3KKKKKKKK......", // 4
            "..K3K422244K3K.....", // 5
            ".K3K44422244K3K....", // 6
            ".K3KK444444K33K....", // 7
            ".K333KKKKKKK3K.....", // 8
            ".KKK33333KKKKKKKK..", // 9
            ".K11KKKKK1111K111K.", // 10 손잡이
            ".K11111111111K1111K", // 11
            ".K11O1111O111KKK11K", // 12 눈
            ".K11O1111O111K.K11K", // 13
            "..K1O1111O111K.K11K", // 14
            "..K1111111111K.K11K", // 15
            "..K1111111111K.K11K", // 16
            "..K111KK11111KKK11K", // 17 입
            "..K1111111111K111K.", // 18
            "..K1111111111K111K.", // 19
            "..K1111111111KKKK..", // 20
            "..K144444444K......", // 21 띠
            "..K144444444K......", // 22
            "...K12222221K......", // 23
            "...K11111111K......", // 24
            "....K111111K.......", // 25
            ".....KKKKKKK.......", // 26 바닥
        ),
    ),
    fixed = mapOf('1' to 0xFFDCCDB3.toInt(), '2' to 0xFFF0E5CE.toInt(), '3' to 0xFF69503E.toInt(), '4' to 0xFFA57E58.toInt()),
    label = "강릉 커피",
    lines = BodyLines(19, 26),
    gait = Gait.HOP,
    eye = 0xFF6B3E26.toInt(),
)

/** 영덕 대게: 넓은 빨간 몸, 위로 든 집게, 눈자루, 다리 여섯. 눈 = 바다 파랑. */
internal val YEONGDEOK = ShapeDef(
    "yeongdeok", ShapeKind.MASCOT,
    StandSprite(
        listOf(
            "...KK.K..............K.K...", // 0  집게
            "..K1K.KK............KK.KK..", // 1
            "..K1KK11K..........K11KK1K.", // 2
            ".K11KK11KKKK...KKK.K11KK1K.", // 3
            ".K111111KKOK...KOK.K11111K.", // 4  눈자루
            ".K111111KKOK...KOK.K11111K.", // 5
            "..K1111KKKKKKKKKKK.K11111K.", // 6
            "..K111KK1KKK111KKKKK1111K..", // 7
            "...KK1KK11111111111K111K...", // 8
            ".....KK1111111111111KKK....", // 9
            "..KKKKK1111KKK111111KKKKK..", // 10 입
            "KK111K111111111111111K111KK", // 11 다리
            ".KKKKK11KKKKKKKKKK111KKKKK.", // 12
            "..KKKK11K222222222K11KKKK..", // 13 배
            "KK1111KKK222222222KKK1111KK", // 14
            ".KKKKKKK1KKKKKKKKK1KKKKKKK.", // 15
            "......K1KKKKKKKKKKK1K......", // 16
            "......K11K.......K11K......", // 17
            ".....KKKKK.......KKKKK.....", // 18
        ),
    ),
    fixed = mapOf('1' to 0xFFC87359.toInt(), '2' to 0xFFEBC39E.toInt()),
    label = "영덕 대게",
    eye = 0xFF4A90D9.toInt(),
)
