// File: VoiceRecorderModule.kt
package com.metromessages.voicerecorder

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object VoiceRecorderModule {

    @Provides
    @Singleton
    fun provideVoiceRecorder(@ApplicationContext context: Context): VoiceRecorder {
        return MediaRecorderVoiceRecorder(context)
    }

    @Provides
    @Singleton
    fun provideVoiceMemoViewModel(
        voiceRecorder: VoiceRecorder
    ): VoiceMemoViewModel {
        return VoiceMemoViewModel(voiceRecorder)
    }
}