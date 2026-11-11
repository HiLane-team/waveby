package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.BottomColor
import com.drivepark.app.domain.CharacterKind
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.DRAWN_SHAPE
import com.drivepark.app.domain.Drawing
import com.drivepark.app.domain.EyeColor
import com.drivepark.app.domain.HairColor
import com.drivepark.app.domain.PersonLook
import com.drivepark.app.domain.SkinTone

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

    /**
     * (어두운 털 = 몸 색, 밝은 털). 예전 6색은 위 표 그대로, 색판 16색은 그 색 + 아주 옅게 물든 흰 털.
     * 사람은 어두운 털 자리를 상의 색으로 쓴다.
     */
    private fun fur(body: BodyColor): Pair<Int, Int> =
        FUR[body] ?: body.palette!!.let { c -> c to blend(0xFFFAFAF7.toInt(), c, 0.12f) }

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

    /**
     * 모양(서 있는 그림)에서 동작 장면을 만들고 털 색을 입힌다. 모자·소품은 몸 지도에 맞춰 놓고,
     * 머리가 움직인 만큼 같이 옮긴다.
     */
    fun render(
        parts: CharacterParts,
        pose: Pose,
        frame: Int,
        eyesClosed: Boolean = false,
        walk: WalkStyle = WalkStyle.DEFAULT,
        shape: String = "wolf",
        look: PersonLook = PersonLook(),
        kind: CharacterKind = CharacterKind.PERSON,
        drawing: String? = null,
    ): PixelImage {
        // 직접 그린 그림: 형식이 맞으면 그 그림을 늑대와 같은 색표(동물)로 엔진에 넣는다.
        if (shape == DRAWN_SHAPE) {
            Drawing.parse(drawing)?.let { d ->
                val stand = StandSprite(d.rows)
                val def = ShapeDef(DRAWN_SHAPE, ShapeKind.ANIMAL, stand, fixed = d.colors, strict = false)
                return renderStand(def, Anatomy.of(stand, def.bodyLines, def.gait), parts, pose, frame, eyesClosed, walk, look)
            }
        }
        // 모르는 모양 id(잘못된 그림 포함)는 누구에 따라 기본 모양으로 대신 그린다.
        val def = Shapes.resolve(shape, kind)
        return renderStand(def, Shapes.anatomy(def.id), parts, pose, frame, eyesClosed, walk, look)
    }

    /**
     * 그림판에 보일 붓 색: 서 있는 그림 글자(K B W L O P H N 1~4) → 실제로 그려질 색(동물 색표 + 머리·하의·고정 색·눈 색).
     * 엔진과 같은 글자 변환(Poser)과 색표를 따르므로 그림판과 공원의 색이 같다.
     */
    fun drawingColors(body: BodyColor, eye: EyeColor, look: PersonLook, colors: Map<Char, Int>): Map<Char, Int> {
        val palette = COMMON + bodyPalette(DRAWN_DEF, body, look) + ('E' to EYE.getValue(eye)) +
            colors.filterKeys { it in FIXED_SLOT }.mapKeys { (c, _) -> FIXED_SLOT.getValue(c) }
        return mapOf(
            'K' to 'o', 'B' to 'D', 'W' to 'L', 'L' to 'M', 'O' to 'E', 'P' to 'o',
            'H' to 'h', 'N' to 'n', '1' to 'a', '2' to 'b', '3' to 'c', '4' to 'd',
        ).mapValues { (_, slot) -> palette.getValue(slot) }
    }

    /** 눈 색(슬롯 E). 초록은 예전 고정 색과 같고, 검정은 테두리(#2A2C30)와 구분되게 더 진하다. */
    private val EYE = mapOf(
        EyeColor.GREEN to 0xFF3DBE6B.toInt(), EyeColor.BLUE to 0xFF3B82F6.toInt(), EyeColor.BROWN to 0xFF8A5A3C.toInt(),
        EyeColor.BLACK to 0xFF111114.toInt(), EyeColor.PURPLE to 0xFF8B5CF6.toInt(), EyeColor.GOLD to 0xFFF5B301.toInt(),
    )

    private val DRAWN_DEF by lazy { ShapeDef(DRAWN_SHAPE, ShapeKind.ANIMAL, Shapes.WOLF) }

    /** 목록에 없는 모양(사용자가 그린 그림 등)도 바로 그린다. */
    fun renderStand(
        def: ShapeDef,
        anatomy: Anatomy,
        parts: CharacterParts,
        pose: Pose,
        frame: Int,
        eyesClosed: Boolean = false,
        walk: WalkStyle = WalkStyle.DEFAULT,
        look: PersonLook = PersonLook(),
    ): PixelImage {
        val stand = def.stand
        // 눈 색: 지역 캐릭터는 고른 눈 색을 따르지 않고 자기 눈 색(퍼스널 컬러, 없으면 COMMON의 초록)을 쓴다.
        val eye = if (def.kind == ShapeKind.MASCOT) def.eye?.let { mapOf('E' to it) } ?: emptyMap() else mapOf('E' to EYE.getValue(parts.eye))
        val palette = COMMON + bodyPalette(def, parts.body, look) + eye +
            def.fixed.mapKeys { (c, _) -> FIXED_SLOT.getValue(c) }
        val w = Poser.canvasW(stand.size)
        val h = Poser.canvasH(stand.size)
        val out = IntArray(w * h)
        val posed = Poser.pose(stand, anatomy, pose, frame, parts.face, eyesClosed, walk, nose = def.nose)
        paint(out, w, posed.sprite, palette)
        Outfits.hat(parts.hat, anatomy)?.let { paint(out, w, it, palette, posed.headDy) }
        Outfits.accessory(parts.accessory, anatomy, stand)?.let { paint(out, w, it, palette, posed.headDy) }
        return PixelImage(w, h, out)
    }

    /** 서 있는 그림 고정 색 글자 → 앱 슬롯(모자 색 1~4와 겹치지 않게). */
    private val FIXED_SLOT = mapOf('1' to 'a', '2' to 'b', '3' to 'c', '4' to 'd')

    private val SKIN = mapOf(
        SkinTone.LIGHT to 0xFFF6D7C3.toInt(), SkinTone.MEDIUM to 0xFFD9A27E.toInt(), SkinTone.DEEP to 0xFF8D5A3B.toInt(),
    )
    private val HAIR = mapOf(
        HairColor.BLACK to 0xFF3A3132.toInt(), HairColor.BROWN to 0xFF7B4B2A.toInt(),
        HairColor.BLOND to 0xFFE2B85C.toInt(), HairColor.GRAY to 0xFFA1A1AA.toInt(),
    )
    private val BOTTOM = mapOf(
        BottomColor.NAVY to 0xFF3B4A6B.toInt(), BottomColor.DENIM to 0xFF5B7DB1.toInt(), BottomColor.BLACK to 0xFF2F3033.toInt(),
        BottomColor.BEIGE to 0xFFCDB89A.toInt(), BottomColor.GRAY to 0xFF8C8F96.toInt(),
    )

    /** 종류별 역할 색: D 몸 색, L 밝은 색, M 그늘, h 머리, n 하의. */
    private fun bodyPalette(def: ShapeDef, body: BodyColor, look: PersonLook): Map<Char, Int> {
        val (dark, light) = fur(body)
        return when (def.kind) {
            ShapeKind.ANIMAL -> {
                // M = 흰 털의 그늘(설명서 L #B8BAC4). 다른 털 색은 밝은 털에 어두운 털을 조금 섞는다.
                val shade = if (body == BodyColor.PURPLE) 0xFFB8BAC4.toInt() else blend(light, dark, 0.3f)
                // h·n은 기본 동물 모양에는 없고, 직접 그린 그림의 머리·하의 붓에 쓴다.
                mapOf('D' to dark, 'L' to light, 'M' to shade, 'h' to HAIR.getValue(look.hair), 'n' to BOTTOM.getValue(look.bottom))
            }
            ShapeKind.PERSON -> {
                val skin = SKIN.getValue(look.skin)
                mapOf('D' to dark, 'L' to skin, 'M' to blend(skin, dark, 0.3f), 'h' to HAIR.getValue(look.hair), 'n' to BOTTOM.getValue(look.bottom))
            }
            ShapeKind.MASCOT -> mapOf('L' to 0xFFF7F7F5.toInt(), 'M' to 0xFFD4D4D8.toInt())
        }
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
            // 머리와 같이 옮긴 덧그림이 그림판 밖으로 나가면 조용히 자르지 않고 알린다(Outfits가 여유를 두고 놓는다).
            check(ty in 0 until sprite.height) { "덧그림이 그림판 밖으로 나감: ($x,$y) + $dy" }
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
