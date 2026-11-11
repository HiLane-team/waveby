package com.drivepark.app.ui.drawing

import com.drivepark.app.domain.Drawing
import com.drivepark.app.domain.GridSize

/** 붓: 테두리·몸 색·밝은 색·그늘·눈·코·머리·하의·고정 색 1~4, 그리고 지우개(빈칸). 이 순서가 화면 순서다. */
enum class Brush(val letter: Char) {
    OUTLINE('K'), BODY('B'), LIGHT('W'), SHADE('L'), EYE('O'), NOSE('P'),
    HAIR('H'), BOTTOM('N'), FIXED1('1'), FIXED2('2'), FIXED3('3'), FIXED4('4'), ERASER('.'),
    ;

    companion object {
        /** 화면에 보이는 붓. 코는 엔진이 눈 위치로 정하므로 뺀다(예전 그림의 코 글자는 그대로 읽고 지우개로 지울 수 있다). */
        val PICKABLE: List<Brush> = entries - NOSE
    }
}

/** 되돌리기 한 번에 돌아갈 상태: 그림과 고정 색을 함께 기록한다(색 바꾸기도 되돌릴 수 있게). */
data class Snapshot internal constructor(val rows: List<String>, val colors: Map<Char, Int>)

/**
 * 그림판(크기 [GridSize] 네 가지) + 고정 색 붓(1~4)의 색. 불변 값이라 칠할 때마다 새 판을 돌려준다. 안드로이드를 모른다.
 * 되돌리기는 최대 [UNDO_LIMIT]번. 문지르기([beginStroke]~[endStroke]) 한 번은 되돌리기 한 번이다.
 */
data class DrawingBoard(
    val rows: List<String>,
    /** 고정 색 1~4의 색(항상 네 개). 저장할 때는 그림에 쓰인 글자만 남는다. */
    val colors: Map<Char, Int> = Drawing.DEFAULT_COLORS,
    private val history: List<Snapshot> = emptyList(),
    /** 문지르는 중이면 문지르기 시작 전 상태. */
    private val strokeStart: Snapshot? = null,
) {
    /** 판 크기(줄 수·폭). */
    val size: GridSize get() = requireNotNull(GridSize.of(rows.first().length, rows.size)) { "그림판 크기" }

    val canUndo: Boolean get() = history.isNotEmpty()

    /** 칠한 칸이 하나도 없는지(빈 그림은 완성할 수 없다). */
    val isBlank: Boolean get() = rows.all { r -> r.all { it == '.' } }

    fun at(x: Int, y: Int): Char = rows[y][x]

    private fun snapshot() = Snapshot(rows, colors)

    /** 한 칸 칠하기. 판 밖이거나 이미 같은 글자면 그대로. */
    fun paint(x: Int, y: Int, brush: Brush): DrawingBoard {
        if (x !in 0 until size.w || y !in 0 until size.h) return this
        if (rows[y][x] == brush.letter) return this
        val next = rows.toMutableList().also { r -> r[y] = r[y].substring(0, x) + brush.letter + r[y].substring(x + 1) }
        return if (strokeStart != null) copy(rows = next) else copy(rows = next, history = push(snapshot()))
    }

    /** 고정 색 붓의 색 바꾸기. 그 글자로 칠한 칸이 모두 새 색이 된다. 되돌리기 한 번. */
    fun setColor(letter: Char, argb: Int): DrawingBoard {
        require(letter in Drawing.FIXED_LETTERS) { "고정 색 글자가 아님: $letter" }
        if (colors[letter] == argb) return this
        val ended = endStroke()
        return ended.copy(colors = ended.colors + (letter to argb), history = ended.push(ended.snapshot()))
    }

    fun beginStroke(): DrawingBoard = if (strokeStart != null) this else copy(strokeStart = snapshot())

    fun endStroke(): DrawingBoard {
        val start = strokeStart ?: return this
        return if (start == snapshot()) copy(strokeStart = null) else copy(history = push(start), strokeStart = null)
    }

    fun undo(): DrawingBoard {
        val ended = endStroke()
        if (!ended.canUndo) return ended
        val last = ended.history.last()
        return ended.copy(rows = last.rows, colors = last.colors, history = ended.history.dropLast(1))
    }

    /** 빈 칸으로(되돌릴 수 있다). 크기와 고른 색은 그대로 둔다. */
    fun clear(): DrawingBoard {
        val empty = blankRows(size)
        return if (rows == empty) this else copy(rows = empty, history = push(snapshot()), strokeStart = null)
    }

    /**
     * 다른 크기로 옮긴 판. 아래 가운데 맞춤(발바닥 줄끼리, 가로는 가운데끼리). 새 판 밖 칸은 버린다.
     * 되돌리기 한 번으로 원래 크기·그림으로 돌아간다. 고른 색은 그대로.
     */
    fun resized(to: GridSize): DrawingBoard {
        if (to == size) return this
        val (dx, dy) = offset(to)
        val next = List(to.h) { y -> String(CharArray(to.w) { x -> rows.getOrNull(y - dy)?.getOrNull(x - dx) ?: '.' }) }
        val ended = endStroke()
        return ended.copy(rows = next, history = ended.push(ended.snapshot()))
    }

    /** [resized]로 옮기면 잘려 나가는 칠한 칸 수. */
    fun cropCount(to: GridSize): Int {
        val (dx, dy) = offset(to)
        return rows.withIndex().sumOf { (y, r) -> r.withIndex().count { (x, c) -> c != '.' && (x + dx !in 0 until to.w || y + dy !in 0 until to.h) } }
    }

    private fun offset(to: GridSize) = (to.w - size.w) / 2 to (to.h - size.h)

    /** 저장 글자(그림에 쓰인 고정 색이 있으면 색 줄 포함). */
    fun toText(): String = Drawing.of(rows, colors)!!.text()

    private fun push(state: Snapshot): List<Snapshot> = (history + listOf(state)).takeLast(UNDO_LIMIT)

    companion object {
        const val UNDO_LIMIT = 50
        private fun blankRows(size: GridSize) = List(size.h) { ".".repeat(size.w) }

        fun blank(size: GridSize = GridSize.BASIC) = DrawingBoard(blankRows(size))

        /** 저장된 그림 불러오기. 형식이 틀리면 빈 판. 그림의 색은 기본색 위에 덮는다. */
        fun fromText(text: String?): DrawingBoard =
            Drawing.parse(text)?.let { DrawingBoard(it.rows, Drawing.DEFAULT_COLORS + it.colors) } ?: blank()

        /**
         * 기본 모양(서 있는 그림) 불러오기. 모든 글자가 붓에 있으므로 그대로 가져오고,
         * 모양의 고정 색([fixed])은 그 붓의 시작 색이 된다. 크기는 그림을 따르고, 네 크기가 아니면 기본 크기에 빈칸으로 맞춘다.
         */
        fun fromStand(stand: List<String>, fixed: Map<Char, Int> = emptyMap()): DrawingBoard {
            val size = GridSize.of(stand.firstOrNull()?.length ?: 0, stand.size) ?: GridSize.BASIC
            return fromStandSized(stand, fixed, size)
        }

        private fun fromStandSized(stand: List<String>, fixed: Map<Char, Int>, size: GridSize): DrawingBoard = DrawingBoard(
            List(size.h) { y ->
                val r = stand.getOrNull(y).orEmpty()
                String(CharArray(size.w) { x ->
                    val c = r.getOrNull(x) ?: '.'
                    if (c in Drawing.LETTERS) c else '.'
                })
            },
            Drawing.DEFAULT_COLORS + fixed.filterKeys { it in Drawing.FIXED_LETTERS },
        )
    }
}

/** 커서 칸(그리기 화면에서 칠할 자리). */
data class Cursor(val x: Int, val y: Int) {
    /** 칸 단위로 옮긴다. 판 밖으로는 나가지 않는다. */
    fun moved(dx: Int, dy: Int, size: GridSize): Cursor =
        Cursor((x + dx).coerceIn(0, size.w - 1), (y + dy).coerceIn(0, size.h - 1))
}

/** 그림판 위 손가락 자리(px) → 그 아래 칸. 판 밖이면 null. 손가락으로 그리기(누른 칸을 바로 칠함)에 쓴다. */
fun cellAt(xPx: Float, yPx: Float, cellPx: Float, size: GridSize): Cursor? {
    if (xPx < 0f || yPx < 0f) return null
    val x = (xPx / cellPx).toInt()
    val y = (yPx / cellPx).toInt()
    return if (x < size.w && y < size.h) Cursor(x, y) else null
}

/**
 * 두 칸 사이를 빈틈없이 잇는 칸들(양 끝 포함, [from]부터). 손가락이 빨리 움직여 한 번에 여러 칸을 건너뛰어도
 * 지나간 칸이 모두 칠해지게 한다(브레젠험 직선).
 */
fun cellsBetween(from: Cursor, to: Cursor): List<Cursor> {
    val out = ArrayList<Cursor>()
    var x = from.x
    var y = from.y
    val dx = kotlin.math.abs(to.x - x)
    val dy = -kotlin.math.abs(to.y - y)
    val sx = if (x < to.x) 1 else -1
    val sy = if (y < to.y) 1 else -1
    var err = dx + dy
    while (true) {
        out += Cursor(x, y)
        if (x == to.x && y == to.y) return out
        val e2 = 2 * err
        if (e2 >= dy) { err += dy; x += sx }
        if (e2 <= dx) { err += dx; y += sy }
    }
}
