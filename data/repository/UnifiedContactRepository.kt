package com.metromessages.data.repository

import com.metromessages.data.local.peoplescreen.ContactEdits
import com.metromessages.data.local.peoplescreen.PeopleDao
import com.metromessages.data.local.peoplescreen.PeopleEmail
import com.metromessages.data.local.peoplescreen.PeoplePhone
import com.metromessages.data.local.peoplescreen.PeopleRepository
import com.metromessages.data.local.peoplescreen.PersonWithDetails
import com.metromessages.data.local.peoplescreen.PersonWithPhonesAndEmails
import com.metromessages.data.model.facebook.FacebookConversationEntity
import com.metromessages.data.model.facebook.FacebookDao
import com.metromessages.data.model.facebook.PhoneNumberId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class UnifiedContact(
    val id: Long,
    val displayName: String,
    val photoUri: String?,
    val starred: Boolean,
    // SMS communication
    val smsConversationId: String?,
    val phoneNumber: String?,
    // Activity state
    val hasUnreadMessages: Boolean,
    val hasMissedCalls: Boolean,
    val lastActivity: Long,
    val lastMessage: String?,
    val lastActivityType: ActivityType,
    // SMS-specific fields
    val isSmsContact: Boolean = false,
    val hasSmsThread: Boolean = false,
    val smsThreadId: Long? = null,
    val lastSmsActivity: Long = 0L
) {
    fun getNavigationTarget(): NavigationTarget? {
        return smsConversationId?.let {
            NavigationTarget(it, MessagePlatform.SMS)
        }
    }

    fun hasSmsActivity(): Boolean {
        return smsConversationId != null || hasSmsThread
    }

    fun getSmsDisplayInfo(): SmsDisplayInfo {
        return SmsDisplayInfo(
            hasActiveSms = hasSmsThread,
            lastSmsTimestamp = lastSmsActivity,
            unreadSmsCount = if (hasUnreadMessages) 1 else 0,
            phoneNumber = phoneNumber
        )
    }
}

data class SmsDisplayInfo(
    val hasActiveSms: Boolean,
    val lastSmsTimestamp: Long,
    val unreadSmsCount: Int,
    val phoneNumber: String?
)

enum class ActivityType {
    SMS_MESSAGE,
    MISSED_CALL,
    OUTGOING_CALL,
    INCOMING_CALL
}

enum class MessagePlatform {
    SMS
}

data class NavigationTarget(
    val conversationId: String,
    val platform: MessagePlatform
)

@Singleton
class UnifiedContactRepository @Inject constructor(
    private val peopleDao: PeopleDao,
    private val facebookDao: FacebookDao,
    private val peopleRepository: PeopleRepository
) {

    private val _cachedContacts = MutableStateFlow<List<UnifiedContact>?>(null)
    private var isInitialized = false

    // 🚨 CONTRACT METHOD 1: Create new contact
    suspend fun createContact(contactEdits: ContactEdits): Long? {
        return try {
            // Create new PeopleEntity
            val newPerson = com.metromessages.data.local.peoplescreen.PeopleEntity(
                id = 0, // Room will auto-generate
                displayName = contactEdits.displayName,
                photoUri = contactEdits.photoUri,
                starred = false,
                contactId = null, // No Android contact ID for new contacts
                lookupKey = generateLookupKey(contactEdits.displayName),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            // Convert phone edits to PeoplePhone entities
            val phones = contactEdits.phones.map { phoneEdit ->
                com.metromessages.data.local.peoplescreen.PeoplePhone(
                    id = 0, // Auto-generate
                    peopleId = 0, // Will be set after person insertion
                    number = phoneEdit.number,
                    type = phoneEdit.type,
                    label = phoneEdit.label
                )
            }

            // Convert email edits to PeopleEmail entities
            val emails = contactEdits.emails.map { emailEdit ->
                com.metromessages.data.local.peoplescreen.PeopleEmail(
                    id = 0, // Auto-generate
                    peopleId = 0, // Will be set after person insertion
                    address = emailEdit.address,
                    type = emailEdit.type,
                    label = emailEdit.label
                )
            }

            // Insert using repository
            val newContactId = peopleRepository.insertPerson(newPerson, phones, emails)

            // Refresh unified data to include new contact
            refreshData()

            newContactId
        } catch (e: Exception) {
            null
        }
    }

    // 🚨 CONTRACT METHOD 2: Delete contact
    suspend fun deleteContact(contactId: Long) {
        try {
            // Get the person to delete
            val person = peopleDao.getPersonById(contactId)
            if (person != null) {
                // Use repository to delete with all related data
                peopleRepository.deletePerson(
                    PersonWithDetails(
                        person = person,
                        phones = peopleDao.getPhonesForPerson(contactId),
                        emails = peopleDao.getEmailsForPerson(contactId)
                    )
                )

                // Remove from cache and refresh
                refreshData()
            }
        } catch (e: Exception) {
            // Silent fail
        }
    }

    // 🚨 CONTRACT METHOD 3: Update recent activity (for Live Tiles)
    suspend fun updateRecentActivity(contactId: Long, lastMessage: String, timestamp: Long) {
        try {
            // This updates the cached contact state for Live Tiles
            // Update the conversation in the database
            val conversation = getConversationByContactId(contactId)
            if (conversation != null) {
                val updatedConversation = conversation.copy(
                    lastMessage = lastMessage,
                    timestamp = timestamp
                )
                facebookDao.insertConversation(updatedConversation)
            }

            // Refresh unified data to reflect changes
            refreshData()

        } catch (e: Exception) {
            // Silent fail
        }
    }

    // Helper method for conversation lookup
    private suspend fun getConversationByContactId(contactId: Long): FacebookConversationEntity? {
        return try {
            facebookDao.getFacebookConversations().first().find { conversation ->
                conversation.threadId == contactId ||
                        conversation.id == "sms_$contactId"
            }
        } catch (e: Exception) {
            null
        }
    }

    // Helper method for lookup key generation
    private fun generateLookupKey(displayName: String): String {
        return "${displayName.replace(" ", "_").lowercase()}_${UUID.randomUUID().toString().take(8)}"
    }

    // ===== EXISTING PEOPLE HUB METHODS =====

    suspend fun initializeContactsOnce() {
        if (isInitialized) {
            return
        }

        val people = peopleDao.getAllPeopleWithDetails().first()
        val smsConvos = facebookDao.getFacebookConversations().first()

        val unifiedContacts = people.map { personRelation ->
            createUnifiedContact(personRelation, smsConvos)
        }

        _cachedContacts.value = unifiedContacts
        isInitialized = true
    }

    fun getUnifiedContacts(): Flow<List<UnifiedContact>> {
        return _cachedContacts.filterNotNull()
    }

    private fun createUnifiedContact(
        personRelation: PersonWithPhonesAndEmails,
        smsConvos: List<FacebookConversationEntity>
    ): UnifiedContact {
        val person = personRelation.person
        val phones = personRelation.phones
        val primaryPhone = phones.firstOrNull()?.number

        // Find matching SMS conversation
        val (smsConvo, matchType) = findBestSmsConversationMatch(person, phones, smsConvos)

        // Determine activity
        val (lastActivity, lastMessage, lastActivityType) =
            determineRecentActivity(smsConvo, person.updatedAt)

        // Calculate SMS-specific fields
        val hasSmsThread = smsConvo != null
        val lastSmsActivity = smsConvo?.timestamp ?: 0L
        val isSmsContact = primaryPhone != null

        // Use unreadCount instead of hasUnreadMessages
        val hasUnreadMessages = (smsConvo?.unreadCount ?: 0) > 0

        return UnifiedContact(
            id = person.id,
            displayName = person.displayName,
            photoUri = person.photoUri,
            starred = person.starred,
            smsConversationId = smsConvo?.id,
            phoneNumber = primaryPhone,
            hasUnreadMessages = hasUnreadMessages,
            hasMissedCalls = false,
            lastActivity = lastActivity,
            lastMessage = lastMessage,
            lastActivityType = lastActivityType,
            isSmsContact = isSmsContact,
            hasSmsThread = hasSmsThread,
            smsThreadId = smsConvo?.threadId,
            lastSmsActivity = lastSmsActivity
        )
    }

    private fun findBestSmsConversationMatch(
        person: com.metromessages.data.local.peoplescreen.PeopleEntity,
        phones: List<PeoplePhone>,
        smsConvos: List<FacebookConversationEntity>
    ): Pair<FacebookConversationEntity?, String> {
        val primaryPhone = phones.firstOrNull()?.number

        // 1. Try direct phone number match
        primaryPhone?.let { phone ->
            val phoneMatch = smsConvos.find { smsConvo ->
                smsConvo.address == phone || smsConvo.facebookId == phone
            }
            if (phoneMatch != null) {
                return Pair(phoneMatch, "Phone match")
            }
        }

        // 2. Try contact ID matching (sms_123 ↔ contact ID 123)
        val directMatch = smsConvos.find { smsConvo ->
            val contactIdFromSms = smsConvo.id.removePrefix("sms_").toLongOrNull()
            contactIdFromSms == person.id
        }
        if (directMatch != null) {
            return Pair(directMatch, "ID match")
        }

        // 3. Try name matching
        val nameMatch = smsConvos.find { smsConvo ->
            smsConvo.name.equals(person.displayName, ignoreCase = true)
        }
        if (nameMatch != null) {
            return Pair(nameMatch, "Name match")
        }

        // 4. Try all phone numbers
        phones.forEach { phone ->
            val phoneMatch = smsConvos.find { smsConvo ->
                smsConvo.address == phone.number || smsConvo.facebookId == phone.number
            }
            if (phoneMatch != null) {
                return Pair(phoneMatch, "Secondary phone match")
            }
        }

        return Pair(null, "No match")
    }

    private fun determineRecentActivity(
        smsConvo: FacebookConversationEntity?,
        fallbackTimestamp: Long
    ): Triple<Long, String?, ActivityType> {
        return if (smsConvo != null) {
            Triple(smsConvo.timestamp, smsConvo.lastMessage, ActivityType.SMS_MESSAGE)
        } else {
            Triple(fallbackTimestamp, null, ActivityType.SMS_MESSAGE)
        }
    }

    suspend fun refreshData() {
        isInitialized = false
        _cachedContacts.value = null
        initializeContactsOnce()
    }

    // 🔍 FIXED: Individual contact Flow
    fun getUnifiedContact(contactId: Long): Flow<UnifiedContact?> {
        return _cachedContacts.map { contacts ->
            contacts?.find { it.id == contactId }
        }
    }

    fun getActiveContacts(): Flow<List<UnifiedContact>> {
        return getUnifiedContacts().map { contacts ->
            contacts
                .filter { it.hasUnreadMessages || it.hasMissedCalls }
                .sortedByDescending { it.lastActivity }
        }
    }

    fun getContactsWithSms(): Flow<List<UnifiedContact>> {
        return getUnifiedContacts().map { contacts ->
            contacts
                .filter { it.hasSmsThread }
                .sortedByDescending { it.lastSmsActivity }
        }
    }

    fun getContactsWithoutSms(): Flow<List<UnifiedContact>> {
        return getUnifiedContacts().map { contacts ->
            contacts
                .filter { it.phoneNumber != null && !it.hasSmsThread }
                .sortedBy { it.displayName }
        }
    }

    fun getContactByPhoneNumber(phoneNumber: String): Flow<UnifiedContact?> {
        return getUnifiedContacts().map { contacts ->
            contacts.find { contact ->
                contact.phoneNumber == phoneNumber
            }
        }
    }

    suspend fun getNavigationTarget(contactId: Long): NavigationTarget? {
        return getUnifiedContacts().first().find { it.id == contactId }?.getNavigationTarget()
    }

    suspend fun getOrCreateSmsConversation(contactId: Long): String? {
        val contact = getUnifiedContacts().first().find { it.id == contactId }
        return contact?.smsConversationId ?: run {
            // Create new SMS conversation if needed
            val person = peopleDao.getPersonById(contactId)
            val phone = peopleDao.getPhonesForPerson(contactId).firstOrNull()?.number

            if (person != null && phone != null) {
                createSmsConversation(person, phone)
            } else {
                null
            }
        }
    }

    private suspend fun createSmsConversation(
        person: com.metromessages.data.local.peoplescreen.PeopleEntity,
        phoneNumber: String
    ): String? {
        try {
            val phoneNumberId = PhoneNumberId.fromPhoneNumber(phoneNumber)
            val conversationId = phoneNumberId.toConversationId()
            val timestamp = System.currentTimeMillis()

            val smsConversation = FacebookConversationEntity(
                id = conversationId,
                name = person.displayName,
                facebookId = phoneNumber,
                lastMessage = "",
                timestamp = timestamp,
                isPinned = false,
                unreadCount = 0,
                avatarUrl = person.photoUri,
                isGroup = false,
                customColor = null,
                lastUpdated = timestamp,
                linkedContactId = person.id,
                address = phoneNumber,
                threadId = person.id,
                isSmsGroup = false,
                participants = listOf(phoneNumber),
                smsUnreadCount = 0,
                isUnknownContact = false,
                rawPhoneNumber = phoneNumber,
                phoneNumberId = phoneNumberId.value,
                isArchived = false,
                lastMessageType = "TEXT",
                isMuted = false,
                lastReadTimestamp = 0L,
                lastActivityType = "MESSAGE",
                lastContacted = timestamp,
                isBlocked = false
            )

            facebookDao.insertConversation(smsConversation)

            // Refresh to include new conversation
            refreshData()

            return conversationId
        } catch (e: Exception) {
            return null
        }
    }

    suspend fun toggleFavorite(contactId: Long, starred: Boolean) {
        peopleRepository.toggleStar(contactId, starred)
        updateCachedContactStarStatus(contactId, starred)
    }

    suspend fun starContact(contactId: Long) {
        peopleRepository.starPerson(contactId)
        updateCachedContactStarStatus(contactId, true)
    }

    suspend fun unstarContact(contactId: Long) {
        peopleRepository.unstarPerson(contactId)
        updateCachedContactStarStatus(contactId, false)
    }

    private fun updateCachedContactStarStatus(contactId: Long, starred: Boolean) {
        val currentContacts = _cachedContacts.value
        if (currentContacts != null) {
            val updatedContacts = currentContacts.map { contact ->
                if (contact.id == contactId) {
                    contact.copy(starred = starred)
                } else {
                    contact
                }
            }
            _cachedContacts.value = updatedContacts
        }
    }

    // 🎯 CRITICAL FIX: Update cached contact immediately
    private fun updateCachedContact(contactId: Long, contactEdits: ContactEdits) {
        val currentContacts = _cachedContacts.value
        if (currentContacts != null) {
            val updatedContacts = currentContacts.map { contact ->
                if (contact.id == contactId) {
                    // Update phone number from contactEdits if available
                    val updatedPhoneNumber = contactEdits.phones.firstOrNull()?.number ?: contact.phoneNumber

                    contact.copy(
                        displayName = contactEdits.displayName,
                        photoUri = contactEdits.photoUri ?: contact.photoUri,
                        phoneNumber = updatedPhoneNumber
                    )
                } else {
                    contact
                }
            }
            _cachedContacts.value = updatedContacts
        }
    }

    // 🔍 ENHANCED: Update contact with proper error handling
    suspend fun updateContact(contactId: Long, contactEdits: ContactEdits) {
        // 🚨 CRITICAL FIX: If contactId is 0, create a new contact instead
        if (contactId == 0L) {
            createContact(contactEdits)
            return
        }
        try {
            val existingPerson = peopleDao.getPersonById(contactId)
            if (existingPerson != null) {
                val updatedPerson = existingPerson.copy(
                    displayName = contactEdits.displayName,
                    photoUri = contactEdits.photoUri ?: existingPerson.photoUri,
                    updatedAt = System.currentTimeMillis()
                )

                peopleDao.updatePerson(updatedPerson)

                if (contactEdits.phones.isNotEmpty()) {
                    peopleDao.deletePhonesForPerson(contactId)
                    val phoneEntities = contactEdits.phones.map { phoneEdit ->
                        PeoplePhone(
                            peopleId = contactId,
                            number = phoneEdit.number,
                            type = phoneEdit.type,
                            label = phoneEdit.label
                        )
                    }
                    peopleDao.insertPhones(phoneEntities)
                }

                if (contactEdits.emails.isNotEmpty()) {
                    peopleDao.deleteEmailsForPerson(contactId)
                    val emailEntities = contactEdits.emails.map { emailEdit ->
                        PeopleEmail(
                            peopleId = contactId,
                            address = emailEdit.address,
                            type = emailEdit.type,
                            label = emailEdit.label
                        )
                    }
                    peopleDao.insertEmails(emailEntities)
                }

                // 🎯 CRITICAL FIX: Update cache IMMEDIATELY
                updateCachedContact(contactId, contactEdits)

            }
        } catch (e: Exception) {
            throw e
        }
    }

    fun markMissedCall(contactId: Long, phoneNumber: String) {
        // Future call log integration
    }

    fun markCallAnswered(contactId: Long) {
        // Future call log integration
    }

    fun markMessagesRead(contactId: Long) {
        // Update unread status when SMS messages are read
    }

    fun searchContacts(query: String): Flow<List<UnifiedContact>> {
        return getUnifiedContacts().map { contacts ->
            contacts.filter { contact ->
                contact.displayName.contains(query, ignoreCase = true) ||
                        contact.phoneNumber?.contains(query) == true ||
                        contact.lastMessage?.contains(query, ignoreCase = true) == true
            }
        }
    }

    fun searchContactsByPhone(query: String): Flow<List<UnifiedContact>> {
        return getUnifiedContacts().map { contacts ->
            contacts.filter { contact ->
                contact.phoneNumber?.contains(query) == true
            }
        }
    }
}
