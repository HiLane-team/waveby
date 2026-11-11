package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.Hat
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

/**
 * 디자인 비교용 GIF 두 장(build/pixel-preview/).
 * walk-variants.gif: 걷기 모양 4가지(발 높이 1·2칸 × 꼬리 살랑 없음·있음)를 제자리걸음으로 크게.
 * walk-speeds.gif: 같은 모양으로 그림 넘김 간격 3가지를 공원 크기·속도로 걸어가며.
 */
class WalkVariantsGifTest {
    private val parts = CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.SCARF)
    private val frameMs = 20
    private val bg = Color(0xF4F4F5)
    private val ink = Color(0x18181B)

    @Test
    fun writeVariants() {
        val variants = listOf(
            "A. 발 1칸" to WalkStyle(lift = 1),
            "B. 발 2칸" to WalkStyle(lift = 2),
            "C. 꼬리 살랑" to WalkStyle(lift = 1, tailWag = true),
            "D. 발 2칸 + 꼬리 (지금)" to WalkStyle(lift = 2, tailWag = true),
        )
        val s = 12
        val cellW = 19 * s + 60
        val cellH = 24 * s + 70
        val stepMs = com.drivepark.app.ui.park.WALK_FRAME_MS
        gif("walk-variants.gif", cellW * 2, cellH * 2, totalMs = stepMs * 4 * 4) { img, g, t ->
            variants.forEachIndexed { i, (label, style) ->
                val ox = (i % 2) * cellW
                val oy = (i / 2) * cellH
                g.color = ink; g.drawString(label, ox + 16, oy + 34)
                val sprite = CharacterRenderer.render(parts, Pose.WALK, (t / stepMs) % 4, walk = style)
                draw(img, sprite, ox + 30, oy + 50, s, flip = false)
            }
            g.color = Color(0xE4E4E7)
            g.fillRect(cellW - 1, 0, 2, cellH * 2); g.fillRect(0, cellH - 1, cellW * 2, 2)
        }
    }

    @Test
    fun writeSpeeds() {
        val style = WalkStyle(lift = 2, tailWag = true)
        val speeds = listOf(110, 140, 180)
        val s = 9
        val w = 640
        val rowH = 24 * s + 60
        val pxPerMs = 2.0 / 18.0 // 공원 걷기 속도: 1dp당 18ms, 1dp = 2px
        val travel = w - 19 * s - 40
        val legMs = (travel / pxPerMs).toInt()
        gif("walk-speeds.gif", w, rowH * speeds.size, totalMs = legMs * 2) { img, g, t ->
            val right = t < legMs
            val x = 20 + if (right) t * pxPerMs else travel - (t - legMs) * pxPerMs
            speeds.forEachIndexed { i, ms ->
                val oy = i * rowH
                g.color = ink; g.drawString("그림 ${ms}ms마다" + if (ms == com.drivepark.app.ui.park.WALK_FRAME_MS) " (지금)" else "", 16, oy + 30)
                val sprite = CharacterRenderer.render(parts, Pose.WALK, (t / ms) % 4, walk = style)
                draw(img, sprite, x.toInt(), oy + 44, s, flip = right)
                g.color = Color(0xE4E4E7); g.fillRect(0, oy + rowH - 1, w, 2)
            }
        }
    }

    private fun gif(name: String, w: Int, h: Int, totalMs: Int, paint: (BufferedImage, java.awt.Graphics2D, Int) -> Unit) {
        val file = File(File("build/pixel-preview").apply { mkdirs() }, name).apply { delete() }
        val writer = ImageIO.getImageWritersByFormatName("gif").next()
        FileImageOutputStream(file).use { stream ->
            writer.output = stream
            writer.prepareWriteSequence(null)
            var t = 0
            while (t < totalMs) {
                val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
                val g = img.createGraphics()
                g.color = bg; g.fillRect(0, 0, w, h)
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                g.font = Font("Malgun Gothic", Font.BOLD, 22)
                paint(img, g, t)
                g.dispose()
                writer.writeToSequence(IIOImage(img, null, meta(writer, img, t == 0)), null)
                t += frameMs
            }
            writer.endWriteSequence()
        }
        writer.dispose()
    }

    private fun draw(img: BufferedImage, p: PixelImage, left: Int, top: Int, s: Int, flip: Boolean) {
        for (y in 0 until p.height) for (x in 0 until p.width) {
            val v = p.argb[y * p.width + x]
            if (v == 0) continue
            val sx = if (flip) p.width - 1 - x else x
            for (dy in 0 until s) for (dx in 0 until s) {
                val px = left + sx * s + dx
                val py = top + y * s + dy
                if (px in 0 until img.width && py in 0 until img.height) img.setRGB(px, py, v)
            }
        }
    }

    private fun meta(writer: ImageWriter, img: BufferedImage, first: Boolean) =
        writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(img), null).apply {
            val format = nativeMetadataFormatName
            val root = getAsTree(format) as IIOMetadataNode
            val gce = root.getElementsByTagName("GraphicControlExtension").item(0) as IIOMetadataNode
            gce.setAttribute("disposalMethod", "none")
            gce.setAttribute("userInputFlag", "FALSE")
            gce.setAttribute("transparentColorFlag", "FALSE")
            gce.setAttribute("delayTime", (frameMs / 10).toString())
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
