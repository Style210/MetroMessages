// File: data/local/peoplescreen/PeopleScreenViewModel.kt
package com.metromessages.data.local.peoplescreen

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metromessages.data.repository.ActivityType
import com.metromessages.data.repository.NavigationTarget
import com.metromessages.data.repository.UnifiedContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PeopleScreenViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val unifiedContactRepository: UnifiedContactRepository,
    private val deviceContactsImporter: DeviceContactsImporter
) : ViewModel() {

    private val _isLoading = mutableStateOf(true)
    val isLoading = _isLoading

    private val _loadingMessage = mutableStateOf("Loading contacts...")
    val loadingMessage = _loadingMessage

    // Import state management
    private val _importState = mutableStateOf<ImportState>(ImportState.Idle)
    val importState = _importState

    private val _shouldShowImportDialog = mutableStateOf(false)
    val shouldShowImportDialog = _shouldShowImportDialog

    sealed class ImportState {
        object Idle : ImportState()
        object Checking : ImportState()
        object Importing : ImportState()
        data class Progress(val current: Int, val total: Int) : ImportState()
        data class Completed(val count: Int) : ImportState()
        data class Error(val message: String) : ImportState()
    }

    // Unified contact data for SMS only
    val unifiedContacts = unifiedContactRepository.getUnifiedContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeContacts = unifiedContactRepository.getActiveContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Convert unified contacts to existing PersonWithDetails for compatibility
    val people = unifiedContacts.map { contacts ->
        contacts.map { unifiedContact ->
            // Try to get the original contact data to preserve timestamps
            val originalContact = getOriginalContactData(unifiedContact.id)

            PersonWithDetails(
                person = PeopleEntity(
                    id = unifiedContact.id,
                    displayName = unifiedContact.displayName,
                    photoUri = unifiedContact.photoUri,
                    starred = unifiedContact.starred,
                    createdAt = originalContact?.person?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = originalContact?.person?.updatedAt ?: unifiedContact.lastActivity
                ),
                phones = if (unifiedContact.phoneNumber != null) {
                    listOf(PeoplePhone(
                        id = unifiedContact.id * 10,
                        peopleId = unifiedContact.id,
                        number = unifiedContact.phoneNumber,
                        type = 1,
                        label = null
                    ))
                } else {
                    emptyList()
                },
                emails = emptyList()
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val starredPeople = unifiedContacts.map { contacts ->
        contacts.filter { it.starred }.map { unifiedContact ->
            // Try to get the original contact data to preserve timestamps
            val originalContact = getOriginalContactData(unifiedContact.id)

            PersonWithDetails(
                person = PeopleEntity(
                    id = unifiedContact.id,
                    displayName = unifiedContact.displayName,
                    photoUri = unifiedContact.photoUri,
                    starred = true,
                    createdAt = originalContact?.person?.createdAt ?: System.currentTimeMillis(),
                    updatedAt = originalContact?.person?.updatedAt ?: unifiedContact.lastActivity
                ),
                phones = if (unifiedContact.phoneNumber != null) {
                    listOf(PeoplePhone(
                        id = unifiedContact.id * 10,
                        peopleId = unifiedContact.id,
                        number = unifiedContact.phoneNumber,
                        type = 1,
                        label = null
                    ))
                } else {
                    emptyList()
                },
                emails = emptyList()
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live Tile state management
    data class LiveTileState(
        val hasUnreadMessages: Boolean,
        val hasMissedCalls: Boolean,
        val displayMessage: String,
        val lastActivityType: ActivityType,
        val lastActivityTime: Long,
        val conversationId: String?
    )

    private val _liveTileStates = mutableStateMapOf<Long, LiveTileState>()

    private val _searchResults = mutableStateOf<List<PersonWithDetails>>(emptyList())
    val searchResults = _searchResults

    private val _isSearching = mutableStateOf(false)
    val isSearching = _isSearching

    private val _databaseStats = mutableStateOf<DatabaseStats?>(null)
    val databaseStats = _databaseStats

    init {
        viewModelScope.launch {
            initializeDatabase()
            initializeLiveTileStates()
            preloadConversationData()
            setupPeriodicRefresh()
        }
    }

    // ===== CONTACT EDITING METHODS =====

    /**
     * Update contact information from editor
     */
    fun updateContact(contactId: Long, contactEdits: ContactEdits) = viewModelScope.launch {
        try {
            println("🔍 DEBUG - Contact Update Flow Started:")
            println("   Contact ID: $contactId")
            println("   Name: '${contactEdits.displayName}'")
            println("   Photo: ${contactEdits.photoUri}")
            println("   Phones: ${contactEdits.phones.size}")
            contactEdits.phones.forEachIndexed { i, phone ->
                println("     Phone $i: ${phone.number} (type: ${phone.type})")
            }

            // 1. Update in local Room database
            updateContactInDatabase(contactId, contactEdits)

            // 2. Update in Android Contacts Provider (if we have original contact ID)
            updateContactInAndroid(contactId, contactEdits)

            // 3. Refresh unified data to reflect changes
            unifiedContactRepository.refreshData()

            // 4. Update Live Tile state if name changed
            if (contactEdits.displayName.isNotBlank()) {
                updateLiveTileName(contactId, contactEdits.displayName)
            }

            // 5. Force refresh the UI
            refreshUnifiedData()

            println("✅ Contact $contactId updated successfully")

        } catch (e: Exception) {
            println("❌ Failed to update contact $contactId: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Update contact in local Room database
     */
    private suspend fun updateContactInDatabase(contactId: Long, contactEdits: ContactEdits) {
        try {
            println("🔄 Updating contact $contactId in database...")

            // Get the unified contact to find the correct local contact ID
            val unifiedContact = unifiedContacts.value.find { it.id == contactId }
            if (unifiedContact == null) {
                println("❌ Unified contact not found for ID: $contactId")
                return
            }

            // Try to get existing contact from local database
            val existingContact = peopleRepository.getPersonById(contactId)

            if (existingContact != null) {
                // Update existing contact
                println("✅ Found existing contact: ${existingContact.person.displayName}")

                // Create updated PeopleEntity
                val updatedPerson = existingContact.person.copy(
                    displayName = contactEdits.displayName,
                    photoUri = contactEdits.photoUri ?: existingContact.person.photoUri,
                    updatedAt = System.currentTimeMillis()
                )

                // Convert edits to proper entities - filter out empty values
                val phones = contactEdits.phones
                    .filter { it.number.isNotBlank() }
                    .map { phoneEdit ->
                        PeoplePhone(
                            id = 0, // Auto-generate
                            peopleId = contactId,
                            number = phoneEdit.number,
                            type = phoneEdit.type,
                            label = phoneEdit.label
                        )
                    }

                val emails = contactEdits.emails
                    .filter { it.address.isNotBlank() }
                    .map { emailEdit ->
                        PeopleEmail(
                            id = 0, // Auto-generate
                            peopleId = contactId,
                            address = emailEdit.address,
                            type = emailEdit.type,
                            label = emailEdit.label
                        )
                    }

                // ✅ FIX: Use the CORRECT repository method
                peopleRepository.updatePerson(updatedPerson, phones, emails)
                println("✅ Successfully updated contact $contactId in local database")

            } else {
                // Contact doesn't exist in local database yet - create it
                println("⚠️ Contact not found in local DB, creating new entry...")

                val newPerson = PeopleEntity(
                    id = contactId,
                    displayName = contactEdits.displayName,
                    photoUri = contactEdits.photoUri,
                    starred = false, // Default to not starred for new contacts
                    contactId = null, // No Android contact ID for new contacts
                    lookupKey = generateLookupKey(contactEdits.displayName),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val phones = contactEdits.phones
                    .filter { it.number.isNotBlank() }
                    .map { phoneEdit ->
                        PeoplePhone(
                            id = 0,
                            peopleId = contactId,
                            number = phoneEdit.number,
                            type = phoneEdit.type,
                            label = phoneEdit.label
                        )
                    }

                val emails = contactEdits.emails
                    .filter { it.address.isNotBlank() }
                    .map { emailEdit ->
                        PeopleEmail(
                            id = 0,
                            peopleId = contactId,
                            address = emailEdit.address,
                            type = emailEdit.type,
                            label = emailEdit.label
                        )
                    }

                // ✅ FIX: Use insertPerson method
                peopleRepository.insertPerson(newPerson, phones, emails)
                println("✅ Successfully created new contact $contactId in local database")
            }

        } catch (e: Exception) {
            println("❌ Error updating contact $contactId in database: ${e.message}")
            e.printStackTrace()
            throw e // Re-throw to let caller handle it
        }
    }

    /**
     * Update contact in Android Contacts Provider
     */
    private suspend fun updateContactInAndroid(contactId: Long, contactEdits: ContactEdits) {
        // For now, we'll just log this - you can implement the actual Android Contacts update later
        println("📱 Would update Android contact for ID $contactId with: $contactEdits")
        // TODO: Implement actual Android Contacts update using ContentResolver
        // This requires WRITE_CONTACTS permission and ContentProviderOperations
    }

    /**
     * Update Live Tile display name when contact name changes
     */
    private fun updateLiveTileName(contactId: Long, newName: String) {
        val currentState = _liveTileStates[contactId]
        if (currentState != null) {
            _liveTileStates[contactId] = currentState.copy(
                displayMessage = currentState.displayMessage.replace(
                    Regex("from .*"), "from $newName"
                )
            )
        }
    }

    /**
     * Get detailed contact information for editing
     */
    suspend fun getDetailedContact(contactId: Long): PersonWithDetails? {
        return peopleRepository.getPersonById(contactId)
    }

    // Add helper method to get original contact data
    private suspend fun getOriginalContactData(contactId: Long): PersonWithDetails? {
        return try {
            peopleRepository.getPersonById(contactId)
        } catch (e: Exception) {
            null
        }
    }

    // Helper method for lookup key generation
    private fun generateLookupKey(displayName: String): String {
        return "${displayName.replace(" ", "_").lowercase()}_${UUID.randomUUID().toString().take(8)}"
    }

    // ===== DEVICE CONTACTS IMPORT METHODS =====

    private suspend fun initializeDatabase() {
        _loadingMessage.value = "Checking for device contacts..."

        // Check if we should import from device
        if (shouldImportFromDevice()) {
            _shouldShowImportDialog.value = true
            return
        }

        _loadingMessage.value = "Verifying data consistency..."
        verifyMockDataConsistency()

        _loadingMessage.value = "Checking database..."
        if (peopleRepository.needsMockData()) {
            _loadingMessage.value = "Loading mock data..."
            addMockData()
        }

        _databaseStats.value = peopleRepository.getDatabaseStats()
        _isLoading.value = false
        _loadingMessage.value = "Ready"
        logDatabaseHealth()
    }

    private suspend fun shouldImportFromDevice(): Boolean {
        return deviceContactsImporter.canImportDeviceContacts() &&
                peopleRepository.needsMockData()
    }

    fun startDeviceContactsImport() {
        viewModelScope.launch {
            _importState.value = ImportState.Importing
            _isLoading.value = true
            _loadingMessage.value = "Importing your contacts..."

            deviceContactsImporter.importDeviceContacts().collect { progress ->
                when (progress) {
                    is ImportProgress.Started -> {
                        _importState.value = ImportState.Importing
                    }
                    is ImportProgress.Progress -> {
                        _importState.value = ImportState.Progress(progress.current, progress.total)
                        _loadingMessage.value = "Importing contacts... ${progress.current}/${progress.total}"
                    }
                    is ImportProgress.Completed -> {
                        _importState.value = ImportState.Completed(progress.totalImported)
                        _loadingMessage.value = "Import complete!"

                        delay(500) // Let UI update
                        onImportCompleted()
                    }
                    is ImportProgress.Error -> {
                        _importState.value = ImportState.Error(progress.message)
                        _loadingMessage.value = "Import failed"
                        onImportFailed()
                    }
                }
            }
        }
    }

    fun skipDeviceImport() {
        viewModelScope.launch {
            _shouldShowImportDialog.value = false
            _loadingMessage.value = "Loading app with sample data..."

            verifyMockDataConsistency()
            if (peopleRepository.needsMockData()) {
                addMockData()
            }

            _databaseStats.value = peopleRepository.getDatabaseStats()
            _isLoading.value = false
            _loadingMessage.value = "Ready"
        }
    }

    private fun onImportCompleted() {
        viewModelScope.launch {
            _databaseStats.value = peopleRepository.getDatabaseStats()
            initializeLiveTileStates()
            preloadConversationData()

            _isLoading.value = false
            _shouldShowImportDialog.value = false

            println("✅ Device contacts import completed successfully!")
        }
    }

    private fun onImportFailed() {
        viewModelScope.launch {
            delay(2000)
            skipDeviceImport()
        }
    }

    // ===== SIMPLIFIED FUNCTIONALITY =====

    private fun setupPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(10000)
                if (hasActiveObservers()) {
                    println("🔄 Periodic refresh of PeopleScreen data")
                    initializeLiveTileStates()
                }
            }
        }
    }

    private fun hasActiveObservers(): Boolean {
        return true
    }

    private suspend fun preloadConversationData() {
        println("🔍 Preloading conversation data for People Hub...")
        try {
            val contacts = unifiedContactRepository.getUnifiedContacts().first()
            println("✅ Preloaded ${contacts.size} unified contacts with conversations")

            val contactsWithConversations = contacts.count { it.smsConversationId != null }
            println("📊 Contacts with SMS conversations: $contactsWithConversations/${contacts.size}")

        } catch (e: Exception) {
            println("❌ Failed to preload conversation data: ${e.message}")
        }
    }

    private suspend fun initializeLiveTileStates() {
        val contacts = unifiedContacts.first()

        println("🔍 Initializing Live Tile States for ${contacts.size} contacts:")
        contacts.forEach { contact ->
            _liveTileStates[contact.id] = LiveTileState(
                hasUnreadMessages = contact.hasUnreadMessages,
                hasMissedCalls = contact.hasMissedCalls,
                displayMessage = contact.lastMessage ?: "No messages yet",
                lastActivityType = contact.lastActivityType,
                lastActivityTime = contact.lastActivity,
                conversationId = contact.smsConversationId ?: "sms_${contact.id}"
            )

            println("   ✅ ${contact.displayName}: Unread=${contact.hasUnreadMessages}, SMS ID=${contact.smsConversationId}")
        }
        println("✅ Initialized LiveTile states for ${contacts.size} contacts")

        val missingStates = contacts.filter { _liveTileStates[it.id] == null }
        if (missingStates.isNotEmpty()) {
            println("⚠️  Missing LiveTile states for: ${missingStates.map { it.displayName }}")
            missingStates.forEach { contact ->
                _liveTileStates[contact.id] = LiveTileState(
                    hasUnreadMessages = contact.hasUnreadMessages,
                    hasMissedCalls = contact.hasMissedCalls,
                    displayMessage = contact.lastMessage ?: "No messages yet",
                    lastActivityType = contact.lastActivityType,
                    lastActivityTime = contact.lastActivity,
                    conversationId = contact.smsConversationId ?: "sms_${contact.id}"
                )
            }
        }
    }

    // Live Tile Management
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
        println("📭 Marked contact $contactId as read")
    }

    fun markContactAsUnread(contactId: Long, message: String? = null, activityType: ActivityType = ActivityType.SMS_MESSAGE) {
        val currentState = _liveTileStates[contactId]
        val unifiedContact = unifiedContacts.value.find { it.id == contactId }

        if (currentState != null) {
            _liveTileStates[contactId] = currentState.copy(
                hasUnreadMessages = true,
                displayMessage = message ?: currentState.displayMessage,
                lastActivityType = activityType,
                lastActivityTime = System.currentTimeMillis()
            )
        } else {
            _liveTileStates[contactId] = LiveTileState(
                hasUnreadMessages = true,
                hasMissedCalls = false,
                displayMessage = message ?: "New message",
                lastActivityType = activityType,
                lastActivityTime = System.currentTimeMillis(),
                conversationId = unifiedContact?.smsConversationId ?: "sms_$contactId"
            )
        }
        println("📬 Marked contact $contactId as unread: $message")
    }

    fun updateRecentMessage(contactId: Long, message: String) {
        markContactAsUnread(contactId, message, ActivityType.SMS_MESSAGE)
    }

    fun getHasNewMessagesMap(): Map<Long, Boolean> {
        return _liveTileStates.mapValues { it.value.hasUnreadMessages }
    }

    fun getRecentMessage(contactId: Long): String {
        return _liveTileStates[contactId]?.displayMessage ?: "No recent messages"
    }

    fun clearAllNotifications() {
        _liveTileStates.keys.forEach { contactId ->
            val currentState = _liveTileStates[contactId]
            if (currentState != null) {
                _liveTileStates[contactId] = currentState.copy(
                    hasUnreadMessages = false
                )
            }
        }
        println("📭 Cleared all notifications")
    }

    // Navigation
    fun getNavigationTarget(contactId: Long): NavigationTarget? {
        val unifiedContact = unifiedContacts.value.find { it.id == contactId }
        return unifiedContact?.getNavigationTarget()
    }

    fun hasSmsActivity(contactId: Long): Boolean {
        val unifiedContact = unifiedContacts.value.find { it.id == contactId }
        return unifiedContact?.hasSmsActivity() ?: false
    }

    fun refreshUnifiedData() {
        viewModelScope.launch {
            println("🔄 PeopleScreenViewModel: Manually refreshing unified data")
            initializeLiveTileStates()
        }
    }

    // Search functions
    fun searchPeople(query: String) = viewModelScope.launch {
        if (query.isBlank()) {
            _isSearching.value = false
            _searchResults.value = emptyList()
            return@launch
        }

        _isSearching.value = true
        try {
            val results = peopleRepository.searchPeople(query)
            _searchResults.value = results
        } catch (e: Exception) {
            println("Search error: ${e.message}")
            _searchResults.value = emptyList()
        } finally {
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        _isSearching.value = false
        _searchResults.value = emptyList()
    }

    // Star management
    fun toggleStar(personId: Long, starred: Boolean) = viewModelScope.launch {
        peopleRepository.toggleStar(personId, starred)
        _databaseStats.value = peopleRepository.getDatabaseStats()
    }

    fun starPerson(personId: Long) = viewModelScope.launch {
        peopleRepository.starPerson(personId)
        _databaseStats.value = peopleRepository.getDatabaseStats()
    }

    fun unstarPerson(personId: Long) = viewModelScope.launch {
        peopleRepository.unstarPerson(personId)
        _databaseStats.value = peopleRepository.getDatabaseStats()
    }

    // Contact management
    fun deletePerson(person: PersonWithDetails) = viewModelScope.launch {
        peopleRepository.deletePerson(person.person)
        _liveTileStates.remove(person.person.id)
        _databaseStats.value = peopleRepository.getDatabaseStats()
    }

    fun deletePeople(personIds: List<Long>) = viewModelScope.launch {
        peopleRepository.deletePeople(personIds)
        personIds.forEach { id ->
            _liveTileStates.remove(id)
        }
        _databaseStats.value = peopleRepository.getDatabaseStats()
    }

    fun updateDisplayName(personId: Long, name: String) = viewModelScope.launch {
        peopleRepository.updateDisplayName(personId, name)
    }

    fun updatePhotoUri(personId: Long, photoUri: String?) = viewModelScope.launch {
        peopleRepository.updatePhotoUri(personId, photoUri)
    }

    // Validation and info
    suspend fun validatePerson(personId: Long): Boolean {
        return peopleRepository.validatePersonIntegrity(personId)
    }

    suspend fun getPersonContactInfo(personId: Long): ContactInfo {
        return peopleRepository.getPersonContactInfo(personId)
    }

    // Database operations
    fun refreshDatabaseStats() = viewModelScope.launch {
        _databaseStats.value = peopleRepository.getDatabaseStats()
    }

    fun reloadMockData() = viewModelScope.launch {
        _isLoading.value = true
        _loadingMessage.value = "Reloading mock data..."

        val mockPeople = com.metromessages.mockdata.MockDataGenerator.generateMockPeople(30)
        peopleRepository.resetToMockData(mockPeople)

        _liveTileStates.clear()
        initializeLiveTileStates()

        _databaseStats.value = peopleRepository.getDatabaseStats()
        _isLoading.value = false
        _loadingMessage.value = "Reload complete"
    }

    fun clearAllData() = viewModelScope.launch {
        _isLoading.value = true
        _loadingMessage.value = "Clearing all data..."

        peopleRepository.clearAllData()
        _liveTileStates.clear()

        _databaseStats.value = peopleRepository.getDatabaseStats()
        _isLoading.value = false
        _loadingMessage.value = "Data cleared"
    }

    // Utility functions
    fun getCurrentDisplayedPeople(): List<PersonWithDetails> {
        return if (_isSearching.value) {
            _searchResults.value
        } else {
            people.value
        }
    }

    private suspend fun logDatabaseHealth() {
        val stats = peopleRepository.getDatabaseStats()
        val unreadCount = _liveTileStates.count { it.value.hasUnreadMessages }

        println("""
            📊 Database Health Report:
            👥 People: ${stats.totalPeople}
            ⭐ Starred: ${stats.starredCount}
            📞 Phones: ${stats.totalPhones}
            📧 Emails: ${stats.totalEmails}
            🔔 Unread notifications: $unreadCount
            🔄 Unified contacts: ${unifiedContacts.value.size}
        """.trimIndent())
    }

    private suspend fun verifyMockDataConsistency() {
        try {
            println("🔍 Verifying data consistency for contact-conversation matching...")

            val samplePeople = com.metromessages.mockdata.MockDataGenerator.generateMockPeople(5)
            val sampleSmsConvos = com.metromessages.mockdata.MockDataGenerator.generateFacebookConversations(5)

            var exactMatches = 0
            var phoneMatches = 0
            var nameMatches = 0
            var noMatches = 0
            var groupsSkipped = 0

            println("📊 Checking ${samplePeople.size} contacts against ${sampleSmsConvos.size} conversations")

            // Check each conversation for matching contact
            sampleSmsConvos.forEach { conversation ->
                // Skip group conversations (they don't map to individual contacts)
                if (conversation.isSmsGroup || conversation.id.startsWith("mms_group_")) {
                    println("   👥 Skipping group conversation: ${conversation.name}")
                    groupsSkipped++
                    return@forEach
                }

                var matchedContact: PersonWithDetails? = null
                var matchType = "No match"

                // ✅ STRATEGY 1: Direct ID matching (for mock data)
                val contactIdFromConversation = when {
                    conversation.id.startsWith("sms_") -> conversation.id.removePrefix("sms_").toLongOrNull()
                    conversation.id.startsWith("fb_") -> conversation.id.removePrefix("fb_").toLongOrNull()
                    else -> null
                }

                if (contactIdFromConversation != null) {
                    matchedContact = samplePeople.find { it.person.id == contactIdFromConversation }
                    if (matchedContact != null) {
                        matchType = "ID match"
                        exactMatches++
                    }
                }

                // ✅ STRATEGY 2: Phone number matching (for real data)
                if (matchedContact == null && conversation.address != null) {
                    matchedContact = samplePeople.find { person ->
                        person.phones.any { phone ->
                            phone.number == conversation.address
                        }
                    }
                    if (matchedContact != null) {
                        matchType = "Phone match"
                        phoneMatches++
                    }
                }

                // ✅ STRATEGY 3: Name matching (fallback)
                if (matchedContact == null && conversation.name != null) {
                    matchedContact = samplePeople.find { person ->
                        person.person.displayName.equals(conversation.name, ignoreCase = true)
                    }
                    if (matchedContact != null) {
                        matchType = "Name match"
                        nameMatches++
                    }
                }

                // ✅ STRATEGY 4: Facebook ID as phone number (for mock data compatibility)
                if (matchedContact == null) {
                    matchedContact = samplePeople.find { person ->
                        person.phones.any { phone ->
                            phone.number == conversation.facebookId
                        }
                    }
                    if (matchedContact != null) {
                        matchType = "Facebook ID as phone match"
                        phoneMatches++
                    }
                }

                if (matchedContact == null) {
                    println("   ❌ No contact found for: ${conversation.name} (ID: ${conversation.id}, Addr: ${conversation.address})")
                    noMatches++
                } else {
                    println("   ✅ $matchType: ${matchedContact.person.displayName} ↔ ${conversation.name}")

                    // Verify the match is good
                    if (matchedContact.person.displayName != conversation.name) {
                        println("      ⚠️  Name differs: '${matchedContact.person.displayName}' vs '${conversation.name}'")
                    }
                }
            }

            // Check for contacts without conversations
            val contactsWithoutConversations = samplePeople.filter { person ->
                !sampleSmsConvos.any { conversation ->
                    // Skip groups
                    if (conversation.isSmsGroup) return@any false

                    // Check all matching strategies
                    val contactIdFromConversation = when {
                        conversation.id.startsWith("sms_") -> conversation.id.removePrefix("sms_").toLongOrNull()
                        conversation.id.startsWith("fb_") -> conversation.id.removePrefix("fb_").toLongOrNull()
                        else -> null
                    }

                    contactIdFromConversation == person.person.id ||
                            person.phones.any { phone -> phone.number == conversation.address } ||
                            person.person.displayName.equals(conversation.name, ignoreCase = true) ||
                            person.phones.any { phone -> phone.number == conversation.facebookId }
                }
            }

            if (contactsWithoutConversations.isNotEmpty()) {
                println("   📞 Contacts without conversations: ${contactsWithoutConversations.size}")
                contactsWithoutConversations.forEach { contact ->
                    println("      - ${contact.person.displayName} (${contact.phones.firstOrNull()?.number ?: "no phone"})")
                }
                noMatches += contactsWithoutConversations.size
            }

            println("""
                📊 Data Consistency Report:
                ✅ Exact ID matches: $exactMatches
                📞 Phone number matches: $phoneMatches  
                📛 Name matches: $nameMatches
                ❌ No matches: $noMatches
                👥 Groups skipped: $groupsSkipped
                👥 Total contacts: ${samplePeople.size}
                💬 Total conversations: ${sampleSmsConvos.size}
            """.trimIndent())

            // ✅ FUTURE-PROOF: Different thresholds for mock vs real data
            val isMockData = samplePeople.isNotEmpty() && samplePeople.all { it.person.id in 1000L..1100L }

            if (isMockData) {
                // For mock data, expect high match rate
                if (exactMatches == 0 && sampleSmsConvos.isNotEmpty()) {
                    println("⚠️  WARNING: Mock data has no ID matches - check ID generation")
                }
            } else {
                // For real data, phone number matches are most important
                if (phoneMatches == 0 && sampleSmsConvos.isNotEmpty()) {
                    println("⚠️  WARNING: Real data has no phone number matches - check contact permissions")
                }
            }

            // Only throw for critical failures
            val totalMatches = exactMatches + phoneMatches + nameMatches
            if (totalMatches == 0 && sampleSmsConvos.isNotEmpty()) {
                throw IllegalStateException("CRITICAL: No conversations could be matched to contacts!")
            }

            println("✅ Data consistency verification completed")

        } catch (e: Exception) {
            println("❌ Data consistency check failed: ${e.message}")
            // Don't crash the app - real data might still work
            // Just log and continue
            e.printStackTrace()
        }
    }

    private suspend fun addMockData() {
        try {
            val mockPeople = com.metromessages.mockdata.MockDataGenerator.generateMockPeople(30)
            peopleRepository.insertMockData(mockPeople)
            println("✅ Loaded ${mockPeople.size} consistent mock contacts via batch operation")
        } catch (e: Exception) {
            println("❌ Failed to add mock data: ${e.message}")
            // Don't crash - the app can still function
            e.printStackTrace()
        }
    }
}
