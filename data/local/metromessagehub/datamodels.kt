// datamodels.kt - COMPLETE WITH PREVENTION OF INVALID IDs
package com.metromessages.data.local.metromessagehub

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import android.util.Log
import java.util.concurrent.TimeUnit

// ✅ SIMPLIFIED: Basic OTP detection
private const val OTP_EXPIRATION_HOURS = 24L
private const val TAG = "MetroDataModels"

/**
 * Simple OTP detection - basic "contains digits" logic
 */
private fun isPotentialOTP(body: String?, sender: String?): Boolean {
    if (body.isNullOrBlank()) return false

    val digitCount = body.count { it.isDigit() }
    val totalLength = body.length

    return digitCount in 4..8 && (digitCount.toFloat() / totalLength) > 0.3f
}

// MessageType enum
enum class MessageType {
    TEXT, IMAGE, VIDEO, AUDIO, GIF, STICKER, FILE, LINK, LOCATION, CONTACT, OTP;

    companion object {
        fun fromMimeType(mimeType: String?): MessageType {
            return when {
                mimeType == null -> TEXT
                mimeType.startsWith("image/") -> when {
                    mimeType.contains("gif") -> GIF
                    else -> IMAGE
                }
                mimeType.startsWith("video/") -> VIDEO
                mimeType.startsWith("audio/") -> AUDIO
                else -> FILE
            }
        }
    }
}

/**
 * Robust conversation model - WITH GROUP DETECTION & CONTACT PHOTOS
 * ✅ FIXED: Returns null for invalid data instead of creating invalid objects
 */
data class MetroConversation(
    val threadId: Long,
    val snippet: String?,
    val messageCount: Int,
    val date: Long,
    val read: Boolean,
    val phoneNumber: String?,
    val contactName: String? = null,
    val contactPhotoUri: String? = null,
    val isArchived: Boolean = false,
    val participants: List<String> = emptyList(),
    val hasUnreadMessages: Boolean = false,
    val starred: Boolean = false,
    val hasMissedCalls: Boolean = false,
    val lastMessageType: MessageType = MessageType.TEXT,
    val otpMessageCount: Int = 0
) {
    companion object {
        /**
         * ✅ FIXED: Returns null if conversation has invalid threadId or other required data
         * This prevents creation of objects with invalid IDs
         */
        fun fromCursor(cursor: Cursor, contentResolver: ContentResolver): MetroConversation? {
            return try {
                val threadId = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.Conversations.THREAD_ID))

                // ✅ CRITICAL FIX: Validate threadId before creating object
                if (threadId <= 0) {
                    Log.w(TAG, "Invalid threadId: $threadId, skipping conversation")
                    return null
                }

                val snippet = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.Conversations.SNIPPET))
                val messageCount = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.Conversations.MESSAGE_COUNT))
                val date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.Conversations.DATE))
                val read = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.Conversations.READ)) == 1

                val participants = resolveParticipants(threadId, contentResolver)
                val phoneNumber = resolvePrimaryPhoneNumber(threadId, contentResolver)
                val contactPhotoUri = resolveContactPhoto(phoneNumber, contentResolver)

                MetroConversation(
                    threadId = threadId,
                    snippet = snippet,
                    messageCount = messageCount,
                    date = date,
                    read = read,
                    phoneNumber = phoneNumber,
                    participants = participants,
                    contactPhotoUri = contactPhotoUri,
                    hasUnreadMessages = !read
                )
            } catch (e: Exception) {
                // ✅ FIXED: Return null instead of creating object with invalid ID
                Log.e(TAG, "Failed to create conversation from cursor", e)
                null
            }
        }

        private fun resolveParticipants(threadId: Long, contentResolver: ContentResolver): List<String> {
            return try {
                val participants = mutableSetOf<String>()

                val cursor = contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    arrayOf(Telephony.Sms.ADDRESS),
                    "${Telephony.Sms.THREAD_ID} = ?",
                    arrayOf(threadId.toString()),
                    null
                )

                cursor?.use {
                    val addressIndex = it.getColumnIndex(Telephony.Sms.ADDRESS)
                    if (addressIndex != -1) {
                        while (it.moveToNext()) {
                            val address = it.getString(addressIndex)
                            if (!address.isNullOrBlank()) {
                                participants.add(address)
                            }
                        }
                    }
                }

                participants.toList()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resolve participants for thread $threadId", e)
                emptyList()
            }
        }

        private fun resolvePrimaryPhoneNumber(threadId: Long, contentResolver: ContentResolver): String? {
            return try {
                val cursor = contentResolver.query(
                    Telephony.Sms.CONTENT_URI,
                    arrayOf(Telephony.Sms.ADDRESS),
                    "${Telephony.Sms.THREAD_ID} = ?",
                    arrayOf(threadId.toString()),
                    "${Telephony.Sms.DATE} DESC LIMIT 1"
                )

                cursor?.use {
                    if (it.moveToFirst()) {
                        it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resolve primary phone number for thread $threadId", e)
                null
            }
        }

        private fun resolveContactPhoto(phoneNumber: String?, contentResolver: ContentResolver): String? {
            if (phoneNumber.isNullOrBlank()) return null

            return try {
                val contactCursor = contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID),
                    "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?",
                    arrayOf(phoneNumber),
                    null
                )

                contactCursor?.use {
                    if (it.moveToFirst()) {
                        val contactId = it.getLong(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))

                        val photoCursor = contentResolver.query(
                            ContactsContract.Contacts.CONTENT_URI,
                            arrayOf(ContactsContract.Contacts.PHOTO_URI),
                            "${ContactsContract.Contacts._ID} = ?",
                            arrayOf(contactId.toString()),
                            null
                        )

                        photoCursor?.use { photo ->
                            if (photo.moveToFirst()) {
                                photo.getString(photo.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI))
                            } else {
                                null
                            }
                        }
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resolve contact photo for $phoneNumber", e)
                null
            }
        }
    }

    val displayName: String
        get() = contactName ?: phoneNumber ?: "Unknown Contact"

    val lastMessage: String?
        get() = snippet?.takeIf { it.isNotBlank() }

    val lastActivity: Long
        get() = date

    val isGroup: Boolean
        get() = participants.size > 2

    val isIndividual: Boolean
        get() = !isGroup

    val hasExpiredOTP: Boolean
        get() = otpMessageCount > 0
}

/**
 * Comprehensive message model - SIMPLIFIED OTP
 * ✅ FIXED: Returns null for invalid data instead of creating invalid objects
 */
data class MetroMessage(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String?,
    val date: Long,
    val type: Int,
    val read: Boolean,
    val messageType: MessageType = MessageType.TEXT,
    val mediaUri: Uri? = null,
    val mediaSize: Long = 0L,
    val mediaDuration: Long = 0L,
    val mediaWidth: Int = 0,
    val mediaHeight: Int = 0,
    val mimeType: String? = null,
    val fileName: String? = null,
    val fileSize: Long = 0L,
    val linkPreview: LinkPreview? = null,
    val location: MessageLocation? = null,
    val contact: MessageContact? = null,
    val reactions: List<MessageReaction> = emptyList(),
    val replyToMessageId: Long? = null,
    val editedAt: Long? = null,
    val delivered: Boolean = false,
    val viewed: Boolean = false,
    val errorCode: Int = 0,
    val isOTP: Boolean = false,
    val otpExpiresAt: Long? = null
) {
    companion object {
        /**
         * ✅ FIXED: Returns null if message has invalid id or other required data
         * This prevents creation of objects with invalid IDs
         */
        fun fromSmsCursor(cursor: Cursor): MetroMessage? {
            return try {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID))

                // ✅ CRITICAL FIX: Validate id before creating object
                if (id <= 0) {
                    Log.w(TAG, "Invalid message ID: $id, skipping message")
                    return null
                }

                val threadId = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID))
                val address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                val body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY))
                val date = cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val type = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE))
                val read = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms.READ)) == 1

                val isOTP = isPotentialOTP(body, address)
                val otpExpiresAt = if (isOTP) {
                    date + TimeUnit.HOURS.toMillis(OTP_EXPIRATION_HOURS)
                } else null

                MetroMessage(
                    id = id,
                    threadId = threadId,
                    address = address ?: "Unknown",
                    body = body,
                    date = date,
                    type = type,
                    read = read,
                    isOTP = isOTP,
                    otpExpiresAt = otpExpiresAt
                )
            } catch (e: Exception) {
                // ✅ FIXED: Return null instead of creating object with invalid ID
                Log.e(TAG, "Failed to create message from cursor", e)
                null
            }
        }
    }

    val isIncoming: Boolean
        get() = type == Telephony.Sms.MESSAGE_TYPE_INBOX

    val isOutgoing: Boolean
        get() = type == Telephony.Sms.MESSAGE_TYPE_SENT

    val isFailed: Boolean
        get() = type == Telephony.Sms.MESSAGE_TYPE_FAILED || errorCode != 0

    val hasMedia: Boolean
        get() = mediaUri != null || messageType != MessageType.TEXT

    val isExpiredOTP: Boolean
        get() = isOTP && otpExpiresAt?.let { System.currentTimeMillis() > it } == true

    val shouldAutoDelete: Boolean
        get() = isOTP && isExpiredOTP

    val displayBody: String
        get() = when {
            body != null -> if (isOTP) maskOTPBody(body) else body
            hasMedia -> getMediaDescription()
            else -> ""
        }

    private fun getMediaDescription(): String {
        return when (messageType) {
            MessageType.IMAGE -> "📷 Image"
            MessageType.VIDEO -> "🎥 Video"
            MessageType.AUDIO -> "🎵 Audio"
            MessageType.GIF -> "🎬 GIF"
            MessageType.STICKER -> "😊 Sticker"
            MessageType.FILE -> "📄 ${fileName ?: "File"}"
            MessageType.LINK -> "🔗 Link"
            MessageType.LOCATION -> "📍 Location"
            MessageType.CONTACT -> "👤 Contact"
            MessageType.OTP -> "🔐 OTP Code"
            else -> "📎 Media"
        }
    }

    private fun maskOTPBody(originalBody: String): String {
        return originalBody.replace(Regex("""\d"""), "•")
    }
}

// Contact resolution classes
sealed class ConversationContact {
    data class ExistingContact(val contact: com.metromessages.data.local.metropeoplehub.MetroContact) : ConversationContact()
    data class UnsavedContact(override val displayName: String, override val phoneNumber: String) : ConversationContact()
    data object Unknown : ConversationContact()

    open val displayName: String
        get() = when (this) {
            is ExistingContact -> contact.displayName
            is UnsavedContact -> displayName
            is Unknown -> "Unknown"
        }

    open val phoneNumber: String?
        get() = when (this) {
            is ExistingContact -> contact.phones.firstOrNull()?.number
            is UnsavedContact -> phoneNumber
            is Unknown -> null
        }

    val photoUri: String?
        get() = when (this) {
            is ExistingContact -> contact.photoUri
            else -> null
        }
}

// Supporting data classes
data class LinkPreview(
    val url: String,
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val domain: String
)

data class MessageLocation(
    val latitude: Double,
    val longitude: Double,
    val name: String?,
    val address: String?
)

data class MessageContact(
    val name: String,
    val phones: List<String>,
    val emails: List<String>,
    val photoUri: String?
)

data class MessageReaction(
    val emoji: String,
    val userId: String,
    val timestamp: Long
)