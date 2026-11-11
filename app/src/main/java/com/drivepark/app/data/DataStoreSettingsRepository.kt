package com.drivepark.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.drivepark.app.domain.ThemeMode
import com.drivepark.app.domain.StartupMode
import com.drivepark.app.domain.SplitPane
import androidx.datastore.preferences.preferencesDataStore
import com.drivepark.app.domain.AppSettings
import com.drivepark.app.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "settings")

class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {

    override fun observe(): Flow<AppSettings> = store.data.map { it.toSettings() }

    override suspend fun current(): AppSettings = observe().first()

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[ARRIVAL_NOTIFICATION] = next.arrivalNotification
            prefs[GREETING_VOICE] = next.greetingVoice
            prefs[GLEO_BRIEFING] = next.gleoBriefing
            prefs[PAUSE_VISITS] = next.pauseVisits
            prefs[THEME_MODE] = next.themeMode.name
            prefs[STARTUP_MODE] = next.startupMode.name
            prefs[SHOW_PARK_WHEN_IDLE] = next.showParkWhenIdle
            prefs[REST_REMINDER] = next.restReminder
            prefs[KEEPER_TUTORIAL_DONE] = next.keeperTutorialDone
            prefs[LAST_SPLIT_PANE] = next.lastSplitPane.name
            next.lastAutoNaviBoot?.let { prefs[LAST_AUTO_NAVI_BOOT] = it } ?: prefs.remove(LAST_AUTO_NAVI_BOOT)
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            arrivalNotification = this[ARRIVAL_NOTIFICATION] ?: d.arrivalNotification,
            greetingVoice = this[GREETING_VOICE] ?: d.greetingVoice,
            gleoBriefing = this[GLEO_BRIEFING] ?: d.gleoBriefing,
            pauseVisits = this[PAUSE_VISITS] ?: d.pauseVisits,
            themeMode = this[THEME_MODE]?.let { n -> ThemeMode.entries.firstOrNull { it.name == n } } ?: d.themeMode,
            startupMode = this[STARTUP_MODE]?.let { n -> StartupMode.entries.firstOrNull { it.name == n } } ?: d.startupMode,
            showParkWhenIdle = this[SHOW_PARK_WHEN_IDLE] ?: d.showParkWhenIdle,
            restReminder = this[REST_REMINDER] ?: d.restReminder,
            keeperTutorialDone = this[KEEPER_TUTORIAL_DONE] ?: d.keeperTutorialDone,
            lastSplitPane = this[LAST_SPLIT_PANE]?.let { n -> SplitPane.entries.firstOrNull { it.name == n } } ?: d.lastSplitPane,
            lastAutoNaviBoot = this[LAST_AUTO_NAVI_BOOT],
        )
    }

    companion object {
        private val ARRIVAL_NOTIFICATION = booleanPreferencesKey("arrival_notification")
        private val LAST_AUTO_NAVI_BOOT = longPreferencesKey("last_auto_navi_boot")
        private val GREETING_VOICE = booleanPreferencesKey("greeting_voice")
        private val GLEO_BRIEFING = booleanPreferencesKey("gleo_briefing")
        private val PAUSE_VISITS = booleanPreferencesKey("pause_visits")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val STARTUP_MODE = stringPreferencesKey("startup_mode")
        private val SHOW_PARK_WHEN_IDLE = booleanPreferencesKey("show_park_when_idle")
        private val REST_REMINDER = booleanPreferencesKey("rest_reminder")
        private val KEEPER_TUTORIAL_DONE = booleanPreferencesKey("keeper_tutorial_done")
        private val LAST_SPLIT_PANE = stringPreferencesKey("last_split_pane")

        fun create(context: Context) = DataStoreSettingsRepository(context.applicationContext.settingsStore)
    }
}
