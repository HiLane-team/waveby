package com.drivepark.app.data

import com.drivepark.app.domain.Character
import com.drivepark.app.domain.Friend
import com.drivepark.app.domain.Sticker
import com.drivepark.app.domain.ParkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomParkRepository(db: AppDatabase) : ParkRepository {
    private val dao = db.parkDao()

    override suspend fun getMyCharacter(): Character? = dao.getMyCharacter()?.character?.toDomain()

    override suspend fun saveMyCharacter(character: Character) =
        dao.upsertMyCharacter(MyCharacterEntity(character = CharacterColumns.from(character)))

    override fun observeMyCharacter(): Flow<Character?> =
        dao.observeMyCharacter().map { it?.character?.toDomain() }

    override suspend fun addPending(characters: List<Character>) {
        if (characters.isEmpty()) return
        dao.insertPending(characters.map { PendingEntity(character = CharacterColumns.from(it)) })
    }

    override suspend fun deliverPending(): List<Character> =
        dao.deliverPending().map { it.character.toDomain() }

    override fun observeFriends(): Flow<List<Friend>> = dao.observeFriends().map { list ->
        // 모르는 스티커 이름(예전·손상 데이터)은 없는 것으로 읽는다.
        list.map { e -> Friend(e.id, e.character.toDomain(), e.isNew, Sticker.entries.firstOrNull { it.name == e.sticker }) }
    }

    override suspend fun markAllSeen() = dao.markAllSeen()

    override suspend fun setSticker(friendId: Long, sticker: Sticker?) = dao.setSticker(friendId, sticker?.name)

    override suspend fun collectRegion(regionId: String): Boolean =
        dao.insertCollectedRegion(CollectedRegionEntity(regionId)) != -1L

    override fun observeCollectedRegionIds(): Flow<Set<String>> =
        dao.observeCollectedRegionIds().map { it.toSet() }

    override suspend fun resetAll() = dao.resetAll()
}
