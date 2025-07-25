// File: SettingsViewModel.kt
package com.metromessages.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    // TODO: Inject SettingsRepository when persistent storage is ready
) : ViewModel() {

    private val _accentColorIndex: MutableStateFlow<Int> = MutableStateFlow(0)
    val accentColorIndex: StateFlow<Int> = _accentColorIndex

    fun setAccentColor(index: Int) {
        viewModelScope.launch {
            _accentColorIndex.value = index
            // TODO: Persist to repository or DataStore
        }
    }
}
