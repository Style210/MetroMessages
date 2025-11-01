// File: MetroMediaPickerSheet.kt
package com.metromessages.attachments

import android.net.Uri
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberPlatformOverscrollFactory
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.metromessages.ui.components.MetroFont
import com.metromessages.ui.theme.MetroTab
import com.metromessages.ui.theme.MetroTabbedScreen
import com.metromessages.ui.theme.MetroTypography
import com.metromessages.ui.theme.formatFriendlyTimestamp
import com.metromessages.ui.thread.MetroEmptyState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetroMediaPickerSheet(
    onDismiss: () -> Unit,
    onMediaSelected: (List<Uri>) -> Unit,
    accentColor: Color,
    metroFont: MetroFont
) {
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()
    val viewModel: MediaAttachmentsViewModel = hiltViewModel()

    // Collect the state flows
    val currentAlbumId by viewModel.currentAlbumId.collectAsState()
    val isViewingAlbum by remember { derivedStateOf { currentAlbumId != null } }
    val selectedMedia by viewModel.selectedMedia.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val recentMedia by viewModel.recentMedia.collectAsState()
    val albumMedia by viewModel.albumMedia.collectAsState()

    val overscrollFactory = rememberPlatformOverscrollFactory(glowColor = accentColor)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Black,
        modifier = Modifier.fillMaxSize()
    ) {
        CompositionLocalProvider(LocalOverscrollFactory provides overscrollFactory) {
            if (isViewingAlbum) {
                // Show album content
                AlbumContentView(
                    viewModel = viewModel,
                    currentAlbumId = currentAlbumId,
                    selectedMedia = selectedMedia,
                    isMultiSelectMode = isMultiSelectMode,
                    albumMedia = albumMedia,
                    onMediaSelected = onMediaSelected,
                    onDismiss = onDismiss,
                    onBackClick = { viewModel.goBackToAlbums() },
                    accentColor = accentColor,
                    metroFont = metroFont
                )
            } else {
                // Show main tabs (recents and albums)
                MetroTabbedScreen(
                    tabs = listOf(
                        MetroTab(
                            title = "recents",
                            content = {
                                RecentMediaTab(
                                    viewModel = viewModel,
                                    selectedMedia = selectedMedia,
                                    isMultiSelectMode = isMultiSelectMode,
                                    recentMedia = recentMedia,
                                    onMediaSelected = onMediaSelected,
                                    onDismiss = onDismiss,
                                    accentColor = accentColor,
                                    metroFont = metroFont
                                )
                            }
                        ),
                        MetroTab(
                            title = "albums",
                            content = {
                                AlbumsTab(
                                    viewModel = viewModel,
                                    onAlbumSelected = { albumId ->
                                        viewModel.openAlbum(albumId)
                                    },
                                    accentColor = accentColor,
                                    metroFont = metroFont
                                )
                            }
                        )
                    ),
                    screenTitle = "media",
                    onScreenTitleClick = {
                        coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier.background(Color.Black)
                )
            }
        }
    }
}

@Composable
private fun RecentMediaTab(
    viewModel: MediaAttachmentsViewModel,
    selectedMedia: Set<Long>,
    isMultiSelectMode: Boolean,
    recentMedia: List<LocalMedia>,
    onMediaSelected: (List<Uri>) -> Unit,
    onDismiss: () -> Unit,
    accentColor: Color,
    metroFont: MetroFont
) {
    Column(Modifier.fillMaxSize()) {
        // Header with Done button for multi-select mode
        if (isMultiSelectMode && selectedMedia.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${selectedMedia.size} selected",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MetroTypography.MetroSubhead(metroFont)
                )

                Text(
                    text = "done",
                    modifier = Modifier
                        .clickable {
                            val uris = viewModel.getSelectedMediaUris()
                            onMediaSelected(uris)
                            onDismiss()
                        }
                        .padding(8.dp),
                    color = accentColor,
                    style = MetroTypography.MetroSubhead(metroFont)
                )
            }
        }

        if (recentMedia.isEmpty()) {
            MetroEmptyState(
                primaryText = "no media",
                showActionPrompt = false,
                accentColor = accentColor,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
                contentPadding = PaddingValues(
                    top = if (isMultiSelectMode && selectedMedia.isNotEmpty()) 0.dp else 8.dp,
                    bottom = 8.dp
                )
            ) {
                items(recentMedia) { media ->
                    MediaGridItem(
                        media = media,
                        isSelected = selectedMedia.contains(media.id),
                        isMultiSelectMode = isMultiSelectMode,
                        onClick = {
                            if (!isMultiSelectMode) {
                                onMediaSelected(listOf(media.uri))
                                onDismiss()
                            } else {
                                viewModel.toggleMediaSelection(media.id)
                            }
                        },
                        onLongPress = {
                            viewModel.toggleMediaSelection(media.id)
                        },
                        accentColor = accentColor,
                        metroFont = metroFont
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumsTab(
    viewModel: MediaAttachmentsViewModel,
    onAlbumSelected: (Long) -> Unit,
    accentColor: Color,
    metroFont: MetroFont
) {
    val albums by viewModel.albums.collectAsState()

    if (albums.isEmpty()) {
        MetroEmptyState(
            primaryText = "no albums",
            showActionPrompt = false,
            accentColor = accentColor,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(albums) { album ->
                AlbumGridItem(
                    album = album,
                    onClick = { onAlbumSelected(album.id) },
                    accentColor = accentColor,
                    metroFont = metroFont
                )
            }
        }
    }
}

@Composable
private fun AlbumContentView(
    viewModel: MediaAttachmentsViewModel,
    currentAlbumId: Long?,
    selectedMedia: Set<Long>,
    isMultiSelectMode: Boolean,
    albumMedia: List<LocalMedia>,
    onMediaSelected: (List<Uri>) -> Unit,
    onDismiss: () -> Unit,
    onBackClick: () -> Unit,
    accentColor: Color,
    metroFont: MetroFont
) {
    val albums by viewModel.albums.collectAsState()

    val currentAlbum = albums.find { it.id == currentAlbumId }
    val headerText = currentAlbum?.name ?: "album"

    Column(Modifier.fillMaxSize()) {
        // Header with back button - FIXED layout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // PROPER BACK ICON (Material Design)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = accentColor,
                modifier = Modifier
                    .clickable { onBackClick() }
                    .padding(end = 16.dp)
                    .size(24.dp)
            )

            Text(
                text = headerText,
                color = Color.White,
                style = MetroTypography.MetroSubhead(metroFont)
            )

            // Add a "Done" button when in multi-select mode
            if (selectedMedia.isNotEmpty()) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = "done",
                    modifier = Modifier
                        .clickable {
                            val uris = viewModel.getSelectedMediaUris()
                            onMediaSelected(uris)
                            onDismiss()
                        }
                        .padding(8.dp),
                    color = accentColor,
                    style = MetroTypography.MetroSubhead(metroFont)
                )
            }
        }

        // Album media grid
        if (albumMedia.isEmpty()) {
            MetroEmptyState(
                primaryText = "no media",
                showActionPrompt = false,
                accentColor = accentColor,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                horizontalArrangement = Arrangement.spacedBy(1.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(albumMedia) { media ->
                    MediaGridItem(
                        media = media,
                        isSelected = selectedMedia.contains(media.id),
                        isMultiSelectMode = isMultiSelectMode,
                        onClick = {
                            if (!isMultiSelectMode) {
                                onMediaSelected(listOf(media.uri))
                                onDismiss()
                            } else {
                                viewModel.toggleMediaSelection(media.id)
                            }
                        },
                        onLongPress = {
                            viewModel.toggleMediaSelection(media.id)
                        },
                        accentColor = accentColor,
                        metroFont = metroFont
                    )
                }
            }
        }
    }
}


@Composable
private fun MediaGridItem(
    media: LocalMedia,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    accentColor: Color,
    metroFont: MetroFont
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        contentAlignment = Alignment.Center
    ) {
        // Use Coil for both images and video thumbnails with VideoFrameDecoder
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(media.uri)
                .apply {
                    if (media.mediaType == MediaType.VIDEO) {
                        decoderFactory(VideoFrameDecoder.Factory())
                    }
                }
                .crossfade(true)
                .build(),
            contentDescription = if (media.mediaType == MediaType.IMAGE) "Image thumbnail" else "Video thumbnail",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Video indicator (if it's a video)
        if (media.mediaType == MediaType.VIDEO) {
            Text(
                text = "▶",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    .padding(4.dp)
            )
        }

        // SELECTION INDICATOR - Show when selected or in multi-select mode
        if (isSelected) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(3.dp, accentColor, RoundedCornerShape(4.dp))
                    .background(accentColor.copy(alpha = 0.2f))
            )
            Text(
                text = "✓",
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .background(accentColor, CircleShape)
                    .padding(4.dp)
            )
        } else if (isMultiSelectMode) {
            // Show empty selection indicator when in multi-select mode but not selected
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            )
        }

        // Timestamp overlay
        Text(
            text = formatFriendlyTimestamp(media.dateTaken.time),
            color = Color.White.copy(alpha = 0.7f),
            style = MetroTypography.MetroCaption(metroFont),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun AlbumGridItem(
    album: LocalAlbum,
    onClick: () -> Unit,
    accentColor: Color,
    metroFont: MetroFont
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(4.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // If album has a cover image, use it instead of folder icon
            album.coverUri?.let { coverUri ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } ?: run {
                // Fallback to folder icon if no cover
                Text(
                    text = "📁",
                    style = MetroTypography.MetroBody2(metroFont),
                    fontSize = 32.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }

            // Item count badge
            Text(
                text = "${album.itemCount}",
                color = Color.White.copy(alpha = 0.7f),
                style = MetroTypography.MetroCaption(metroFont),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = album.name,
            color = Color.White.copy(alpha = 0.9f),
            style = MetroTypography.MetroBody2(metroFont),
            maxLines = 1
        )

        Text(
            text = "Updated ${formatFriendlyTimestamp(album.lastUpdated)}",
            color = Color.White.copy(alpha = 0.4f),
            style = MetroTypography.MetroCaption(metroFont),
            maxLines = 1
        )
    }
}
