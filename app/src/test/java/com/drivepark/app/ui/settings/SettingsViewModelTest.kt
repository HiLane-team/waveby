package com.drivepark.app.ui.settings

import com.drivepark.app.domain.InMemoryParkRepository
import com.drivepark.app.domain.InMemorySettingsRepository
import com.drivepark.app.ui.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val settings = InMemorySettingsRepository()
    private val vm by lazy { SettingsViewModel(settings, InMemoryParkRepository(), overlayGranted = { false }, locationGranted = { true }) }

    @Test
    fun versionTappedFiveTimes_opensDev() {
        val results = List(6) { vm.onVersionTapped() }
        assertEquals(listOf(false, false, false, false, true, false), results)
    }

    @Test
    fun toggle_updatesSettings() = runTest {
        vm.set { it.copy(pauseVisits = true) }
        assertTrue(settings.current().pauseVisits)
        assertTrue(vm.state.value.settings.pauseVisits)
    }
}
