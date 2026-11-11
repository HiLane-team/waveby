package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.Hat
import com.drivepark.app.ui.park.DOZE_FRAME_MS
import com.drivepark.app.ui.park.WALK_FRAME_MS
import com.drivepark.app.ui.park.hopAt
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.ImageWriter
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.FileImageOutputStream
import kotlin.math.roundToInt

/**
 * 디자인 확인용: build/pixel-preview/all-poses.gif
 * 줄 = 모양(늑대, 두 발 사람형, 네 발 옆모습), 장면 = 서기(깜빡임) → 걷기 → 앉기 → 졸기 → 폴짝, 꾸미기를 바꿔 끼움.
 * 그림이 바뀌는 순간에만 장면을 써서 폰에서도 움직이도록 작게 만든다.
 */
class AllPosesGifTest {
    private data class Step(val ms: Long, val pose: Pose, val frame: Int, val closed: Boolean, val lift: Float, val parts: CharacterParts, val label: String)

    private val base = CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.NONE)

    private fun timeline(): List<Step> = buildList {
        val idle = base.copy(accessory = Accessory.SCARF)
        // 서기 4장(600ms), 2장째에 깜빡임 120ms
        add(Step(600, Pose.IDLE, 0, false, 0f, idle, "서기"))
        add(Step(480, Pose.IDLE, 1, false, 0f, idle, "서기"))
        add(Step(120, Pose.IDLE, 1, true, 0f, idle, "깜빡임"))
        add(Step(600, Pose.IDLE, 2, false, 0f, idle, "서기"))
        add(Step(600, Pose.IDLE, 3, false, 0f, idle, "서기"))
        val walk = base.copy(hat = Hat.CAP)
        repeat(12) { add(Step(WALK_FRAME_MS.toLong(), Pose.WALK, it % 4, false, 0f, walk, "걷기")) }
        add(Step(1000, Pose.SIT, 0, false, 0f, base.copy(hat = Hat.BEANIE), "앉기"))
        val doze = base.copy(hat = Hat.CROWN)
        repeat(4) { add(Step(DOZE_FRAME_MS / 2, Pose.DOZE, it % 2, false, 0f, doze, "졸기")) }
        val hop = base.copy(accessory = Accessory.GLASSES)
        var t = 0L
        while (true) {
            val f = hopAt(t) ?: break
            add(Step(40, f.pose, 0, false, f.liftDots, hop, "폴짝"))
            t += 40
        }
        add(Step(800, Pose.IDLE, 0, false, 0f, hop, "폴짝"))
    }

    @Test
    fun writeAllPosesGif() {
        val rows = listOf("늑대" to Shapes.WOLF, "사람형(시험용)" to TestShapes.BIPED, "네 발(시험용)" to TestShapes.QUADRUPED)
        val s = 6
        val cellW = Poser.CANVAS_W * s + 20
        val rowH = Poser.CANVAS_H * s + 3 * s + 16
        val w = 150 + cellW
        val h = 40 + rowH * rows.size
        val file = File(File("build/pixel-preview").apply { mkdirs() }, "all-poses.gif").apply { delete() }
        val writer = ImageIO.getImageWritersByFormatName("gif").next()
        var last: List<Int>? = null
        var pendingMs = 0L
        var pending: BufferedImage? = null
        var first = true
        FileImageOutputStream(file).use { stream ->
            writer.output = stream
            writer.prepareWriteSequence(null)
            fun flush() {
                val img = pending ?: return
                val cs = maxOf(2, (pendingMs / 10.0).roundToInt())
                writer.writeToSequence(IIOImage(img, null, meta(writer, img, cs, first)), null)
                first = false
            }
            for (step in timeline()) {
                val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
                val g = img.createGraphics()
                g.color = Color(0xF4F4F5); g.fillRect(0, 0, w, h)
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                g.font = Font("Malgun Gothic", Font.BOLD, 18)
                g.color = Color(0xEA580C); g.drawString(step.label, 150, 28)
                g.color = Color(0x18181B)
                rows.forEachIndexed { i, (name, stand) ->
                    g.drawString(name, 12, 40 + i * rowH + rowH / 2)
                    val sprite = CharacterRenderer.renderStand(ShapeDef("preview", ShapeKind.ANIMAL, stand), Anatomy.of(stand), step.parts, step.pose, step.frame, step.closed)
                    draw(img, sprite, 150, 40 + i * rowH + 3 * s - (step.lift * s).roundToInt(), s)
                }
                g.dispose()
                val pixels = img.getRGB(0, 0, w, h, null, 0, w).toList()
                if (pixels == last) { pendingMs += step.ms; continue }
                flush()
                pending = img; pendingMs = step.ms; last = pixels
            }
            flush()
            writer.endWriteSequence()
        }
        writer.dispose()
        assertTrue("GIF ${file.length()} bytes", file.length() <= 300_000)
    }

    private fun draw(img: BufferedImage, p: PixelImage, left: Int, top: Int, s: Int) {
        for (y in 0 until p.height) for (x in 0 until p.width) {
            val v = p.argb[y * p.width + x]
            if (v == 0) continue
            for (dy in 0 until s) for (dx in 0 until s) {
                val px = left + x * s + dx
                val py = top + y * s + dy
                if (px in 0 until img.width && py in 0 until img.height) img.setRGB(px, py, v)
            }
        }
    }

    private fun meta(writer: ImageWriter, img: BufferedImage, cs: Int, first: Boolean) =
        writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(img), null).apply {
            val format = nativeMetadataFormatName
            val root = getAsTree(format) as IIOMetadataNode
            val gce = root.getElementsByTagName("GraphicControlExtension").item(0) as IIOMetadataNode
            gce.setAttribute("disposalMethod", "none")
            gce.setAttribute("userInputFlag", "FALSE")
            gce.setAttribute("transparentColorFlag", "FALSE")
            gce.setAttribute("delayTime", cs.toString())
            gce.setAttribute("transparentColorIndex", "0")
            if (first) {
                val apps = IIOMetadataNode("ApplicationExtensions")
                val app = IIOMetadataNode("ApplicationExtension")
                app.setAttribute("applicationID", "NETSCAPE")
                app.setAttribute("authenticationCode", "2.0")
                app.userObject = byteArrayOf(1, 0, 0)
                apps.appendChild(app)
                root.appendChild(apps)
            }
            setFromTree(format, root)
        }
}
