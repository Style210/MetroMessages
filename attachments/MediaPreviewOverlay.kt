package com.metromessages.attachments

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun MediaPreviewOverlay(
    isVisible: Boolean,
    mediaUris: List<Uri>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit,
    onShare: (Uri) -> Unit,
    onDownload: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible || mediaUris.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, mediaUris.size - 1),
        pageCount = { mediaUris.size }
    )

    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.9f) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            launch { alphaAnim.animateTo(1f, tween(250)) }
            launch { scaleAnim.animateTo(1f, tween(300)) }
        } else {
            launch { scaleAnim.animateTo(0.9f, tween(200)) }
            launch { alphaAnim.animateTo(0f, tween(200)) }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f * alphaAnim.value))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            },
        contentAlignment = Alignment.Center
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                    alpha = alphaAnim.value
                }
        ) { page ->
            val currentUri = mediaUris[page]

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onDismiss() })
                    },
                contentAlignment = Alignment.Center
            ) {
                when {
                    currentUri.toString().contains(".mp4") ||
                            currentUri.toString().contains(".mov") -> {
                        VideoPlayerView(
                            uri = currentUri,
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .fillMaxHeight(0.95f)
                        )
                    }
                    else -> {
                        ImageViewer(
                            uri = currentUri,
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .fillMaxHeight(0.95f)
                        )
                    }
                }
            }
        }

        // Metro-style action buttons (bottom-right corner)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Row {
                // Download button
                IconButton(
                    onClick = {
                        val currentUri = mediaUris[pagerState.currentPage]
                        onDownload(currentUri)
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = Color.White
                    )
                }

                // Share button
                IconButton(
                    onClick = {
                        val currentUri = mediaUris[pagerState.currentPage]
                        onShare(currentUri)
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White
                    )
                }
            }
        }
    }
}