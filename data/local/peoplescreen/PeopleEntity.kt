// File: data/local/peoplescreen/PeopleEntity.kt
package com.metromessages.data.local.peoplescreen

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "people")
data class PeopleEntity(
    @PrimaryKey
    val id: Long = 0,
    val displayName: String,
    val photoUri: String? = null,
    val starred: Boolean = false,
    val contactId: Long? = null,
    val lookupKey: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
