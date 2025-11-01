// File: data/local/device/DeviceContactsImporter.kt
package com.metromessages.data.local.peoplescreen

import android.content.Context
import android.provider.ContactsContract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager

class DeviceContactsImporter @Inject constructor(
    private val context: Context,
    private val peopleRepository: PeopleRepository
) {

    /**
     * Check if we have permission and can read device contacts
     */
    suspend fun canImportDeviceContacts(): Boolean {
        val hasPermission = hasContactsPermission()
        val hasContacts = getDeviceContactsCount() > 0

        println("🔍 PERMISSION DEBUG:")
        println("   hasContactsPermission: $hasPermission")
        println("   deviceContactsCount: ${getDeviceContactsCount()}")
        println("   canImport: ${hasPermission && hasContacts}")

        return hasPermission && hasContacts
    }

    /**
     * Get count of device contacts (for progress tracking)
     */
    suspend fun getDeviceContactsCount(): Int {
        return try {
            val cursor = context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID),
                null, null, null
            )
            val count = cursor?.use { it.count } ?: 0
            println("📱 Device contacts count: $count")
            count
        } catch (e: Exception) {
            println("❌ Error counting device contacts: ${e.message}")
            0
        }
    }

    /**
     * Import device contacts into your local Room database
     * Returns progress as a Flow for UI updates
     */
    fun importDeviceContacts(): Flow<ImportProgress> = flow {
        emit(ImportProgress.Started)

        try {
            val deviceContacts = readDeviceContacts()
            val totalContacts = deviceContacts.size
            var processed = 0

            // Clear existing data first
            peopleRepository.clearAllData()

            // Convert and import in batches to show progress
            deviceContacts.forEach { deviceContact ->
                val convertedContact = convertToLocalFormat(deviceContact)
                peopleRepository.insertPeopleWithDetails(listOf(convertedContact))

                processed++
                emit(ImportProgress.Progress(processed, totalContacts))
            }

            emit(ImportProgress.Completed(totalContacts))
        } catch (e: Exception) {
            emit(ImportProgress.Error(e.message ?: "Unknown error occurred"))
        }
    }

    /**
     * Sync device contacts (add new ones without clearing existing)
     */
    fun syncDeviceContacts(): Flow<ImportProgress> = flow {
        emit(ImportProgress.Started)

        try {
            val deviceContacts = readDeviceContacts()

            // Get existing contact IDs for duplicate checking
            // Use .first() to get the actual list from the Flow
            val existingContactIds = peopleRepository.getAllPeople()
                .first() // <-- This was the missing piece!
                .mapNotNull { it.person.contactId }
                .toSet()

            val newContacts = deviceContacts.filter { deviceContact ->
                // Now this works perfectly!
                !existingContactIds.contains(deviceContact.id)
            }

            val totalContacts = newContacts.size
            var processed = 0

            // Import new contacts with progress updates
            newContacts.forEach { deviceContact ->
                val convertedContact = convertToLocalFormat(deviceContact)
                peopleRepository.insertPeopleWithDetails(listOf(convertedContact))

                processed++
                emit(ImportProgress.Progress(processed, totalContacts))
            }

            emit(ImportProgress.Completed(totalContacts))
        } catch (e: Exception) {
            emit(ImportProgress.Error(e.message ?: "Unknown error occurred"))
        }
    }

    /**
     * Read contacts from Android's Contacts Provider
     */
    private suspend fun readDeviceContacts(): List<DeviceContact> {
        val contacts = mutableListOf<DeviceContact>()

        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Contacts.STARRED,
            ContactsContract.Contacts.PHOTO_URI,
            ContactsContract.Contacts.LOOKUP_KEY
        )

        val cursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC"
        )

        cursor?.use { c ->
            val idIndex = c.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            val starredIndex = c.getColumnIndex(ContactsContract.Contacts.STARRED)
            val photoIndex = c.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)
            val lookupIndex = c.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)

            while (c.moveToNext()) {
                val contactId = c.getLong(idIndex)
                val contact = DeviceContact(
                    id = contactId,
                    displayName = c.getString(nameIndex) ?: "",
                    starred = c.getInt(starredIndex) == 1,
                    photoUri = c.getString(photoIndex),
                    lookupKey = c.getString(lookupIndex),
                    phones = getContactPhones(contactId),
                    emails = getContactEmails(contactId)
                )
                contacts.add(contact)
            }
        }

        return contacts
    }

    private fun getContactPhones(contactId: Long): List<DevicePhone> {
        val phones = mutableListOf<DevicePhone>()

        val phoneCursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL
            ),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )

        phoneCursor?.use { cursor ->
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val typeIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
            val labelIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)

            while (cursor.moveToNext()) {
                phones.add(
                    DevicePhone(
                        number = cursor.getString(numberIndex) ?: "",
                        type = cursor.getInt(typeIndex),
                        label = cursor.getString(labelIndex)
                    )
                )
            }
        }

        return phones
    }

    private fun getContactEmails(contactId: Long): List<DeviceEmail> {
        val emails = mutableListOf<DeviceEmail>()

        val emailCursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.TYPE,
                ContactsContract.CommonDataKinds.Email.LABEL
            ),
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )

        emailCursor?.use { cursor ->
            val addressIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            val typeIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE)
            val labelIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.LABEL)

            while (cursor.moveToNext()) {
                emails.add(
                    DeviceEmail(
                        address = cursor.getString(addressIndex) ?: "",
                        type = cursor.getInt(typeIndex),
                        label = cursor.getString(labelIndex)
                    )
                )
            }
        }

        return emails
    }

    /**
     * Convert device contact format to your local Room format
     */
    private fun convertToLocalFormat(deviceContact: DeviceContact): PersonWithDetails {
        return PersonWithDetails(
            person = PeopleEntity(
                id = 0, // Let Room auto-generate
                displayName = deviceContact.displayName,
                photoUri = deviceContact.photoUri,
                starred = deviceContact.starred,
                contactId = deviceContact.id, // Store original device contact ID
                lookupKey = deviceContact.lookupKey,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            ),
            phones = deviceContact.phones.map { devicePhone ->
                PeoplePhone(
                    id = 0, // Let Room auto-generate
                    peopleId = 0, // Will be set when inserted
                    number = devicePhone.number,
                    type = devicePhone.type,
                    label = devicePhone.label
                )
            },
            emails = deviceContact.emails.map { deviceEmail ->
                PeopleEmail(
                    id = 0, // Let Room auto-generate
                    peopleId = 0, // Will be set when inserted
                    address = deviceEmail.address,
                    type = deviceEmail.type,
                    label = deviceEmail.label
                )
            }
        )
    }

    /**
     * Check if we have contacts permission
     */
    private fun hasContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }
}

// Data classes for device contacts
data class DeviceContact(
    val id: Long,
    val displayName: String,
    val starred: Boolean,
    val photoUri: String?,
    val lookupKey: String?,
    val phones: List<DevicePhone>,
    val emails: List<DeviceEmail>
)

data class DevicePhone(
    val number: String,
    val type: Int,
    val label: String?
)

data class DeviceEmail(
    val address: String,
    val type: Int,
    val label: String?
)

// Progress tracking for UI
sealed class ImportProgress {
    object Started : ImportProgress()
    data class Progress(val current: Int, val total: Int) : ImportProgress()
    data class Completed(val totalImported: Int) : ImportProgress()
    data class Error(val message: String) : ImportProgress()
}
