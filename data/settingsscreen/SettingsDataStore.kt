// File: SettingsDataStore.kt
package com.metromessages.data.settingsscreen

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore



// ✅ Correct way to define delegate at top-level
val Context.settingsDataStore: androidx.datastore.core.DataStore<Preferences> by preferencesDataStore(
    name = "user_settings"
)
