// MessageModels.kt
// One file for everything the messages UI reads. Field names that the existing UI touches
// (messageType, mediaUri, fileName, isOTP, displayBody, hasMedia, isIncoming, body, date, address, id,
//  displayName, contactPhotoUri, lastMessage, lastActivity, hasUnreadMessages, isGroup, isArchived)
// are unchanged so MetroMessageBubble / MessagesTab / ConversationThreadScreen compile as-is.
package com.metromessages.data.local.metromessagehub

import android.net.Uri
import android.provider.Telephony
import java.util.concurrent.TimeUnit

enum class MessageType {
    TEXT, IMAGE, VIDEO, AUDIO, GIF, STICKER, FILE, LINK, LOCATION, CONTACT, OTP;

    companion object {
        fun fromMimeType(mimeType: String?): MessageType = when {
            mimeType == null -> TEXT
            mimeType.startsWith("image/") -> if (mimeType.contains("gif")) GIF else IMAGE
            mimeType.startsWith("video/") -> VIDEO
            mimeType.startsWith("audio/") -> AUDIO
            else -> FILE
        }
    }
}

/** One row in the thread list. Built from the provider's thread table, so MMS/group threads are included. */
data class MetroConversation(
    val threadId: Long,
    val snippet: String?,
    val date: Long,
    val read: Boolean,
    /** Every other party on the thread (never "me"). Size 1 = one-to-one, 2+ = group. */
    val participants: List<String>,
    /** Display names parallel to [participants] (falls back to the number). */
    val participantNames: List<String> = participants,
    /** Contact id of the one-to-one contact, if saved. This is what ContactInfoPage opens. */
    val contactId: Long? = null,
    val contactPhotoUri: String? = null,
    val isArchived: Boolean = false,
    val otpMessageCount: Int = 0
) {
    val phoneNumber: String? get() = participants.firstOrNull()
    val isGroup: Boolean get() = participants.size > 1
    val isIndividual: Boolean get() = !isGroup
    val hasUnreadMessages: Boolean get() = !read
    val lastMessage: String? get() = snippet?.takeIf { it.isNotBlank() }
    val lastActivity: Long get() = date

    val displayName: String
        get() = when {
            participants.isEmpty() -> "Unknown"
            isGroup -> participantNames.take(3).joinToString(", ") +
                    if (participantNames.size > 3) " +${participantNames.size - 3}" else ""
            else -> participantNames.first()
        }
}

/**
 * One bubble. SMS and MMS rows live in different provider tables and their _ids overlap, so MMS ids are
 * shifted by [MMS_ID_OFFSET]. That keeps [id] unique for LazyColumn keys and keeps `id > 0` (the thread
 * screen filters on that). Use [providerId] when talking to the provider.
 */
data class MetroMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String?,
    val date: Long,
    val type: Int,
    val read: Boolean,
    val isMms: Boolean = false,
    val messageType: MessageType = MessageType.TEXT,
    val mediaUri: Uri? = null,
    val mimeType: String? = null,
    val fileName: String? = null,
    val isOTP: Boolean = false
) {
    val providerId: Long get() = if (isMms) id - MMS_ID_OFFSET else id
    val isIncoming: Boolean get() = type == Telephony.Sms.MESSAGE_TYPE_INBOX
    val isOutgoing: Boolean get() = !isIncoming
    val isFailed: Boolean get() = type == Telephony.Sms.MESSAGE_TYPE_FAILED
    val hasMedia: Boolean get() = mediaUri != null

    /** Codes are readable while fresh and masked once stale. Nothing is ever deleted from the provider. */
    val isExpiredOTP: Boolean
        get() = isOTP && System.currentTimeMillis() - date > OTP_LIFETIME_MS

    val displayBody: String
        get() = when {
            body == null -> if (hasMedia) mediaLabel() else ""
            isExpiredOTP -> body.replace(Regex("""\d"""), "•")
            else -> body
        }

    private fun mediaLabel() = when (messageType) {
        MessageType.IMAGE -> "📷 Image"
        MessageType.VIDEO -> "🎥 Video"
        MessageType.AUDIO -> "🎵 Audio"
        MessageType.GIF -> "🎬 GIF"
        MessageType.FILE -> "📄 ${fileName ?: "File"}"
        else -> "📎 Attachment"
    }

    companion object {
        const val MMS_ID_OFFSET = 1_000_000_000_000L
        val OTP_LIFETIME_MS: Long = TimeUnit.HOURS.toMillis(24)

        private val CODE = Regex("""(?<!\d)\d{4,8}(?!\d)""")
        private val KEYWORD = Regex("""code|otp|passcode|verif|one[- ]time|\bpin\b|security|login|sign[- ]?in""", RegexOption.IGNORE_CASE)

        /** The single OTP detector: a 4–8 digit number AND a "this is a code" word. */
        fun looksLikeOtp(body: String?): Boolean =
            !body.isNullOrBlank() && CODE.containsMatchIn(body) && KEYWORD.containsMatchIn(body)
    }
}
