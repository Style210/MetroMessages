// File: com.metromessages.data.local.metromessagehub.MetroConversationMessage.kt
package com.metromessages.data.local.metromessagehub

import androidx.compose.runtime.Immutable

/**
 * FOSSIFY-COMPLIANT: UI representation for conversation messages
 * Minimal wrapper that only adds UI-specific data to MetroMessage
 * Unlike Facebook architecture, this doesn't duplicate data
 */
@Immutable
data class MetroConversationMessage(
    // ✅ FOSSIFY PATTERN: Direct composition of MetroMessage
    val metroMessage: MetroMessage,

    // ✅ UI-specific data only (not in database)
    val contactName: String? = null,
    val contactPhotoUri: String? = null,
    val isGroupConversation: Boolean = false,
    val showContactInfo: Boolean = false
) {
    // ✅ Computed properties for easy UI access
    val displayName: String
        get() = contactName ?: metroMessage.address

    val shouldShowContactHeader: Boolean
        get() = showContactInfo && isGroupConversation && metroMessage.isIncoming

    // ✅ Delegated properties to avoid duplication
    val id: Long get() = metroMessage.id
    val threadId: Long get() = metroMessage.threadId
    val body: String? get() = metroMessage.body
    val date: Long get() = metroMessage.date
    val isIncoming: Boolean get() = metroMessage.isIncoming
    val isOutgoing: Boolean get() = metroMessage.isOutgoing
    val hasMedia: Boolean get() = metroMessage.hasMedia
    val mediaUri: android.net.Uri? get() = metroMessage.mediaUri
    val messageType: MessageType get() = metroMessage.messageType
    val isOTP: Boolean get() = metroMessage.isOTP
    val displayBody: String get() = metroMessage.displayBody
}

/**
 * FOSSIFY-COMPLIANT: Extension functions for conversion
 * Minimal, focused utilities like Fossify patterns
 */

/**
 * Convert raw MetroMessage to UI-ready MetroConversationMessage
 * ✅ FOSSIFY PATTERN: Simple conversion with contact resolution
 */
fun MetroMessage.toConversationMessage(
    contactName: String? = null,
    contactPhotoUri: String? = null,
    isGroup: Boolean = false,
    showContactInfo: Boolean = false
): MetroConversationMessage {
    return MetroConversationMessage(
        metroMessage = this,
        contactName = contactName,
        contactPhotoUri = contactPhotoUri,
        isGroupConversation = isGroup,
        showContactInfo = showContactInfo
    )
}

/**
 * Batch convert messages for conversation display
 * ✅ FOSSIFY PATTERN: Efficient batch processing
 */
fun List<MetroMessage>.toConversationMessages(
    contactResolver: (String) -> Pair<String?, String?> = { _ -> null to null },
    isGroup: Boolean = false
): List<MetroConversationMessage> {
    return mapIndexed { index, message ->
        val (contactName, contactPhotoUri) = contactResolver(message.address)
        val showContactInfo = shouldShowContactHeader(this, index, isGroup)

        message.toConversationMessage(
            contactName = contactName,
            contactPhotoUri = contactPhotoUri,
            isGroup = isGroup,
            showContactInfo = showContactInfo
        )
    }
}

/**
 * FOSSIFY-COMPLIANT: Contact header logic for group conversations
 * Determines when to show contact name above message
 */
private fun shouldShowContactHeader(
    messages: List<MetroMessage>,
    currentIndex: Int,
    isGroup: Boolean
): Boolean {
    if (!isGroup) return false

    val currentMessage = messages.getOrNull(currentIndex) ?: return false
    if (!currentMessage.isIncoming) return false

    // Show header if first message or different sender than previous
    return when {
        currentIndex == 0 -> true
        else -> {
            val previousMessage = messages.getOrNull(currentIndex - 1)
            previousMessage?.address != currentMessage.address
        }
    }
}

/**
 * FOSSIFY-COMPLIANT: Simple contact resolver using direct Android Contacts API
 * No ViewModel dependency - direct queries like Fossify
 */
fun createDefaultContactResolver(context: android.content.Context): (String) -> Pair<String?, String?> {
    return { phoneNumber ->
        try {
            // ✅ FOSSIFY PATTERN: Direct ContactsContract query
            val uri = android.net.Uri.withAppendedPath(
                android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(phoneNumber)
            )

            val projection = arrayOf(
                android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME,
                android.provider.ContactsContract.PhoneLookup.PHOTO_URI
            )

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(
                        android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME
                    ))
                    val photoUri = cursor.getString(cursor.getColumnIndexOrThrow(
                        android.provider.ContactsContract.PhoneLookup.PHOTO_URI
                    ))
                    name to photoUri
                } else {
                    null to null
                }
            } ?: (null to null)
        } catch (e: Exception) {
            // ✅ FOSSIFY PATTERN: Graceful fallback
            null to null
        }
    }
}
