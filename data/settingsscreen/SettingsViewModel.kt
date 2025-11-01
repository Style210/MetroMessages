package com.metromessages.data.settingsscreen

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metromessages.ui.components.AccentColor
import com.metromessages.ui.components.MetroFont
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val currentFont: StateFlow<MetroFont> = settingsRepository.currentFont

    val currentAccentColor: StateFlow<Color> =
        settingsRepository.currentAccentColor.map { it.color }
            .stateIn(viewModelScope, SharingStarted.Eagerly, AccentColor.Blue.color)

    val isFacebookConnected: StateFlow<Boolean> = settingsRepository.isFacebookConnected
    val isWhatsAppConnected: StateFlow<Boolean> = settingsRepository.isWhatsAppConnected
    val isTelegramConnected: StateFlow<Boolean> = settingsRepository.isTelegramConnected

    val availableAccents = AccentColor.entries.map { it.color }

    fun setCurrentFont(font: MetroFont) {
        viewModelScope.launch {
            settingsRepository.setCurrentFont(font)
        }
    }

    fun setAccentColor(color: Color) {
        viewModelScope.launch {
            AccentColor.entries.find { it.color == color }?.let {
                settingsRepository.setAccentColor(it)
            }
        }
    }

    fun onFacebookToggle(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.onFacebookToggle(enabled)
        }
    }

    fun onWhatsAppToggle(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.onWhatsAppToggle(enabled)
        }
    }

    fun onTelegramToggle(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.onTelegramToggle(enabled)
        }
    }

    fun getFormattedAccentName(color: Color): String {
        return settingsRepository.getFormattedAccentName(color)
    }

    val isFacebookComingSoon = true
    val isWhatsAppComingSoon = true
    val isTelegramComingSoon = true
}