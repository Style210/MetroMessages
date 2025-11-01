package com.metromessages.attachments

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.StatFs
import android.provider.OpenableColumns
import android.util.Log
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object FileHelper {
    private const val TAG = "FileHelper"
    private const val MAX_FILE_SIZE_MB = 25 // 25MB limit
    private const val BUFFER_SIZE = 32768 // ↑ Increased to 32KB for better performance
    private const val THUMBNAIL_SIZE = 100 // Thumbnail dimensions

    sealed class FileResult {
        data class Success(val uri: Uri, val fileSize: Long) : FileResult()
        data class Error(val message: String, val originalUri: Uri) : FileResult()
    }

    suspend fun copyUriToAppStorage(context: Context, uri: Uri): FileResult {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val contentResolver = context.contentResolver

                // 1. Quick MIME type validation
                val mimeType = contentResolver.getType(uri)
                if (!isValidMimeType(mimeType)) {
                    val failTime = System.currentTimeMillis() - startTime
                    Log.w(TAG, "MIME validation failed in ${failTime}ms")
                    return@withContext FileResult.Error("Unsupported file type: $mimeType", uri)
                }

                // 2. Get file info for validation
                val (fileName, fileSize) = getFileInfo(contentResolver, uri)

                // 3. Quick size validation (before copy)
                if (!isFileSizeValid(fileSize)) {
                    val failTime = System.currentTimeMillis() - startTime
                    Log.w(TAG, "Size validation failed in ${failTime}ms")
                    return@withContext FileResult.Error("File too large: ${fileSize / (1024 * 1024)}MB", uri)
                }

                // 4. Check disk space
                if (!hasEnoughDiskSpace(context, fileSize)) {
                    val failTime = System.currentTimeMillis() - startTime
                    Log.w(TAG, "Disk space check failed in ${failTime}ms")
                    return@withContext FileResult.Error("Insufficient disk space", uri)
                }

                // 5. Create output file
                val fileExtension = getFileExtension(fileName) ?: getExtensionFromMimeType(mimeType) ?: "dat"
                val outputDir = context.filesDir
                val outputFile = File.createTempFile(
                    "media_${System.currentTimeMillis()}_",
                    ".$fileExtension",
                    outputDir
                )

                // 6. Copy with optimized buffer
                copyWithBuffer(contentResolver, uri, outputFile)

                // 7. Verify copy was successful
                if (!outputFile.exists() || outputFile.length() == 0L) {
                    outputFile.delete()
                    val failTime = System.currentTimeMillis() - startTime
                    Log.e(TAG, "Copy verification failed in ${failTime}ms")
                    return@withContext FileResult.Error("Failed to copy file", uri)
                }

                val copyTime = System.currentTimeMillis() - startTime
                val fileSizeKB = outputFile.length() / 1024
                Log.d(TAG, "Successfully copied ${fileSizeKB}KB in ${copyTime}ms to ${outputFile.name}")

                FileResult.Success(Uri.fromFile(outputFile), outputFile.length())

            } catch (e: Exception) {
                val failTime = System.currentTimeMillis() - startTime
                Log.e(TAG, "Error copying file in ${failTime}ms: ${e.message}", e)
                FileResult.Error("Copy failed: ${e.message}", uri)
            }
        }
    }

    suspend fun generateVideoThumbnail(context: Context, videoUri: Uri): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                // Create a temporary file for the thumbnail
                val thumbnailFile = File.createTempFile(
                    "video_thumb_${System.currentTimeMillis()}",
                    ".jpg",
                    context.cacheDir
                )

                // Use Coil with VideoFrameDecoder (same as MediaPicker and MessageBubble)
                val imageLoader = ImageLoader.Builder(context)
                    .components {
                        add(VideoFrameDecoder.Factory())
                    }
                    .build()

                // Load the video frame using the same setup as other components
                val result = imageLoader.execute(
                    ImageRequest.Builder(context)
                        .data(videoUri)
                        .decoderFactory(VideoFrameDecoder.Factory())
                        .size(THUMBNAIL_SIZE, THUMBNAIL_SIZE)
                        .build()
                )

                val drawable = result.drawable
                if (drawable is BitmapDrawable) {
                    // Save the bitmap to file
                    thumbnailFile.outputStream().use { output ->
                        drawable.bitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
                    }
                    Log.d(TAG, "Generated video thumbnail: ${thumbnailFile.name}")
                    Uri.fromFile(thumbnailFile)
                } else {
                    Log.w(TAG, "Failed to generate video thumbnail - drawable is not BitmapDrawable")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate video thumbnail for $videoUri", e)
                null
            }
        }
    }

    private fun copyWithBuffer(contentResolver: ContentResolver, uri: Uri, outputFile: File) {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(outputFile).use { outputStream ->
                val buffer = ByteArray(BUFFER_SIZE) // 32KB buffer for better performance
                var bytesRead: Int
                var totalBytesRead = 0L
                val totalSize = getFileSize(contentResolver, uri)
                var lastProgressLog = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead

                    // Progress logging (less frequent to reduce overhead)
                    if (totalSize > 0) {
                        val progress = (totalBytesRead * 100 / totalSize).toInt()
                        if (progress >= lastProgressLog + 25) { // Log every 25%
                            Log.d(TAG, "Copy progress: $progress%")
                            lastProgressLog = progress
                        }
                    }
                }
                outputStream.flush() // Ensure all data is written to disk
            }
        }
    }

    private fun getFileInfo(contentResolver: ContentResolver, uri: Uri): Pair<String, Long> {
        var name = "file_${System.currentTimeMillis()}"
        var size = 0L

        // Use a targeted query for better performance
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
            }
        }

        return Pair(name, size)
    }

    private fun getFileSize(contentResolver: ContentResolver, uri: Uri): Long {
        // Optimized query for just the size
        contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) return cursor.getLong(sizeIndex)
            }
        }
        return 0L
    }

    private fun isValidMimeType(mimeType: String?): Boolean {
        if (mimeType == null) return false

        // Quick prefix check first (faster than set containment)
        return when {
            mimeType.startsWith("image/") -> true
            mimeType.startsWith("video/") -> true
            mimeType.startsWith("audio/") -> true
            else -> false
        }
    }

    private fun isFileSizeValid(fileSize: Long): Boolean {
        val sizeInMb = fileSize / (1024 * 1024)
        return sizeInMb <= MAX_FILE_SIZE_MB
    }

    private fun hasEnoughDiskSpace(context: Context, requiredSize: Long): Boolean {
        val filesDir = context.filesDir
        val stat = StatFs(filesDir.absolutePath)
        val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
        return availableBytes > requiredSize * 2 // 2x buffer for safety
    }

    private fun getFileExtension(fileName: String): String? {
        val lastDotIndex = fileName.lastIndexOf('.')
        if (lastDotIndex != -1 && lastDotIndex < fileName.length - 1) {
            return fileName.substring(lastDotIndex + 1).lowercase()
        }
        return null
    }

    private fun getExtensionFromMimeType(mimeType: String?): String? {
        if (mimeType == null) return null

        // Fast path for common types
        return when {
            mimeType.contains("jpeg") || mimeType.contains("jpg") -> "jpg"
            mimeType.contains("png") -> "png"
            mimeType.contains("gif") -> "gif"
            mimeType.contains("webp") -> "webp"
            mimeType.contains("mp4") -> "mp4"
            mimeType.contains("quicktime") -> "mov"
            mimeType.contains("3gpp") -> "3gp"
            mimeType.contains("webm") -> "webm"
            mimeType.contains("mpeg") -> "mp3"
            mimeType.contains("aac") -> "aac"
            mimeType.contains("wav") -> "wav"
            mimeType.contains("ogg") -> "ogg"
            else -> null
        }
    }

    suspend fun cleanupFiles(fileUris: List<Uri>) {
        withContext(Dispatchers.IO) {
            fileUris.forEach { uri ->
                try {
                    // Handle both file:// and content:// URIs safely
                    val filePath = uri.path ?: return@forEach
                    if (uri.scheme == "file") {
                        val file = File(filePath)
                        if (file.exists() && file.delete()) {
                            Log.d(TAG, "Cleaned up file: ${file.name}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error cleaning up file ${uri.lastPathSegment}: ${e.message}")
                }
            }
        }
    }

    // New function for quick validation without full processing
    suspend fun validateUriQuick(context: Context, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri)
                val (_, size) = getFileInfo(contentResolver, uri)

                isValidMimeType(mimeType) && isFileSizeValid(size) && hasEnoughDiskSpace(context, size)
            } catch (e: Exception) {
                false
            }
        }
    }

    // Helper function to check if URI is a video
    fun isVideoUri(uri: Uri): Boolean {
        val uriString = uri.toString().lowercase()
        return uriString.contains(".mp4") || uriString.contains(".mov") ||
                uriString.contains(".avi") || uriString.contains(".mkv") ||
                uriString.contains(".3gp") || uriString.contains(".webm")
    }
}

