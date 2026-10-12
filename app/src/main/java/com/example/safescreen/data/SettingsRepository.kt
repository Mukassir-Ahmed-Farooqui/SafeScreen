package com.example.safescreen.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.safescreen.ui.theme.PtyxisPalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val palette: Flow<PtyxisPalette> =
        dataStore.data.map { PtyxisPalette.fromKey(it[PALETTE_KEY]) }

    /** Parent's switch for "app blocked" and "almost out of time" alerts. On by default. */
    val notificationsEnabled: Flow<Boolean> =
        dataStore.data.map { it[NOTIFICATIONS_KEY] ?: true }

    suspend fun setPalette(palette: PtyxisPalette) {
        dataStore.edit { it[PALETTE_KEY] = palette.key }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFICATIONS_KEY] = enabled }
    }

    private companion object {
        val PALETTE_KEY = stringPreferencesKey("palette")
        val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications_enabled")
    }
}
