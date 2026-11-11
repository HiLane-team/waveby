package com.drivepark.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.drivepark.app.domain.AppSettings
import com.drivepark.app.domain.ThemeMode
import com.drivepark.app.domain.StartupMode
import com.drivepark.app.domain.SplitPane
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DataStoreSettingsRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun repoAt(file: File, scope: CoroutineScope) =
        DataStoreSettingsRepository(PreferenceDataStoreFactory.create(scope = scope) { file })

    @Test
    fun defaults_whenEmpty() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val repo = repoAt(File(tmp.root, "a.preferences_pb"), scope)
        assertEquals(AppSettings(true, true, true, false, ThemeMode.SYSTEM, StartupMode.TO_NAVI, true, SplitPane.RIGHT), repo.current())
        assertEquals(AppSettings(), repo.observe().first())
        scope.cancel()
    }

    @Test
    fun update_persists() = runBlocking {
        val file = File(tmp.root, "b.preferences_pb")
        val s1 = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        repoAt(file, s1).update {
            it.copy(pauseVisits = true, gleoBriefing = false, themeMode = ThemeMode.DARK, startupMode = StartupMode.STAY, showParkWhenIdle = false, lastSplitPane = SplitPane.MIDDLE, lastAutoNaviBoot = 1234L, keeperTutorialDone = true)
        }
        s1.cancel()
        val s2 = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val again = repoAt(file, s2).current()
        assertTrue(again.pauseVisits)
        assertEquals(false, again.gleoBriefing)
        assertEquals(ThemeMode.DARK, again.themeMode)
        assertEquals(StartupMode.STAY, again.startupMode)
        assertEquals(false, again.showParkWhenIdle)
        assertEquals(1234L, again.lastAutoNaviBoot)
        assertEquals(SplitPane.MIDDLE, again.lastSplitPane)
        assertTrue(again.keeperTutorialDone)
        s2.cancel()
    }
}
