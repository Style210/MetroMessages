package com.metromessages.data.model.facebook

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(
    tableName = "facebook_conversations",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["isPinned"]),
        Index(value = ["phoneNumberId"]),
        Index(value = ["isSmsGroup"]),
        Index(value = ["threadId"]),
        Index(value = ["isUnknownContact"]),
        Index(value = ["linkedContactId"])
    ]
)
@TypeConverters(StringListConverter::class)
data class FacebookConversationEntity(
    @PrimaryKey val id: String,
    val name: String?,
    val facebookId: String? = null,
    val lastMessage: String? = null,
    val timestamp: Long,
    val isPinned: Boolean = false,
    val unreadCount: Int = 0,
    val avatarUrl: String? = null,
    val isGroup: Boolean = false,
    val customColor: Int? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    val linkedContactId: Long? = null,
    val address: String? = null,
    val threadId: Long? = null,
    val isSmsGroup: Boolean = false,
    val participants: List<String> = emptyList(),
    val smsUnreadCount: Int = 0,
    val isUnknownContact: Boolean = false,
    val rawPhoneNumber: String? = null,
    val phoneNumberId: String,
    val isArchived: Boolean = false,
    val lastMessageType: String = "TEXT",
    val isMuted: Boolean = false,
    val lastReadTimestamp: Long = 0L,
    val lastActivityType: String = "MESSAGE",
    val lastContacted: Long = System.currentTimeMillis(),
    val isBlocked: Boolean = false
) {
    @get:Ignore
    val displayName: String
        get() = when {
            !name.isNullOrBlank() -> name
            isSmsGroup -> "Group (${participants.size})"
            rawPhoneNumber != null -> formatPhoneNumberForDisplay(rawPhoneNumber)
            !facebookId.isNullOrBlank() -> formatPhoneNumberForDisplay(facebookId)
            else -> "Unknown Contact"
        }

    @get:Ignore
    val hasUnreadMessages: Boolean
        get() = unreadCount > 0 || smsUnreadCount > 0

    @get:Ignore
    val isSmsConversation: Boolean
        get() = true

    @get:Ignore
    val primaryPhoneNumber: String
        get() = when {
            isSmsGroup -> "Group"
            !facebookId.isNullOrBlank() -> facebookId
            !rawPhoneNumber.isNullOrBlank() -> rawPhoneNumber
            else -> ""
        }

    @get:Ignore
    val isFromUnknownNumber: Boolean
        get() = isUnknownContact || (name == null && rawPhoneNumber != null && !isSmsGroup)

    @get:Ignore
    val participantCount: Int
        get() = participants.size

    @get:Ignore
    val profilePictureUrl: String?
        get() = avatarUrl

    @get:Ignore
    val groupDisplayInfo: String
        get() = if (isSmsGroup) {
            "Group • ${participants.size} participants"
        } else {
            ""
        }

    // ✅ SINGLE getPhoneNumberIdObject method
    @Ignore
    fun getPhoneNumberIdObject(): PhoneNumberId = PhoneNumberId(phoneNumberId)

    private fun formatPhoneNumberForDisplay(phoneNumber: String): String {
        val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")
        return when {
            digitsOnly.length == 10 -> "(${digitsOnly.substring(0, 3)}) ${digitsOnly.substring(3, 6)}-${digitsOnly.substring(6)}"
            digitsOnly.length == 11 && digitsOnly.startsWith("1") -> "+1 (${digitsOnly.substring(1, 4)}) ${digitsOnly.substring(4, 7)}-${digitsOnly.substring(7)}"
            else -> phoneNumber.replace(Regex("[^+0-9\\-()\\s]"), "").ifBlank { "Unknown Number" }
        }
    }

    companion object {
        fun createGroupConversation(
            name: String?,
            participants: List<String>,
            lastMessage: String? = null,
            timestamp: Long = System.currentTimeMillis()
        ): FacebookConversationEntity {
            val groupId = "group_${participants.sorted().joinToString("_") { it.hashCode().toString() }}"
            val phoneNumberId = PhoneNumberId(groupId)

            return FacebookConversationEntity(
                id = phoneNumberId.toConversationId(),
                name = name,
                facebookId = null,
                lastMessage = lastMessage,
                timestamp = timestamp,
                isPinned = false,
                unreadCount = 0,
                avatarUrl = null,
                isGroup = true,
                isSmsGroup = true,
                participants = participants,
                smsUnreadCount = 0,
                isUnknownContact = false,
                rawPhoneNumber = null,
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

        fun createSingleConversation(
            phoneNumber: String,
            name: String? = null,
            lastMessage: String? = null,
            timestamp: Long = System.currentTimeMillis()
        ): FacebookConversationEntity {
            val phoneNumberId = PhoneNumberId.fromPhoneNumber(phoneNumber)
            return FacebookConversationEntity(
                id = phoneNumberId.toConversationId(),
                name = name,
                facebookId = phoneNumber,
                lastMessage = lastMessage,
                timestamp = timestamp,
                isPinned = false,
                unreadCount = 0,
                avatarUrl = null,
                isGroup = false,
                isSmsGroup = false,
                participants = listOf(phoneNumber),
                smsUnreadCount = 0,
                isUnknownContact = name == null,
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

        fun createFromContactId(
            contactId: Long,
            name: String,
            phoneNumber: String,
            lastMessage: String? = null,
            timestamp: Long = System.currentTimeMillis()
        ): FacebookConversationEntity {
            val phoneNumberId = PhoneNumberId.fromContactId(contactId)
            return FacebookConversationEntity(
                id = phoneNumberId.toConversationId(),
                name = name,
                facebookId = phoneNumber,
                lastMessage = lastMessage,
                timestamp = timestamp,
                isPinned = false,
                unreadCount = 0,
                avatarUrl = null,
                isGroup = false,
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
        }
    }
}