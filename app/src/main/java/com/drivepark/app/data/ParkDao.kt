package com.drivepark.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ParkDao {
    @Query("SELECT * FROM my_character WHERE id = 0")
    abstract suspend fun getMyCharacter(): MyCharacterEntity?

    @Query("SELECT * FROM my_character WHERE id = 0")
    abstract fun observeMyCharacter(): Flow<MyCharacterEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertMyCharacter(entity: MyCharacterEntity)

    @Insert
    abstract suspend fun insertPending(items: List<PendingEntity>)

    @Query("SELECT * FROM pending ORDER BY id ASC")
    abstract suspend fun getPending(): List<PendingEntity>

    @Query("DELETE FROM pending")
    abstract suspend fun clearPending()

    @Insert
    abstract suspend fun insertFriends(items: List<FriendEntity>)

    /** 조회 → 친구 삽입 → 대기함 삭제를 하나의 트랜잭션으로 처리한다. */
    @Transaction
    open suspend fun deliverPending(): List<PendingEntity> {
        val pending = getPending()
        if (pending.isEmpty()) return pending
        insertFriends(pending.map { FriendEntity(character = it.character, isNew = true) })
        clearPending()
        return pending
    }

    @Query("SELECT * FROM friend ORDER BY id DESC")
    abstract fun observeFriends(): Flow<List<FriendEntity>>

    @Query("UPDATE friend SET sticker = :sticker WHERE id = :id")
    abstract suspend fun setSticker(id: Long, sticker: String?)

    @Query("UPDATE friend SET isNew = 0 WHERE isNew = 1")
    abstract suspend fun markAllSeen()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertCollectedRegion(entity: CollectedRegionEntity): Long

    @Query("DELETE FROM friend")
    abstract suspend fun clearFriends()

    @Query("DELETE FROM collected_region")
    abstract suspend fun clearCollectedRegions()

    /** 대기함, 친구, 도감을 하나의 트랜잭션으로 지운다(내 캐릭터는 유지). */
    @Transaction
    open suspend fun resetAll() {
        clearPending()
        clearFriends()
        clearCollectedRegions()
    }

    @Query("SELECT regionId FROM collected_region")
    abstract fun observeCollectedRegionIds(): Flow<List<String>>
}
