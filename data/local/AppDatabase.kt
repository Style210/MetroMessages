package com.metromessages.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.metromessages.data.model.facebook.FacebookConversationEntity
import com.metromessages.data.model.facebook.FacebookDao
import com.metromessages.data.model.facebook.FacebookMessageEntity

@Database(
    entities = [
        FacebookConversationEntity::class,
        FacebookMessageEntity::class
    ],
    version = 7, // ✅ Make sure this is version 7
    exportSchema = true
)
@TypeConverters(MessageTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun facebookDao(): FacebookDao
}