package com.metromessages.attachments

import android.net.Uri
import com.metromessages.data.local.metromessagehub.MessageType

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
// ALTERNATIVE: Without MEDIA in enum
fun MessageType.toMediaCategory(): String {
    return when (this) {
        MessageType.TEXT -> "text"
        MessageType.IMAGE -> "image"
        MessageType.VIDEO -> "video"
        MessageType.AUDIO -> "audio"
        MessageType.GIF -> "gif"
        MessageType.STICKER -> "sticker"
        MessageType.FILE -> "file"
        MessageType.LINK -> "link"
        MessageType.LOCATION -> "location"
        MessageType.CONTACT -> "contact"
        MessageType.OTP -> "otp"
        // No MEDIA case - it doesn't exist in your enum
        else -> "unknown"  // Handle any future enum additions
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

