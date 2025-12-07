// File: data/local/metropeoplehub/MetroPeopleHubViewModel.kt
package com.metromessages.data.local.metropeoplehub

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Complete ViewModel with Fossify-style backend
 * Full contact browsing AND management capabilities
 * Enhanced with consistent data flow using helper methods
 */
@HiltViewModel
class MetroPeopleHubViewModel @Inject constructor(
    private val contactsRepository: MetroContactsRepository
) : ViewModel() {

    // ✅ CONTACT BROWSING STATE (Existing)
    private val _contacts = MutableStateFlow<List<MetroContact>>(emptyList())
    val contacts: StateFlow<List<MetroContact>> = _contacts.asStateFlow()

    private val _starredContacts = MutableStateFlow<List<MetroContact>>(emptyList())
    val starredContacts: StateFlow<List<MetroContact>> = _starredContacts.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MetroContact>>(emptyList())
    val searchResults: StateFlow<List<MetroContact>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false) // ✅ CHANGED: Default to false
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingMessage = MutableStateFlow("")
    val loadingMessage: StateFlow<String> = _loadingMessage.asStateFlow()

    // 🆕 CONTACT EDITING STATE (New)
    private val _currentContact = MutableStateFlow<MetroContact?>(null)
    val currentContact: StateFlow<MetroContact?> = _currentContact.asStateFlow()

    private val _editState = MutableStateFlow<ContactEditState>(ContactEditState.Idle)
    val editState: StateFlow<ContactEditState> = _editState.asStateFlow()

    private val _operationResult = MutableStateFlow<ContactOperationResult?>(null)
    val operationResult: StateFlow<ContactOperationResult?> = _operationResult.asStateFlow()

    // ✅ LIVE TILE FUNCTIONALITY (Existing)
    private val _liveTileStates = mutableStateMapOf<Long, LiveTileState>()

    // ✅ ADDED: Track if contacts have been loaded
    private var contactsLoaded = false

    // ✅ REMOVED: init { loadContacts() } - Don't load automatically!
    // init {
    //     loadContacts() // ❌ REMOVE THIS!
    // }

    // ✅ EXISTING CONTACT BROWSING METHODS - UPDATED
    fun loadContacts() {
        // ✅ Don't load if already loading
        if (_isLoading.value) {
            println("⚠️ Contacts already loading, skipping duplicate call")
            return
        }

        // ✅ Don't load if already loaded (contacts will be cached)
        if (contactsLoaded && _contacts.value.isNotEmpty()) {
            println("⚠️ Contacts already loaded, skipping duplicate call")
            return
        }

        _isLoading.value = true
        _loadingMessage.value = "Loading contacts..."

        viewModelScope.launch(Dispatchers.IO) { // ✅ Run in IO thread
            try {
                println("📞 ViewModel: Loading contacts...")
                val allContacts = contactsRepository.getAllContacts()
                _contacts.value = allContacts
                _starredContacts.value = allContacts.filter { it.starred }

                // Initialize live tile states
                initializeLiveTileStates(allContacts)

                contactsLoaded = true
                println("✅ ViewModel: Loaded ${allContacts.size} contacts")

            } catch (e: Exception) {
                println("❌ ViewModel ERROR loading contacts: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
                _loadingMessage.value = ""
            }
        }
    }

    // ✅ ADDED: Load only starred contacts (for HomeScreen)
    fun loadStarredContactsOnly() {
        if (_isLoading.value) return

        _isLoading.value = true
        _loadingMessage.value = "Loading favorites..."

        viewModelScope.launch(Dispatchers.IO) {
            try {
                println("⭐ ViewModel: Loading starred contacts only...")
                val starredContacts = contactsRepository.getStarredContacts()
                _starredContacts.value = starredContacts

                // Don't load all contacts
                println("✅ ViewModel: Loaded ${starredContacts.size} starred contacts")

            } catch (e: Exception) {
                println("❌ ViewModel ERROR loading starred contacts: ${e.message}")
            } finally {
                _isLoading.value = false
                _loadingMessage.value = ""
            }
        }
    }

    // ✅ ADDED: Check if we need to load
    fun loadContactsIfNeeded() {
        if (!contactsLoaded && !_isLoading.value) {
            loadContacts()
        }
    }

    fun searchContacts(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                _searchResults.value = contactsRepository.searchContacts(query)
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            }
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }

    // 🆕 CONTACT MANAGEMENT METHODS - FIXED TO USE LONG
    fun loadContactForEditing(contactId: Long?) = viewModelScope.launch {
        _editState.value = ContactEditState.Loading
        _operationResult.value = null

        // ✅ ADDED: Null safety check
        if (contactId == null) {
            _editState.value = ContactEditState.Error("Invalid contact ID")
            _operationResult.value = ContactOperationResult.Error("Invalid contact ID")
            return@launch
        }

        try {
            println("🔍 Loading contact for editing: $contactId")
            val contact = contactsRepository.getContactById(contactId)
            _currentContact.value = contact

            if (contact != null) {
                _editState.value = ContactEditState.Ready
                println("✅ Contact loaded: ${contact.displayName}")
            } else {
                _editState.value = ContactEditState.Error("Contact not found")
                _operationResult.value = ContactOperationResult.Error("Contact not found")
                println("❌ Contact not found: $contactId")
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Failed to load contact"
            _editState.value = ContactEditState.Error(errorMsg)
            _operationResult.value = ContactOperationResult.Error(errorMsg)
            println("❌ Error loading contact: $errorMsg")
        }
    }

    // ✅ ACTUALLY USING: toContactData() method for conversion - FIXED TO USE LONG
    fun updateContact(contactId: Long?, contactEdits: ContactEdits) = viewModelScope.launch {
        _editState.value = ContactEditState.Saving
        _operationResult.value = null

        // ✅ ADDED: Null safety check
        if (contactId == null) {
            _editState.value = ContactEditState.Error("Invalid contact ID")
            _operationResult.value = ContactOperationResult.Error("Invalid contact ID")
            return@launch
        }

        try {
            println("💾 Updating contact $contactId: ${contactEdits.displayName}")

            // ✅ ACTUALLY USING: toContactData() helper method
            val contactData = contactEdits.toContactData()
            val success = contactsRepository.updateContact(contactId, contactData)

            if (success) {
                _editState.value = ContactEditState.Success
                _operationResult.value = ContactOperationResult.Success("Contact updated successfully")
                refreshContacts() // Reload the contact list
                // Also update current contact if it's the one being edited
                if (_currentContact.value?.id == contactId) {
                    _currentContact.value = contactsRepository.getContactById(contactId)
                }
                println("✅ Contact updated successfully")
            } else {
                val errorMsg = "Failed to update contact"
                _editState.value = ContactEditState.Error(errorMsg)
                _operationResult.value = ContactOperationResult.Error(errorMsg)
                println("❌ Failed to update contact")
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Update failed"
            _editState.value = ContactEditState.Error(errorMsg)
            _operationResult.value = ContactOperationResult.Error(errorMsg)
            println("❌ Error updating contact: $errorMsg")
        }
    }

    fun createContact(contactData: ContactData) = viewModelScope.launch {
        _editState.value = ContactEditState.Saving
        _operationResult.value = null

        try {
            println("💾 Creating new contact: ${contactData.displayName}")
            val contactId = contactsRepository.createContact(contactData)

            if (contactId != -1L) {
                _editState.value = ContactEditState.Success
                _operationResult.value = ContactOperationResult.Success("Contact created successfully")
                refreshContacts() // Reload the contact list
                println("✅ Contact created with ID: $contactId")
            } else {
                val errorMsg = "Failed to create contact"
                _editState.value = ContactEditState.Error(errorMsg)
                _operationResult.value = ContactOperationResult.Error(errorMsg)
                println("❌ Failed to create contact")
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Creation failed"
            _editState.value = ContactEditState.Error(errorMsg)
            _operationResult.value = ContactOperationResult.Error(errorMsg)
            println("❌ Error creating contact: $errorMsg")
        }
    }

    fun deleteContact(contactId: Long) = viewModelScope.launch {
        _operationResult.value = null

        try {
            println("🗑️ Deleting contact: $contactId")
            val success = contactsRepository.deleteContact(contactId)

            if (success) {
                _operationResult.value = ContactOperationResult.Success("Contact deleted successfully")
                refreshContacts()
                // Clear current contact if it's the one being deleted
                if (_currentContact.value?.id == contactId) {
                    _currentContact.value = null
                }
                println("✅ Contact deleted successfully")
            } else {
                _operationResult.value = ContactOperationResult.Error("Failed to delete contact")
                println("❌ Failed to delete contact")
            }
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Deletion failed"
            _operationResult.value = ContactOperationResult.Error(errorMsg)
            println("❌ Error deleting contact: $errorMsg")
        }
    }

    fun toggleFavorite(contactId: Long, starred: Boolean) = viewModelScope.launch {
        try {
            println("⭐ Toggling favorite for contact $contactId: $starred")
            val success = contactsRepository.toggleFavorite(contactId, starred)

            if (success) {
                refreshContacts()
                // Update current contact if it's the one being modified
                if (_currentContact.value?.id == contactId) {
                    _currentContact.value = _currentContact.value?.copy(starred = starred)
                }
                println("✅ Favorite toggled successfully")
            } else {
                println("❌ Failed to toggle favorite")
            }
        } catch (e: Exception) {
            println("❌ Error toggling favorite: ${e.message}")
        }
    }

    // 🆕 INDIVIDUAL FIELD OPERATIONS (Future hooks)
    fun addPhoneNumber(contactId: Long, phone: MetroPhone) = viewModelScope.launch {
        try {
            val success = contactsRepository.addPhoneNumber(contactId, phone)
            if (success) {
                refreshContacts()
                // Update current contact
                if (_currentContact.value?.id == contactId) {
                    _currentContact.value = contactsRepository.getContactById(contactId)
                }
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun updatePhoneNumber(contactId: Long, phoneId: Long, newNumber: String) = viewModelScope.launch {
        try {
            val success = contactsRepository.updatePhoneNumber(contactId, phoneId, newNumber)
            if (success) {
                refreshContacts()
                if (_currentContact.value?.id == contactId) {
                    _currentContact.value = contactsRepository.getContactById(contactId)
                }
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun deletePhoneNumber(contactId: Long, phoneId: Long) = viewModelScope.launch {
        try {
            val success = contactsRepository.deletePhoneNumber(contactId, phoneId)
            if (success) {
                refreshContacts()
                if (_currentContact.value?.id == contactId) {
                    _currentContact.value = contactsRepository.getContactById(contactId)
                }
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun addEmail(contactId: Long, email: MetroEmail) = viewModelScope.launch {
        try {
            val success = contactsRepository.addEmail(contactId, email)
            if (success) {
                refreshContacts()
                if (_currentContact.value?.id == contactId) {
                    _currentContact.value = contactsRepository.getContactById(contactId)
                }
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun updateEmail(contactId: Long, emailId: Long, newEmail: String) = viewModelScope.launch {
        try {
            val success = contactsRepository.updateEmail(contactId, emailId, newEmail)
            if (success) {
                refreshContacts()
                if (_currentContact.value?.id == contactId) {
                    _currentContact.value = contactsRepository.getContactById(contactId)
                }
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    fun deleteEmail(contactId: Long, emailId: Long) = viewModelScope.launch {
        try {
            val success = contactsRepository.deleteEmail(contactId, emailId)
            if (success) {
                refreshContacts()
                if (_currentContact.value?.id == contactId) {
                    _currentContact.value = contactsRepository.getContactById(contactId)
                }
            }
        } catch (e: Exception) {
            // Handle error
        }
    }

    // 🆕 CONTACT CREATION FROM PHONE NUMBER (Useful for SMS app)
    fun createContactFromPhoneNumber(displayName: String, phoneNumber: String) = viewModelScope.launch {
        _editState.value = ContactEditState.Saving

        try {
            val contactData = ContactData(
                displayName = displayName,
                phones = listOf(
                    MetroPhone(
                        number = phoneNumber,
                        type = android.provider.ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
                        label = "Mobile"
                    )
                )
            )

            val contactId = contactsRepository.createContact(contactData)
            if (contactId != -1L) {
                _editState.value = ContactEditState.Success
                _operationResult.value = ContactOperationResult.Success("Contact created successfully")
                refreshContacts()
            } else {
                _editState.value = ContactEditState.Error("Failed to create contact")
                _operationResult.value = ContactOperationResult.Error("Failed to create contact")
            }
        } catch (e: Exception) {
            _editState.value = ContactEditState.Error(e.message ?: "Creation failed")
            _operationResult.value = ContactOperationResult.Error(e.message ?: "Creation failed")
        }
    }

    // 🆕 CLEAR EDITING STATE
    fun clearEditState() {
        _editState.value = ContactEditState.Idle
        _operationResult.value = null
        _currentContact.value = null
    }

    fun clearOperationResult() {
        _operationResult.value = null
    }

    // ✅ EXISTING LIVE TILE METHODS
    private fun initializeLiveTileStates(contacts: List<MetroContact>) {
        contacts.forEach { contact ->
            _liveTileStates[contact.id] = LiveTileState(
                hasUnreadMessages = false,
                hasMissedCalls = false,
                displayMessage = "No messages yet",
                lastActivityType = ActivityType.NONE,
                lastActivityTime = System.currentTimeMillis(),
                conversationId = "sms_${contact.id}"
            )
        }
    }

    fun getLiveTileState(contactId: Long): LiveTileState? {
        return _liveTileStates[contactId]
    }

    fun markContactAsRead(contactId: Long) {
        val currentState = _liveTileStates[contactId]
        if (currentState != null) {
            _liveTileStates[contactId] = currentState.copy(
                hasUnreadMessages = false
            )
        }
    }

    fun updateRecentMessage(contactId: Long, message: String) {
        val currentState = _liveTileStates[contactId]
        if (currentState != null) {
            _liveTileStates[contactId] = currentState.copy(
                hasUnreadMessages = true,
                displayMessage = message,
                lastActivityType = ActivityType.SMS_MESSAGE,
                lastActivityTime = System.currentTimeMillis()
            )
        }
    }

    // ✅ EXISTING STAR MANAGEMENT (now uses repository)
    fun toggleStar(contactId: Long) {
        viewModelScope.launch {
            val contact = _contacts.value.find { it.id == contactId }
            contact?.let {
                val newStarredState = !it.starred
                toggleFavorite(contactId, newStarredState)
            }
        }
    }

    private fun refreshContacts() {
        // Invalidate the loaded flag so contacts will load fresh
        contactsLoaded = false
        loadContacts()
    }

    // 🆕 STATE CLASSES
    sealed class ContactEditState {
        object Idle : ContactEditState()
        object Loading : ContactEditState()
        object Ready : ContactEditState()
        object Saving : ContactEditState()
        object Success : ContactEditState()
        data class Error(val message: String) : ContactEditState()
    }

    sealed class ContactOperationResult {
        data class Success(val message: String) : ContactOperationResult()
        data class Error(val message: String) : ContactOperationResult()
    }

    // ✅ PRESERVE YOUR EXISTING DATA STRUCTURES
    data class LiveTileState(
        val hasUnreadMessages: Boolean,
        val hasMissedCalls: Boolean,
        val displayMessage: String,
        val lastActivityType: ActivityType,
        val lastActivityTime: Long,
        val conversationId: String
    )

    enum class ActivityType {
        NONE, SMS_MESSAGE, MISSED_CALL
    }
}
