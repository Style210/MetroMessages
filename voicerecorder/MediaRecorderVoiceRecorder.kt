// File: MediaRecorderVoiceRecorder.kt
package com.metromessages.voicerecorder

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * A robust VoiceRecorder implementation using the standard Android MediaRecorder.
 *
 * ### Important Note on Deprecation:
 * The `android.media.MediaRecorder` class is deprecated in API level 34, but a stable
 * replacement in the AndroidX libraries is not yet available. This class is the current
 * best practice for audio recording and will be supported for many years.
 * The deprecation warning is suppressed intentionally.
 */
@SuppressLint("Deprecation") // Using the stable, standard API intentionally.
class MediaRecorderVoiceRecorder(
    private val context: Context
) : VoiceRecorder {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null

    override fun startRecording() {
        // 1. Guard against double-start
        if (isRecording()) {
            Log.w(TAG, "startRecording: Ignoring, already recording.")
            return
        }

        // 2. Create audio file
        outputFile = try {
            createAudioFile().also { it.createNewFile() }
        } catch (e: IOException) {
            Log.e(TAG, "startRecording: Failed to create audio file", e)
            return
        }

        // 3. Create, configure, and start MediaRecorder
        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile?.absolutePath)

                prepare()
                start()
            }
            Log.d(TAG, "startRecording: Success. File: ${outputFile?.absolutePath}")
        } catch (e: IllegalStateException) {
            Log.e(TAG, "startRecording: MediaRecorder illegal state", e)
            cleanup(deleteFile = true)
        } catch (e: IOException) {
            Log.e(TAG, "startRecording: MediaRecorder preparation failed", e)
            cleanup(deleteFile = true)
        } catch (e: RuntimeException) {
            Log.e(TAG, "startRecording: MediaRecorder start failed", e)
            cleanup(deleteFile = true)
        } catch (e: Exception) {
            Log.e(TAG, "startRecording: Unexpected error", e)
            cleanup(deleteFile = true)
        }
    }

    override fun stopRecording(): String? {
        if (!isRecording()) {
            Log.w(TAG, "stopRecording: Ignoring, not currently recording.")
            return null
        }

        return try {
            mediaRecorder?.stop()
            val savedFilePath = outputFile?.absolutePath
            Log.d(TAG, "stopRecording: Success. Saved to: $savedFilePath")
            savedFilePath
        } catch (e: IllegalStateException) {
            Log.e(TAG, "stopRecording: MediaRecorder was not recording", e)
            null
        } catch (e: RuntimeException) {
            Log.e(TAG, "stopRecording: MediaRecorder stop failed", e)
            null
        } finally {
            // Always cleanup the MediaRecorder object, but keep the file
            cleanup(deleteFile = false)
        }
    }

    override fun cancelRecording() {
        Log.d(TAG, "cancelRecording: Explicit cancellation requested.")
        cleanup(deleteFile = true) // Delete the file when cancelling
    }

    override fun isRecording(): Boolean = mediaRecorder != null

    /**
     * Gets the current maximum amplitude from the active recording.
     * This is a key feature for providing real-time waveform visualization in the UI.
     * @return The maximum amplitude since the last call, or 0 if not recording.
     */
    internal fun getMaxAmplitude(): Int {
        return if (isRecording()) {
            try {
                mediaRecorder?.maxAmplitude ?: 0
            } catch (e: Exception) {
                0
            }
        } else {
            0
        }
    }

    /**
     * Releases all resources used by this recorder. This should be called when the
     * recorder is no longer needed (e.g., in the owning ViewModel's onCleared() method).
     * Any active recording will be cancelled and its file deleted.
     */
    fun release() {
        Log.d(TAG, "release: Cleaning up resources.")
        cleanup(deleteFile = true)
    }

    /**
     * Internal cleanup method.
     * @param deleteFile If true, the recorded file will be deleted (useful for cancellation, errors, or release).
     */
    private fun cleanup(deleteFile: Boolean) {
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "cleanup: Error releasing MediaRecorder", e)
        } finally {
            mediaRecorder = null
        }

        if (deleteFile) {
            try {
                val deleted = outputFile?.delete() ?: false
                if (deleted) {
                    Log.d(TAG, "cleanup: Deleted temporary file: ${outputFile?.absolutePath}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "cleanup: Could not delete temporary file", e)
            }
        }
        outputFile = null
    }

    private fun createAudioFile(): File {
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: throw IOException("External storage (Music directory) is not available")

        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        return File.createTempFile(
            "VM_${System.currentTimeMillis()}_",
            ".m4a",
            storageDir
        )
    }

    companion object {
        private const val TAG = "MediaRecorderVoiceRecorder"
        const val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 1001

    }
}