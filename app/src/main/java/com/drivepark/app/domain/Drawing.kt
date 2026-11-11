package com.drivepark.app.domain

/** 모습 id: "이 캐릭터의 그림 칸으로 그린다". */
const val DRAWN_SHAPE = "drawn"

/**
 * 몸 색으로 고를 수 있는 16색(불투명 ARGB). 옷·털에 쓰는 선명한 색.
 * 빨강·주황·노랑·연두·초록·청록·파랑·남색·보라·분홍·갈색·베이지·흰색·회색·진회색·검정 순서.
 */
val BODY_PALETTE: List<Int> = listOf(
    0xFFE5484D, 0xFFF97316, 0xFFF5B301, 0xFF84CC16,
    0xFF2E7D32, 0xFF14B8A6, 0xFF3B82F6, 0xFF1E3A8A,
    0xFF8B5CF6, 0xFFF2A7B5, 0xFF8A5A3C, 0xFFD9B48F,
    0xFFFFFFFF, 0xFFA1A1AA, 0xFF52525B, 0xFF18181B,
).map { it.toInt() }

/**
 * 사람 색 한 줄: 밝은 피부·중간 피부(기본 사람 캐릭터와 같은 색)·구릿빛 피부·검은 머리(테두리와 구분되는 따뜻한 먹색).
 * 짙은 피부는 [BODY_PALETTE]의 갈색을 쓴다.
 */
private val PERSON_ROW: List<Int> = listOf(0xFFF6D7C3, 0xFFD9A27E, 0xFFB57A52, 0xFF3A3132).map { it.toInt() }

/** 고정 색 붓(1~4)에서 고를 수 있는 색판 20색 = 몸 색 16색 + 사람 색 4색. */
val DRAWING_PALETTE: List<Int> = BODY_PALETTE + PERSON_ROW

/**
 * 사용자가 직접 그린 서 있는 그림([GridSize] 네 크기 중 하나, h줄 × w칸) + 고정 색 붓의 색.
 *
 * 저장 형식은 줄을 `\n`으로 이은 글자다. 고정 색 글자(1~4)를 썼으면 그림 줄 뒤에 한 줄 더 그 색을 적는다:
 * `1=#8A5A3C 3=#E5484D` (글자 순서, 공백으로 나눔). 색 줄이 없는 17칸 23줄은 예전 형식 그대로 읽는다.
 * 크기는 그림 줄 수와 폭으로 안다(네 크기의 가로·세로 조합이 모두 달라 헷갈리지 않는다).
 *
 * @property colors 그림에 **쓰인** 고정 색 글자의 색만 담는다(쓰였는데 색이 없으면 기본색).
 */
class Drawing private constructor(val rows: List<String>, val colors: Map<Char, Int>, val size: GridSize) {
    fun text(): String {
        val body = rows.joinToString("\n")
        if (colors.isEmpty()) return body
        val line = colors.toSortedMap().entries.joinToString(" ") { (c, argb) -> "%c=#%06X".format(c, argb and 0xFFFFFF) }
        return body + "\n" + line
    }

    companion object {
        /** 붓 글자: 테두리·몸 색·밝은 색·그늘·눈·코·머리·하의·고정 색 1~4, 그리고 빈칸. */
        val LETTERS: Set<Char> = setOf('K', 'B', 'W', 'L', 'O', 'P', 'H', 'N', '1', '2', '3', '4', '.')

        val FIXED_LETTERS: Set<Char> = setOf('1', '2', '3', '4')

        /** 고정 색 붓의 처음 색: 갈색·분홍·노랑·파랑. */
        val DEFAULT_COLORS: Map<Char, Int> = mapOf(
            '1' to 0xFF8A5A3C.toInt(), '2' to 0xFFF2A7B5.toInt(), '3' to 0xFFF5B301.toInt(), '4' to 0xFF3B82F6.toInt(),
        )

        private val COLOR_LINE_START = Regex("^[1-4]=")
        private val COLOR_ITEM = Regex("^([1-4])=#([0-9A-Fa-f]{6})$")

        /** 그림 줄과 색으로 만든다. 크기·글자가 틀리면 null. 쓰이지 않은 글자의 색은 버린다. */
        fun of(rows: List<String>, colors: Map<Char, Int>): Drawing? {
            val size = GridSize.of(rows.firstOrNull()?.length ?: 0, rows.size) ?: return null
            if (rows.any { r -> r.length != size.w || r.any { it !in LETTERS } }) return null
            val used = rows.flatMapTo(HashSet()) { r -> r.filter { it in FIXED_LETTERS }.toList() }
            // 받은 그림의 색은 투명도를 무시하고 불투명으로 쓴다.
            val kept = used.associateWith { c -> (colors[c] ?: DEFAULT_COLORS.getValue(c)) or 0xFF000000.toInt() }
            return Drawing(rows, kept, size)
        }

        /** 형식이 맞지 않으면 null. */
        fun parse(text: String?): Drawing? {
            if (text == null) return null
            val lines = text.split("\n")
            // 마지막 줄이 "숫자=" 로 시작하면 색 줄(그림 줄은 글자 다음에 "="가 올 수 없다).
            val hasColors = lines.size > 1 && COLOR_LINE_START.containsMatchIn(lines.last())
            val colors = if (hasColors) parseColorLine(lines.last()) ?: return null else emptyMap()
            return of(if (hasColors) lines.dropLast(1) else lines, colors)
        }

        private fun parseColorLine(line: String): Map<Char, Int>? {
            val out = HashMap<Char, Int>()
            for (item in line.split(" ").filter { it.isNotEmpty() }) {
                val m = COLOR_ITEM.matchEntire(item) ?: return null
                val c = m.groupValues[1][0]
                if (c in out) return null
                out[c] = m.groupValues[2].toInt(16) or 0xFF000000.toInt()
            }
            return out
        }
    }
}
