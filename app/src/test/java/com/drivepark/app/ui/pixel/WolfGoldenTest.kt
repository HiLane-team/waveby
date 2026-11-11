package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.Face
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * 늑대 서기·걷기 장면의 고정본(src/test/resources/wolf-golden.txt).
 * 동작 엔진으로 옮긴 뒤에도 늑대가 같은 모습으로 움직이는지 확인한다.
 * 블록 형식: 첫 줄 "<POSE> <frame> <FACE>", 이어서 그림판 24줄(슬롯 글자), 블록 사이 빈 줄.
 */
class WolfGoldenTest {

    private fun current(): Map<String, List<String>> = buildMap {
        for (pose in listOf(Pose.IDLE, Pose.WALK)) for (f in 0..3) for (face in Face.entries) {
            put("$pose $f $face", Poser.pose(Shapes.WOLF, Shapes.anatomy("wolf"), pose, f, face).sprite.rows)
        }
    }

    /** 지금 결과를 build/pixel-preview/wolf-golden.txt 로 쓴다(고정본을 새로 만들 때 resources로 복사). */
    @Test
    fun dumpCurrent() {
        val text = current().entries.joinToString("\n\n") { (k, rows) -> k + "\n" + rows.joinToString("\n") }
        File("build/pixel-preview").apply { mkdirs() }.resolve("wolf-golden.txt").writeText(text + "\n")
    }

    @Test
    fun wolfMatchesGolden() {
        val stream = javaClass.classLoader!!.getResourceAsStream("wolf-golden.txt") ?: error("wolf-golden.txt 없음")
        val golden = stream.bufferedReader().readText().trim().split(Regex("\\r?\\n\\r?\\n")).associate { block ->
            val lines = block.lines().map { it.trimEnd('\r') }
            lines.first() to lines.drop(1)
        }
        val now = current()
        assertEquals(golden.keys, now.keys)
        golden.forEach { (k, rows) -> assertEquals(k, rows, now.getValue(k)) }
    }
}
