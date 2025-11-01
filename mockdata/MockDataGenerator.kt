// File: mockdata/MockDataGenerator.kt
package com.metromessages.mockdata

import com.metromessages.data.local.MessageType
import com.metromessages.data.local.peoplescreen.PeopleEmail
import com.metromessages.data.local.peoplescreen.PeopleEntity
import com.metromessages.data.local.peoplescreen.PeoplePhone
import com.metromessages.data.local.peoplescreen.PersonWithDetails
import com.metromessages.data.model.facebook.FacebookConversationEntity
import com.metromessages.data.model.facebook.FacebookMessageEntity
import com.metromessages.data.model.facebook.MessageStatus
import com.metromessages.data.model.facebook.PhoneNumberId
import com.metromessages.data.model.facebook.SmsType
import com.metromessages.data.model.facebook.SyncState
import kotlin.random.Random

object MockDataGenerator {

    private val firstNames = listOf(
        "Ava", "Liam", "Emma", "Noah", "Olivia", "Elijah", "Isabella", "Lucas", "Mia", "Logan",
        "Sophia", "Ethan", "Amelia", "James", "Harper", "Benjamin", "Evelyn", "Jacob", "Ella", "Mason",
        "Alexander", "Charlotte", "Michael", "Avery", "Daniel", "Sofia", "Matthew", "Abigail", "Jackson", "Emily",
        "David", "Scarlett", "Joseph", "Victoria", "Samuel", "Grace", "Henry", "Chloe", "Owen", "Zoey",
        "Sebastian", "Lily", "Gabriel", "Hannah", "Carter", "Aria", "Jayden", "Layla", "John", "Ellie"
    )

    private val lastNames = listOf(
        "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez",
        "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin",
        "Lee", "Perez", "Thompson", "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson",
        "Walker", "Young", "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores"
    )

    private val shortMessages = listOf(
        "omw", "hey", "yup", "ok", "thanks", "where u at?", "call me", "lol", "got it", "same"
    )

    private val mediumMessages = listOf(
        "are we still on for tonight?",
        "did you get my email earlier today?",
        "i'll bring the charger you asked for",
        "can you pick me up after work?",
        "i'm stuck in traffic, running late"
    )

    private val longMessages = listOf(
        "hey! just wanted to check in and see how things are going. it's been a while since we last caught up and i'd love to hear how everything's going with work and life in general.",
        "i got the documents you sent, but there were a few things that didn't look right. i'll mark them up and send them back later tonight. hope that's ok.",
        "i've been thinking about our conversation from last week and i just wanted to say i really appreciate you being there. it helped more than you know.",
        "remember that time in high school when we all got caught sneaking out to the lake? i just drove past it and it brought back everything. wild days.",
        "happy birthday!! hope you have an amazing day filled with love, laughter, and cake. you deserve it all and more 🎉🎂🥳"
    )

    private val smsSpecificMessages = listOf(
        "Just left the office", "Running 5 min late", "Can you grab milk?",
        "Dinner at 7?", "Got your voicemail", "Call when free",
        "At the grocery store", "What's the address?", "See you soon!",
        "Flight landed safely", "Meeting ran long", "On my way home"
    )

    private val mmsMediaTypes = listOf(
        "image/jpeg", "image/png", "video/mp4", "audio/mp3", "image/gif"
    )

    // Shared contact pool - alphabetical order for People Hub
    private val sharedContacts by lazy {
        generateSharedContactPool(30).sortedBy { it.person.displayName }
    }

    // ===== SHARED CONTACT POOL =====
    private fun generateSharedContactPool(count: Int): List<PersonWithDetails> {
        val peopleList = mutableListOf<PersonWithDetails>()

        repeat(count) { index ->
            val personId = (1000 + index).toLong()
            val firstName = firstNames[index % firstNames.size]
            val lastName = lastNames[index % lastNames.size]
            val displayName = "$firstName $lastName"

            val person = PeopleEntity(
                id = personId,
                displayName = displayName,
                photoUri = generateProfilePictureUrl(displayName),
                starred = index % 5 == 0,
                createdAt = System.currentTimeMillis() - Random.nextLong(30_000_000_000L, 300_000_000_000L),
                updatedAt = System.currentTimeMillis() - Random.nextLong(1_000_000_000L, 30_000_000_000L)
            )

            val phoneNumber = generateConsistentPhoneNumber(personId)
            val phones = listOf(
                PeoplePhone(
                    id = personId * 10,
                    peopleId = personId,
                    number = phoneNumber,
                    type = 1,
                    label = null
                )
            )

            val email = generateConsistentEmail(firstName, lastName, personId)
            val emails = listOf(
                PeopleEmail(
                    id = personId * 10,
                    peopleId = personId,
                    address = email,
                    type = 1,
                    label = null
                )
            )

            peopleList.add(PersonWithDetails(person, phones, emails))
        }

        return peopleList
    }

    private fun generateConsistentPhoneNumber(personId: Long): String {
        return "+1-555-${1000 + (personId % 1000)}"
    }

    private fun generateConsistentEmail(firstName: String, lastName: String, personId: Long): String {
        return "${firstName.lowercase()}.${lastName.lowercase()}${personId % 100}@example.com"
    }

    private fun generateProfilePictureUrl(name: String): String {
        val encodedName = name.replace(" ", "%20")
        val style = listOf("initials", "avataaars", "micah", "lorelei").random()
        return "https://api.dicebear.com/7.x/$style/png?seed=$encodedName&size=100&radius=50"
    }

    private fun generateGroupAvatarUrl(groupName: String): String {
        val encodedName = groupName.replace(" ", "%20")
        return "https://api.dicebear.com/7.x/identicon/png?seed=$encodedName&size=100&radius=50"
    }

    // ===== SMS/MMS CONVERSATIONS =====
    fun generateSmsConversations(count: Int): List<FacebookConversationEntity> {
        val contactsToUse = sharedContacts.take(count)

        val conversations = contactsToUse.mapIndexed { index, contact ->
            val phoneNumber = contact.phones.firstOrNull()?.number ?: "+1-555-0000"
            val phoneNumberId = PhoneNumberId.fromPhoneNumber(phoneNumber)
            val message = generateSmsMessageContent()
            val timestamp = System.currentTimeMillis() - Random.nextLong(600_000L, 7_776_000_000L)

            // ✅ CRITICAL FIX: Use the ACTUAL contact ID from the shared pool
            val actualContactId = contact.person.id

            FacebookConversationEntity(
                id = phoneNumberId.toConversationId(),
                name = contact.person.displayName,
                facebookId = phoneNumber,
                lastMessage = message,
                timestamp = timestamp,
                isPinned = index % 6 == 0,
                unreadCount = if (index % 3 == 0) 1 else 0,
                avatarUrl = contact.person.photoUri,
                isGroup = false,
                customColor = null,
                lastUpdated = timestamp,
                linkedContactId = actualContactId, // ✅ ACTUAL contact ID
                address = phoneNumber,
                threadId = actualContactId, // ✅ Use same ID for threadId
                isSmsGroup = false,
                participants = listOf(phoneNumber),
                smsUnreadCount = if (index % 4 == 0) Random.nextInt(1, 5) else 0,
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
        }

        return conversations.sortedByDescending { it.timestamp }
    }

    // ===== MMS GROUP CONVERSATIONS =====
    fun generateMmsGroupConversations(count: Int): List<FacebookConversationEntity> {
        val allContacts = sharedContacts.shuffled()
        val groups = mutableListOf<FacebookConversationEntity>()

        repeat(count) { index ->
            val groupSize = Random.nextInt(3, 6)
            val groupContacts = allContacts.take(groupSize)

            val contactNames = groupContacts.map { it.person.displayName }
            val groupName = if (contactNames.size <= 2) {
                contactNames.joinToString(", ")
            } else {
                "${contactNames.take(2).joinToString(", ")} +${contactNames.size - 2}"
            }

            val participants = groupContacts.map { it.phones.firstOrNull()?.number ?: "+1-555-0000" }
            val phoneNumberId = PhoneNumberId("group_${participants.sorted().joinToString("_") { it.hashCode().toString() }}")

            val groupConversation = FacebookConversationEntity(
                id = phoneNumberId.toConversationId(),
                name = groupName,
                facebookId = null,
                lastMessage = generateSmsMessageContent(),
                timestamp = System.currentTimeMillis() - Random.nextLong(600_000L, 7_776_000_000L),
                isPinned = index % 4 == 0,
                unreadCount = if (index % 3 == 0) Random.nextInt(1, 3) else 0,
                avatarUrl = generateGroupAvatarUrl(groupName),
                isGroup = true,
                customColor = null,
                lastUpdated = System.currentTimeMillis(),
                linkedContactId = null,
                address = participants.first(),
                threadId = 1000L + index,
                isSmsGroup = true,
                participants = participants,
                smsUnreadCount = if (index % 4 == 0) Random.nextInt(1, 5) else 0,
                isUnknownContact = false,
                rawPhoneNumber = null,
                phoneNumberId = phoneNumberId.value,
                isArchived = false,
                lastMessageType = "TEXT",
                isMuted = false,
                lastReadTimestamp = 0L,
                lastActivityType = "MESSAGE",
                lastContacted = System.currentTimeMillis() - Random.nextLong(600_000L, 7_776_000_000L),
                isBlocked = false
            )

            groups.add(groupConversation)
        }

        return groups.sortedByDescending { it.timestamp }
    }

    // ===== UNKNOWN NUMBER CONVERSATIONS =====
    fun generateUnknownNumberConversations(count: Int): List<FacebookConversationEntity> {
        val unknownPrefixes = listOf("+1-555-UNKN", "+1-555-PRIV", "+1-555-ANON", "+1-555-BLCK")

        return (1..count).map { index ->
            val prefix = unknownPrefixes[index % unknownPrefixes.size]
            val phoneNumber = "$prefix-${1000 + index}"
            val phoneNumberId = PhoneNumberId.fromPhoneNumber(phoneNumber)
            val message = generateSmsMessageContent()
            val timestamp = System.currentTimeMillis() - Random.nextLong(600_000L, 7_776_000_000L)
            val formattedNumber = formatPhoneNumberForDisplay(phoneNumber)

            FacebookConversationEntity(
                id = phoneNumberId.toConversationId(),
                name = formattedNumber,
                facebookId = phoneNumber,
                lastMessage = message,
                timestamp = timestamp,
                isPinned = false,
                unreadCount = if (index % 4 == 0) 1 else 0,
                avatarUrl = null,
                isGroup = false,
                customColor = null,
                lastUpdated = timestamp,
                linkedContactId = null,
                address = phoneNumber,
                threadId = 20000L + index,
                isSmsGroup = false,
                participants = listOf(phoneNumber),
                smsUnreadCount = if (index % 3 == 0) 1 else 0,
                isUnknownContact = true,
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
        }.sortedByDescending { it.timestamp }
    }

    // ===== SMS/MMS MESSAGES =====
    fun generateSmsMessages(conversation: FacebookConversationEntity): List<FacebookMessageEntity> {
        val messageCount = Random.nextInt(3, 12)
        val isGroup = conversation.isSmsGroup

        return (1..messageCount).map { index ->
            val isIncoming = Random.nextBoolean()
            val timestamp = conversation.timestamp - (messageCount - index) * 60000L
            val isMms = Random.nextInt(10) == 0
            val messageContent = if (isMms) generateMmsContent() else generateSmsMessageContent()

            FacebookMessageEntity(
                messageId = "sms_msg_${conversation.id}_$index",
                conversationId = conversation.id,
                sender = if (isIncoming) conversation.facebookId else "current_user_id",
                body = messageContent,
                timestamp = timestamp,
                isSentByUser = !isIncoming,
                messageType = if (isMms) MessageType.IMAGE else MessageType.TEXT,
                mediaUri = if (isMms) generateMockMediaUri() else null,
                messageStatus = if (isIncoming) MessageStatus.SENT else MessageStatus.values().random(),
                readBy = emptyList(),
                deliveredTo = emptyList(),
                reactions = emptyList(),
                replyToMessageId = null,
                editedAt = null,
                originalBody = null,
                lastUpdated = System.currentTimeMillis(),
                syncState = SyncState.SYNCED,
                address = conversation.address,
                threadId = conversation.threadId,
                smsType = if (isIncoming) SmsType.INBOX else SmsType.SENT,
                isMms = isMms,
                participants = if (isGroup) conversation.participants else emptyList(),
                mmsSubject = if (isMms) "Shared media" else null,
                simSlot = if (!isIncoming) Random.nextInt(0, 2) else 0,
                serviceCenter = null,
                phoneNumberId = conversation.phoneNumberId
            )
        }
    }

    // ===== HELPER FUNCTIONS =====
    private fun generateSmsMessageContent(): String {
        return when (Random.nextInt(100)) {
            in 0..60 -> smsSpecificMessages.random()
            in 61..85 -> mediumMessages.random()
            else -> longMessages.random()
        }
    }

    private fun generateMmsContent(): String {
        return when (Random.nextInt(5)) {
            0 -> "Check out this photo!"
            1 -> "Sent a video"
            2 -> "Voice message"
            3 -> "Shared location"
            else -> "Media shared"
        }
    }

    private fun generateMockMediaUri(): String {
        val mediaTypes = listOf("photo", "video", "audio", "document")
        val type = mediaTypes.random()
        return "content://media/$type/${System.currentTimeMillis()}"
    }

    private fun formatPhoneNumberForDisplay(phoneNumber: String): String {
        val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")

        return when {
            digitsOnly.length == 10 -> {
                "(${digitsOnly.substring(0, 3)}) ${digitsOnly.substring(3, 6)}-${digitsOnly.substring(6)}"
            }
            digitsOnly.length == 11 && digitsOnly.startsWith("1") -> {
                "+1 (${digitsOnly.substring(1, 4)}) ${digitsOnly.substring(4, 7)}-${digitsOnly.substring(7)}"
            }
            else -> {
                if (phoneNumber.contains("-")) phoneNumber else {
                    val cleaned = phoneNumber.replace(Regex("[^0-9]"), "")
                    if (cleaned.length <= 3) cleaned
                    else if (cleaned.length <= 6) "${cleaned.substring(0, 3)}-${cleaned.substring(3)}"
                    else "${cleaned.substring(0, 3)}-${cleaned.substring(3, 6)}-${cleaned.substring(6)}"
                }
            }
        }
    }

    // ===== PEOPLE/CONTACTS (Alphabetical Order) =====
    fun generateMockPeople(count: Int = 30): List<PersonWithDetails> {
        return sharedContacts.take(count)
    }

    // ===== BACKWARD COMPATIBILITY - Keep old method names =====
    fun generateFacebookConversations(count: Int): List<FacebookConversationEntity> {
        return generateSmsConversations(count)
    }

    fun generateFacebookMessages(conversation: FacebookConversationEntity): List<FacebookMessageEntity> {
        return generateSmsMessages(conversation)
    }

    // ===== UTILITY FUNCTIONS =====
    fun getContactById(contactId: Long): PersonWithDetails? {
        return sharedContacts.find { it.person.id == contactId }
    }

    fun getAllSharedContacts(): List<PersonWithDetails> {
        return sharedContacts
    }

    fun findContactByPhone(phoneNumber: String): PersonWithDetails? {
        return sharedContacts.find { contact ->
            contact.phones.any { it.number == phoneNumber }
        }
    }

    fun findContactByName(name: String): PersonWithDetails? {
        return sharedContacts.find { it.person.displayName == name }
    }

    fun findConversationByPhone(phoneNumber: String, conversations: List<FacebookConversationEntity>): FacebookConversationEntity? {
        return conversations.find { it.address == phoneNumber || it.facebookId == phoneNumber }
    }

    fun generateMixedConversations(): List<FacebookConversationEntity> {
        val smsConversations = generateSmsConversations(8)
        val mmsGroups = generateMmsGroupConversations(2)
        val unknownConversations = generateUnknownNumberConversations(3)
        return smsConversations + mmsGroups + unknownConversations
    }
}
