package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.CharacterParts

/** 레이어(몸+표정 → 모자 → 소품)를 팔레트로 칠해 그림을 만든다(늑대 17×23, 지역 캐릭터 32×32). 순수 함수. */
object CharacterRenderer {

    /** 털 색 팔레트(어두운 털, 밝은 털). 기본 늑대는 PURPLE 자리. */
    private val FUR: Map<BodyColor, Pair<Int, Int>> = mapOf(
        BodyColor.PURPLE to (0xFF585D69.toInt() to 0xFFF5F5F2.toInt()),
        BodyColor.RED to (0xFF8A4B45.toInt() to 0xFFFBEFEA.toInt()),
        BodyColor.ORANGE to (0xFFC9733A.toInt() to 0xFFFFF4E8.toInt()),
        BodyColor.YELLOW to (0xFFA8915E.toInt() to 0xFFFBF6E6.toInt()),
        BodyColor.GREEN to (0xFF4F6B55.toInt() to 0xFFEEF5EF.toInt()),
        BodyColor.BLUE to (0xFF4A5F7E.toInt() to 0xFFEEF2F8.toInt()),
    )

    private val COMMON: Map<Char, Int> = mapOf(
        'o' to 0xFF2A2C30.toInt(),
        'E' to 0xFF3DBE6B.toInt(),
        'H' to 0xFFFFFFFF.toInt(),
        'P' to 0xFFF2A7B5.toInt(),
        '1' to 0xFF3B82F6.toInt(), // 캡모자
        '2' to 0xFF8B5CF6.toInt(), // 비니
        '3' to 0xFFF7F7F5.toInt(), // 흰색
        '4' to 0xFFF5B301.toInt(), // 금색
        'R' to 0xFFE5484D.toInt(), // 목도리
        'B' to 0xFFFF6F91.toInt(), // 풍선
        'S' to 0x33000000,         // 그림자
    )

    fun render(parts: CharacterParts, pose: Pose, frame: Int): PixelImage {
        val (dark, light) = FUR.getValue(parts.body)
        // M = 흰 털의 그늘(설명서 L #B8BAC4). 다른 털 색은 밝은 털에 어두운 털을 조금 섞는다.
        val shade = if (parts.body == BodyColor.PURPLE) 0xFFB8BAC4.toInt() else blend(light, dark, 0.3f)
        val palette = COMMON + mapOf('D' to dark, 'L' to light, 'M' to shade)
        val w = WolfSprites.CANVAS_W
        val h = WolfSprites.CANVAS_H
        val out = IntArray(w * h)
        paint(out, w, WolfSprites.body(pose, frame, parts.face), palette)
        // 머리가 움직인 만큼 모자·소품도 같이 움직인다.
        val dy = WolfSprites.headDy(pose, frame)
        WolfSprites.hat(parts.hat)?.let { paint(out, w, it, palette, dy) }
        WolfSprites.accessory(parts.accessory)?.let { paint(out, w, it, palette, dy) }
        return PixelImage(w, h, out)
    }

    fun renderMascot(regionId: String): PixelImage {
        val palette = COMMON + (MascotSprites.PALETTES[regionId] ?: emptyMap())
        val sprite = MascotSprites.sprite(regionId)
        val out = IntArray(sprite.width * sprite.height)
        paint(out, sprite.width, sprite, palette)
        return PixelImage(sprite.width, sprite.height, out)
    }

    private fun paint(out: IntArray, w: Int, sprite: PixelSprite, palette: Map<Char, Int>, dy: Int = 0) {
        for (y in 0 until sprite.height) for (x in 0 until sprite.width) {
            val ch = sprite[x, y]
            if (ch == '.') continue
            val ty = y + dy
            if (ty !in 0 until sprite.height) continue
            out[ty * w + x] = palette[ch] ?: continue
        }
    }

    /** a에 b를 t만큼 섞는다. */
    private fun blend(a: Int, b: Int, t: Float): Int {
        fun ch(c: Int, s: Int) = (c shr s) and 0xFF
        fun mix(s: Int) = (ch(a, s) * (1 - t) + ch(b, s) * t).toInt()
        val r = mix(16)
        val g = mix(8)
        val bl = mix(0)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
    }
}
