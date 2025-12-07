package com.metromessages.data.settingsscreen

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.metromessages.ui.components.AccentColor
import com.metromessages.ui.components.MetroFont
import com.metromessages.ui.components.toColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

class SettingsRepositoryImpl(
    private val context: Context
) : SettingsRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    override val currentAccentColor: StateFlow<AccentColor> =
        SettingsPreferences(context).accentColorFlow
            .map { color -> AccentColor.fromColor(color) ?: AccentColor.Blue }
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = AccentColor.Blue
            )

    override val currentFont: StateFlow<MetroFont> =
        SettingsPreferences(context).fontFlow
            .stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = MetroFont.Segoe
            )

    override val isFacebookConnected: StateFlow<Boolean> = MutableStateFlow(false)
    override val isWhatsAppConnected: StateFlow<Boolean> = MutableStateFlow(false)
    override val isTelegramConnected: StateFlow<Boolean> = MutableStateFlow(false)

    override suspend fun setCurrentFont(font: MetroFont) {
        SettingsPreferences(context).setFont(font)
    }

    override suspend fun setAccentColor(accent: AccentColor) {
        SettingsPreferences(context).setAccentColor(accent.toColor())
    }

    override suspend fun onFacebookToggle(enabled: Boolean) { /* no-op */ }
    override suspend fun onWhatsAppToggle(enabled: Boolean) { /* no-op */ }
    override suspend fun onTelegramToggle(enabled: Boolean) { /* no-op */ }

    override fun getFormattedAccentName(color: Color): String {
        return AccentColor.fromColor(color)?.displayName ?: "Blue"
    }
}
