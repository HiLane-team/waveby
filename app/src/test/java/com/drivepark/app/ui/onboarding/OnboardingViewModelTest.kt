package com.drivepark.app.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingViewModelTest {
    @Test
    fun startRoute_dependsOnCharacter() {
        assertEquals("park", startRoute(hasCharacter = true))
        assertEquals("onboarding", startRoute(hasCharacter = false))
    }

    @Test
    fun steps_clampAtBounds() {
        val vm = OnboardingViewModel()
        vm.back()
        assertEquals(0, vm.step.value)
        repeat(10) { vm.next() }
        assertEquals(OnboardingViewModel.LAST_STEP, vm.step.value)
    }

    @Test
    fun fiveSteps_whoComesBeforeCharacter() {
        // 0 환영, 1 권한, 2 누구, 3 캐릭터, 4 완료 (취향 단계는 빼고 기본값 → 나중에 설정에서)
        assertEquals(4, OnboardingViewModel.LAST_STEP)
        assertEquals(2, OnboardingViewModel.STEP_WHO)
        assertEquals(3, OnboardingViewModel.STEP_CHARACTER)
    }
}
