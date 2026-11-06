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
        assertEquals(4, OnboardingViewModel.LAST_STEP)
    }
}
