package com.metromessages.attachments

import android.net.Uri
import com.metromessages.data.local.MessageType

// Media type detection utilities
fun Uri.getMediaType(): MediaType {
    val path = this.toString().lowercase()
    return when {
        path.endsWith(".gif") -> MediaType.IMAGE
        path.contains(".mp4") || path.contains(".mov") ||
                path.contains(".avi") || path.contains(".mkv") ||
                path.contains(".webm") || path.contains(".3gp") -> MediaType.VIDEO
        path.contains(".jpg") || path.contains(".jpeg") ||
                path.contains(".png") || path.contains(".webp") -> MediaType.IMAGE
        path.contains(".mp3") || path.contains(".wav") ||
                path.contains(".aac") || path.contains(".ogg") ||
                path.contains(".flac") -> MediaType.AUDIO
        path.startsWith("http://") || path.startsWith("https://") -> MediaType.LINK
        else -> MediaType.FILE
    }
}

// Extension to check if media is previewable
fun Uri.isPreviewable(): Boolean {
    return when (getMediaType()) {
        MediaType.IMAGE, MediaType.VIDEO, MediaType.GIF -> true
        else -> false
    }
}

// Convert MessageType to simpler media category
fun MessageType.toMediaCategory(): String {
    return when (this) {
        MessageType.IMAGE -> "image"
        MessageType.VIDEO -> "video"
        MessageType.AUDIO -> "audio"
        MessageType.FILE -> "file"
        MessageType.LINK -> "link"
        MessageType.TEXT -> "text"
        MessageType.MEDIA -> "media"
    }
}

// Helper to get file extension from URI
fun Uri.getFileExtension(): String? {
    val path = this.toString()
    val lastDotIndex = path.lastIndexOf('.')
    if (lastDotIndex != -1 && lastDotIndex < path.length - 1) {
        return path.substring(lastDotIndex + 1).lowercase()
    }
    return null
}

