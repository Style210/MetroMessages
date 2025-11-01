// File: LocalAlbum.kt
package com.metromessages.attachments

import android.net.Uri

data class LocalAlbum(
    val id: Long,
    val name: String,
    val coverUri: Uri?,
    var itemCount: Int,
    val lastUpdated: Long
)

