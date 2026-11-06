package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.Hat
import com.drivepark.app.domain.Regions
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/** 디자인 확인용: build/pixel-preview/sheet.png 에 전체 조합을 크게 그려 저장한다. */
class PixelPreviewTest {
    @Test
    fun writePreviewSheet() {
        val scale = 8
        val cell = 32 * scale + 8 // 칸 크기는 가장 큰 그림(32×32) 기준
        val rows = mutableListOf<List<PixelImage>>()
        rows += Pose.entries.flatMap { p -> (0..3).map { f -> CharacterRenderer.render(CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.NONE), p, f) } }
        rows += BodyColor.entries.map { CharacterRenderer.render(CharacterParts(it, Face.SMILE, Hat.NONE, Accessory.NONE), Pose.IDLE, 0) }
        rows += Face.entries.map { CharacterRenderer.render(CharacterParts(BodyColor.PURPLE, it, Hat.NONE, Accessory.NONE), Pose.IDLE, 0) } +
            Hat.entries.drop(1).map { CharacterRenderer.render(CharacterParts(BodyColor.PURPLE, Face.SMILE, it, Accessory.NONE), Pose.IDLE, 0) }
        rows += Accessory.entries.drop(1).map { CharacterRenderer.render(CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, it), Pose.IDLE, 0) } +
            Regions.ALL.map { CharacterRenderer.renderMascot(it.id) }
        val cols = rows.maxOf { it.size }
        val img = BufferedImage(cols * cell, rows.size * cell, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics(); g.color = java.awt.Color(0xF4F4F5); g.fillRect(0, 0, img.width, img.height); g.dispose()
        rows.forEachIndexed { r, list ->
            list.forEachIndexed { c, p ->
                val ox = (32 - p.width) / 2 * scale
                for (y in 0 until p.height) for (x in 0 until p.width) {
                    val v = p.argb[y * p.width + x]
                    if (v == 0) continue
                    for (dy in 0 until scale) for (dx in 0 until scale) img.setRGB(c * cell + ox + x * scale + dx, r * cell + y * scale + dy, v)
                }
            }
        }
        val out = File("build/pixel-preview").apply { mkdirs() }
        ImageIO.write(img, "png", File(out, "sheet.png"))
    }
}
