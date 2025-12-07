package com.metromessages.di

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Entity
import androidx.room.PrimaryKey

// 🎯 MINIMAL PLACEHOLDER ENTITY FOR ROOM COMPILATION
@Entity(tableName = "database_version")
data class DatabaseVersionEntity(
    @PrimaryKey val id: Int = 1,
    val schemaVersion: Int = 8
)

/**
 * Clean MetroMessages database with minimal placeholder entity
 * Ready for future MetroMessages features like caching, favorites, etc.
 */
@Database(
    entities = [
        DatabaseVersionEntity::class, // ✅ REQUIRED: Minimal entity for Room compilation

        // 🗑️ REMOVED: All Facebook entities
        // FacebookConversationEntity::class,
        // FacebookMessageEntity::class

        // ✅ FUTURE: Add MetroMessages entities here:
        // MessageCacheEntity::class,
        // FavoriteConversationEntity::class,
        // DraftMessageEntity::class,
    ],
    version = 8, // ✅ Incremented version since schema changed significantly
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    // 🗑️ REMOVED: FacebookDao
    // abstract fun facebookDao(): FacebookDao

    // ✅ EMPTY FOR NOW - Add future MetroMessages DAOs here:
    // abstract fun messageCacheDao(): MessageCacheDao
    // abstract fun favoriteConversationDao(): FavoriteConversationDao

    // ✅ Optional: Placeholder DAO (uncomment if needed)
    // abstract fun databaseVersionDao(): DatabaseVersionDao
}