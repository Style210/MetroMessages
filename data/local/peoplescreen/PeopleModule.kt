// File: data/local/peoplescreen/PeopleModule.kt
package com.metromessages.data.local.peoplescreen

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PeopleModule {

    @Provides
    @Singleton
    fun providePeopleDatabase(@ApplicationContext context: Context): PeopleDatabase {
        return PeopleDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun providePeopleDao(database: PeopleDatabase): PeopleDao {
        return database.peopleDao()
    }

    @Provides
    @Singleton
    fun providePeopleRepository(peopleDao: PeopleDao): PeopleRepository {
        return PeopleRepository(peopleDao)
    }

    // ✅ ADD: Device Contacts Importer to existing module
    @Provides
    @Singleton
    fun provideDeviceContactsImporter(
        @ApplicationContext context: Context,
        peopleRepository: PeopleRepository
    ): DeviceContactsImporter {
        return DeviceContactsImporter(context, peopleRepository)
    }
}
