// MetroMessagesRepository.kt - FIXED TO HANDLE NULLABLE MODELS
package com.metromessages.data.local.metromessagehub

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import com.metromessages.data.local.metropeoplehub.MetroContactsRepository
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

@Singleton
class MetroMessagesRepository @Inject constructor(
    private val contentResolver: ContentResolver,
    private val context: Context,
    private val contactsRepository: MetroContactsRepository
) {
    private val TAG = "MetroMessagesRepository"

    // -------------------------
    // CONVERSATION OPERATIONS - RAW DATA ONLY
    // -------------------------

    fun getConversations(): List<MetroConversation> {
        // ⚠️ CRITICAL: Check if we're default SMS app FIRST
        if (!isDefaultSmsApp()) {
            Log.d(TAG, "⚠️ Not default SMS app - returning empty conversations")
            return emptyList()
        }

        return try {
            val cursor = contentResolver.query(
                Telephony.Sms.Conversations.CONTENT_URI,
                null, null, null, "${Telephony.Sms.Conversations.DATE} DESC"
            )

            val conversations = mutableListOf<MetroConversation>()
            cursor?.use {
                while (cursor.moveToNext()) {
                    try {
                        val conversation = MetroConversation.fromCursor(cursor, contentResolver)
                        if (conversation != null) {
                            conversations.add(conversation)
                        } else {
                            Log.w(TAG, "Skipping null conversation (invalid threadId)")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipping corrupted conversation", e)
                    }
                }
            }
            conversations
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException reading SMS - not default app?", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading conversations", e)
            emptyList()
        }
    }

    // 🗑️ REMOVED: getIndividualConversations() - UI handles filtering
    // 🗑️ REMOVED: getGroupConversations() - UI handles filtering

    // -------------------------
    // MESSAGE OPERATIONS - RAW DATA ONLY
    // -------------------------

    fun getMessages(threadId: Long): List<MetroMessage> {
        // ⚠️ CRITICAL: Check if we're default SMS app FIRST
        if (!isDefaultSmsApp()) {
            Log.d(TAG, "⚠️ Not default SMS app - returning empty messages")
            return emptyList()
        }

        return try {
            val cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                "${Telephony.Sms.THREAD_ID} = ?",
                arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} ASC"
            )

            val messages = mutableListOf<MetroMessage>()
            cursor?.use {
                while (cursor.moveToNext()) {
                    try {
                        val message = MetroMessage.fromSmsCursor(cursor)
                        if (message != null) {
                            messages.add(message)
                        } else {
                            Log.w(TAG, "Skipping null message (invalid ID)")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipping corrupted message", e)
                    }
                }
            }
            messages
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException reading messages - not default app?", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading messages", e)
            emptyList()
        }
    }

    // -------------------------
    // SEND MESSAGES - PURE OPERATIONS
    // -------------------------

    fun sendMessage(phoneNumber: String, text: String): Boolean {
        return try {
            SmsManager.getDefault().sendTextMessage(
                phoneNumber,
                null,
                text,
                null,
                null
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
            false
        }
    }

    fun sendMediaMessage(phoneNumber: String, mediaUri: String, type: MessageType, text: String? = null): Boolean {
        return try {
            when (type) {
                MessageType.TEXT, MessageType.OTP, MessageType.LINK -> {
                    sendMessage(phoneNumber, text ?: "")
                }
                else -> {
                    sendMmsDirect(phoneNumber, mediaUri.toUri(), text)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send media message", e)
            false
        }
    }

    private fun sendMmsDirect(phoneNumber: String, mediaUri: Uri, text: String?): Boolean {
        return try {
            val threadId = Telephony.Threads.getOrCreateThreadId(context, phoneNumber)

            val values = ContentValues().apply {
                put(Telephony.Mms.THREAD_ID, threadId)
                put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_OUTBOX)
                put(Telephony.Mms.MESSAGE_TYPE, 128)
                put(Telephony.Mms.DATE, System.currentTimeMillis() / 1000L)
                put(Telephony.Mms.DATE_SENT, System.currentTimeMillis())
                put(Telephony.Mms.READ, 1)
                put(Telephony.Mms.SEEN, 1)
                text?.let { put(Telephony.Mms.SUBJECT, it) }
            }

            val inserted = contentResolver.insert(Telephony.Mms.CONTENT_URI, values) != null
            if (inserted) {
                Log.d(TAG, "MMS inserted to outbox for $phoneNumber")
            }
            inserted
        } catch (e: Exception) {
            Log.e(TAG, "MMS insertion failed", e)
            false
        }
    }

    // -------------------------
    // MESSAGE MANAGEMENT - PURE OPERATIONS
    // -------------------------

    fun deleteMessage(messageId: Long): Boolean {
        return try {
            val deleted = contentResolver.delete(
                Telephony.Sms.CONTENT_URI,
                "${Telephony.Sms._ID} = ?",
                arrayOf(messageId.toString())
            ) > 0
            deleted
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete message", e)
            false
        }
    }

    // -------------------------
    // CONTACT INTEGRATION - STANDARDIZED ASYNC
    // -------------------------

    suspend fun resolveConversationContact(threadId: Long): ConversationContact {
        val messages = getMessages(threadId)
        val phoneNumber = messages.firstOrNull()?.address ?: return ConversationContact.Unknown

        val contact = contactsRepository.getContactByPhoneNumber(phoneNumber)
        return if (contact != null) {
            ConversationContact.ExistingContact(contact)
        } else {
            ConversationContact.UnsavedContact(
                displayName = formatPhoneNumber(phoneNumber),
                phoneNumber = phoneNumber
            )
        }
    }

    // ✅ STANDARDIZED: All contact methods now async
    suspend fun getContactByPhoneNumber(phoneNumber: String): com.metromessages.data.local.metropeoplehub.MetroContact? {
        return contactsRepository.getContactByPhoneNumber(phoneNumber)
    }

    // -------------------------
    // OTP MANAGEMENT - SIMPLIFIED
    // -------------------------

    fun getExpiredOTPMessages(): List<MetroMessage> {
        val allMessages = getConversations().flatMap { conversation ->
            getMessages(conversation.threadId)
        }
        return allMessages.filter { it.isOTP && it.shouldAutoDelete }
    }

    fun cleanupExpiredOTPMessages(): Boolean {
        return try {
            val expiredMessages = getExpiredOTPMessages()
            expiredMessages.forEach { message ->
                deleteMessage(message.id)
            }
            Log.d(TAG, "Cleaned up ${expiredMessages.size} expired OTP messages")
            true
        } catch (e: Exception) {
            Log.e(TAG, "OTP cleanup failed", e)
            false
        }
    }

    // 🗑️ REMOVED: searchConversations() - UI handles search filtering

    // -------------------------
    // UTILITY METHODS - PURE DATA
    // -------------------------

    fun findThreadId(phoneNumber: String): Long? {
        return try {
            Telephony.Threads.getOrCreateThreadId(context, phoneNumber)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun resolvePhoneNumberFromThread(threadId: Long): String? {
        val messages = getMessages(threadId)
        return messages.firstOrNull()?.address
    }

    fun isDefaultSmsApp(): Boolean {
        return try {
            val isDefault = context.packageName == Telephony.Sms.getDefaultSmsPackage(context)
            Log.d(TAG, "isDefaultSmsApp check: $isDefault")
            isDefault
        } catch (e: Exception) {
            Log.e(TAG, "Error checking default SMS app", e)
            false
        }
    }

    fun hasSmsPermissions(): Boolean {
        return try {
            contentResolver.query(
                Telephony.Sms.Conversations.CONTENT_URI,
                null, null, null, null
            )?.close()
            true
        } catch (e: SecurityException) {
            false
        }
    }

    // -------------------------
    // PRIVATE HELPERS
    // -------------------------

    private fun formatPhoneNumber(phoneNumber: String): String {
        val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")
        return when {
            digitsOnly.length == 10 -> "(${digitsOnly.substring(0, 3)}) ${digitsOnly.substring(3, 6)}-${digitsOnly.substring(6)}"
            digitsOnly.length == 11 && digitsOnly.startsWith("1") ->
                "+1 (${digitsOnly.substring(1, 4)}) ${digitsOnly.substring(4, 7)}-${digitsOnly.substring(7)}"
            else -> phoneNumber
        }
    }
}
