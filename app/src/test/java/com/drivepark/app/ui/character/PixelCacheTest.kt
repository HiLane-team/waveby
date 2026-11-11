package com.drivepark.app.ui.character

import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.Hat
import com.drivepark.app.domain.PersonLook
import com.drivepark.app.domain.Regions
import com.drivepark.app.domain.SkinTone
import com.drivepark.app.ui.pixel.Pose
import com.drivepark.app.ui.pixel.Shapes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PixelCacheTest {
    private val parts = CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.NONE)

    @Test
    fun cacheKey_includesShapeAndLook() {
        val wolf = PixelCache.character(parts, Pose.IDLE, 0, false, "wolf", PersonLook())
        val person = PixelCache.character(parts, Pose.IDLE, 0, false, "person", PersonLook())
        val darker = PixelCache.character(parts, Pose.IDLE, 0, false, "person", PersonLook(skin = SkinTone.DEEP))
        assertNotSame(wolf, person)
        assertNotSame(person, darker)
        assertSame(person, PixelCache.character(parts, Pose.IDLE, 0, false, "person", PersonLook()))
    }

    @Test
    fun cacheKey_includesEyeColor() {
        // 눈 색만 달라도 다른 그림이어야 한다(열쇠의 parts에 눈 색이 들어 있다).
        val green = PixelCache.character(parts, Pose.IDLE, 0, false, "wolf", PersonLook())
        val gold = PixelCache.character(parts.copy(eye = com.drivepark.app.domain.EyeColor.GOLD), Pose.IDLE, 0, false, "wolf", PersonLook())
        assertNotSame(green, gold)
    }

    @Test
    fun mascotAnimates_onlyWhenCollectedAndAnimationsAllowed() {
        // 주행 중·쉬는 중(LocalAnimationsEnabled false)이나 실루엣이면 숨쉬기·깜빡임을 하지 않는다.
        org.junit.Assert.assertTrue(mascotAnimates(silhouette = false, animationsEnabled = true))
        assertFalse(mascotAnimates(silhouette = false, animationsEnabled = false))
        assertFalse(mascotAnimates(silhouette = true, animationsEnabled = true))
    }

    @Test
    fun everyRegionHasItsNewMascot() {
        // 2026-11-08: 지역 5종 새 그림(Codex 시안)이 모두 등록됐다.
        Regions.ALL.forEach { org.junit.Assert.assertTrue(it.id, Shapes.hasMascot(it.id)) }
    }
}
