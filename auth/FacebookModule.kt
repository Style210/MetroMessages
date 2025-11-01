package com.metromessages.auth

import android.content.Context
import com.metromessages.auth.FacebookScraper
import com.metromessages.auth.FacebookTokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FacebookModule {

    @Provides
    @Singleton
    fun provideFacebookTokenManager(@ApplicationContext context: Context): FacebookTokenManager {
        return FacebookTokenManager(context)
    }

    @Provides
    @Singleton
    fun provideFacebookScraper(tokenManager: FacebookTokenManager): FacebookScraper {
        return FacebookScraper(tokenManager)
    }
}

