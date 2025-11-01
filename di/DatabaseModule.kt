package com.metromessages.di

import android.content.Context
import androidx.room.Room
import com.metromessages.data.local.AppDatabase
import com.metromessages.data.model.facebook.FacebookDao
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
            .fallbackToDestructiveMigration() // ✅ This will wipe the database and recreate it
            .build()

    @Provides
    fun provideFacebookDao(db: AppDatabase): FacebookDao = db.facebookDao()

    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository = SettingsRepositoryImpl(context)
}