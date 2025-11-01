package com.metromessages.attachments

import android.net.Uri
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.platform.LocalContext

@Composable
fun VideoPlayerView(
    uri: Uri,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = ExoPlayer.Builder(context).build().apply {
        setMediaItem(MediaItem.fromUri(uri))
        prepare()
        playWhenReady = true
    }

    DisposableEffect(uri) { onDispose { exoPlayer.release() } }

    AndroidView(
        modifier = modifier.size(300.dp),
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = false
            }
        }
    )
}

