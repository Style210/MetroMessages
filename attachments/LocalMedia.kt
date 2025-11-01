// File: LocalImage.kt → Rename to LocalMedia.kt
package com.metromessages.attachments

import android.net.Uri
import java.util.Date

// Rename to LocalMedia and add mediaType
data class LocalMedia(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateTaken: Date,
    val width: Int,
    val height: Int,
    val size: Long,
    val mediaType: MediaType, // NEW: Track if it's image or video
    val albumId: Long? = null,
    val albumName: String? = null
)

// NEW: Enum to distinguish media types
enum class MediaType {
    IMAGE, VIDEO, LINK, AUDIO, FILE, GIF
}

