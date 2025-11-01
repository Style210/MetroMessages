// File: AndroidMediaRepository.kt
package com.metromessages.attachments

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Date
import javax.inject.Inject

class AndroidMediaRepository @Inject constructor(
    private val context: Context
) : MediaRepository {

    override fun getRecentMedia(limit: Int): Flow<List<LocalMedia>> = flow {
        val images = mutableListOf<LocalMedia>()
        val videos = mutableListOf<LocalMedia>()

        // DEBUG: Log before queries
        Log.d("MediaRepository", "Querying images...")

        // Query images
        queryMedia(
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.IMAGE,
            limit = limit,
            resultList = images
        )

        // DEBUG: Log image results
        Log.d("MediaRepository", "Found ${images.size} images")

        // DEBUG: Log before video query
        Log.d("MediaRepository", "Querying videos...")

        // Query videos
        queryMedia(
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.VIDEO,
            limit = limit,
            resultList = videos
        )

        // DEBUG: Log video results
        Log.d("MediaRepository", "Found ${videos.size} videos")
        videos.forEach { video ->
            Log.d("MediaRepository", "Video: ${video.displayName}, date: ${video.dateTaken}, uri: ${video.uri}")
        }

        // Combine and ensure we get a mix of both images and videos
        val combined = (images + videos)
            .sortedByDescending { it.dateTaken }
            .take(limit)

        emit(combined)
    }.flowOn(Dispatchers.IO)

    override fun getNonEmptyAlbums(): Flow<List<LocalAlbum>> = flow {
        val albumsMap = mutableMapOf<Long, LocalAlbum>()

        // Query both images and videos for albums
        queryAlbums(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, albumsMap)
        queryAlbums(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, albumsMap)

        emit(albumsMap.values.toList())
    }.flowOn(Dispatchers.IO)

    override fun getMediaForAlbum(albumId: Long): Flow<List<LocalMedia>> = flow {
        val allMedia = mutableListOf<LocalMedia>()

        // Get images from album
        queryAlbumMedia(
            albumId = albumId,
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.IMAGE,
            resultList = allMedia
        )

        // Get videos from album
        queryAlbumMedia(
            albumId = albumId,
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            mediaType = MediaType.VIDEO,
            resultList = allMedia
        )

        // Sort by date
        val sortedMedia = allMedia.sortedByDescending { it.dateTaken }
        emit(sortedMedia)
    }.flowOn(Dispatchers.IO)

    override suspend fun getMediaUri(mediaId: Long, mediaType: MediaType): Uri? {
        val contentUri = when (mediaType) {
            MediaType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MediaType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            MediaType.LINK -> TODO()
            MediaType.AUDIO -> TODO()
            MediaType.FILE -> TODO()
            MediaType.GIF -> TODO()
        }
        return ContentUris.withAppendedId(contentUri, mediaId)
    }

    private fun queryMedia(
        contentUri: Uri,
        mediaType: MediaType,
        limit: Int,
        resultList: MutableList<LocalMedia>
    ) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.MediaColumns.DATE_TAKEN} DESC"

        // DEBUG: Log the query
        Log.d("MediaRepository", "Executing query for: $contentUri")

        val cursor = context.contentResolver.query(
            contentUri,
            projection,
            null,
            null,
            sortOrder
        )

        if (cursor == null) {
            Log.e("MediaRepository", "Query failed for: $contentUri")
            return
        }

        cursor.use {
            Log.d("MediaRepository", "Query successful, cursor count: ${cursor.count}")

            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

            var count = 0
            while (cursor.moveToNext() && count < limit) {
                val id = cursor.getLong(idColumn)
                val mediaUri = ContentUris.withAppendedId(contentUri, id)
                val displayName = cursor.getString(nameColumn)
                val dateTaken = Date(cursor.getLong(dateColumn))

                // DEBUG: Log each item found
                Log.d("MediaRepository", "Found $mediaType: $displayName, date: $dateTaken")

                resultList.add(LocalMedia(
                    id = id,
                    uri = mediaUri,
                    displayName = displayName,
                    dateTaken = dateTaken,
                    width = cursor.getInt(widthColumn),
                    height = cursor.getInt(heightColumn),
                    size = cursor.getLong(sizeColumn),
                    mediaType = mediaType,
                    albumId = cursor.getLong(bucketIdColumn),
                    albumName = cursor.getString(bucketNameColumn)
                ))
                count++
            }

            Log.d("MediaRepository", "Added $count $mediaType items")
        }
    }

    private fun queryAlbums(contentUri: Uri, albumsMap: MutableMap<Long, LocalAlbum>) {
        val projection = arrayOf(
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_TAKEN
        )

        context.contentResolver.query(
            contentUri,
            projection,
            null,
            null,
            "${MediaStore.MediaColumns.BUCKET_DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)

            while (cursor.moveToNext()) {
                val bucketId = cursor.getLong(bucketIdColumn)
                val bucketName = cursor.getString(bucketNameColumn)
                val lastUpdated = cursor.getLong(dateColumn)

                val mediaCount = getMediaCountForAlbum(bucketId, contentUri)

                if (mediaCount > 0) {
                    val existingAlbum = albumsMap[bucketId]
                    if (existingAlbum == null || lastUpdated > existingAlbum.lastUpdated) {
                        albumsMap[bucketId] = LocalAlbum(
                            id = bucketId,
                            name = bucketName,
                            coverUri = getAlbumCoverUri(bucketId, contentUri),
                            itemCount = mediaCount + (existingAlbum?.itemCount ?: 0),
                            lastUpdated = lastUpdated
                        )
                    } else {
                        existingAlbum.itemCount += mediaCount
                    }
                }
            }
        }
    }

    private fun queryAlbumMedia(
        albumId: Long,
        contentUri: Uri,
        mediaType: MediaType,
        resultList: MutableList<LocalMedia>
    ) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.SIZE
        )

        val selection = "${MediaStore.MediaColumns.BUCKET_ID} = ?"
        val selectionArgs = arrayOf(albumId.toString())
        val sortOrder = "${MediaStore.MediaColumns.DATE_TAKEN} DESC"

        context.contentResolver.query(
            contentUri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val mediaUri = ContentUris.withAppendedId(contentUri, id)

                resultList.add(LocalMedia(
                    id = id,
                    uri = mediaUri,
                    displayName = cursor.getString(nameColumn),
                    dateTaken = Date(cursor.getLong(dateColumn)),
                    width = cursor.getInt(widthColumn),
                    height = cursor.getInt(heightColumn),
                    size = cursor.getLong(sizeColumn),
                    mediaType = mediaType
                ))
            }
        }
    }

    private fun getMediaCountForAlbum(albumId: Long, contentUri: Uri): Int {
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.BUCKET_ID} = ?"
        val selectionArgs = arrayOf(albumId.toString())

        return context.contentResolver.query(
            contentUri,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor -> cursor.count } ?: 0
    }

    private fun getAlbumCoverUri(albumId: Long, contentUri: Uri): Uri? {
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.BUCKET_ID} = ?"
        val selectionArgs = arrayOf(albumId.toString())
        val sortOrder = "${MediaStore.MediaColumns.DATE_TAKEN} DESC"

        context.contentResolver.query(
            contentUri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                return ContentUris.withAppendedId(contentUri, id)
            }
        }
        return null
    }
}
