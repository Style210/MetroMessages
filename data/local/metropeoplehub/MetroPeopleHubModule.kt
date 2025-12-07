// File: data/local/metropeoplehub/MetroPeopleHubModule.kt
package com.metromessages.data.local.metropeoplehub

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MetroPeopleHubModule {

    @Provides
    @Singleton
    fun provideMetroContactsRepository(
        @ApplicationContext context: Context
    ): MetroContactsRepository {
        return MetroContactsRepository(context.contentResolver)
    }
}