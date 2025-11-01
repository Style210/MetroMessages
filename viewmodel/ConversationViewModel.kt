package com.metromessages.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metromessages.data.local.peoplescreen.ContactEdits
import com.metromessages.data.repository.UnifiedContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val unifiedContactRepository: UnifiedContactRepository
) : ViewModel() {

    // ===== FLOW-BASED METHODS (for UI) =====
    fun getUnifiedContact(contactId: Long) = unifiedContactRepository.getUnifiedContact(contactId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeContacts = unifiedContactRepository.getActiveContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getContactsWithFacebook() = unifiedContactRepository.getContactsWithSms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getContactsWithSms() = unifiedContactRepository.getContactsWithSms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getUnifiedContacts() = unifiedContactRepository.getUnifiedContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ===== IMMEDIATE METHODS (for navigation/sync) =====
    suspend fun getUnifiedContactsImmediate(): List<com.metromessages.data.repository.UnifiedContact> {
        return unifiedContactRepository.getUnifiedContacts().first()
    }

    suspend fun getUnifiedContactImmediate(contactId: Long): com.metromessages.data.repository.UnifiedContact? {
        return unifiedContactRepository.getUnifiedContact(contactId).first()
    }

    suspend fun getContactByPhoneNumberImmediate(phoneNumber: String): com.metromessages.data.repository.UnifiedContact? {
        return unifiedContactRepository.getContactByPhoneNumber(phoneNumber).first()
    }

    // ===== WRITE OPERATIONS =====
    suspend fun createContactImmediate(contactEdits: ContactEdits): Long? {
        return unifiedContactRepository.createContact(contactEdits)
    }

    fun updateContact(contactId: Long, contactEdits: ContactEdits) = viewModelScope.launch {
        unifiedContactRepository.updateContact(contactId, contactEdits)
    }

    // ===== CONVERSATION METHODS - RESTORE ORIGINAL LOGIC =====
    fun getConversationWithData(conversationId: String, contactId: Long) =
        unifiedContactRepository.getUnifiedContact(contactId).map { contact ->
            ConversationData(
                contact = contact,
                conversationId = conversationId,
                existsInUnifiedSystem = contact?.smsConversationId == conversationId // ✅ ORIGINAL LOGIC
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    suspend fun getConversationWithDataImmediate(conversationId: String, contactId: Long): ConversationData {
        val contact = if (contactId != 0L) {
            unifiedContactRepository.getUnifiedContact(contactId).first()
        } else {
            null
        }
        return ConversationData(
            contact = contact,
            conversationId = conversationId,
            existsInUnifiedSystem = contact?.smsConversationId == conversationId // ✅ ORIGINAL LOGIC
        )
    }

    // ===== FAVORITE MANAGEMENT =====
    fun toggleFavorite(contactId: Long, starred: Boolean) = viewModelScope.launch {
        unifiedContactRepository.toggleFavorite(contactId, starred)
    }

    fun starContact(contactId: Long) = viewModelScope.launch {
        unifiedContactRepository.starContact(contactId)
    }

    fun unstarContact(contactId: Long) = viewModelScope.launch {
        unifiedContactRepository.unstarContact(contactId)
    }

    // ===== REFRESH OPERATIONS =====
    suspend fun refreshUnifiedData() {
        unifiedContactRepository.refreshData()
    }

    // ===== MESSAGES OPERATIONS =====
    fun getMessagesForConversation(conversationId: String) =
        unifiedContactRepository.getUnifiedContacts().map { contacts ->
            val contact = contacts.find { it.smsConversationId == conversationId }
            if (contact != null) {
                emptyList<com.metromessages.data.model.facebook.FacebookUiMessage>()
            } else {
                emptyList()
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ===== DATA CLASS =====
    data class ConversationData(
        val contact: com.metromessages.data.repository.UnifiedContact?,
        val conversationId: String,
        val existsInUnifiedSystem: Boolean
    )
}
