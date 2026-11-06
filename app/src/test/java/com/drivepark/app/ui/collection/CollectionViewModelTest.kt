package com.drivepark.app.ui.collection

import com.drivepark.app.domain.InMemoryParkRepository
import com.drivepark.app.ui.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class CollectionViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun marksCollected() = runTest {
        val repo = InMemoryParkRepository()
        repo.collectRegion("jeju")
        val vm = CollectionViewModel(repo, hasLocationPermission = { false })
        val s = vm.state.first { it.items.any { (_, got) -> got } }
        assertEquals(5, s.items.size)
        assertEquals(listOf("jeju"), s.items.filter { it.second }.map { it.first.id })
        assertFalse(s.locationGranted)
    }
}
