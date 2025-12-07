// MetroMessagesModule.kt
package com.metromessages.data.local.metromessagehub

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MetroMessagesModule {

    @Provides
    @Singleton
    fun provideMetroMessagesRepository(
        @ApplicationContext context: Context,
        contactsRepository: com.metromessages.data.local.metropeoplehub.MetroContactsRepository
    ): MetroMessagesRepository {
        return MetroMessagesRepository(
            contentResolver = context.contentResolver,
            context = context,
            contactsRepository = contactsRepository
        )
    }

    // 🗑️ REMOVED: MMSManager provider
}