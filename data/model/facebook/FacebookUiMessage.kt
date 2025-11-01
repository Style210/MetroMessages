package com.metromessages.data.model.facebook

import android.net.Uri
import androidx.core.net.toUri
import com.metromessages.data.local.MessageType

// File: FacebookUiMessage.kt
data class FacebookUiMessage(
    val id: String,
    val conversationId: String,
    val sender: String?,
    val body: String,
    val timestamp: Long,
    val isSentByUser: Boolean,
    val messageType: MessageType,
    val mediaUri: Uri? = null,
    val messageStatus: MessageStatus = MessageStatus.SENT,
    val reactions: List<Reaction> = emptyList(),
    val replyToMessageId: String? = null,
    val isEdited: Boolean = false,
    // ✅ ADD: SMS/MMS specific fields
    val address: String? = null,
    val threadId: Long? = null,
    val smsType: SmsType = SmsType.INBOX,
    val isMms: Boolean = false,
    val participants: List<String?> = emptyList(),
    val simSlot: Int = 0,
    val phoneNumberId: String
) {
    companion object {
        fun fromEntity(entity: FacebookMessageEntity): FacebookUiMessage {
            // SIMPLIFIED LOGIC: Always check the dedicated mediaUri field first
            val mediaUri = entity.mediaUri?.toUri()

            val displayBody = when {
                // If we have a media URI, use the body as caption or default text
                mediaUri != null -> entity.body ?: "[Media]"
                // Regular text message
                else -> entity.body ?: ""
            }

            return FacebookUiMessage(
                id = entity.messageId,
                conversationId = entity.conversationId,
                sender = entity.sender,
                body = displayBody,
                timestamp = entity.timestamp,
                isSentByUser = entity.isSentByUser,
                messageType = entity.messageType,
                mediaUri = mediaUri,
                messageStatus = entity.messageStatus,
                reactions = entity.reactions,
                replyToMessageId = entity.replyToMessageId,
                isEdited = entity.editedAt != null,
                // ✅ ADD: SMS/MMS specific fields
                address = entity.address,
                threadId = entity.threadId,
                smsType = entity.smsType,
                isMms = entity.isMms,
                participants = entity.participants,
                simSlot = entity.simSlot,
                phoneNumberId = entity.phoneNumberId
            )
        }

        fun toEntity(uiMessage: FacebookUiMessage): FacebookMessageEntity {
            // Store media URI in the dedicated field, body remains for text
            val bodyForEntity = uiMessage.body
            val mediaUriForEntity = uiMessage.mediaUri?.toString()

            return FacebookMessageEntity(
                messageId = uiMessage.id,
                conversationId = uiMessage.conversationId,
                sender = uiMessage.sender,
                body = bodyForEntity,
                timestamp = uiMessage.timestamp,
                isSentByUser = uiMessage.isSentByUser,
                messageType = uiMessage.messageType,
                mediaUri = mediaUriForEntity,
                messageStatus = uiMessage.messageStatus,
                readBy = emptyList(), // ✅ ADD: Required field
                deliveredTo = emptyList(), // ✅ ADD: Required field
                reactions = uiMessage.reactions,
                replyToMessageId = uiMessage.replyToMessageId,
                editedAt = if (uiMessage.isEdited) uiMessage.timestamp else null, // ✅ ADD: Required field
                originalBody = if (uiMessage.isEdited) null else uiMessage.body, // ✅ ADD: Required field
                lastUpdated = System.currentTimeMillis(), // ✅ ADD: Required field
                syncState = SyncState.SYNCED, // ✅ ADD: Required field
                // ✅ ADD: SMS/MMS specific fields
                address = uiMessage.address,
                threadId = uiMessage.threadId,
                smsType = uiMessage.smsType,
                isMms = uiMessage.isMms,
                participants = uiMessage.participants,
                mmsSubject = null, // ✅ ADD: Required field
                simSlot = uiMessage.simSlot,
                serviceCenter = null, // ✅ ADD: Required field
                phoneNumberId = uiMessage.phoneNumberId
            )
        }
    }

    // ✅ ADD: Helper methods for SMS functionality
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