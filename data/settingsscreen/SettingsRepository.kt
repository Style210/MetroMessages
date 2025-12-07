// File: SettingsRepository.kt
package com.metromessages.data.settingsscreen

import com.metromessages.ui.components.MetroFont
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.graphics.Color
import com.metromessages.ui.components.AccentColor

interface SettingsRepository {
    val currentAccentColor: StateFlow<AccentColor>
    val currentFont: StateFlow<MetroFont>

    val isFacebookConnected: StateFlow<Boolean>
    val isWhatsAppConnected: StateFlow<Boolean>
    val isTelegramConnected: StateFlow<Boolean>

    suspend fun setCurrentFont(font: MetroFont)
    suspend fun setAccentColor(accent: AccentColor)

    suspend fun onFacebookToggle(enabled: Boolean)
    suspend fun onWhatsAppToggle(enabled: Boolean)
    suspend fun onTelegramToggle(enabled: Boolean)

    fun getFormattedAccentName(color: Color): String
}

