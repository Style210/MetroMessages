// File: data/local/metropeoplehub/MetroContactsRepository.kt
package com.metromessages.data.local.metropeoplehub

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FOSSIFY-STYLE: Direct Android Contacts API
 * WITH CACHING to prevent duplicate loads
 * NO DUPLICATE STORAGE - Single source of truth
 * Clean, focused API with hooks for future features
 */
@Singleton
class MetroContactsRepository @Inject constructor(
    private val contentResolver: ContentResolver
) {

    // ✅ ADDED: CACHE to prevent duplicate contact loading
    private val contactsCache = AtomicReference<List<MetroContact>?>(null)
    private var cacheTimestamp = 0L
    private val CACHE_VALIDITY_MS = 30000L // Cache valid for 30 seconds

    // ✅ ADDED: Thread-safe cache clearing
    fun clearCache() {
        contactsCache.set(null)
        cacheTimestamp = 0L
        println("🗑️ Contacts cache cleared")
    }

    private fun isCacheValid(): Boolean {
        return contactsCache.get() != null &&
                (System.currentTimeMillis() - cacheTimestamp) < CACHE_VALIDITY_MS
    }

    // ✅ CORE CONTACT BROWSING - WITH CACHE
    suspend fun getAllContacts(): List<MetroContact> = withContext(Dispatchers.IO) {
        val stackTrace = Thread.currentThread().stackTrace.take(10).joinToString("\n") { it.toString() }
        println("📞 WHO IS CALLING getAllContacts()?")
        println(stackTrace)
        // ✅ CHECK CACHE FIRST
        if (isCacheValid()) {
            val cachedContacts = contactsCache.get()
            if (cachedContacts != null) {
                println("🔄 Returning ${cachedContacts.size} contacts from cache")
                return@withContext cachedContacts
            }
        }

        println("📞 Loading contacts from system...")
        val startTime = System.currentTimeMillis()

        val contacts = mutableListOf<MetroContact>()

        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val photoIndex = it.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
            val starredIndex = it.getColumnIndex(ContactsContract.Contacts.STARRED)

            // Safe column indices
            if (idIndex == -1 || nameIndex == -1) {
                println("⚠️ Contact columns not found")
                return@withContext emptyList()
            }

            var contactCount = 0
            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex) ?: "Unknown"
                val photoUri = if (photoIndex != -1) it.getString(photoIndex) else null
                val starred = if (starredIndex != -1) it.getInt(starredIndex) == 1 else false

                val phones = getContactPhones(id)
                val emails = getContactEmails(id)

                // Only include contacts with phone numbers (for SMS app compatibility)
                if (phones.isNotEmpty()) {
                    contacts.add(MetroContact(
                        id = id,
                        displayName = name,
                        photoUri = photoUri,
                        starred = starred,
                        phones = phones,
                        emails = emails
                    ))
                    contactCount++
                }
            }

            println("✅ Loaded $contactCount contacts in ${System.currentTimeMillis() - startTime}ms")
        }

        // ✅ UPDATE CACHE
        contactsCache.set(contacts)
        cacheTimestamp = System.currentTimeMillis()

        println("💾 Cached ${contacts.size} contacts")

        return@withContext contacts
    }

    // ✅ ADDED: Fast method to get only starred contacts
    suspend fun getStarredContacts(): List<MetroContact> = withContext(Dispatchers.IO) {
        println("⭐ Loading starred contacts only...")
        val startTime = System.currentTimeMillis()

        val contacts = mutableListOf<MetroContact>()

        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            "${ContactsContract.Contacts.STARRED} = ?",
            arrayOf("1"), // Only starred contacts
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val photoIndex = it.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
            val starredIndex = it.getColumnIndex(ContactsContract.Contacts.STARRED)

            if (idIndex == -1 || nameIndex == -1) return@withContext emptyList()

            var contactCount = 0
            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex) ?: "Unknown"
                val photoUri = if (photoIndex != -1) it.getString(photoIndex) else null
                val starred = if (starredIndex != -1) it.getInt(starredIndex) == 1 else false

                val phones = getContactPhones(id)
                val emails = getContactEmails(id)

                if (phones.isNotEmpty()) {
                    contacts.add(MetroContact(
                        id = id,
                        displayName = name,
                        photoUri = photoUri,
                        starred = starred,
                        phones = phones,
                        emails = emails
                    ))
                    contactCount++
                }
            }

            println("✅ Loaded $contactCount starred contacts in ${System.currentTimeMillis() - startTime}ms")
        }

        return@withContext contacts
    }

    // ✅ SEARCH - Uses cache when possible
    suspend fun searchContacts(query: String): List<MetroContact> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        // Try to search in cache first
        val cachedContacts = contactsCache.get()
        if (isCacheValid() && cachedContacts != null) {
            val filtered = cachedContacts.filter { contact ->
                contact.displayName.contains(query, ignoreCase = true) ||
                        contact.phones.any { it.number.contains(query) }
            }
            if (filtered.isNotEmpty()) {
                println("🔍 Found ${filtered.size} contacts in cache for '$query'")
                return@withContext filtered
            }
        }

        // Fall back to database search
        println("🔍 Searching contacts in database for '$query'")

        val contacts = mutableListOf<MetroContact>()

        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME} LIKE ?",
            arrayOf("%$query%"),
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val photoIndex = it.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
            val starredIndex = it.getColumnIndex(ContactsContract.Contacts.STARRED)

            if (idIndex == -1 || nameIndex == -1) return@withContext emptyList()

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex) ?: "Unknown"
                val photoUri = if (photoIndex != -1) it.getString(photoIndex) else null
                val starred = if (starredIndex != -1) it.getInt(starredIndex) == 1 else false

                val phones = getContactPhones(id)
                val emails = getContactEmails(id)

                if (phones.isNotEmpty()) {
                    contacts.add(MetroContact(
                        id = id,
                        displayName = name,
                        photoUri = photoUri,
                        starred = starred,
                        phones = phones,
                        emails = emails
                    ))
                }
            }
        }

        println("✅ Found ${contacts.size} contacts for '$query'")
        return@withContext contacts
    }

    suspend fun getContactById(contactId: Long): MetroContact? = withContext(Dispatchers.IO) {
        // Check cache first
        val cachedContacts = contactsCache.get()
        if (isCacheValid() && cachedContacts != null) {
            val cached = cachedContacts.find { it.id == contactId }
            if (cached != null) {
                println("👤 Found contact $contactId in cache")
                return@withContext cached
            }
        }

        // Fall back to database
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            "${ContactsContract.Contacts._ID} = ?",
            arrayOf(contactId.toString()),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                val photoIndex = it.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
                val starredIndex = it.getColumnIndex(ContactsContract.Contacts.STARRED)

                if (idIndex == -1 || nameIndex == -1) return@withContext null

                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex) ?: "Unknown"
                val photoUri = if (photoIndex != -1) it.getString(photoIndex) else null
                val starred = if (starredIndex != -1) it.getInt(starredIndex) == 1 else false

                val phones = getContactPhones(id)
                val emails = getContactEmails(id)

                return@withContext MetroContact(
                    id = id,
                    displayName = name,
                    photoUri = photoUri,
                    starred = starred,
                    phones = phones,
                    emails = emails
                )
            }
        }

        return@withContext null
    }

    // ✅ FUTURE HOOK: For dialer integration
    suspend fun getContactByPhoneNumber(phoneNumber: String): MetroContact? = withContext(Dispatchers.IO) {
        // Check cache first
        val cachedContacts = contactsCache.get()
        if (isCacheValid() && cachedContacts != null) {
            val normalizedNumber = normalizePhoneNumber(phoneNumber)
            val cached = cachedContacts.find { contact ->
                contact.phones.any { normalizePhoneNumber(it.number) == normalizedNumber }
            }
            if (cached != null) {
                println("📞 Found contact by phone in cache")
                return@withContext cached
            }
        }

        // Fall back to database
        val normalizedNumber = normalizePhoneNumber(phoneNumber)

        val phoneCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            "${ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER} = ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?",
            arrayOf(normalizedNumber, phoneNumber),
            null
        )

        phoneCursor?.use {
            val contactIdIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            if (contactIdIndex != -1 && it.moveToFirst()) {
                val contactId = it.getLong(contactIdIndex)
                return@withContext getContactById(contactId)
            }
        }

        return@withContext null
    }

    // ✅ CORE CONTACT MANAGEMENT - Clear cache on modifications
    suspend fun createContact(contactData: ContactData): Long = withContext(Dispatchers.IO) {
        try {
            println("💾 Creating new contact: ${contactData.displayName}")

            val ops = ArrayList<ContentProviderOperation>()

            // Add raw contact
            val rawContactIndex = ops.size
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            // Add display name
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contactData.displayName)
                    .build()
            )

            // Add phone numbers
            contactData.phones.forEachIndexed { index, phone ->
                if (phone.number.isNotBlank()) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.number)
                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phone.type)
                            .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, phone.label)
                            .withValue(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY, if (index == 0) 1 else 0)
                            .build()
                    )
                }
            }

            // Add email addresses
            contactData.emails.forEach { email ->
                if (email.address.isNotBlank()) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.address)
                            .withValue(ContactsContract.CommonDataKinds.Email.TYPE, email.type)
                            .withValue(ContactsContract.CommonDataKinds.Email.LABEL, email.label)
                            .build()
                    )
                }
            }

            // Add notes if available
            contactData.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Note.NOTE, notes)
                        .build()
                )
            }

            // Add organization if available
            contactData.organization?.takeIf { it.isNotBlank() }?.let { organization ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, rawContactIndex)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, organization)
                        .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
                        .build()
                )
            }

            // Execute batch operation
            val results = contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            println("✅ Batch operation completed with ${results.size} results")

            // ✅ CLEAR CACHE after modification
            clearCache()

            // Find and return the new contact ID
            val newContactId = findContactIdByName(contactData.displayName)
            println("🆕 New contact ID: $newContactId")

            newContactId ?: -1L

        } catch (e: Exception) {
            println("❌ Failed to create contact: ${e.message}")
            e.printStackTrace()
            -1L
        }
    }

    suspend fun updateContact(contactId: Long, contactData: ContactData): Boolean = withContext(Dispatchers.IO) {
        try {
            println("💾 Updating contact $contactId: ${contactData.displayName}")

            val ops = ArrayList<ContentProviderOperation>()

            // First, delete existing structured data (phones, emails, notes, organization)
            ops.add(
                ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                    .withSelection(
                        "${ContactsContract.Data.CONTACT_ID} = ? AND (${ContactsContract.Data.MIMETYPE} = ? OR ${ContactsContract.Data.MIMETYPE} = ? OR ${ContactsContract.Data.MIMETYPE} = ? OR ${ContactsContract.Data.MIMETYPE} = ?)",
                        arrayOf(
                            contactId.toString(),
                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE,
                            ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE,
                            ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
                        )
                    )
                    .build()
            )

            // Update display name (or insert if doesn't exist)
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.CONTACT_ID, contactId)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contactData.displayName)
                    .build()
            )

            // Add updated phone numbers
            contactData.phones.forEachIndexed { index, phone ->
                if (phone.number.isNotBlank()) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.CONTACT_ID, contactId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.number)
                            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phone.type)
                            .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, phone.label)
                            .withValue(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY, if (index == 0) 1 else 0)
                            .build()
                    )
                }
            }

            // Add updated email addresses
            contactData.emails.forEach { email ->
                if (email.address.isNotBlank()) {
                    ops.add(
                        ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                            .withValue(ContactsContract.Data.CONTACT_ID, contactId)
                            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                            .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.address)
                            .withValue(ContactsContract.CommonDataKinds.Email.TYPE, email.type)
                            .withValue(ContactsContract.CommonDataKinds.Email.LABEL, email.label)
                            .build()
                    )
                }
            }

            // Add updated notes if available
            contactData.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.CONTACT_ID, contactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Note.NOTE, notes)
                        .build()
                )
            }

            // Add updated organization if available
            contactData.organization?.takeIf { it.isNotBlank() }?.let { organization ->
                ops.add(
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.CONTACT_ID, contactId)
                        .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE)
                        .withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, organization)
                        .withValue(ContactsContract.CommonDataKinds.Organization.TYPE, ContactsContract.CommonDataKinds.Organization.TYPE_WORK)
                        .build()
                )
            }

            // Execute batch operation
            val results = contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            println("✅ Contact update completed with ${results.size} operations")

            // ✅ CLEAR CACHE after modification
            clearCache()

            true

        } catch (e: Exception) {
            println("❌ Failed to update contact: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteContact(contactId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            println("🗑️ Deleting contact: $contactId")

            val result = contentResolver.delete(
                ContactsContract.RawContacts.CONTENT_URI,
                "${ContactsContract.RawContacts.CONTACT_ID} = ?",
                arrayOf(contactId.toString())
            )

            val success = result > 0
            println("✅ Contact deletion ${if (success) "successful" else "failed"}")

            // ✅ CLEAR CACHE after modification
            if (success) {
                clearCache()
            }

            success

        } catch (e: Exception) {
            println("❌ Failed to delete contact: ${e.message}")
            false
        }
    }

    suspend fun toggleFavorite(contactId: Long, starred: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            println("⭐ Toggling favorite for contact $contactId: $starred")

            val values = android.content.ContentValues().apply {
                put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0)
            }

            val result = contentResolver.update(
                ContactsContract.Contacts.CONTENT_URI,
                values,
                "${ContactsContract.Contacts._ID} = ?",
                arrayOf(contactId.toString())
            )

            val success = result > 0
            println("✅ Favorite toggle ${if (success) "successful" else "failed"}")

            // ✅ CLEAR CACHE after modification
            if (success) {
                clearCache()
            }

            success

        } catch (e: Exception) {
            println("❌ Failed to toggle favorite: ${e.message}")
            false
        }
    }

    // ✅ FUTURE HOOKS: For advanced contact editing features - ALL ORIGINAL METHODS PRESERVED
    suspend fun addPhoneNumber(contactId: Long, phone: MetroPhone): Boolean = withContext(Dispatchers.IO) {
        try {
            val ops = ArrayList<ContentProviderOperation>()

            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.CONTACT_ID, contactId)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone.number)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phone.type)
                    .withValue(ContactsContract.CommonDataKinds.Phone.LABEL, phone.label)
                    .build()
            )

            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)

            // ✅ CLEAR CACHE after modification
            clearCache()

            true
        } catch (e: Exception) {
            false
        }
    }



        suspend fun updatePhoneNumber(contactId: Long, phoneId: Long, newNumber: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val values = android.content.ContentValues().apply {
                put(ContactsContract.CommonDataKinds.Phone.NUMBER, newNumber)
            }

            val result = contentResolver.update(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                values,
                "${ContactsContract.CommonDataKinds.Phone._ID} = ?",
                arrayOf(phoneId.toString())
            )

            val success = result > 0

            // ✅ CLEAR CACHE after modification
            if (success) {
                clearCache()
            }

            success
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deletePhoneNumber(contactId: Long, phoneId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = contentResolver.delete(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                "${ContactsContract.CommonDataKinds.Phone._ID} = ?",
                arrayOf(phoneId.toString())
            )

            val success = result > 0

            // ✅ CLEAR CACHE after modification
            if (success) {
                clearCache()
            }

            success
        } catch (e: Exception) {
            false
        }
    }

    suspend fun addEmail(contactId: Long, email: MetroEmail): Boolean = withContext(Dispatchers.IO) {
        try {
            val ops = ArrayList<ContentProviderOperation>()

            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.CONTACT_ID, contactId)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Email.ADDRESS, email.address)
                    .withValue(ContactsContract.CommonDataKinds.Email.TYPE, email.type)
                    .withValue(ContactsContract.CommonDataKinds.Email.LABEL, email.label)
                    .build()
            )

            contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)

            // ✅ CLEAR CACHE after modification
            clearCache()

            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateEmail(contactId: Long, emailId: Long, newEmail: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val values = android.content.ContentValues().apply {
                put(ContactsContract.CommonDataKinds.Email.ADDRESS, newEmail)
            }

            val result = contentResolver.update(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                values,
                "${ContactsContract.CommonDataKinds.Email._ID} = ?",
                arrayOf(emailId.toString())
            )

            val success = result > 0

            // ✅ CLEAR CACHE after modification
            if (success) {
                clearCache()
            }

            success
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteEmail(contactId: Long, emailId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val result = contentResolver.delete(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                "${ContactsContract.CommonDataKinds.Email._ID} = ?",
                arrayOf(emailId.toString())
            )

            val success = result > 0

            // ✅ CLEAR CACHE after modification
            if (success) {
                clearCache()
            }

            success
        } catch (e: Exception) {
            false
        }
    }

    // ✅ INTERNAL HELPER METHODS (unchanged)
    private fun getContactPhones(contactId: Long): List<MetroPhone> {
        val phones = mutableListOf<MetroPhone>()

        val phoneCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )

        phoneCursor?.use {
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val typeIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
            val labelIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)

            if (numberIndex != -1) {
                while (it.moveToNext()) {
                    val number = it.getString(numberIndex)
                    val type = if (typeIndex != -1) it.getInt(typeIndex) else ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    val label = if (labelIndex != -1) it.getString(labelIndex) else null

                    if (!number.isNullOrBlank()) {
                        phones.add(MetroPhone(
                            number = number.trim(),
                            type = type,
                            label = label
                        ))
                    }
                }
            }
        }

        return phones
    }

    private fun getContactEmails(contactId: Long): List<MetroEmail> {
        val emails = mutableListOf<MetroEmail>()

        val emailCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            null,
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )

        emailCursor?.use {
            val addressIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            val typeIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE)
            val labelIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.LABEL)

            if (addressIndex != -1) {
                while (it.moveToNext()) {
                    val address = it.getString(addressIndex)
                    val type = if (typeIndex != -1) it.getInt(typeIndex) else ContactsContract.CommonDataKinds.Email.TYPE_WORK
                    val label = if (labelIndex != -1) it.getString(labelIndex) else null

                    if (!address.isNullOrBlank()) {
                        emails.add(MetroEmail(
                            address = address.trim(),
                            type = type,
                            label = label
                        ))
                    }
                }
            }
        }

        return emails
    }

    private fun findContactIdByName(displayName: String): Long? {
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID),
            "${ContactsContract.Contacts.DISPLAY_NAME} = ?",
            arrayOf(displayName),
            null
        )

        return cursor?.use {
            if (it.moveToFirst()) {
                it.getLong(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
            } else {
                null
            }
        }
    }

    private fun normalizePhoneNumber(phoneNumber: String): String {
        return phoneNumber.replace(Regex("[^0-9+]"), "")
    }

    // ✅ ADDED: Method to invalidate cache (for testing/debugging)
    fun invalidateCacheForTesting() {
        clearCache()
    }
}