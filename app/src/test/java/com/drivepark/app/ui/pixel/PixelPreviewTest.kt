package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.BottomColor
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.HairColor
import com.drivepark.app.domain.Hat
import com.drivepark.app.domain.PersonLook
import com.drivepark.app.domain.SkinTone
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * 디자인 확인용: build/pixel-preview/sheet.png
 * 1줄: 모든 모양 서 있는 모습. 2줄: 사람 외모 조합. 3줄: 동물 털 색. 4줄: 늑대 동작 몇 장 + 꾸미기.
 */
class PixelPreviewTest {
    private val base = CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.NONE)

    @Test
    fun writePreviewSheet() {
        val scale = 8
        val cell = Poser.CANVAS_H * scale + 8
        val rows = mutableListOf<List<PixelImage>>()
        rows += (Shapes.ALL.values + Shapes.DRAFT_SHAPES).map { d -> CharacterRenderer.renderStand(d, Anatomy.of(d.stand), base, Pose.IDLE, 0) }
        rows += listOf(
            base to PersonLook(),
            base.copy(body = BodyColor.RED) to PersonLook(SkinTone.MEDIUM, HairColor.BROWN, BottomColor.DENIM),
            base.copy(body = BodyColor.GREEN) to PersonLook(SkinTone.DEEP, HairColor.BLACK, BottomColor.BEIGE),
            base.copy(body = BodyColor.YELLOW) to PersonLook(SkinTone.LIGHT, HairColor.BLOND, BottomColor.BLACK),
            base.copy(body = BodyColor.BLUE, hat = Hat.CAP) to PersonLook(SkinTone.MEDIUM, HairColor.GRAY, BottomColor.GRAY),
        ).map { (p, look) -> CharacterRenderer.render(p, Pose.IDLE, 0, shape = "person", look = look) }
        rows += Shapes.DRAFT_SHAPES.filter { it.kind == ShapeKind.ANIMAL }.flatMap { d ->
            listOf(BodyColor.ORANGE, BodyColor.YELLOW).map { CharacterRenderer.renderStand(d, Anatomy.of(d.stand), base.copy(body = it), Pose.IDLE, 0) }
        }
        rows += listOf(Pose.WALK to 1, Pose.SIT to 0, Pose.DOZE to 1, Pose.AIR to 0).map { (p, f) -> CharacterRenderer.render(base, p, f) } +
            Hat.entries.drop(1).map { CharacterRenderer.render(base.copy(hat = it), Pose.IDLE, 0) } +
            Accessory.entries.drop(1).map { CharacterRenderer.render(base.copy(accessory = it), Pose.IDLE, 0) }
        val cols = rows.maxOf { it.size }
        val img = BufferedImage(cols * cell, rows.size * cell, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics(); g.color = java.awt.Color(0xF4F4F5); g.fillRect(0, 0, img.width, img.height); g.dispose()
        rows.forEachIndexed { r, list ->
            list.forEachIndexed { c, p ->
                for (y in 0 until p.height) for (x in 0 until p.width) {
                    val v = p.argb[y * p.width + x]
                    if (v == 0) continue
                    for (dy in 0 until scale) for (dx in 0 until scale) img.setRGB(c * cell + x * scale + dx + 4, r * cell + y * scale + dy + 4, v)
                }
            }
        }
        val out = File("build/pixel-preview").apply { mkdirs() }
        ImageIO.write(img, "png", File(out, "sheet.png"))
    }
}
