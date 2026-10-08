// MetroMessagesViewModel.kt
// Same public surface the screens already use (conversations, messages, isLoading, error, loadMessages,
// sendMessage, draft helpers...) but it now stays current by itself: one debounced change signal from the
// repository re-runs the two loads. No per-feature observers, no gates scattered through every method.
package com.metromessages.data.local.metromessagehub

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class MetroMessagesViewModel @Inject constructor(
    private val repo: MetroMessagesRepository
) : ViewModel() {

    private val _conversations = MutableStateFlow<List<MetroConversation>>(emptyList())
    val conversations: StateFlow<List<MetroConversation>> = _conversations.asStateFlow()

    private val _messages = MutableStateFlow<List<MetroMessage>>(emptyList())
    val messages: StateFlow<List<MetroMessage>> = _messages.asStateFlow()

    private val _currentThreadId = MutableStateFlow<Long?>(null)
    val currentThreadId: StateFlow<Long?> = _currentThreadId.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _messageDraft = MutableStateFlow("")
    val messageDraft: StateFlow<String> = _messageDraft.asStateFlow()

    private val _mediaAttachments = MutableStateFlow<List<Uri>>(emptyList())
    val mediaAttachments: StateFlow<List<Uri>> = _mediaAttachments.asStateFlow()

    private var watcher: Job? = null

    // ------------------------------------------------------------ lifecycle

    /** Called by MainActivity once the app is the default SMS app. Safe to call repeatedly. */
    fun enableSmsLoading() = start()

    private fun start() {
        if (watcher?.isActive == true || !repo.isDefaultSmsApp()) return
        watcher = viewModelScope.launch {
            reloadConversations()
            // every provider change (incoming, sent, read, deleted) lands here, coalesced
            repo.changes().debounce(300).collect {
                reloadConversations()
                _currentThreadId.value?.let { reloadMessages(it) }
            }
        }
    }

    // ------------------------------------------------------------ conversations

    fun loadAllConversations() {
        start()
        viewModelScope.launch { reloadConversations() }
    }

    fun refreshAllConversations() {
        viewModelScope.launch {
            repo.clearCaches()
            reloadConversations()
        }
    }

    private suspend fun reloadConversations() {
        // spinner only for the very first load, never for background refreshes (it would blank the list)
        val first = _conversations.value.isEmpty()
        if (first) _isLoading.value = true
        try {
            _conversations.value = repo.getConversations()
            _error.value = null
        } catch (e: Exception) {
            _error.value = "Failed to load conversations: ${e.message}"
        } finally {
            if (first) _isLoading.value = false
        }
    }

    fun conversationFor(threadId: Long): MetroConversation? =
        _conversations.value.firstOrNull { it.threadId == threadId }

    fun setArchived(threadId: Long, archived: Boolean) {
        repo.setArchived(threadId, archived)
        viewModelScope.launch { reloadConversations() }
    }

    // ------------------------------------------------------------ one thread

    fun loadMessages(threadId: Long) {
        start()
        if (_currentThreadId.value != threadId) _messages.value = emptyList()
        _currentThreadId.value = threadId
        viewModelScope.launch {
            reloadMessages(threadId)
            repo.markThreadRead(threadId)   // the provider change this causes triggers one more refresh
        }
    }

    private suspend fun reloadMessages(threadId: Long) {
        try {
            val loaded = repo.getMessages(threadId)
            if (_currentThreadId.value == threadId) _messages.value = loaded   // drop stale results
        } catch (e: Exception) {
            _error.value = "Failed to load messages: ${e.message}"
        }
    }

    fun clearCurrentConversation() {
        _currentThreadId.value = null
        _messages.value = emptyList()
        _messageDraft.value = ""
        _mediaAttachments.value = emptyList()
    }

    // ------------------------------------------------------------ sending

    /** Send to the open thread. */
    fun sendMessage(text: String) {
        val threadId = _currentThreadId.value ?: run { _error.value = "No conversation selected"; return }
        val conv = conversationFor(threadId)
        val number = conv?.phoneNumber ?: _messages.value.firstOrNull { it.address.isNotBlank() }?.address
        when {
            conv?.isGroup == true -> _error.value = "Group messaging needs MMS, which isn't built yet"
            number == null -> _error.value = "Could not resolve a number for this thread"
            else -> send(number, text)
        }
    }

    /** Send to a number, opening (or creating) its thread. Used for new conversations. */
    fun sendMessage(phoneNumber: String, text: String) {
        viewModelScope.launch {
            if (!repo.sendSms(phoneNumber, text)) { _error.value = "Failed to send message"; return@launch }
            _messageDraft.value = ""
            repo.findThreadId(phoneNumber)?.let { loadMessages(it) }
        }
    }

    private fun send(number: String, text: String) {
        viewModelScope.launch {
            _error.value = null
            if (repo.sendSms(number, text)) _messageDraft.value = "" else _error.value = "Failed to send message"
            // no manual reload: the outbox insert fires the change signal
        }
    }

    /** MMS sending is not implemented; say so instead of pretending. */
    @Suppress("UNUSED_PARAMETER")
    fun sendMediaMessage(mediaPath: String, type: MessageType) {
        _error.value = "Sending attachments isn't built yet"
    }

    // ------------------------------------------------------------ drafts

    fun updateDraftText(text: String) { _messageDraft.value = text }
    fun addMediaToDraft(uris: List<Uri>) { _mediaAttachments.value += uris }
    fun removeMediaFromDraft(uri: Uri) { _mediaAttachments.value = _mediaAttachments.value.filter { it != uri } }
    fun clearMediaDraft() { _mediaAttachments.value = emptyList() }

    fun clearError() { _error.value = null }
}
