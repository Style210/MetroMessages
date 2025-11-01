// File: data/local/peoplescreen/PeopleRepository.kt
package com.metromessages.data.local.peoplescreen

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PeopleRepository(private val peopleDao: PeopleDao) {

    // ===== FLOW-BASED QUERIES (Proper async) =====

    fun getAllPeople(): Flow<List<PersonWithDetails>> {
        return peopleDao.getAllPeopleWithDetails().map { peopleWithRelations ->
            peopleWithRelations.map { relation ->
                PersonWithDetails(relation.person, relation.phones, relation.emails)
            }
        }
    }

    fun getStarredPeople(): Flow<List<PersonWithDetails>> {
        return peopleDao.getStarredPeopleWithDetails().map { peopleWithRelations ->
            peopleWithRelations.map { relation ->
                PersonWithDetails(relation.person, relation.phones, relation.emails)
            }
        }
    }

    // ===== SINGLE OPERATIONS =====

    suspend fun getPersonById(id: Long): PersonWithDetails? {
        val relation = peopleDao.getPersonWithDetailsById(id) ?: return null
        return PersonWithDetails(relation.person, relation.phones, relation.emails)
    }

    suspend fun searchPeople(query: String): List<PersonWithDetails> {
        val results = peopleDao.searchPeopleWithDetails("%$query%")
        return results.map { relation ->
            PersonWithDetails(relation.person, relation.phones, relation.emails)
        }
    }

    // ===== BATCH OPERATIONS =====

    suspend fun insertPeopleWithDetails(peopleWithDetails: List<PersonWithDetails>) {
        val people = peopleWithDetails.map { it.person }
        val phones = peopleWithDetails.flatMap { it.phones }
        val emails = peopleWithDetails.flatMap { it.emails }

        peopleDao.insertPeople(people)
        peopleDao.insertPhones(phones)
        peopleDao.insertEmails(emails)
    }

    suspend fun insertMockData(mockPeople: List<PersonWithDetails>) {
        insertPeopleWithDetails(mockPeople)
    }

    // ===== INDIVIDUAL PERSON OPERATIONS =====

    suspend fun insertPerson(person: PeopleEntity, phones: List<PeoplePhone>, emails: List<PeopleEmail>): Long {
        return peopleDao.insertPersonWithDetails(person, phones, emails)
    }

    suspend fun updatePerson(person: PeopleEntity, phones: List<PeoplePhone>, emails: List<PeopleEmail>) {
        peopleDao.updatePersonWithDetails(person, phones, emails)
    }

    // ✅ FIXED: Add overloaded deletePerson methods to handle both cases
    suspend fun deletePerson(person: PeopleEntity) {
        // Get the person first to ensure it exists
        val existingPerson = peopleDao.getPersonById(person.id)
        if (existingPerson != null) {
            peopleDao.deletePerson(existingPerson)
            peopleDao.deletePhonesForPerson(person.id)
            peopleDao.deleteEmailsForPerson(person.id)
        }
    }

    suspend fun deletePerson(person: PersonWithDetails) {
        // Get the person first to ensure it exists
        val existingPerson = peopleDao.getPersonById(person.person.id)
        if (existingPerson != null) {
            peopleDao.deletePerson(existingPerson)
            peopleDao.deletePhonesForPerson(person.person.id)
            peopleDao.deleteEmailsForPerson(person.person.id)
        }
    }

    // ✅ ADD: Alternative method that takes just the ID
    suspend fun deletePersonById(personId: Long) {
        val existingPerson = peopleDao.getPersonById(personId)
        if (existingPerson != null) {
            peopleDao.deletePerson(existingPerson)
            peopleDao.deletePhonesForPerson(personId)
            peopleDao.deleteEmailsForPerson(personId)
        }
    }

    // ===== STAR OPERATIONS =====

    suspend fun toggleStar(personId: Long, starred: Boolean) {
        peopleDao.updateStarStatus(personId, starred)
    }

    suspend fun starPerson(personId: Long) {
        peopleDao.updateStarStatus(personId, true)
    }

    suspend fun unstarPerson(personId: Long) {
        peopleDao.updateStarStatus(personId, false)
    }

    // ===== QUICK UPDATE OPERATIONS =====

    suspend fun updateDisplayName(personId: Long, name: String) {
        peopleDao.updateDisplayName(personId, name)
    }

    suspend fun updatePhotoUri(personId: Long, photoUri: String?) {
        peopleDao.updatePhotoUri(personId, photoUri)
    }

    // ===== BATCH DELETE OPERATIONS =====

    suspend fun deletePeople(personIds: List<Long>) {
        // Get all people first, then delete them one by one
        personIds.forEach { personId ->
            deletePersonById(personId)
        }
    }

    // ===== DATABASE STATUS & INITIALIZATION =====

    suspend fun needsMockData(): Boolean {
        return peopleDao.getPersonCount() == 0
    }

    suspend fun getDatabaseStats(): DatabaseStats {
        return DatabaseStats(
            totalPeople = peopleDao.getPersonCount(),
            totalPhones = peopleDao.getPhoneCount(),
            totalEmails = peopleDao.getEmailCount(),
            starredCount = peopleDao.getStarredCount()
        )
    }

    suspend fun isDatabaseEmpty(): Boolean {
        return peopleDao.getPersonCount() == 0 &&
                peopleDao.getPhoneCount() == 0 &&
                peopleDao.getEmailCount() == 0
    }

    // ===== TESTING & MAINTENANCE OPERATIONS =====

    suspend fun clearAllData() {
        peopleDao.clearEmails()
        peopleDao.clearPhones()
        peopleDao.clearPeople()
    }

    suspend fun resetToMockData(mockPeople: List<PersonWithDetails>) {
        clearAllData()
        insertMockData(mockPeople)
    }

    // ===== VALIDATION & VERIFICATION =====

    suspend fun validatePersonIntegrity(personId: Long): Boolean {
        val person = peopleDao.getPersonById(personId)
        return person != null
    }

    suspend fun getPersonContactInfo(personId: Long): ContactInfo {
        val person = peopleDao.getPersonById(personId) ?: throw IllegalArgumentException("Person not found")
        val phones = peopleDao.getPhonesForPerson(personId)
        val emails = peopleDao.getEmailsForPerson(personId)

        return ContactInfo(
            person = person,
            phones = phones,
            emails = emails
        )
    }
}

// ===== SUPPORTING DATA CLASSES =====

data class PersonWithDetails(
    val person: PeopleEntity,
    val phones: List<PeoplePhone>,
    val emails: List<PeopleEmail>
)

data class DatabaseStats(
    val totalPeople: Int,
    val totalPhones: Int,
    val totalEmails: Int,
    val starredCount: Int
)

data class ContactInfo(
    val person: PeopleEntity,
    val phones: List<PeoplePhone>,
    val emails: List<PeopleEmail>
) {
    val primaryPhone: String? get() = phones.firstOrNull()?.number
    val primaryEmail: String? get() = emails.firstOrNull()?.address
}
