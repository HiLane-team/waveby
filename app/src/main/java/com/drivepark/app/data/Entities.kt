package com.drivepark.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.drivepark.app.domain.Accessory
import com.drivepark.app.domain.BodyColor
import com.drivepark.app.domain.Character
import com.drivepark.app.domain.CharacterParts
import com.drivepark.app.domain.Face
import com.drivepark.app.domain.Hat

// 어떤 엔티티에도 시각 컬럼을 두지 않는다. 파츠는 enum name 문자열로 저장한다.

data class CharacterColumns(
    val body: String,
    val face: String,
    val hat: String,
    val accessory: String,
    val nickname: String,
    val greeting: String,
) {
    fun toDomain() = Character(
        CharacterParts(BodyColor.valueOf(body), Face.valueOf(face), Hat.valueOf(hat), Accessory.valueOf(accessory)),
        nickname,
        greeting,
    )

    companion object {
        fun from(c: Character) = CharacterColumns(
            c.parts.body.name, c.parts.face.name, c.parts.hat.name, c.parts.accessory.name,
            c.nickname, c.greeting,
        )
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
)

@Entity(tableName = "collected_region")
data class CollectedRegionEntity(@PrimaryKey val regionId: String)
