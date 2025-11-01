// File: MediaModule.kt
package com.metromessages.attachments

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {

    @Provides
    @Singleton
    fun provideMediaRepository(@ApplicationContext context: Context): MediaRepository {
        return AndroidMediaRepository(context)
    }

    // ADD THESE NEW PROVIDERS:
    @Provides
    @Singleton
    fun provideAttachmentProcessorRepository(
        @ApplicationContext context: Context,
        fileHelper: FileHelper
    ): AttachmentProcessorRepository {
        return AttachmentProcessorRepository(context, fileHelper)
    }

    @Provides
    @Singleton
    fun provideFileHelper(): FileHelper {
        return FileHelper
    }
}