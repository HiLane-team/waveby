package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.Face
import com.drivepark.app.domain.GridSize
import com.drivepark.app.ui.pixel.StandSprite.Companion.WIDTH

/** 동작. 서 있는 그림 한 장에서 모두 자동으로 만든다. */
enum class Pose { IDLE, WALK, SIT, DOZE, CROUCH, AIR }

/** 걷기 모양 선택지. lift = 발을 드는 높이(칸), tailWag = 한 걸음마다 꼬리를 살랑. */
data class WalkStyle(val lift: Int = 1, val tailWag: Boolean = false) {
    init { require(lift in 1..2) { "발 높이는 1~2칸" } }

    companion object {
        /** 앱에서 쓰는 걷기 모양: 발 2칸 + 꼬리 살랑(2026-11-06 사용자 선택 "D"). */
        val DEFAULT = WalkStyle(lift = 2, tailWag = true)
    }
}

/** 동작 한 장면과, 그 장면에서 머리가 움직인 칸 수(모자·소품을 같이 옮길 때 씀, 아래가 +). */
data class Posed(val sprite: PixelSprite, val headDy: Int)

/**
 * 서 있는 그림 + 몸 지도 → 동작 장면(그림판 = 그림 크기 + 좌우 1칸·위 1칸 여백, 앱 슬롯 글자).
 * 줄 번호는 몸 지도의 몸 구역([BodyLines])에서 계산한다. 17×23 기본값이면 늑대와 똑같다.
 * 규칙은 docs/superpowers/specs/2026-11-06-auto-animation-engine-design.md 4장.
 */
object Poser {
    const val PAD_X = 1
    const val PAD_TOP = 1
    /** 기본(17×23) 그림판 크기. 테스트·미리보기 도구용. 그림마다는 [canvasW]·[canvasH]. */
    const val CANVAS_W = WIDTH + PAD_X * 2 // 19
    const val CANVAS_H = StandSprite.HEIGHT + PAD_TOP // 24

    /** 화면에서 도트 크기를 정하는 기준 높이(17×23 그림판 높이). 모든 크기가 같은 도트 크기로 보인다. */
    const val REF_CANVAS_H = CANVAS_H

    /** 다리 없는 모양이 걸을 때 뛰는 높이(칸). 그림 위에 빈 줄이 모자라면 그만큼 덜 뛴다(그림판 밖으로 나가지 않게). */
    const val HOP_HEIGHT = 2

    fun canvasW(size: GridSize) = size.w + PAD_X * 2
    fun canvasH(size: GridSize) = size.h + PAD_TOP

    /** 서 있는 그림 글자 → 앱 팔레트 슬롯. 몸 색(B)이 슬롯 D라 털 색 고르기가 그대로 적용된다. */
    private val TO_SLOT = mapOf(
        'K' to 'o', 'W' to 'L', 'L' to 'M', 'B' to 'D', 'O' to 'E', 'P' to 'o',
        'H' to 'h', 'N' to 'n', '1' to 'a', '2' to 'b', '3' to 'c', '4' to 'd',
    )

    fun pose(
        s: StandSprite,
        a: Anatomy,
        pose: Pose,
        frame: Int,
        face: Face = Face.SMILE,
        eyesClosed: Boolean = false,
        walk: WalkStyle = WalkStyle.DEFAULT,
        /** 모양이 정해 둔 코 기준점([ShapeDef.nose]). 없으면 그림의 코(P) → 눈 위치 순서로 정한다. */
        nose: Pair<Int, Int>? = null,
    ): Posed {
        val head = a.lines.headEnd
        val legs = a.lines.legsFrom
        val sole = a.size.h - 1
        var g = Grid.of(s, a.size)
        // 표정과 눈 감기는 몸을 움직이기 전에 고쳐 둔다(머리와 함께 움직이도록).
        when (face) {
            Face.SMILE -> Unit
            Face.WINK -> a.eyes.firstOrNull()?.let { g.closeEye(it) }
            Face.SLEEPY -> a.eyes.forEach { g.closeEye(it) }
            // 놀람: 코 두 칸 아래에 입. 그 아래 칸도 머리 구역 안이면 한 칸 더 길게(벌린 입). 이호랑은 입이 머리 마지막 줄이라 한 칸.
            Face.SURPRISED -> (nose ?: noseAnchor(s, a))?.let { (x, y) ->
                for (my in listOf(y + 2, y + 3)) if (my <= head && (x to my) in g.px) g.px[x to my] = 'K'
            }
        }
        if (eyesClosed || pose == Pose.DOZE) a.eyes.forEach { g.closeEye(it) }

        val f = frame.mod(4)
        val dy: Int
        // 다리 없는 모양(통통): 걷기는 통째로 2칸 뛰고, 앉기·웅크리기는 몸 가운데 1줄을 빼서 1칸 낮춘다.
        val squash = setOf((head + 1 + legs - 1) / 2)
        val hop = a.gait == Gait.HOP
        when (pose) {
            Pose.IDLE -> {
                if (f % 2 == 1) g = g.shift(0..head, 1)
                dy = if (f % 2 == 1) 1 else 0
            }
            Pose.WALK -> if (hop) {
                val top = s.rows.indexOfFirst { r -> r.any { it != '.' } }.coerceAtLeast(0)
                val lift = minOf(HOP_HEIGHT, top + PAD_TOP)
                if (f % 2 == 1) g = g.shift(0..sole, -lift)
                dy = if (f % 2 == 1) -lift else 0
            } else {
                if (f % 2 == 1) {
                    g = g.raiseBody(legs)
                    val single = a.feet.size == 1
                    val pair = if (f == 1) a.pairA else a.pairB
                    pair.forEach { g.liftFoot(it, if (single) 1 else walk.lift, sole) }
                    if (walk.tailWag && f == 1 && !single) g.stretchTail(a.tail, head + 1, legs)
                }
                dy = if (f % 2 == 1) -1 else 0
            }
            Pose.SIT -> if (hop) {
                g = g.drop(squash); dy = 1
            } else {
                g = g.sit(a, legs, sole); dy = 3
            }
            Pose.DOZE -> {
                val nod = frame.mod(2) == 1
                if (nod) g = g.shift(0..head, 1)
                if (hop) {
                    g = g.drop(squash)
                    dy = if (nod) 2 else 1
                } else {
                    g = g.sit(a, legs, sole)
                    dy = if (nod) 4 else 3
                }
            }
            Pose.CROUCH -> { g = g.drop(if (hop) squash else setOf(legs - 2)); dy = 1 }
            Pose.AIR -> { if (!hop) a.feet.forEach { g.liftFoot(it, 1, sole) }; dy = 0 }
        }
        return Posed(g.toSprite(), dy)
    }

    /**
     * 표정(놀람)의 기준이 되는 코 칸. 그림에 코(P)가 있으면 그 칸, 없으면 눈으로 정한다(이호랑 기준):
     * 두 눈 → 두 눈 왼쪽 열의 가운데(왼쪽으로 내림)·두 눈 아래 한 줄. 눈 하나 → 그 눈 두 칸 앞(왼쪽)·아래 한 줄. 눈 없음 → null.
     */
    fun noseAnchor(s: StandSprite, a: Anatomy): Pair<Int, Int>? {
        firstNose(s)?.let { return it }
        val eyes = a.eyes
        if (eyes.isEmpty()) return null
        val below = eyes.take(2).flatten().maxOf { it.y } + 1
        if (eyes.size == 1) return maxOf(0, eyes[0].minOf { it.x } - 2) to below
        return (eyes[0].minOf { it.x } + eyes[1].minOf { it.x }) / 2 to below
    }

    private fun firstNose(s: StandSprite): Pair<Int, Int>? {
        for (y in s.rows.indices) for (x in 0 until s.size.w) if (s[x, y] == 'P') return x to y
        return null
    }

    /** 칸 묶음. 키 = (x, y) 서 있는 그림 좌표. 그림판 여백까지 넘어갈 수 있다. */
    private class Grid(val px: MutableMap<Pair<Int, Int>, Char>, val size: GridSize) {

        /** rows 줄의 칸을 dy만큼 옮긴다. 옮긴 칸이 다른 칸을 덮는다. */
        fun shift(rows: IntRange, dy: Int): Grid {
            val moved = HashMap<Pair<Int, Int>, Char>()
            val out = HashMap<Pair<Int, Int>, Char>()
            for ((p, c) in px) if (p.second in rows) moved[p.first to p.second + dy] = c else out[p] = c
            out.putAll(moved)
            return Grid(out, size)
        }

        /** 몸(다리 위 줄까지)을 1칸 올리고, 원래 다리 바로 위 줄(엉덩이)을 빈 자리에만 남겨 다리와 잇는다. */
        fun raiseBody(legs: Int): Grid {
            val hip = px.filterKeys { it.second == legs - 1 }
            val g = shift(0 until legs, -1)
            hip.forEach { (p, c) -> g.px.putIfAbsent(p, c) }
            return g
        }

        /** 몇 줄을 빼고 위쪽을 내려 붙인다. */
        fun drop(rows: Set<Int>): Grid {
            val out = HashMap<Pair<Int, Int>, Char>()
            for ((p, c) in px) {
                if (p.second in rows) continue
                out[p.first to p.second + rows.count { it > p.second }] = c
            }
            return Grid(out, size)
        }

        /** 발 하나를 height칸 든다: 아래 height줄을 지우고 그 위 줄을 발바닥(K)으로. */
        fun liftFoot(cols: IntRange, height: Int, sole: Int) {
            for (x in cols) {
                for (k in 0 until height) px.remove(x to sole - k)
                val top = x to sole - height
                if (top in px) px[top] = 'K'
            }
        }

        /**
         * 꼬리 살랑: 꼬리 아래쪽(몸 구역 셋째 줄부터, 늑대 17줄)을 오른쪽으로 1칸 늘린다(원래 칸은 남김).
         * 꼬리 윗부분은 등에 붙어 있어 그대로 둔다. 몸을 올린 뒤라 다리 위 줄까지는 1칸 위에 있다.
         */
        fun stretchTail(tail: Set<Cell>, bodyFrom: Int, legs: Int) {
            if (tail.isEmpty()) return
            val rows = tail.filter { it.y >= bodyFrom + 2 }
                .flatMap { c -> if (c.y < legs) listOfNotNull(c.y - 1, if (c.y == legs - 1) c.y else null) else listOf(c.y) }
                .toSet()
            val cols = tail.minOf { it.x }..tail.maxOf { it.x }
            for (y in rows) for (x in cols.reversed()) px[x to y]?.let { px[x + 1 to y] = it }
        }

        /** 앉기: 다리 시작 3줄 위·2줄 위·다리 시작 줄(늑대 17·18·20)을 빼서 몸을 내리고, 맨 아래 2줄을 접은 다리로 다시 그린다. */
        fun sit(a: Anatomy, legs: Int, sole: Int): Grid {
            val belly = (0 until size.w).mapNotNull { x -> px[x to legs - 1] }.filter { it in "WLBN1234" }
                .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: 'W'
            val g = drop(setOf(legs - 3, legs - 2, legs))
            if (a.feet.isEmpty()) return g
            val from = a.feet.first().first - 1
            val to = a.feet.last().last + 1
            for (x in from..to) {
                g.px[x to sole - 1] = if (x == from || x == to) 'K' else belly
                g.px[x to sole] = 'K'
            }
            return g
        }

        /** 눈 감기: 가운데 칸은 테두리, 나머지는 바로 왼쪽 털 글자(없으면 W). */
        fun closeEye(eye: List<Cell>) {
            val sorted = eye.sortedWith(compareBy({ it.y }, { it.x }))
            val mid = sorted[sorted.size / 2]
            for (c in sorted) {
                val p = c.x to c.y
                if (p !in px) continue
                px[p] = if (c == mid) 'K' else px[c.x - 1 to c.y]?.takeIf { it !in "OK." } ?: 'W'
            }
        }

        fun toSprite(): PixelSprite {
            val cw = canvasW(size)
            val ch = canvasH(size)
            px.keys.forEach { (x, y) ->
                check(x + PAD_X in 0 until cw && y + PAD_TOP in 0 until ch) { "그림판 밖 칸 ($x,$y)" }
            }
            return PixelSprite(
                (0 until ch).map { cy ->
                    String(CharArray(cw) { cx -> px[(cx - PAD_X) to (cy - PAD_TOP)]?.let { TO_SLOT.getValue(it) } ?: '.' })
                },
            )
        }

        companion object {
            fun of(s: StandSprite, size: GridSize): Grid {
                val px = HashMap<Pair<Int, Int>, Char>()
                s.rows.forEachIndexed { y, row -> row.forEachIndexed { x, c -> if (c != '.') px[x to y] = c } }
                return Grid(px, size)
            }
        }
    }
}
