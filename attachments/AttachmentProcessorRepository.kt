package com.metromessages.attachments

import android.content.Context
import android.net.Uri
import com.metromessages.data.local.metromessagehub.MessageType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentProcessorRepository @Inject constructor(
    private val context: Context,
    private val fileHelper: FileHelper
) {
    private val _processingProgress = MutableStateFlow<Map<Uri, Float>>(emptyMap())
    val processingProgress: StateFlow<Map<Uri, Float>> = _processingProgress

    private val _filesToCleanup = mutableSetOf<Uri>()

    suspend fun processMediaAttachments(uris: List<Uri>): List<ProcessedAttachment> {
        val results = mutableListOf<ProcessedAttachment>()
        val errors = mutableListOf<String>()

        uris.forEach { uri ->
            when (val result = fileHelper.copyUriToAppStorage(context, uri)) {
                is FileHelper.FileResult.Success -> {
                    val mediaType = getMessageTypeFromUri(uri)
                    results.add(ProcessedAttachment(result.uri, mediaType))
                    _filesToCleanup.add(result.uri)
                }
                is FileHelper.FileResult.Error -> {
                    errors.add("Failed to process ${uri.lastPathSegment}: ${result.message}")
                }
            }
        }

        if (errors.isNotEmpty()) {
            throw AttachmentProcessingException(errors.joinToString(", "))
        }

        return results
    }

    suspend fun cleanupTempFiles() {
        fileHelper.cleanupFiles(_filesToCleanup.toList())
        _filesToCleanup.clear()
    }

    fun markFilesForPersistence(uris: List<Uri>) {
        _filesToCleanup.removeAll(uris.toSet())
    }

    private fun getMessageTypeFromUri(uri: Uri): MessageType {
        val mimeType = context.contentResolver.getType(uri)
        return getMessageTypeFromMimeType(mimeType)
    }

    private fun getMessageTypeFromMimeType(mimeType: String?): MessageType {
        return when {
            mimeType?.startsWith("image/gif") == true -> MessageType.IMAGE
            mimeType?.startsWith("image/") == true -> MessageType.IMAGE
            mimeType?.startsWith("video/") == true -> MessageType.VIDEO
            mimeType?.startsWith("audio/") == true -> MessageType.AUDIO
            else -> MessageType.FILE
        }
    }

    suspend fun <T> withRetry(
        maxRetries: Int = 3,
        initialDelay: Long = 1000,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                if (attempt == maxRetries - 1) throw e
                kotlinx.coroutines.delay(currentDelay)
                currentDelay *= 2
            }
        }
        throw IllegalStateException("Unreachable")
    }
}

data class ProcessedAttachment(
    val uri: Uri,
    val type: MessageType
)

class AttachmentProcessingException(message: String) : Exception(message)
