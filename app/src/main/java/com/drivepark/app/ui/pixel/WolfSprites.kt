package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.Hat

/**
 * 기본 캐릭터: 오리지널 늑대. 17×23, 왼쪽을 바라보는 3/4 구도(오른쪽을 볼 때는 화면에서 뒤집는다).
 * 격자와 동작 규칙은 docs/character-ref/baekho/백호_캐릭터_참고.md 를 따른다(같은 글자 체계, 같은 줄 구간).
 *
 * 글자(설명서 기준): K 먹 테두리, W 흰 털, L 그늘, B 진회색 털(몸 색 선택에 따라 바뀜), O 눈(초록), P 코(검정)
 * 얼굴: 눈 = 3·8열 9~11줄 초록 세로 3칸, 눈썹 = 눈 바로 위 검은 가로 2칸(ㄱ자), 코 = (5,12) 검은 한 칸.
 * 줄 구간: 0~14 머리, 15~19 몸통, 20~22 다리(왼쪽 0~8칸, 오른쪽 9~16칸). 22줄이 늘 발바닥.
 * 디자인 담당이 BASE 글자를 고쳐 그림을 바꿀 수 있다. 크기 검사는 PixelSpriteTest가 한다.
 */
object WolfSprites {
    const val WIDTH = 17
    const val HEIGHT = 23

    // 동작 때 격자 밖으로 1칸씩 나가는 칸(걷기에서 몸이 올라간 귀 끝, 옮긴 다리·꼬리 끝)이 잘리지 않게
    // 그림판은 둘레에 여백을 둔다. 백호 원본도 여백 있는 판에 그린다.
    const val PAD_X = 1
    const val PAD_TOP = 1
    const val CANVAS_W = WIDTH + PAD_X * 2 // 19
    const val CANVAS_H = HEIGHT + PAD_TOP // 24 (22줄 발바닥이 판의 맨 아래)
    private const val HEAD_END = 14
    private const val LEGS_FROM = 20

    private val BASE = listOf(
        ".K.........K.....", // 0  귀 끝
        "KBK.......KBK....", // 1
        "KLBK.....KBLBK...", // 2
        "KLLBK...KBLLBK...", // 3
        "KLLBBKKKBBBBBBK..", // 4  머리 위
        ".KBBBBBBBBBBBBK..", // 5
        ".KBBBBBBBBBBBBBK.", // 6
        ".KBBBBBBBWWBBBBK.", // 7
        "KKKKWBWKKWWWWBBKK", // 8  눈썹(ㄱ자의 가로)
        "KWWOWWWWOWWWWWBBK", // 9  눈
        ".KWOWWWWOWWWWWBK.", // 10
        "KLWOWWWWOWWWWLBK.", // 11
        ".KWWWPWWWWWWWWBK.", // 12 코
        "..KWWWWWWWWWLBK..", // 13
        "...KWWWWWWWLLK...", // 14 턱
        "...KKWWWWLBBKK...", // 15 몸통
        "...KLWWWWWBBBBK..", // 16
        "..KLWWWWWWBBBBKKK", // 17
        "..KLWWWWWWLBBKBBK", // 18 꼬리
        "..KKWWWWWWLBKBBBK", // 19
        "...KWLWWWKBLKBWWK", // 20 다리
        "...KWLKKKLBLKKKK.", // 21
        "...KKKK..KKKK....", // 22 발바닥
    )

    /** 설명서 글자 → 앱 팔레트 슬롯. 진회색(B)이 몸 색 슬롯 D라서 털 색 선택이 그대로 적용된다. */
    private val TO_SLOT = mapOf('K' to 'o', 'W' to 'L', 'L' to 'M', 'B' to 'D', 'O' to 'E', 'Y' to 'H', 'P' to 'o', '.' to '.')

    private val EYES = listOf(3 to 9, 3 to 10, 3 to 11, 8 to 9, 8 to 10, 8 to 11)

    /** 칸 묶음. 키 = (x,y). 범위를 벗어난 칸은 그릴 때 버린다. */
    private class Grid(val px: MutableMap<Pair<Int, Int>, Char>) {
        fun shift(x0: Int, y0: Int, x1: Int, y1: Int, dx: Int, dy: Int): Grid {
            if (dx == 0 && dy == 0) return this
            val moved = HashMap<Pair<Int, Int>, Char>()
            val out = HashMap<Pair<Int, Int>, Char>()
            for ((p, c) in px) {
                if (p.first in x0..x1 && p.second in y0..y1) moved[(p.first + dx) to (p.second + dy)] = c else out[p] = c
            }
            out.putAll(moved)
            return Grid(out)
        }

        /** 몇 줄을 빼고 위쪽을 내려 붙인다(앉기). */
        fun drop(rows: Set<Int>): Grid {
            val out = HashMap<Pair<Int, Int>, Char>()
            for ((p, c) in px) {
                if (p.second in rows) continue
                out[p.first to (p.second + rows.count { it > p.second })] = c
            }
            return Grid(out)
        }

        fun legs(lx: Int, ly: Int, rx: Int, ry: Int): Grid =
            shift(0, LEGS_FROM, 8, HEIGHT - 1, lx, ly).shift(9, LEGS_FROM, WIDTH, HEIGHT - 1, rx, ry)

        /** 눈 감기: 가운데 칸만 검게 남긴다. dy는 머리가 내려간 만큼. */
        fun closeEyes(which: List<Pair<Int, Int>>, dy: Int = 0): Grid {
            for ((x, y) in which) {
                val p = x to (y + dy)
                if (p in px) px[p] = if (y == 10) 'K' else 'W'
            }
            return this
        }

        /** 여백 있는 그림판으로 옮겨 적는다. 판 밖으로 나간 칸이 있으면 버리지 않고 오류로 알린다. */
        fun toSprite(): PixelSprite {
            px.keys.forEach { (x, y) ->
                check(x + PAD_X in 0 until CANVAS_W && y + PAD_TOP in 0 until CANVAS_H) { "그림판 밖 칸 ($x,$y)" }
            }
            return PixelSprite(
                (0 until CANVAS_H).map { cy ->
                    String(CharArray(CANVAS_W) { cx -> TO_SLOT.getValue(px[(cx - PAD_X) to (cy - PAD_TOP)] ?: '.') })
                },
            )
        }
    }

    private fun base(): Grid {
        val px = HashMap<Pair<Int, Int>, Char>()
        BASE.forEachIndexed { y, row -> row.forEachIndexed { x, c -> if (c != '.') px[x to y] = c } }
        return Grid(px)
    }

    /** 표정은 몸을 움직이기 전에 얼굴 칸을 고쳐 둔다(머리와 함께 움직이도록). */
    private fun withFace(face: Face): Grid {
        val g = base()
        when (face) {
            Face.SMILE -> Unit
            Face.WINK -> g.closeEyes(EYES.take(3))
            Face.SURPRISED -> g.px[5 to 14] = 'K' // 코 아래 동그랗게 벌린 입
            Face.SLEEPY -> g.closeEyes(EYES)
        }
        return g
    }

    /**
     * 백호 규칙(설명서 '동작' 표)을 앱의 4프레임에 맞춘 것.
     * IDLE: 0·2 기본, 1·3 머리가 1칸 내려감(숨쉬기). WALK: 다리 ±1칸, 홀수 장에서 몸이 1칸 올라감.
     * SIT: 17·18·20줄을 빼고 아래 두 줄을 포갠 다리로 다시 그림.
     */
    fun body(pose: Pose, frame: Int, face: Face = Face.SMILE): PixelSprite {
        var g = withFace(face)
        val f = frame % 4
        when (pose) {
            Pose.IDLE -> if (f % 2 == 1) g = g.shift(0, 0, WIDTH, HEAD_END, 0, 1)
            Pose.WALK -> {
                val st = intArrayOf(-1, 0, 1, 0)[f]
                g = g.legs(st, 0, -st, 0)
                if (f % 2 == 1) {
                    // 백호 규칙은 몸(0~19줄)만 1칸 올려서 19줄이 통째로 비어 몸통과 다리가 끊겨 보인다.
                    // 원래 19줄(엉덩이·꼬리)을 그대로 남겨 몸이 1칸 늘어난 것처럼 이어 붙인다.
                    val hip = g.px.filterKeys { it.second == LEGS_FROM - 1 }
                    g = g.shift(0, 0, WIDTH, LEGS_FROM - 1, 0, -1)
                    hip.forEach { (p, c) -> g.px.putIfAbsent(p, c) }
                }
            }
            Pose.SIT -> {
                g = g.drop(setOf(17, 18, 20))
                for (x in 2 until 15) { g.px.remove(x to 21); g.px.remove(x to 22) }
                for (x in 2 until 15) {
                    g.px[x to 21] = if (x in setOf(5, 6, 10, 11)) 'L' else 'W'
                    g.px[x to 22] = 'K'
                }
                listOf(1 to 21, 15 to 21, 1 to 22, 15 to 22).forEach { g.px[it] = 'K' }
            }
        }
        return g.toSprite()
    }

    /** 모자·소품을 머리와 같이 움직일 칸 수. */
    fun headDy(pose: Pose, frame: Int): Int = when (pose) {
        Pose.IDLE -> if (frame % 2 == 1) 1 else 0
        Pose.WALK -> if (frame % 2 == 1) -1 else 0
        Pose.SIT -> 3
    }

    /** 모자·소품: 격자(17×23) 좌표로 적고, 몸과 같은 여백을 붙여 그림판 크기로 만든다. */
    private fun overlay(vararg rows: Pair<Int, String>): PixelSprite {
        val pad = ".".repeat(PAD_X)
        val list = MutableList(CANVAS_H) { ".".repeat(CANVAS_W) }
        rows.forEach { (y, r) -> require(r.length == WIDTH) { "${y}줄 길이 ${r.length}" }; list[y + PAD_TOP] = pad + r + pad }
        return PixelSprite(list)
    }

    // 모자·소품은 앱 팔레트 슬롯으로 적는다(o 외곽선, 1~4 모자색, H 흰 하이라이트, R 목도리, B 풍선).
    fun hat(hat: Hat): PixelSprite? = when (hat) {
        Hat.NONE -> null
        Hat.CAP -> overlay(
            2 to ".....ooooo.......",
            3 to "....o11111o......",
            4 to "..ooo111111o.....",
        )
        Hat.BEANIE -> overlay(
            0 to "......33.........",
            1 to ".....o33o........",
            2 to "....o2222o.......",
            3 to "...o222222o......",
            4 to "...o333333o......",
        )
        Hat.CROWN -> overlay(
            1 to "....4.4.4........",
            2 to "....44444........",
            3 to "....4H4H4........",
        )
    }

    fun accessory(accessory: Accessory): PixelSprite? = when (accessory) {
        Accessory.NONE -> null
        // 두 눈(3·8열)을 감싸는 동그란 테, 가운데 다리.
        Accessory.GLASSES -> overlay(
            9 to "..o.o..o.o.......",
            10 to "..o.oooo.o.......",
            11 to "..o.o..o.o.......",
            12 to "..ooo..ooo.......",
        )
        Accessory.SCARF -> overlay(
            14 to "...oRRRRRRRRRo...",
            15 to "...oRRRRRRRRRo...",
            16 to "...oRRo..........",
            17 to "...oRRo..........",
        )
        // 귀 뒤에 묶은 풍선(오른쪽 위).
        Accessory.BALLOON -> overlay(
            0 to "..............BBB",
            1 to ".............BBHB",
            2 to ".............BBBB",
            3 to "..............BB.",
            4 to "...............o.",
            5 to "...............o.",
        )
    }

    /** 테스트용: 모든 스프라이트. */
    fun all(): List<PixelSprite> =
        Pose.entries.flatMap { p -> Face.entries.flatMap { f -> (0..3).map { body(p, it, f) } } } +
            Hat.entries.mapNotNull(::hat) + Accessory.entries.mapNotNull(::accessory)

    /** 테스트용: 기본 자세에서 슬롯이 처음 나오는 그림판 좌표. */
    fun firstPixel(slot: Char): Pair<Int, Int> {
        body(Pose.IDLE, 0).rows.forEachIndexed { y, row -> val x = row.indexOf(slot); if (x >= 0) return x to y }
        error("no $slot")
    }
}

enum class Pose { IDLE, WALK, SIT }
