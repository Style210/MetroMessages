package com.metromessages.data.model.facebook

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.metromessages.data.local.MessageType
import com.metromessages.data.local.MessageTypeConverter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

// ✅ KEEP existing enums but add SMS-specific ones
enum class MessageStatus {
    SENDING, SENT, DELIVERED, READ, FAILED
}

enum class SyncState {
    LOCAL, SYNCED, PENDING, CONFLICT
}

// ✅ ADD SMS-specific types
enum class SmsType {
    INBOX, SENT, DRAFT, OUTBOX, FAILED, QUEUED
}

@Serializable
data class Reaction(
    val userId: String,
    val emoji: String,
    val timestamp: Long
)

// ✅ KEEP existing converters
class MessageStatusConverter {
    @TypeConverter
    fun fromMessageStatus(status: MessageStatus): String = status.name

    @TypeConverter
    fun toMessageStatus(value: String): MessageStatus = MessageStatus.valueOf(value)
}

class SyncStateConverter {
    @TypeConverter
    fun fromSyncState(state: SyncState): String = state.name

    @TypeConverter
    fun toSyncState(value: String): SyncState = SyncState.valueOf(value)
}

class StringListConverter {
    @TypeConverter
    fun fromStringList(list: List<String>): String = Json.encodeToString(ListSerializer(String.serializer()), list)

    @TypeConverter
    fun toStringList(data: String): List<String> = Json.decodeFromString(ListSerializer(String.serializer()), data)
}

class ReactionListConverter {
    @TypeConverter
    fun fromReactions(reactions: List<Reaction>): String {
        return Json.encodeToString(ListSerializer(Reaction.serializer()), reactions)
    }

    @TypeConverter
    fun toReactions(data: String): List<Reaction> {
        return try {
            Json.decodeFromString(ListSerializer(Reaction.serializer()), data)
        } catch (e: Exception) {
            emptyList()
        }
    }
}

// ✅ ADD SMS Type Converter
class SmsTypeConverter {
    @TypeConverter
    fun fromSmsType(type: SmsType): String = type.name

    @TypeConverter
    fun toSmsType(value: String): SmsType = SmsType.valueOf(value)
}

@Entity(
    tableName = "facebook_messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["timestamp"]),
        Index(value = ["messageId"]),
        Index(value = ["conversationId", "timestamp"]),
        Index(value = ["phoneNumberId"]),
        Index(value = ["threadId"]),
        Index(value = ["smsType"]),
        Index(value = ["simSlot"])
    ]
)
@TypeConverters(
    MessageTypeConverter::class,
    ReactionListConverter::class,
    MessageStatusConverter::class,
    SyncStateConverter::class,
    StringListConverter::class,
    SmsTypeConverter::class
)
data class FacebookMessageEntity(
    @PrimaryKey
    val messageId: String,
    val conversationId: String,
    val sender: String?,
    val body: String?,
    val timestamp: Long,
    val isSentByUser: Boolean,
    val messageType: MessageType = MessageType.TEXT,
    val mediaUri: String? = null,
    val messageStatus: MessageStatus = MessageStatus.SENT,
    val readBy: List<String> = emptyList(),
    val deliveredTo: List<String> = emptyList(),
    val reactions: List<Reaction> = emptyList(),
    val replyToMessageId: String? = null,
    val editedAt: Long? = null,
    val originalBody: String? = null,
    val lastUpdated: Long = System.currentTimeMillis(),
    val syncState: SyncState = SyncState.SYNCED,
    val address: String? = null,
    val threadId: Long? = null,
    val smsType: SmsType = SmsType.INBOX,
    val isMms: Boolean = false,
    val mmsSubject: String? = null,
    val participants: List<String?> = emptyList(),
    val simSlot: Int = 0,
    val serviceCenter: String? = null,
    val phoneNumberId: String
) {
    fun isIncoming(): Boolean = smsType == SmsType.INBOX
    fun isOutgoing(): Boolean = smsType == SmsType.SENT || smsType == SmsType.OUTBOX
    fun getDisplayAddress(): String = address ?: sender ?: "Unknown"
    fun wasSentViaSim(slotIndex: Int): Boolean = isOutgoing() && simSlot == slotIndex
    fun getSimDisplayName(): String = when (simSlot) {
        0 -> "SIM 1"
        1 -> "SIM 2"
        else -> "SIM ${simSlot + 1}"
    }

    fun getPhoneNumberId(): PhoneNumberId = PhoneNumberId(phoneNumberId)
}
