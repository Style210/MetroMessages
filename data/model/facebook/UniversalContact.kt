package com.metromessages.data.model.facebook

data class UniversalContact(
    val phoneNumberId: PhoneNumberId, // PRIMARY KEY - NO EXCEPTIONS
    val rawDisplayName: String, // ✅ CHANGED: Renamed property
    val photoUri: String? = null,
    val androidContactId: Long? = null,
    val isSavedContact: Boolean = true,
    val rawPhoneNumber: String? = null,
    val isStarred: Boolean = false
) {
    // ✅ FIXED: Use different name for computed property
    val displayName: String
        get() = rawDisplayName.ifBlank {
            rawPhoneNumber ?: phoneNumberId.value.removePrefix("pn_")
        }

    // ✅ ADD: Bridge from your UnifiedContact
    companion object {
        fun fromUnifiedContact(unifiedContact: com.metromessages.data.repository.UnifiedContact): UniversalContact {
            val phoneNumberId = if (unifiedContact.phoneNumber != null) {
                PhoneNumberId.fromPhoneNumber(unifiedContact.phoneNumber)
            } else {
                PhoneNumberId.fromContactId(unifiedContact.id)
            }

            return UniversalContact(
                phoneNumberId = phoneNumberId,
                rawDisplayName = unifiedContact.displayName, // ✅ CHANGED
                photoUri = unifiedContact.photoUri,
                androidContactId = unifiedContact.id,
                isSavedContact = true,
                rawPhoneNumber = unifiedContact.phoneNumber,
                isStarred = unifiedContact.starred
            )
        }

        // ✅ ADD: Create from unknown number in ConversationThreadScreen
        fun fromUnknownNumber(phoneNumber: String, displayName: String? = null): UniversalContact {
            return UniversalContact(
                phoneNumberId = PhoneNumberId.fromPhoneNumber(phoneNumber),
                rawDisplayName = displayName ?: phoneNumber, // ✅ CHANGED
                isSavedContact = false,
                rawPhoneNumber = phoneNumber,
                isStarred = false
            )
        }
    }
}