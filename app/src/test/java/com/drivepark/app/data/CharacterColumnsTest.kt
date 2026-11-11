package com.drivepark.app.data

import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.BottomColor
import com.drivepark.app.domain.Character
import com.drivepark.app.domain.CharacterKind
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.EyeColor
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.HairColor
import com.drivepark.app.domain.Hat
import com.drivepark.app.domain.PersonLook
import com.drivepark.app.domain.SkinTone
import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterColumnsTest {
    @Test
    fun roundTrip_keepsKindShapeLook() {
        val c = Character(
            CharacterParts(BodyColor.RED, Face.WINK, Hat.CAP, Accessory.SCARF), "콩이", "멍",
            CharacterKind.PET, "wolf", PersonLook(SkinTone.DEEP, HairColor.BLOND, BottomColor.DENIM),
        )
        assertEquals(c, CharacterColumns.from(c).toDomain())
    }

    @Test
    fun unknownEnumNames_fallBackToDefaults() {
        // 예전·손상된 데이터에 모르는 이름이 있어도 앱이 멈추지 않고 기본값으로 읽는다.
        val cols = CharacterColumns("PINK", "GRIN", "TOP_HAT", "CAPE", "나", "안녕", "ROBOT", "wolf", "X", "Y", "Z")
        val c = cols.toDomain()
        assertEquals(CharacterParts(BodyColor.PURPLE, Face.SMILE, Hat.NONE, Accessory.NONE), c.parts)
        assertEquals(CharacterKind.PERSON, c.kind)
        assertEquals(PersonLook(), c.look)
        assertEquals("wolf", c.shape)
    }

    @Test
    fun eye_roundTrips() {
        val c = Character(CharacterParts(BodyColor.RED, Face.WINK, Hat.CAP, Accessory.SCARF, EyeColor.GOLD), "콩이", "멍")
        assertEquals(EyeColor.GOLD, CharacterColumns.from(c).toDomain().parts.eye)
    }

    @Test
    fun unknownEye_fallsBackToGreen() {
        val cols = CharacterColumns("RED", "WINK", "CAP", "SCARF", "나", "안녕", eye = "NOPE")
        assertEquals(EyeColor.GREEN, cols.toDomain().parts.eye)
    }
}
