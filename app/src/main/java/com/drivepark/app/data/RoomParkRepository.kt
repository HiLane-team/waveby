package com.drivepark.app.data

import com.drivepark.app.domain.Character
import com.drivepark.app.domain.Friend
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
        list.map { Friend(it.id, it.character.toDomain(), it.isNew) }
    }

    override suspend fun markAllSeen() = dao.markAllSeen()

    override suspend fun collectRegion(regionId: String): Boolean =
        dao.insertCollectedRegion(CollectedRegionEntity(regionId)) != -1L

    override fun observeCollectedRegionIds(): Flow<Set<String>> =
        dao.observeCollectedRegionIds().map { it.toSet() }

    override suspend fun resetAll() = dao.resetAll()
}
