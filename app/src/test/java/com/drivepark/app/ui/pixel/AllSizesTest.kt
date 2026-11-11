package com.drivepark.app.ui.pixel

import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.Hat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 네 크기 × 다리 있음/없음 그림에서 모든 동작·표정·모자·소품이 그림판 안에 그려지고, 덧그림이 사라지지 않는지. */
class AllSizesTest {
    private val parts = CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.NONE)

    @Test
    fun everySize_everyPoseAndOutfit_staysInside() {
        for (s in TestShapes.SIZED) {
            val def = ShapeDef("t", ShapeKind.ANIMAL, s)
            val a = Anatomy.of(s, def.bodyLines, def.gait)
            for (p in Pose.entries) for (f in 0..3) for (face in Face.entries) for (h in Hat.entries) for (acc in Accessory.entries) {
                val img = CharacterRenderer.renderStand(def, a, parts.copy(face = face, hat = h, accessory = acc), p, f)
                assertEquals("${s.size}", Poser.canvasW(s.size) to Poser.canvasH(s.size), img.width to img.height)
            }
        }
    }

    @Test
    fun outfits_neverVanish() {
        for (s in TestShapes.SIZED) {
            val a = Anatomy.of(s)
            for (h in Hat.entries.filter { it != Hat.NONE }) {
                val hat = Outfits.hat(h, a)
                assertNotNull("${s.size} $h", hat)
                assertTrue("${s.size} $h 빈 모자", hat!!.rows.any { r -> r.any { it != '.' } })
            }
            for (acc in Accessory.entries.filter { it != Accessory.NONE }) {
                val o = Outfits.accessory(acc, a, s)
                assertNotNull("${s.size} $acc", o)
                assertTrue("${s.size} $acc 빈 소품", o!!.rows.any { r -> r.any { it != '.' } })
            }
        }
    }

    @Test
    fun scarf_followsHeadEnd() {
        // 머리 끝을 12로 정하면 목도리 띠가 12·13줄(그림판 줄 +1)에 놓인다(기본 14면 14·15줄).
        val s = TestShapes.BIPED
        val scarf = Outfits.accessory(Accessory.SCARF, Anatomy.of(s, BodyLines(12, 20)), s)!!
        val rows = scarf.rows.indices.filter { y -> scarf.rows[y].any { it == 'R' } }
        assertEquals(12 + Poser.PAD_TOP, rows.first())
        assertTrue(13 + Poser.PAD_TOP in rows)
    }
}
