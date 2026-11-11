package com.drivepark.app.ui.park

import com.drivepark.app.domain.Sticker

import com.drivepark.app.domain.InMemoryParkRepository
import com.drivepark.app.domain.testCharacter
import com.drivepark.app.ui.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ParkViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repo = InMemoryParkRepository()

    @Test
    fun wandering_capsAtTen() = runTest {
        repo.addPending((1..12).map { testCharacter("친구$it") })
        repo.deliverPending()
        val vm = ParkViewModel(repo)
        val s = vm.state.first { it.newFriends.isNotEmpty() }
        assertEquals(10, s.wandering.size)
        assertEquals("친구12", s.wandering.first().character.nickname)
        assertEquals("친구3", s.wandering.last().character.nickname)
    }

    @Test
    fun collectedRegions_skipsUnknownIds() = runTest {
        repo.collectRegion("jeju")
        repo.collectRegion("atlantis") // 예전 데이터나 잘못 저장된 id
        val vm = ParkViewModel(repo)
        val s = vm.state.first { it.collectedRegions.isNotEmpty() }
        assertEquals(listOf("jeju"), s.collectedRegions.map { it.id })
    }

    @Test
    fun splitsNewAndSeen() = runTest {
        repo.addPending(listOf(testCharacter("옛")))
        repo.deliverPending()
        repo.markAllSeen()
        repo.addPending(listOf(testCharacter("새")))
        repo.deliverPending()
        val vm = ParkViewModel(repo)
        val s = vm.state.first { it.newFriends.isNotEmpty() }
        assertEquals(listOf("새"), s.newFriends.map { it.character.nickname })
        assertEquals(listOf("옛"), s.seenFriends.map { it.character.nickname })
    }

    @Test
    fun setSticker_storesAndSameStickerClears() = runTest {
        repo.addPending(listOf(testCharacter("하나")))
        repo.deliverPending()
        val vm = ParkViewModel(repo)
        val id = vm.state.first { it.newFriends.isNotEmpty() }.newFriends.single().id
        vm.toggleSticker(id, Sticker.HEART)
        assertEquals(Sticker.HEART, vm.state.first { it.newFriends.single().sticker != null }.newFriends.single().sticker)
        vm.toggleSticker(id, Sticker.WAVE)
        assertEquals(Sticker.WAVE, vm.state.first { it.newFriends.single().sticker == Sticker.WAVE }.newFriends.single().sticker)
        // 같은 스티커를 다시 누르면 취소된다.
        vm.toggleSticker(id, Sticker.WAVE)
        assertEquals(null, vm.state.first { it.newFriends.single().sticker == null }.newFriends.single().sticker)
    }
}
