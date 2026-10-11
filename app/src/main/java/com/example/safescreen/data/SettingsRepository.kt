package com.example.safescreen.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.safescreen.ui.theme.PtyxisPalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val palette: Flow<PtyxisPalette> =
        dataStore.data.map { PtyxisPalette.fromKey(it[PALETTE_KEY]) }

    suspend fun setPalette(palette: PtyxisPalette) {
        dataStore.edit { it[PALETTE_KEY] = palette.key }
    }

    private companion object {
        val PALETTE_KEY = stringPreferencesKey("palette")
    }
}
