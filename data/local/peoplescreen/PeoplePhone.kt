// File: data/local/peoplescreen/PeoplePhone.kt
package com.metromessages.data.local.peoplescreen

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "people_phones")
data class PeoplePhone(
    @PrimaryKey
    val id: Long = 0,
    val peopleId: Long,
    val number: String,
    val type: Int,
    val label: String?
)
