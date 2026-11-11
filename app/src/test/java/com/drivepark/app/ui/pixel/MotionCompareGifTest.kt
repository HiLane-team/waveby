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
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.FileImageOutputStream

/**
 * 디자인 비교용: build/pixel-preview/motion-compare.gif 에 공원 걷기를 두 방식으로 나란히 그린다.
 * 위: 지금처럼 부드럽게(매 프레임 위치가 바뀜). 아래: 도트 한 칸 단위로 이동(초당 약 12번만 바뀜).
 * 걷기 속도(1dp당 18ms)와 걷기 그림 넘김(WALK_FRAME_MS)은 ParkScreen과 같다. 화면 밀도 2(1dp = 2px) 기준.
 */
class MotionCompareGifTest {
    private val scale = 9 // AVATAR 112dp = 224px → 19×24 그림판을 정수배 9로 키움(CharacterAvatar와 같은 계산)
    private val pxPerMs = 2.0 / 18.0
    private val frameMs = 20 // GIF가 안정적으로 보여 주는 가장 짧은 간격(초당 50장)
    private val panelW = 680
    private val panelH = 290

    @Test
    fun writeMotionCompareGif() {
        val parts = CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.NONE)
        val walk = (0..3).map { CharacterRenderer.render(parts, Pose.WALK, it) }
        val spriteW = walk[0].width * scale
        val startX = 40.0
        val endX = (panelW - spriteW - 40).toDouble()
        val legMs = ((endX - startX) / pxPerMs).toInt()
        val pauseMs = 600
        val totalMs = 2 * (legMs + pauseMs)

        val out = File("build/pixel-preview").apply { mkdirs() }
        val file = File(out, "motion-compare.gif").apply { delete() }
        val writer = ImageIO.getImageWritersByFormatName("gif").next()
        FileImageOutputStream(file).use { stream ->
            writer.output = stream
            writer.prepareWriteSequence(null)
            var t = 0
            var first = true
            while (t < totalMs) {
                // 0 → 오른쪽으로 걷기 → 잠깐 멈춤 → 왼쪽으로 걷기 → 잠깐 멈춤
                val (x, walking, right) = when {
                    t < legMs -> Triple(startX + t * pxPerMs, true, true)
                    t < legMs + pauseMs -> Triple(endX, false, true)
                    t < 2 * legMs + pauseMs -> Triple(endX - (t - legMs - pauseMs) * pxPerMs, true, false)
                    else -> Triple(startX, false, false)
                }
                val walkFrame = walk[(t / com.drivepark.app.ui.park.WALK_FRAME_MS) % 4]
                val idleFrame = CharacterRenderer.render(parts, Pose.IDLE, (t / 600) % 4)
                val sprite = if (walking) walkFrame else idleFrame
                val snapped = startX + Math.floor((x - startX) / scale) * scale

                val img = BufferedImage(panelW, panelH * 2, BufferedImage.TYPE_INT_RGB)
                val g = img.createGraphics()
                g.color = Color(0xF4F4F5); g.fillRect(0, 0, img.width, img.height)
                g.color = Color(0xD4D4D8); g.fillRect(0, panelH - 1, panelW, 2)
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                g.font = Font("Malgun Gothic", Font.BOLD, 22)
                g.color = Color(0x18181B)
                g.drawString("지금: 부드럽게 (초당 60번 다시 그림)", 20, 36)
                g.drawString("도트 한 칸씩 (초당 약 12번 다시 그림)", 20, panelH + 36)
                g.dispose()
                // 스프라이트는 왼쪽을 본다. 오른쪽으로 갈 때 뒤집는다(ParkScreen과 같음).
                drawSprite(img, sprite, x.toInt(), 60, flip = right)
                drawSprite(img, sprite, snapped.toInt(), panelH + 60, flip = right)

                writer.writeToSequence(IIOImage(img, null, frameMetadata(writer, img, first)), null)
                first = false
                t += frameMs
            }
            writer.endWriteSequence()
        }
        writer.dispose()
    }

    private fun drawSprite(img: BufferedImage, p: PixelImage, left: Int, top: Int, flip: Boolean) {
        for (y in 0 until p.height) for (x in 0 until p.width) {
            val v = p.argb[y * p.width + x]
            if (v == 0) continue
            val sx = if (flip) p.width - 1 - x else x
            for (dy in 0 until scale) for (dx in 0 until scale) {
                val px = left + sx * scale + dx
                val py = top + y * scale + dy
                if (px in 0 until img.width && py in 0 until img.height) img.setRGB(px, py, v)
            }
        }
    }

    private fun frameMetadata(writer: javax.imageio.ImageWriter, img: BufferedImage, first: Boolean) =
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
                app.userObject = byteArrayOf(1, 0, 0) // 무한 반복
                apps.appendChild(app)
                root.appendChild(apps)
            }
            setFromTree(format, root)
        }
}
