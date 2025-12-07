// MetroMessagesViewModel.kt - FOSSIFY COMPLIANT WITH LOADING CONTROL
package com.metromessages.data.local.metromessagehub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MetroMessagesViewModel @Inject constructor(
    private val messagesRepository: MetroMessagesRepository
) : ViewModel() {

    // -------------------------
    // FOSSIFY COMPLIANT STATE - SINGLE SOURCE OF TRUTH
    // -------------------------

    // ✅ SINGLE conversations flow - UI handles all filtering
    private val _conversations = MutableStateFlow<List<MetroConversation>>(emptyList())
    val conversations: StateFlow<List<MetroConversation>> = _conversations.asStateFlow()

    private val _messages = MutableStateFlow<List<MetroMessage>>(emptyList())
    val messages: StateFlow<List<MetroMessage>> = _messages.asStateFlow()

    private val _currentThreadId = MutableStateFlow<Long?>(null)
    val currentThreadId: StateFlow<Long?> = _currentThreadId.asStateFlow()

    // -------------------------
    // UI STATE - PURE STATE ONLY
    // -------------------------

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _messageDraft = MutableStateFlow("")
    val messageDraft: StateFlow<String> = _messageDraft.asStateFlow()

    private val _mediaAttachments = MutableStateFlow<List<android.net.Uri>>(emptyList())
    val mediaAttachments: StateFlow<List<android.net.Uri>> = _mediaAttachments.asStateFlow()

    // 🎯 FOSSIFY STYLE: SMS Loading Control
    private var smsLoadingEnabled = false

    init {
        // ⚠️ EMPTY - Fossify doesn't auto-load anything on startup
        // All loading is controlled by enableSmsLoading()
        println("📱 ViewModel created - SMS loading DISABLED (waiting for default status)")
    }

    // -------------------------
    // LOADING CONTROL - FOSSIFY STYLE
    // -------------------------

    /**
     * Call this ONLY after app becomes default SMS app
     * Fossify style: No SMS data loading until user explicitly sets as default
     */
    fun enableSmsLoading() {
        println("✅ ViewModel: Enabling SMS loading (app is now default SMS)")
        smsLoadingEnabled = true
        loadAllConversations()
    }

    // -------------------------
    // DATA OPERATIONS ONLY - NO BUSINESS LOGIC
    // -------------------------

    fun loadAllConversations() {
        // 🎯 FOSSIFY STYLE: Only load if SMS loading is enabled
        if (!smsLoadingEnabled) {
            println("⚠️ ViewModel: SMS loading DISABLED - not default SMS app yet")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // ✅ PURE DATA - No filtering logic
                _conversations.value = messagesRepository.getConversations()
                println("✅ ViewModel: Loaded ${_conversations.value.size} conversations")
            } catch (e: Exception) {
                _error.value = "Failed to load conversations: ${e.message}"
                println("❌ ViewModel: Error loading conversations: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshAllConversations() {
        loadAllConversations()
    }

    fun selectConversation(conversationId: String) {
        viewModelScope.launch {
            val threadId = conversationId.toLongOrNull() ?: run {
                messagesRepository.findThreadId(conversationId)
            }
            threadId?.let { loadMessages(it) }
        }
    }

    fun loadMessages(threadId: Long) {
        // 🎯 FOSSIFY STYLE: Only load if SMS loading is enabled
        if (!smsLoadingEnabled) {
            println("⚠️ ViewModel: SMS loading DISABLED - cannot load messages")
            return
        }

        viewModelScope.launch {
            _currentThreadId.value = threadId
            _error.value = null
            try {
                // ✅ PURE DATA - No business logic
                _messages.value = messagesRepository.getMessages(threadId)
                println("✅ ViewModel: Loaded ${_messages.value.size} messages for thread $threadId")
            } catch (e: Exception) {
                _error.value = "Failed to load messages: ${e.message}"
                println("❌ ViewModel: Error loading messages: ${e.message}")
            }
        }
    }

    // -------------------------
    // MESSAGE OPERATIONS - PURE ACTIONS
    // -------------------------

    fun sendMessage(text: String) {
        // 🎯 FOSSIFY STYLE: Only send if SMS loading is enabled
        if (!smsLoadingEnabled) {
            println("⚠️ ViewModel: SMS loading DISABLED - cannot send message")
            _error.value = "App is not default SMS app"
            return
        }

        viewModelScope.launch {
            _error.value = null
            val currentThread = _currentThreadId.value
            if (currentThread != null) {
                val phoneNumber = messagesRepository.resolvePhoneNumberFromThread(currentThread)
                phoneNumber?.let { number ->
                    val success = messagesRepository.sendMessage(number, text)
                    if (success) {
                        loadMessages(currentThread)
                        loadAllConversations()
                        _messageDraft.value = ""
                    } else {
                        _error.value = "Failed to send message"
                    }
                } ?: run {
                    _error.value = "Could not resolve phone number for thread"
                }
            } else {
                _error.value = "No conversation selected"
            }
        }
    }

    fun sendMessage(phoneNumber: String, text: String) {
        // 🎯 FOSSIFY STYLE: Only send if SMS loading is enabled
        if (!smsLoadingEnabled) {
            println("⚠️ ViewModel: SMS loading DISABLED - cannot send message")
            _error.value = "App is not default SMS app"
            return
        }

        viewModelScope.launch {
            _error.value = null
            val success = messagesRepository.sendMessage(phoneNumber, text)
            if (success) {
                val threadId = messagesRepository.findThreadId(phoneNumber)
                threadId?.let { loadMessages(it) }
                loadAllConversations()
                _messageDraft.value = ""
            } else {
                _error.value = "Failed to send message"
            }
        }
    }

    fun sendMediaMessage(mediaPath: String, type: MessageType) {
        // 🎯 FOSSIFY STYLE: Only send if SMS loading is enabled
        if (!smsLoadingEnabled) {
            println("⚠️ ViewModel: SMS loading DISABLED - cannot send media")
            _error.value = "App is not default SMS app"
            return
        }

        viewModelScope.launch {
            _error.value = null
            val currentThread = _currentThreadId.value
            if (currentThread != null) {
                val phoneNumber = messagesRepository.resolvePhoneNumberFromThread(currentThread)
                phoneNumber?.let { number ->
                    val success = messagesRepository.sendMediaMessage(number, mediaPath, type)
                    if (success) {
                        loadMessages(currentThread)
                        loadAllConversations()
                    } else {
                        _error.value = "Failed to send media message"
                    }
                }
            } else {
                _error.value = "No conversation selected"
            }
        }
    }

    // -------------------------
    // DRAFT MANAGEMENT - PURE STATE
    // -------------------------

    fun updateDraftText(text: String) {
        _messageDraft.value = text
    }

    fun addMediaToDraft(mediaUris: List<android.net.Uri>) {
        _mediaAttachments.value += mediaUris
    }

    fun removeMediaFromDraft(uri: android.net.Uri) {
        _mediaAttachments.value = _mediaAttachments.value.filter { it != uri }
    }

    fun clearMediaDraft() {
        _mediaAttachments.value = emptyList()
    }

    // -------------------------
    // OTP OPERATIONS - PURE ACTIONS
    // -------------------------

    fun cleanupExpiredOTPMessages() {
        // 🎯 FOSSIFY STYLE: Only cleanup if SMS loading is enabled
        if (!smsLoadingEnabled) {
            println("⚠️ ViewModel: SMS loading DISABLED - cannot cleanup OTP")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = messagesRepository.cleanupExpiredOTPMessages()
                if (success) {
                    loadAllConversations()
                } else {
                    _error.value = "Failed to cleanup OTP messages"
                }
            } catch (e: Exception) {
                _error.value = "OTP cleanup failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // -------------------------
    // UTILITY METHODS - PURE DATA
    // -------------------------

    suspend fun resolveConversationContact(threadId: Long): ConversationContact {
        return messagesRepository.resolveConversationContact(threadId)
    }

    suspend fun findThreadId(phoneNumber: String): Long? {
        return messagesRepository.findThreadId(phoneNumber)
    }

    fun clearError() {
        _error.value = null
    }

    fun clearCurrentConversation() {
        _currentThreadId.value = null
        _messages.value = emptyList()
        _messageDraft.value = ""
        _mediaAttachments.value = emptyList()
    }

    // 🗑️ REMOVED: searchConversations() - UI handles search filtering
    // 🗑️ REMOVED: clearSearch() - UI handles search state
    // 🗑️ REMOVED: All business logic filtering methods
}