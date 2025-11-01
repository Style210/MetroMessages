// File: data/local/peoplescreen/PeopleDatabase.kt
package com.metromessages.data.local.peoplescreen

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context

@Database(
    entities = [PeopleEntity::class, PeoplePhone::class, PeopleEmail::class],
    version = 2,  // Incremented from 1
    exportSchema = false
)
abstract class

PeopleDatabase : RoomDatabase() {
    abstract fun peopleDao(): PeopleDao

    companion object {
        @Volatile
        private var INSTANCE: PeopleDatabase? = null

        fun getInstance(context: Context): PeopleDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PeopleDatabase::class.java,
                    "people.db"  // ← Consistent naming
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Migration from version 1 to 2: Add indices for better performance
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add indices for faster searching and sorting
                db.execSQL("CREATE INDEX idx_people_display_name ON people(displayName)")
                db.execSQL("CREATE INDEX idx_people_starred ON people(starred)")
                db.execSQL("CREATE INDEX idx_people_phones_number ON people_phones(number)")
                db.execSQL("CREATE INDEX idx_people_emails_address ON people_emails(address)")
                db.execSQL("CREATE INDEX idx_people_phones_people_id ON people_phones(peopleId)")
                db.execSQL("CREATE INDEX idx_people_emails_people_id ON people_emails(peopleId)")
            }
        }
    }
}

