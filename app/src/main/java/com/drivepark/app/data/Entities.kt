package com.drivepark.app.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
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

// 어떤 엔티티에도 시각 컬럼을 두지 않는다. 파츠·외모는 enum name 문자열로 저장한다.

data class CharacterColumns(
    val body: String,
    val face: String,
    val hat: String,
    val accessory: String,
    val nickname: String,
    val greeting: String,
    // 버전 2에서 추가. 기본값은 MIGRATION_1_2와 같아야 한다(기존 사용자는 늑대·나).
    @ColumnInfo(defaultValue = "PERSON") val kind: String = CharacterKind.PERSON.name,
    @ColumnInfo(defaultValue = "wolf") val shape: String = "wolf",
    @ColumnInfo(defaultValue = "LIGHT") val skin: String = SkinTone.LIGHT.name,
    @ColumnInfo(defaultValue = "BLACK") val hair: String = HairColor.BLACK.name,
    @ColumnInfo(defaultValue = "NAVY") val bottom: String = BottomColor.NAVY.name,
    // 버전 3에서 추가. 비어 있어도 된다(직접 그린 그림이 없는 캐릭터). 잘못된 그림도 그대로 읽고 그리는 쪽에서 대신 그린다.
    val drawing: String? = null,
    // 버전 5에서 추가. 기본값은 MIGRATION_4_5와 같아야 한다(기존 캐릭터는 초록 눈).
    @ColumnInfo(defaultValue = "GREEN") val eye: String = EyeColor.GREEN.name,
) {
    /** 모르는 이름(예전·손상 데이터)은 기본값으로 읽는다. */
    fun toDomain() = Character(
        CharacterParts(
            enumOr(body, BodyColor.PURPLE), enumOr(face, Face.SMILE), enumOr(hat, Hat.NONE), enumOr(accessory, Accessory.NONE),
            enumOr(eye, EyeColor.GREEN),
        ),
        nickname,
        greeting,
        enumOr(kind, CharacterKind.PERSON),
        shape.ifBlank { "wolf" },
        PersonLook(enumOr(skin, SkinTone.LIGHT), enumOr(hair, HairColor.BLACK), enumOr(bottom, BottomColor.NAVY)),
        drawing,
    )

    companion object {
        fun from(c: Character) = CharacterColumns(
            c.parts.body.name, c.parts.face.name, c.parts.hat.name, c.parts.accessory.name,
            c.nickname, c.greeting,
            c.kind.name, c.shape, c.look.skin.name, c.look.hair.name, c.look.bottom.name,
            c.drawing, c.parts.eye.name,
        )

        private inline fun <reified E : Enum<E>> enumOr(name: String, default: E): E =
            enumValues<E>().firstOrNull { it.name == name } ?: default
    }
}

@Entity(tableName = "my_character")
data class MyCharacterEntity(
    @PrimaryKey val id: Int = 0,
    @Embedded val character: CharacterColumns,
)

@Entity(tableName = "pending")
data class PendingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @Embedded val character: CharacterColumns,
)

@Entity(tableName = "friend")
data class FriendEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @Embedded val character: CharacterColumns,
    val isNew: Boolean,
    // 버전 4에서 추가. 내가 남긴 답례 스티커 이름(Sticker), 없으면 null. 시각은 남기지 않는다.
    val sticker: String? = null,
)

@Entity(tableName = "collected_region")
data class CollectedRegionEntity(@PrimaryKey val regionId: String)
