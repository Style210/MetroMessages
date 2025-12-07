package com.metromessages.di

import android.content.Context
import androidx.room.Room
import com.metromessages.data.settingsscreen.SettingsRepository
import com.metromessages.data.settingsscreen.SettingsRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "metro_messages_db"
        )
            .fallbackToDestructiveMigration() // ✅ Changed to true for placeholder
            .build()

    // 🗑️ REMOVED: FacebookDao - No longer needed for MetroMessages
    // @Provides
    // fun provideFacebookDao(db: AppDatabase): FacebookDao = db.facebookDao()

    // ✅ Optional: Placeholder DAO provider (uncomment if adding DAO)
    // @Provides
    // fun provideDatabaseVersionDao(db: AppDatabase): DatabaseVersionDao = db.databaseVersionDao()

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository = SettingsRepositoryImpl(context) // ✅ STILL NEEDED
}