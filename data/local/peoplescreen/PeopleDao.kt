// File: data/local/peoplescreen/PeopleDao.kt
package com.metromessages.data.local.peoplescreen

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PeopleDao {
    // ===== COMPLEX QUERIES =====

    @Transaction
    @Query("SELECT * FROM people ORDER BY displayName COLLATE NOCASE ASC")
    fun getAllPeopleWithDetails(): Flow<List<PersonWithPhonesAndEmails>>

    @Transaction
    @Query("SELECT * FROM people WHERE starred = 1 ORDER BY displayName COLLATE NOCASE ASC")
    fun getStarredPeopleWithDetails(): Flow<List<PersonWithPhonesAndEmails>>

    @Transaction
    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun getPersonWithDetailsById(id: Long): PersonWithPhonesAndEmails?

    @Transaction
    @Query("SELECT * FROM people WHERE displayName LIKE :query ORDER BY displayName COLLATE NOCASE ASC")
    suspend fun searchPeopleWithDetails(query: String): List<PersonWithPhonesAndEmails>

    // ===== INDIVIDUAL QUERIES =====
    @Query("SELECT * FROM people ORDER BY displayName COLLATE NOCASE ASC")
    fun getAllPeople(): Flow<List<PeopleEntity>>

    @Query("SELECT * FROM people WHERE starred = 1 ORDER BY displayName COLLATE NOCASE ASC")
    fun getStarredPeople(): Flow<List<PeopleEntity>>

    @Query("SELECT * FROM people WHERE id = :id")
    suspend fun getPersonById(id: Long): PeopleEntity?

    @Query("SELECT * FROM people WHERE displayName LIKE :query ORDER BY displayName COLLATE NOCASE ASC")
    suspend fun searchPeople(query: String): List<PeopleEntity>

    @Query("SELECT * FROM people_phones WHERE peopleId = :peopleId")
    suspend fun getPhonesForPerson(peopleId: Long): List<PeoplePhone>

    @Query("SELECT * FROM people_emails WHERE peopleId = :peopleId")
    suspend fun getEmailsForPerson(peopleId: Long): List<PeopleEmail>

    // ===== COUNT OPERATIONS =====
    @Query("SELECT COUNT(*) FROM people")
    suspend fun getPersonCount(): Int

    @Query("SELECT COUNT(*) FROM people_phones")
    suspend fun getPhoneCount(): Int

    @Query("SELECT COUNT(*) FROM people_emails")
    suspend fun getEmailCount(): Int

    @Query("SELECT COUNT(*) FROM people WHERE starred = 1")
    suspend fun getStarredCount(): Int

    // ===== INSERT OPERATIONS =====
    @Insert
    suspend fun insertPerson(person: PeopleEntity): Long

    @Insert
    suspend fun insertPhone(phone: PeoplePhone)

    @Insert
    suspend fun insertEmail(email: PeopleEmail)

    @Insert
    suspend fun insertPeople(people: List<PeopleEntity>)

    @Insert
    suspend fun insertPhones(phones: List<PeoplePhone>)

    @Insert
    suspend fun insertEmails(emails: List<PeopleEmail>)

    // ===== UPDATE OPERATIONS =====
    @Update
    suspend fun updatePerson(person: PeopleEntity)

    @Query("UPDATE people SET starred = :starred WHERE id = :personId")
    suspend fun updateStarStatus(personId: Long, starred: Boolean)

    @Query("UPDATE people SET displayName = :name WHERE id = :personId")
    suspend fun updateDisplayName(personId: Long, name: String)

    @Query("UPDATE people SET photoUri = :photoUri WHERE id = :personId")
    suspend fun updatePhotoUri(personId: Long, photoUri: String?)

    // ===== DELETE OPERATIONS =====
    @Delete
    suspend fun deletePerson(person: PeopleEntity)

    @Query("DELETE FROM people_phones WHERE peopleId = :peopleId")
    suspend fun deletePhonesForPerson(peopleId: Long)

    @Query("DELETE FROM people_emails WHERE peopleId = :peopleId")
    suspend fun deleteEmailsForPerson(peopleId: Long)

    @Query("DELETE FROM people WHERE id IN (:personIds)")
    suspend fun deletePeopleByIds(personIds: List<Long>)

    @Query("DELETE FROM people_phones WHERE peopleId IN (:peopleIds)")
    suspend fun deletePhonesForPeople(peopleIds: List<Long>)

    @Query("DELETE FROM people_emails WHERE peopleId IN (:peopleIds)")
    suspend fun deleteEmailsForPeople(peopleIds: List<Long>)

    // ===== CLEAR OPERATIONS =====
    @Query("DELETE FROM people")
    suspend fun clearPeople()

    @Query("DELETE FROM people_phones")
    suspend fun clearPhones()

    @Query("DELETE FROM people_emails")
    suspend fun clearEmails()

    // ===== TRANSACTION OPERATIONS =====
    @Transaction
    suspend fun insertPersonWithDetails(
        person: PeopleEntity,
        phones: List<PeoplePhone>,
        emails: List<PeopleEmail>
    ): Long {
        val personId = insertPerson(person)
        phones.forEach { insertPhone(it.copy(peopleId = personId)) }
        emails.forEach { insertEmail(it.copy(peopleId = personId)) }
        return personId
    }

    @Transaction
    suspend fun updatePersonWithDetails(
        person: PeopleEntity,
        phones: List<PeoplePhone>,
        emails: List<PeopleEmail>
    ) {
        updatePerson(person)
        deletePhonesForPerson(person.id)
        deleteEmailsForPerson(person.id)
        insertPhones(phones.map { it.copy(peopleId = person.id) })
        insertEmails(emails.map { it.copy(peopleId = person.id) })
    }

    @Transaction
    suspend fun deletePersonWithDetails(person: PeopleEntity) {
        deletePhonesForPerson(person.id)
        deleteEmailsForPerson(person.id)
        deletePerson(person)
    }
}

// Relationship class for Room
data class PersonWithPhonesAndEmails(
    @Embedded val person: PeopleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "peopleId"
    )
    val phones: List<PeoplePhone>,
    @Relation(
        parentColumn = "id",
        entityColumn = "peopleId"
    )
    val emails: List<PeopleEmail>
)
