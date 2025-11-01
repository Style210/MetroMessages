// File: MediaRepository.kt
package com.metromessages.attachments

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getRecentMedia(limit: Int = 100): Flow<List<LocalMedia>>
    fun getNonEmptyAlbums(): Flow<List<LocalAlbum>>
    fun getMediaForAlbum(albumId: Long): Flow<List<LocalMedia>>
    suspend fun getMediaUri(mediaId: Long, mediaType: MediaType): Uri?
}

