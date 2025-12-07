package com.metromessages.data.settingsscreen

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.metromessages.ui.components.MetroFont
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("metro_settings")

@Singleton
class SettingsPreferences @Inject constructor(private val context: Context) {

    private val FONT_KEY = stringPreferencesKey("font")
    private val ACCENT_KEY = intPreferencesKey("accent_color")
    private val ARCHIVE_SECURITY_KEY = booleanPreferencesKey("archive_security") // ONLY NEW ADDITION

    val fontFlow: Flow<MetroFont>
        get() = context.dataStore.data.map { preferences ->
            val fontName = preferences[FONT_KEY] ?: MetroFont.Segoe.displayName
            MetroFont.entries.find { it.displayName == fontName } ?: MetroFont.Segoe
        }

    val accentColorFlow: Flow<Color>
        get() = context.dataStore.data.map { preferences ->
            val colorInt = preferences[ACCENT_KEY] ?: Color(0xFF00BFFF).toArgb()
            Color(colorInt)
        }

    // ONLY NEW ADDITION: Archive Security Flow
    val archiveSecurityFlow: Flow<Boolean>
        get() = context.dataStore.data.map { preferences ->
            preferences[ARCHIVE_SECURITY_KEY] ?: true // Default: enabled
        }

    suspend fun setFont(font: MetroFont) {
        context.dataStore.edit { preferences ->
            preferences[FONT_KEY] = font.displayName
        }
    }

    suspend fun setAccentColor(color: Color) {
        context.dataStore.edit { preferences ->
            preferences[ACCENT_KEY] = color.toArgb()
        }
    }

    // ONLY NEW ADDITION: Archive Security Setter
    suspend fun setArchiveSecurity(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ARCHIVE_SECURITY_KEY] = enabled
        }
    }
}

