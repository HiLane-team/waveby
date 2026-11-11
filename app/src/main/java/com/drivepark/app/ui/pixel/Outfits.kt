package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.Hat
import com.drivepark.app.ui.pixel.Poser.PAD_TOP
import com.drivepark.app.ui.pixel.Poser.PAD_X

/**
 * 모자·소품을 몸 지도에 맞춰 놓는다. 결과는 그 그림의 그림판 크기 덧그림(앱 슬롯 글자: o 테두리, 1~4 모자색, H, R 목도리, B 풍선).
 * 모자·풍선 그림은 늑대 기준 좌표로 적어 두고(정수리 4줄, 머리 가운데 7열, 머리 오른쪽 끝 16열),
 * 각 그림의 몸 지도만큼 옮긴다. 늑대에서는 예전 위치와 똑같다.
 */
object Outfits {
    private const val REF_CROWN = 4
    private const val REF_CENTER = 7
    private const val REF_RIGHT = 16

    private val CAP = cells(
        2 to ".....ooooo.......",
        3 to "....o11111o......",
        4 to "..ooo111111o.....",
    )
    private val BEANIE = cells(
        0 to "......33.........",
        1 to ".....o33o........",
        2 to "....o2222o.......",
        3 to "...o222222o......",
        4 to "...o333333o......",
    )
    private val CROWN = cells(
        1 to "....4.4.4........",
        2 to "....44444........",
        3 to "....4H4H4........",
    )
    private val BALLOON = cells(
        0 to "..............BBB",
        1 to ".............BBHB",
        2 to ".............BBBB",
        3 to "..............BB.",
        4 to "...............o.",
        5 to "...............o.",
    )

    fun hat(h: Hat, a: Anatomy): PixelSprite? {
        val art = when (h) {
            Hat.NONE -> return null
            Hat.CAP -> CAP
            Hat.BEANIE -> BEANIE
            Hat.CROWN -> CROWN
        }
        return toSprite(clamp(art.mapKeys { (p, _) -> Cell(p.x + a.headCenterX - REF_CENTER, p.y + a.crownRow - REF_CROWN) }, a), a)
    }

    fun accessory(acc: Accessory, a: Anatomy, s: StandSprite): PixelSprite? = when (acc) {
        Accessory.NONE -> null
        Accessory.GLASSES -> if (a.eyes.isEmpty()) null else toSprite(clamp(glasses(a), a), a)
        Accessory.SCARF -> toSprite(clamp(scarf(a, s), a), a)
        Accessory.BALLOON -> toSprite(clamp(BALLOON.mapKeys { (p, _) -> Cell(p.x + a.headRight - REF_RIGHT, p.y + a.crownRow - REF_CROWN) }, a), a)
    }

    /** 눈마다 둘레 1칸 테, 테 가운데 줄에서 두 테 사이를 다리로 잇는다. */
    private fun glasses(a: Anatomy): Map<Cell, Char> {
        val out = HashMap<Cell, Char>()
        val boxes = a.eyes.map { eye -> (eye.minOf { it.x } - 1..eye.maxOf { it.x } + 1) to (eye.minOf { it.y } - 1..eye.maxOf { it.y } + 1) }
        for ((xs, ys) in boxes) for (x in xs) for (y in ys) {
            if (x == xs.first || x == xs.last || y == ys.first || y == ys.last) out[Cell(x, y)] = 'o'
        }
        boxes.zipWithNext { (xa, ya), (xb, _) ->
            val y = (ya.first + ya.last) / 2
            for (x in xa.last + 1 until xb.first) out[Cell(x, y)] = 'o'
        }
        return out
    }

    /** 목 줄(머리 끝 줄과 그 아래 줄, 늑대 14·15줄) 몸 폭만큼 빨간 띠, 앞쪽(왼쪽 끝)에서 2줄 늘어진 끝. */
    private fun scarf(a: Anatomy, s: StandSprite): Map<Cell, Char> {
        val out = HashMap<Cell, Char>()
        val bodyEnd = a.feet.lastOrNull()?.last?.plus(1) ?: (a.size.w - 1)
        val neck = a.lines.headEnd
        var left = a.headCenterX
        for (y in neck..neck + 1) {
            val run = run(s, y, a.headCenterX) ?: continue
            val from = run.first
            val to = minOf(run.last, bodyEnd)
            if (to <= from) continue
            left = from
            for (x in from..to) out[Cell(x, y)] = if (x == from || x == to) 'o' else 'R'
        }
        for (y in neck + 2..neck + 3) {
            out[Cell(left, y)] = 'o'; out[Cell(left + 1, y)] = 'R'; out[Cell(left + 2, y)] = 'R'; out[Cell(left + 3, y)] = 'o'
        }
        return out
    }

    /** 줄 y에서 x를 포함하는 칸 덩어리(없으면 가장 긴 덩어리). */
    private fun run(s: StandSprite, y: Int, x: Int): IntRange? {
        val runs = mutableListOf<IntRange>()
        var start = -1
        for (i in 0..s.size.w) {
            val on = i < s.size.w && s.filled(i, y)
            if (on && start < 0) start = i
            if (!on && start >= 0) { runs += start until i; start = -1 }
        }
        return runs.firstOrNull { x in it } ?: runs.maxByOrNull { it.last - it.first }
    }

    /**
     * 동작에서 머리가 움직이는 범위(Poser의 headDy): 걷기 −1 ~ 졸기 +4, 통통 걷기는 −2까지. 덧그림은 이만큼 여유를 둔다.
     * 위 여유는 다리 있는 모양 1칸(늑대 모자 위치 그대로), 통통 모양 [Poser.HOP_HEIGHT]칸.
     */
    private fun headUp(a: Anatomy) = if (a.gait == Gait.HOP) Poser.HOP_HEIGHT else 1
    private const val HEAD_DOWN = 4

    /**
     * 그림판(여백 포함) 밖으로 나가면 통째로 안쪽으로 민다. 위아래는 머리가 움직여도 그림판 안에 남도록
     * 머리 이동 범위만큼 더 안쪽으로 맞춘다(렌더러는 밖으로 나가면 예외).
     */
    private fun clamp(m: Map<Cell, Char>, a: Anatomy): Map<Cell, Char> {
        val cw = Poser.canvasW(a.size)
        val ch = Poser.canvasH(a.size)
        if (m.isEmpty()) return m
        val minX = m.keys.minOf { it.x }; val maxX = m.keys.maxOf { it.x }
        val minY = m.keys.minOf { it.y }; val maxY = m.keys.maxOf { it.y }
        val dx = when {
            minX < -PAD_X -> -PAD_X - minX
            maxX > cw - 1 - PAD_X -> cw - 1 - PAD_X - maxX
            else -> 0
        }
        val top = -PAD_TOP + headUp(a)
        val bottom = ch - 1 - PAD_TOP - HEAD_DOWN
        // 직접 그린 이상한 그림(예: 눈이 세로로 아주 긴 그림)에서는 덧그림이 어디에도 들어가지 않는다. 그때는 빼고 그린다.
        if (maxY - minY > bottom - top) return emptyMap()
        val dy = when {
            minY < top -> top - minY
            maxY > bottom -> bottom - maxY
            else -> 0
        }
        return m.mapKeys { (c, _) -> Cell(c.x + dx, c.y + dy) }
    }

    private fun toSprite(m: Map<Cell, Char>, a: Anatomy): PixelSprite = PixelSprite(
        (0 until Poser.canvasH(a.size)).map { cy -> String(CharArray(Poser.canvasW(a.size)) { cx -> m[Cell(cx - PAD_X, cy - PAD_TOP)] ?: '.' }) },
    )

    private fun cells(vararg rows: Pair<Int, String>): Map<Cell, Char> = buildMap {
        rows.forEach { (y, r) -> r.forEachIndexed { x, c -> if (c != '.') put(Cell(x, y), c) } }
    }
}
