package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.CharacterKind
import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.BottomColor
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.HairColor
import com.drivepark.app.domain.Hat
import com.drivepark.app.domain.PersonLook
import com.drivepark.app.domain.SkinTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapesTest {
    private val parts = CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.NONE)

    /** 두 발 사람형 시험 그림에서 (x,y) 칸들을 글자 c로 바꾼다. */
    private fun biped(vararg cells: Pair<Pair<Int, Int>, Char>): StandSprite {
        val rows = TestShapes.BIPED.rows.map { StringBuilder(it) }
        cells.forEach { (p, c) -> rows[p.second][p.first] = c }
        return StandSprite(rows.map { it.toString() })
    }

    private fun color(img: PixelImage, x: Int, y: Int) = img.argb[(y + Poser.PAD_TOP) * img.width + x + Poser.PAD_X]

    @Test(expected = IllegalArgumentException::class)
    fun fixedLetterWithoutColor_isRejected() {
        ShapeDef("x", ShapeKind.ANIMAL, biped((5 to 16) to '1'))
    }

    @Test(expected = IllegalArgumentException::class)
    fun hairOnAnimal_isRejected() {
        ShapeDef("x", ShapeKind.ANIMAL, biped((5 to 5) to 'H'))
    }

    @Test(expected = IllegalArgumentException::class)
    fun bottomOnMascot_isRejected() {
        ShapeDef("x", ShapeKind.MASCOT, biped((4 to 21) to 'N'))
    }

    @Test(expected = IllegalArgumentException::class)
    fun mascotUsingBodyColor_isRejected() {
        // 두 발 사람형 시험 그림은 몸이 B로 칠해져 있다.
        ShapeDef("x", ShapeKind.MASCOT, TestShapes.BIPED)
    }

    @Test
    fun standSprite_acceptsNewLetters() {
        biped((5 to 5) to 'H', (4 to 21) to 'N', (5 to 16) to '1', (6 to 16) to '2', (7 to 16) to '3', (8 to 16) to '4')
    }

    @Test
    fun personPalette_followsLook() {
        val stand = biped((5 to 5) to 'H', (4 to 21) to 'N')
        val def = ShapeDef("p", ShapeKind.PERSON, stand)
        val img = CharacterRenderer.renderStand(
            def, Anatomy.of(stand), parts.copy(body = BodyColor.RED), Pose.IDLE, 0,
            look = PersonLook(SkinTone.DEEP, HairColor.BLOND, BottomColor.DENIM),
        )
        assertEquals(0xFFE2B85C.toInt(), color(img, 5, 5))
        assertEquals(0xFF5B7DB1.toInt(), color(img, 4, 21))
        assertEquals(0xFF8D5A3B.toInt(), color(img, 5, 7))
        assertEquals(0xFF8A4B45.toInt(), color(img, 5, 16))
    }

    /** 앱에 넣는 모양 + 아직 초안인 모양. 초안도 규칙은 지켜야 한다. */
    private val everyShape = Shapes.ALL.values + Shapes.DRAFT_SHAPES

    @Test
    fun codexDesignsShip_rabbitAndHamsterStayDrafts() {
        // 2026-11-08 대표 결정: Codex 시안(강아지·고양이·지역 5종)을 앱에 넣는다. 토끼·햄스터는 시안에 없어 초안으로 남긴다.
        assertEquals(
            setOf("person", "wolf", "dog", "cat", "hoengseong", "boseong", "jeju", "gangneung", "yeongdeok"),
            Shapes.ALL.keys,
        )
        assertEquals(setOf("rabbit", "hamster"), Shapes.DRAFT_SHAPES.map { it.id }.toSet())
    }

    @Test
    fun kinds() {
        assertEquals(ShapeKind.PERSON, Shapes.def("person").kind)
        assertEquals(ShapeKind.ANIMAL, Shapes.def("wolf").kind)
        com.drivepark.app.domain.Regions.ALL.forEach { r ->
            assertEquals(r.id, ShapeKind.MASCOT, Shapes.def(r.id).kind)
            assertTrue(r.id, Shapes.hasMascot(r.id))
        }
    }

    @Test
    fun mascots_haveTheirOwnEyeColour() {
        // 지역 캐릭터마다 어울리는 눈 색(퍼스널 컬러). 고른 눈 색은 따르지 않는다.
        val eyes = com.drivepark.app.domain.Regions.ALL.map { r -> r.id to Shapes.def(r.id).eye }.toMap()
        eyes.forEach { (id, c) -> assertTrue("$id 눈 색 없음", c != null) }
        assertEquals("지역마다 다른 눈 색", eyes.size, eyes.values.toSet().size)
        for (r in com.drivepark.app.domain.Regions.ALL) {
            val d = Shapes.def(r.id)
            val img = CharacterRenderer.renderStand(d, Anatomy.of(d.stand), parts.copy(eye = com.drivepark.app.domain.EyeColor.BLUE), Pose.IDLE, 0)
            val eyeCells = d.stand.rows.flatMapIndexed { y, row -> row.mapIndexedNotNull { x, ch -> if (ch == 'O') x to y else null } }
            assertTrue(r.id, eyeCells.isNotEmpty())
            val w = Poser.canvasW(d.stand.size)
            eyeCells.forEach { (x, y) -> assertEquals(r.id, d.eye, img.argb[(y + Poser.PAD_TOP) * w + x + Poser.PAD_X]) }
        }
    }

    @Test
    fun builtInShapes_haveNoDrawingHints() {
        everyShape.forEach { d -> assertEquals(d.id, emptyList<DrawingHint>(), Anatomy.of(d.stand).hints) }
    }

    @Test
    fun builtInShapes_animateAndDressWithoutLeavingCanvas() {
        for (d in everyShape) {
            val a = Anatomy.of(d.stand)
            for (p in Pose.entries) for (f in 0..3) for (closed in listOf(false, true)) for (face in Face.entries) {
                val rows = Poser.pose(d.stand, a, p, f, face, closed).sprite.rows
                val painted = rows.indices.filter { y -> rows[y].any { it != '.' } }
                val gaps = (painted.first()..painted.last()).filter { it !in painted }
                assertEquals("${d.id} $p $f", emptyList<Int>(), gaps)
                for (h in Hat.entries) for (acc in Accessory.entries) {
                    CharacterRenderer.renderStand(d, a, parts.copy(face = face, hat = h, accessory = acc), p, f, closed)
                }
            }
        }
    }

    @Test
    fun resolve_unknownFallsBackByKind() {
        assertEquals("person", Shapes.resolve("ghost", com.drivepark.app.domain.CharacterKind.PERSON).id)
        assertEquals("wolf", Shapes.resolve("ghost", CharacterKind.PET).id)
        assertEquals("wolf", Shapes.resolve("wolf", com.drivepark.app.domain.CharacterKind.PERSON).id)
        assertEquals(null, Shapes.find("ghost"))
        assertEquals(listOf("wolf", "dog", "cat"), Shapes.animals.map { it.id })
        assertEquals("사람", Shapes.def("person").label)
        assertEquals("늑대", Shapes.def("wolf").label)
    }

    @Test
    fun seatedPerson_foldsLegsInTrouserColour() {
        val look = PersonLook(bottom = BottomColor.DENIM)
        val img = CharacterRenderer.render(parts, Pose.SIT, 0, shape = "person", look = look)
        // 접은 다리 줄(격자 21줄)에 하의 색이 있어야 한다(피부색이면 안 됨).
        val row = (0 until img.width).map { img.argb[(21 + Poser.PAD_TOP) * img.width + it] }
        assertTrue(0xFF5B7DB1.toInt() in row)
        assertFalse(0xFFF6D7C3.toInt() in row)
    }

    @Test
    fun personCheeks_arePinkNotInk() {
        val img = CharacterRenderer.render(parts, Pose.IDLE, 0, shape = "person")
        // 볼(격자 3,12)은 분홍, 테두리 먹색이 아니다.
        assertEquals(0xFFF2A7B5.toInt(), color(img, 3, 12))
    }

    @Test
    fun mascotPalette_ignoresBodyColor() {
        val rows = TestShapes.BIPED.rows.map { it.replace('B', '1') }
        val stand = StandSprite(rows)
        val def = ShapeDef("m", ShapeKind.MASCOT, stand, mapOf('1' to 0xFFFFA726.toInt()))
        val a = CharacterRenderer.renderStand(def, Anatomy.of(stand), parts, Pose.IDLE, 0)
        val b = CharacterRenderer.renderStand(def, Anatomy.of(stand), parts.copy(body = BodyColor.RED), Pose.IDLE, 0)
        assertEquals(a.argb.toList(), b.argb.toList())
        assertEquals(0xFFFFA726.toInt(), color(a, 5, 16))
        assertEquals(0xFFF7F7F5.toInt(), color(a, 5, 7))
    }

    @Test
    fun fakeFriendAnimals_areWolfAndCatOnly() {
        // 2026-11-09 대표 결정: 가짜 친구 반려동물은 늑대·고양이만(강아지는 시연 영상에서 어색해 보여 뺀다).
        assertEquals(listOf("wolf", "cat"), Shapes.fakeFriendAnimalIds)
        assertTrue(Shapes.fakeFriendAnimalIds.all { it in Shapes.animalIds })
    }
}
