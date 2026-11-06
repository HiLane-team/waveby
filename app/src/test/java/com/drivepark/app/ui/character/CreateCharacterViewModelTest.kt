package com.drivepark.app.ui.character

import com.drivepark.app.domain.InMemoryParkRepository
import com.drivepark.app.ui.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CreateCharacterViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repo = InMemoryParkRepository()

    @Test
    fun save_rejectsInvalidInput_keepsError() = runTest {
        val vm = CreateCharacterViewModel(repo)
        vm.setNickname("   ")
        vm.setGreeting("안녕!")
        vm.save()
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.saved)
        assertNull(repo.getMyCharacter())
    }

    @Test
    fun typing_stopsAtTheLimit() {
        val vm = CreateCharacterViewModel(repo)
        vm.setNickname("Waveby3Hello") // 12자
        vm.setGreeting("가".repeat(40))
        assertEquals("Waveby3Hel", vm.state.value.nickname) // 닉네임은 10자에서 멈춘다
        assertEquals(30, vm.state.value.greeting.length)   // 인사말은 30자에서 멈춘다
    }

    @Test
    fun save_validInput_persistsTrimmed() = runTest {
        val vm = CreateCharacterViewModel(repo)
        vm.setNickname(" 뚜벅이 ")
        vm.setGreeting("안녕!")
        vm.save()
        assertTrue(vm.state.value.saved)
        assertEquals("뚜벅이", repo.getMyCharacter()?.nickname)
    }
}
