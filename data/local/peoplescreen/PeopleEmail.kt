// File: data/local/peoplescreen/PeopleEmail.kt
package com.metromessages.data.local.peoplescreen

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "people_emails")
data class PeopleEmail(
    @PrimaryKey
    val id: Long = 0,
    val peopleId: Long,
    val address: String,
    val type: Int,
    val label: String?
)
