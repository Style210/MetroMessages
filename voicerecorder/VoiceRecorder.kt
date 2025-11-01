// File: VoiceRecorder.kt
package com.metromessages.voicerecorder

interface VoiceRecorder {
    fun startRecording()
    fun stopRecording(): String?
    fun cancelRecording()
    fun isRecording(): Boolean

    // Add companion object for permission constant
    companion object {
        const val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 1001
    }
}