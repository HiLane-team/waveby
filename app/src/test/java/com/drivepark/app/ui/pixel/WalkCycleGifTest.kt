package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.Hat
import com.drivepark.app.ui.park.WALK_FRAME_MS
import org.junit.Test
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.FileImageOutputStream

/**
 * 디자인 확인용: build/pixel-preview/walk-cycle.gif
 * 왼쪽: 제자리 걷기를 크게(칸이 보이게). 오른쪽: 공원과 같은 크기·속도로 걸어가기.
 */
class WalkCycleGifTest {
    private val frameMs = 20
    private val pxPerMs = 2.0 / 18.0 // 공원 걷기 속도(1dp당 18ms), 1dp = 2px

    @Test
    fun writeWalkCycleGif() {
        val parts = CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.SCARF)
        val walk = (0..3).map { CharacterRenderer.render(parts, Pose.WALK, it) }
        // 칸 단위로 살펴보기 쉽게 글자로도 남긴다.
        File("build/pixel-preview").mkdirs()
        File("build/pixel-preview/walk-frames.txt").writeText(
            (0..3).joinToString("\n\n") { f -> "WALK $f\n" + Poser.pose(Shapes.WOLF, Shapes.anatomy("wolf"), Pose.WALK, f).sprite.rows.joinToString("\n") },
        )
        val big = 14
        val small = 9
        val w = 19 * big + 40 + 560
        val h = 24 * big + 40
        val travel = 560 - 19 * small - 20
        val legMs = (travel / pxPerMs).toInt()
        val totalMs = legMs * 2

        val file = File(File("build/pixel-preview").apply { mkdirs() }, "walk-cycle.gif").apply { delete() }
        val writer = ImageIO.getImageWritersByFormatName("gif").next()
        FileImageOutputStream(file).use { stream ->
            writer.output = stream
            writer.prepareWriteSequence(null)
            var t = 0
            while (t < totalMs) {
                val sprite = walk[(t / WALK_FRAME_MS) % 4]
                val right = t < legMs
                val x = if (right) 10 + (t * pxPerMs) else 10 + travel - (t - legMs) * pxPerMs
                val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
                val g = img.createGraphics()
                g.color = Color(0xF4F4F5); g.fillRect(0, 0, w, h)
                g.color = Color(0xE4E4E7); g.fillRect(19 * big + 40, 0, 2, h)
                g.dispose()
                draw(img, sprite, 20, 20, big, flip = false)
                draw(img, sprite, 19 * big + 40 + x.toInt(), h - 24 * small - 20, small, flip = right)
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

    private fun meta(writer: javax.imageio.ImageWriter, img: BufferedImage, first: Boolean) =
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
