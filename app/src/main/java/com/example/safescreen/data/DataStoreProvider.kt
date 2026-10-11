package com.example.safescreen.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** Single DataStore file shared by PIN state and app settings. */
val Context.safeScreenDataStore: DataStore<Preferences> by preferencesDataStore(name = "safescreen_prefs")
