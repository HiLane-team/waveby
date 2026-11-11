package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.GridSize

/** 그림판에서 보여 줄 그림 검사 안내. 저장은 막지 않는다. */
enum class DrawingHint {
    /** 발바닥 줄(맨 아랫줄)이 비어 있음: "발이 맨 아랫줄에 닿게 그려 주세요" */
    NO_FEET,

    /** 머리 구역([BodyLines.headEnd]까지)이 비어 있음: "머리를 안내선 위쪽에 그려 주세요" */
    NO_HEAD,

    /** 상하좌우로 이어지지 않은 외딴 칸: "떨어진 점이 있어요" */
    STRAY_PIXEL,
}

/**
 * 서 있는 그림의 몸 지도. 동작과 꾸미기가 이것을 기준으로 칸을 옮긴다.
 * @property feet 발마다 열 범위(왼쪽부터)
 * @property pairA 걷기 1장에서 드는 발들, [pairB] 3장에서 드는 발들
 * @property tail 꼬리 칸(몸 구역부터 발바닥 위 줄까지, 가장 오른쪽 발보다 오른쪽)
 * @property eyes 눈마다 칸 묶음(왼쪽부터)
 * @property crownRow 정수리 줄(모자를 올릴 줄)
 * @property headCenterX 정수리 줄의 가운데 열
 * @property headRight 머리 구역에서 가장 오른쪽 칸의 열
 */

data class Anatomy(
    val feet: List<IntRange>,
    val pairA: List<IntRange>,
    val pairB: List<IntRange>,
    val tail: Set<Cell>,
    val eyes: List<List<Cell>>,
    val crownRow: Int,
    val headCenterX: Int,
    val headRight: Int,
    val hints: List<DrawingHint>,
    val size: GridSize,
    val lines: BodyLines,
    /** 걷는 방식(모양이 정했으면 그 값, 아니면 발바닥 모양으로 판단). */
    val gait: Gait,
) {
    companion object {
        /**
         * @param lines 몸 구역(기본 = 크기별 기본값)
         * @param gait 걷는 방식(null이면 판단: 발바닥 줄이 폭 [HOP_MIN_WIDTH]칸 이상 한 덩어리면 통통)
         */
        fun of(s: StandSprite, lines: BodyLines = BodyLines.default(s.size), gait: Gait? = null): Anatomy {
            val w = s.size.w
            val h = s.size.h
            val sole = h - 1
            val feet = feet(s, sole)
            val (pairA, pairB) = when (feet.size) {
                0 -> emptyList<IntRange>() to emptyList()
                1 -> feet to feet
                2 -> listOf(feet[0]) to listOf(feet[1])
                else -> feet.filterIndexed { i, _ -> i % 2 == 0 } to feet.filterIndexed { i, _ -> i % 2 == 1 }
            }
            val tail = feet.lastOrNull()?.let { last ->
                (lines.headEnd + 1 until sole).flatMap { y -> (last.last + 1 until w).filter { s.filled(it, y) }.map { Cell(it, y) } }.toSet()
            } ?: emptySet()

            val (crownRow, crownRun) = crown(s, lines.headEnd)
            val headCells = (0..lines.headEnd).flatMap { y -> (0 until w).filter { s.filled(it, y) }.map { Cell(it, y) } }

            val soleRuns = runs(s, sole)
            val hop = soleRuns.size == 1 && soleRuns.single().let { it.last - it.first + 1 } >= HOP_MIN_WIDTH
            val hints = buildList {
                if (feet.isEmpty()) add(DrawingHint.NO_FEET)
                if (headCells.isEmpty()) add(DrawingHint.NO_HEAD)
                if (hasStray(s)) add(DrawingHint.STRAY_PIXEL)
            }
            return Anatomy(
                feet = feet,
                pairA = pairA,
                pairB = pairB,
                tail = tail,
                eyes = eyes(s, w, h),
                crownRow = crownRow,
                headCenterX = crownRun?.let { (it.first + it.last) / 2 } ?: (w / 2),
                headRight = headCells.maxOfOrNull { it.x } ?: (w / 2),
                hints = hints,
                size = s.size,
                lines = lines,
                gait = gait ?: if (hop) Gait.HOP else Gait.LEGS,
            )
        }

        /** 바닥에 이만큼 넓은 한 덩어리로 붙은 그림은 다리 대신 통통 튀며 걷는다(컵 바닥·게 몸통). */
        const val HOP_MIN_WIDTH = 6

        /** 줄 y에서 칠한 칸 덩어리들(열 범위). */
        private fun runs(s: StandSprite, y: Int): List<IntRange> {
            val out = mutableListOf<IntRange>()
            var start = -1
            val w = s.size.w
            for (x in 0..w) {
                val on = x < w && s.filled(x, y)
                if (on && start < 0) start = x
                if (!on && start >= 0) { out += start until x; start = -1 }
            }
            return out
        }

        private fun feet(s: StandSprite, sole: Int): List<IntRange> {
            val r = runs(s, sole)
            if (r.size != 1) return r
            val only = r.single()
            val w = only.last - only.first + 1
            if (w < 4) return r
            val mid = only.first + w / 2
            return listOf(only.first until mid, mid..only.last)
        }

        /**
         * 정수리: 머리 구역에서 가장 긴 칸 덩어리가 머리 최대 덩어리 폭의 절반을 넘는 첫 줄.
         * 귀처럼 떨어진 작은 덩어리들은 건너뛴다(늑대는 귀 끝 0줄이 아니라 4줄).
         */
        private fun crown(s: StandSprite, headEnd: Int): Pair<Int, IntRange?> {
            // 머리 끝 줄(기본 14줄)은 목·등 줄이라 뺀다(키 작은 네발짐승은 14줄이 등이라 정수리로 잡히면 모자가 등에 올라간다).
            val longest = (0 until headEnd).associateWith { y -> runs(s, y).maxByOrNull { it.last - it.first } }
            val maxW = longest.values.filterNotNull().maxOfOrNull { it.last - it.first + 1 } ?: return headEnd to null
            longest.forEach { (y, run) -> if (run != null && (run.last - run.first + 1) * 2 > maxW) return y to run }
            return headEnd to null
        }

        /** 눈 = 상하좌우로 이어진 O 칸 묶음, 왼쪽부터. */
        private fun eyes(s: StandSprite, w: Int, h: Int): List<List<Cell>> {
            val seen = HashSet<Cell>()
            val groups = mutableListOf<List<Cell>>()
            for (y in 0 until h) for (x in 0 until w) {
                val c = Cell(x, y)
                if (s[x, y] != 'O' || c in seen) continue
                val group = mutableListOf<Cell>()
                val stack = ArrayDeque(listOf(c)).also { seen += c }
                while (stack.isNotEmpty()) {
                    val p = stack.removeLast()
                    group += p
                    listOf(Cell(p.x + 1, p.y), Cell(p.x - 1, p.y), Cell(p.x, p.y + 1), Cell(p.x, p.y - 1))
                        .filter { s[it.x, it.y] == 'O' && seen.add(it) }
                        .forEach(stack::addLast)
                }
                groups += group.sortedWith(compareBy({ it.y }, { it.x }))
            }
            return groups.sortedBy { g -> g.minOf { it.x } }
        }

        private fun hasStray(s: StandSprite): Boolean = (0 until s.size.h).any { y ->
            (0 until s.size.w).any { x ->
                s.filled(x, y) && !s.filled(x - 1, y) && !s.filled(x + 1, y) && !s.filled(x, y - 1) && !s.filled(x, y + 1)
            }
        }
    }
}
