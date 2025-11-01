// File: VoiceMemoViewModel.kt
package com.metromessages.voicerecorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

// The different states our voice memo UI can be in
sealed interface VoiceRecorderState {
    object Idle : VoiceRecorderState
    data class Recording(
        val elapsedTimeMs: Long = 0L,
        val maxAmplitude: Int = 0
    ) : VoiceRecorderState
    data class Preview(
        val audioFilePath: String,
        val durationMs: Long = 0L
    ) : VoiceRecorderState
}

@HiltViewModel
class VoiceMemoViewModel @Inject constructor(
    val voiceRecorder: VoiceRecorder
) : ViewModel() {

    private val modernRecorder = voiceRecorder as? MediaRecorderVoiceRecorder

    private val _recorderState = MutableStateFlow<VoiceRecorderState>(VoiceRecorderState.Idle)
    val recorderState: StateFlow<VoiceRecorderState> = _recorderState.asStateFlow()

    // Callback for sending audio - should be set by the parent component
    var onSendAudio: (String) -> Unit = { _ -> }

    private var recordingJob: Job? = null

    fun startRecording() {
        try {
            voiceRecorder.startRecording()
            _recorderState.value = VoiceRecorderState.Recording()
            startRecordingProgressTracker()
        } catch (e: Exception) {
            _recorderState.value = VoiceRecorderState.Idle
            // You might want to set an error state here
        }
    }

    fun stopRecording() {
        stopRecordingProgressTracker()
        val audioFilePath = voiceRecorder.stopRecording()
        _recorderState.value = if (audioFilePath != null) {
            VoiceRecorderState.Preview(audioFilePath = audioFilePath)
        } else {
            VoiceRecorderState.Idle
        }
    }

    fun cancelRecording() {
        stopRecordingProgressTracker()
        voiceRecorder.cancelRecording()
        _recorderState.value = VoiceRecorderState.Idle
    }

    fun sendAudioMessage() {
        val currentState = _recorderState.value
        if (currentState is VoiceRecorderState.Preview) {
            onSendAudio(currentState.audioFilePath)
            _recorderState.value = VoiceRecorderState.Idle
        }
    }

    fun deletePreview() {
        val currentState = _recorderState.value
        if (currentState is VoiceRecorderState.Preview) {
            // Delete the audio file
            try {
                File(currentState.audioFilePath).delete()
            } catch (e: Exception) {
                // Log error if needed
            }
            _recorderState.value = VoiceRecorderState.Idle
        }
    }

    // ✅ ADDED: Cleanup method for memory management
    fun cleanup() {
        stopRecordingProgressTracker()

        // Clean up any pending recordings
        when (val currentState = _recorderState.value) {
            is VoiceRecorderState.Recording -> {
                voiceRecorder.stopRecording()
            }
            is VoiceRecorderState.Preview -> {
                // Delete the audio file if it wasn't sent
                try {
                    File(currentState.audioFilePath).delete()
                } catch (e: Exception) {
                    // Log error if needed
                }
            }
            else -> {} // Idle state, nothing to clean up
        }

        // Call release if available
        if (voiceRecorder is MediaRecorderVoiceRecorder) {
            voiceRecorder.release()
        }

        _recorderState.value = VoiceRecorderState.Idle
    }

    private fun startRecordingProgressTracker() {
        recordingJob = viewModelScope.launch {
            val startTime = System.currentTimeMillis()

            while (isActive) {
                val elapsedTime = System.currentTimeMillis() - startTime
                val amplitude = modernRecorder?.getMaxAmplitude() ?: 0

                _recorderState.value = VoiceRecorderState.Recording(
                    elapsedTimeMs = elapsedTime,
                    maxAmplitude = amplitude
                )

                delay(100) // Update every 100ms for smooth waveform
            }
        }
    }

    private fun stopRecordingProgressTracker() {
        recordingJob?.cancel()
        recordingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        cleanup() // Use our cleanup method
    }
}