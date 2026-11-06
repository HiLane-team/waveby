package com.drivepark.app.domain

import kotlinx.coroutines.flow.Flow

/** 놀러온 친구. 시각 필드는 두지 않는다. id는 자동 증가이며 정렬에만 쓴다. */
data class Friend(val id: Long, val character: Character, val isNew: Boolean)

interface ParkRepository {
    suspend fun getMyCharacter(): Character?
    suspend fun saveMyCharacter(character: Character)
    fun observeMyCharacter(): Flow<Character?>

    suspend fun addPending(characters: List<Character>)

    /** 대기함 전부를 Friend(isNew=true)로 옮기고 대기함을 비운다. 하나의 트랜잭션이다. */
    suspend fun deliverPending(): List<Character>

    /** id 내림차순(최신 먼저). */
    fun observeFriends(): Flow<List<Friend>>
    suspend fun markAllSeen()

    /** 새로 획득했으면 true. */
    suspend fun collectRegion(regionId: String): Boolean
    fun observeCollectedRegionIds(): Flow<Set<String>>

    /** 대기함, 친구, 도감 기록을 한 트랜잭션으로 지운다. 내 캐릭터는 남긴다. */
    suspend fun resetAll()
}
