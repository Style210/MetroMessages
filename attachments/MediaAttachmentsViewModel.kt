// File: MediaAttachmentsViewModel.kt
package com.metromessages.attachments

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MediaAttachmentsViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _currentAlbumId = MutableStateFlow<Long?>(null)
    val currentAlbumId: StateFlow<Long?> = _currentAlbumId

    private val _selectedMedia = MutableStateFlow<Set<Long>>(emptySet())
    val selectedMedia: StateFlow<Set<Long>> = _selectedMedia

    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode

    val recentMedia: StateFlow<List<LocalMedia>> = mediaRepository.getRecentMedia()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<LocalAlbum>> = mediaRepository.getNonEmptyAlbums()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val albumMedia: StateFlow<List<LocalMedia>> = _currentAlbumId.flatMapLatest { albumId ->
        if (albumId != null) {
            mediaRepository.getMediaForAlbum(albumId)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun openAlbum(albumId: Long) {
        _currentAlbumId.value = albumId
        clearSelection()
    }

    fun goBackToAlbums() {
        _currentAlbumId.value = null
        clearSelection()
    }

    fun toggleMediaSelection(mediaId: Long) {
        val newSelection = _selectedMedia.value.toMutableSet()
        if (newSelection.contains(mediaId)) {
            newSelection.remove(mediaId)
        } else {
            newSelection.add(mediaId)
        }
        _selectedMedia.value = newSelection

        // Auto-enable multi-select mode if something is selected
        if (newSelection.isNotEmpty() && !_isMultiSelectMode.value) {
            _isMultiSelectMode.value = true
        } else if (newSelection.isEmpty()) {
            _isMultiSelectMode.value = false
        }
    }

    fun clearSelection() {
        _selectedMedia.value = emptySet()
        _isMultiSelectMode.value = false
    }

    // FIXED: Return Uri objects instead of Strings to preserve permissions
    fun getSelectedMediaUris(): List<Uri> {
        // Get all media from both recent and album media
        val allMedia = recentMedia.value + albumMedia.value

        // Find the selected media and return their URIs directly
        return _selectedMedia.value.mapNotNull { selectedId ->
            allMedia.find { it.id == selectedId }?.uri
        }
    }
}

