package com.drivepark.app.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** 테스트용 리스트 기반 ParkRepository. Room 구현과 같은 계약을 따른다. */
class InMemoryParkRepository : ParkRepository {
    private val me = MutableStateFlow<Character?>(null)
    val pending = mutableListOf<Character>()
    private val friends = MutableStateFlow<List<Friend>>(emptyList())
    private val regions = MutableStateFlow<Set<String>>(emptySet())
    private var nextId = 1L

    override suspend fun getMyCharacter() = me.value
    override suspend fun saveMyCharacter(character: Character) { me.value = character }
    override fun observeMyCharacter(): Flow<Character?> = me

    override suspend fun addPending(characters: List<Character>) { pending += characters }

    override suspend fun deliverPending(): List<Character> {
        val out = pending.toList()
        pending.clear()
        val added = out.map { Friend(nextId++, it, isNew = true) }
        friends.value = added.reversed() + friends.value
        return out
    }

    override fun observeFriends(): Flow<List<Friend>> = friends
    override suspend fun markAllSeen() { friends.value = friends.value.map { it.copy(isNew = false) } }

    override suspend fun collectRegion(regionId: String): Boolean {
        if (regionId in regions.value) return false
        regions.value = regions.value + regionId
        return true
    }

    override fun observeCollectedRegionIds(): Flow<Set<String>> = regions.map { it }

    override suspend fun resetAll() {
        pending.clear()
        friends.value = emptyList()
        regions.value = emptySet()
    }
}
