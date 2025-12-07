// File: data/local/metropeoplehub/MetroContact.kt
package com.metromessages.data.local.metropeoplehub

import android.provider.ContactsContract

/**
 * Clean data model for Android Contacts
 * No Room entities - direct from ContentResolver
 * Enhanced with helper methods for consistent data flow
 */
data class MetroContact(
    val id: Long,
    val displayName: String,
    val photoUri: String?,
    val starred: Boolean,
    val phones: List<MetroPhone>,
    val emails: List<MetroEmail> = emptyList()
) {
    val primaryPhone: String? get() = phones.firstOrNull()?.number
}

data class MetroPhone(
    val number: String,
    val type: Int,
    val label: String? = null
)

data class MetroEmail(
    val address: String,
    val type: Int,
    val label: String? = null
)

// Contact editing data models
data class ContactData(
    val displayName: String,
    val phones: List<MetroPhone> = emptyList(),
    val emails: List<MetroEmail> = emptyList(),
    val photoUri: String? = null,
    val organization: String? = null,
    val notes: String? = null
)

/**
 * Enhanced ContactEdits with helper methods used throughout the app
 * Ensures consistent data flow between UI and repository
 */
data class ContactEdits(
    val displayName: String,
    val photoUri: String? = null,
    val phones: List<PhoneEdit> = emptyList(),
    val emails: List<EmailEdit> = emptyList(),
    val notes: String = ""
) {
    // ✅ ACTUALLY USED: Helper properties for type-safe field access
    val homePhone: String get() = phones.find { it.type == ContactsContract.CommonDataKinds.Phone.TYPE_HOME }?.number ?: ""
    val cellPhone: String get() = phones.find { it.type == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE }?.number ?: ""
    val workPhone: String get() = phones.find { it.type == ContactsContract.CommonDataKinds.Phone.TYPE_WORK }?.number ?: ""
    val otherPhone: String get() = phones.find { it.type == ContactsContract.CommonDataKinds.Phone.TYPE_OTHER }?.number ?: ""
    val workEmail: String get() = emails.find { it.type == ContactsContract.CommonDataKinds.Email.TYPE_WORK }?.address ?: ""
    val privateEmail: String get() = emails.find { it.type == ContactsContract.CommonDataKinds.Email.TYPE_HOME }?.address ?: ""

    // ✅ ACTUALLY USED: Ensures consistent data updates across UI
    fun updatePhone(type: Int, number: String, label: String): ContactEdits {
        val updatedPhones = phones.filterNot { it.type == type } +
                if (number.isNotBlank()) PhoneEdit(number, type, label) else null
        return copy(phones = updatedPhones.filterNotNull())
    }

    fun updateEmail(type: Int, address: String, label: String): ContactEdits {
        val updatedEmails = emails.filterNot { it.type == type } +
                if (address.isNotBlank()) EmailEdit(address, type, label) else null
        return copy(emails = updatedEmails.filterNotNull())
    }

    // ✅ ACTUALLY USED: Conversion to repository format
    fun toContactData(): ContactData {
        return ContactData(
            displayName = displayName,
            phones = phones.map { MetroPhone(it.number, it.type, it.label) },
            emails = emails.map { MetroEmail(it.address, it.type, it.label) },
            photoUri = photoUri,
            notes = notes
        )
    }

    companion object {
        // ✅ ACTUALLY USED: Loading existing contacts for editing
        fun fromMetroContact(contact: MetroContact): ContactEdits {
            return ContactEdits(
                displayName = contact.displayName,
                photoUri = contact.photoUri,
                phones = contact.phones.map {
                    PhoneEdit(it.number, it.type, it.label)
                },
                emails = contact.emails.map {
                    EmailEdit(it.address, it.type, it.label)
                }
            )
        }
    }
}

data class PhoneEdit(
    val number: String,
    val type: Int,
    val label: String?
)

data class EmailEdit(
    val address: String,
    val type: Int,
    val label: String?
)
