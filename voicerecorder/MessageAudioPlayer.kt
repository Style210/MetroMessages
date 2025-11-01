// File: MessageAudioPlayer.kt
package com.metromessages.voicerecorder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.metromessages.data.settingsscreen.SettingsPreferences
import kotlinx.coroutines.delay

@Composable
fun MessageAudioPlayer(
    audioPath: String,
    isOwnMessage: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val settingsPrefs = remember { SettingsPreferences(context) }
    val accentColor by settingsPrefs.accentColorFlow.collectAsState(initial = Color(0xFF00BFFF))

    val player = remember(audioPath) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(audioPath.toUri()))
            prepare()
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var boxSize by remember { mutableStateOf(Size.Zero) }

    // Player state listeners
    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> isLoading = false
                    Player.STATE_ENDED -> isPlaying = false
                    Player.STATE_BUFFERING -> isLoading = true
                    Player.STATE_IDLE -> {} // Handle idle state if needed
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                errorMessage = "Failed to play audio"
                isLoading = false
            }
        }

        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            if (isPlaying) {
                player.pause()
            }
            player.release()
        }
    }

    // Progress update coroutine
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            player.play()
            while (isPlaying) {
                val duration = player.duration.coerceAtLeast(1L)
                progress = player.currentPosition.toFloat() / duration
                delay(100L)
                // Check if player is still playing in case it ended during delay
                if (!player.isPlaying) {
                    isPlaying = false
                }
            }
        } else {
            player.pause()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.small)
            .background(
                if (isOwnMessage) accentColor.copy(alpha = 0.1f)
                else Color.Gray.copy(alpha = 0.1f)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play/Pause Button
        IconButton(
            onClick = {
                isPlaying = !isPlaying
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            enabled = !isLoading && errorMessage == null
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = if (isOwnMessage) accentColor else Color.Gray,
                        strokeWidth = 2.dp
                    )
                }
                errorMessage != null -> {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Error - Tap to retry",
                        tint = Color.Red
                    )
                }
                else -> {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause audio" else "Play audio",
                        tint = if (isOwnMessage) accentColor else Color.Gray
                    )
                }
            }
        }

        // Progress and Duration
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            // Error message
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Progress bar with seek functionality
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .onSizeChanged { size -> boxSize = size.toSize() }
                    .clickable(
                        enabled = !isLoading && errorMessage == null
                    ) {
                        // Calculate seek position based on click coordinates
                        // Note: This is a simplified version - you might need more complex
                        // logic for precise seeking based on touch position
                        val currentPosition = player.currentPosition
                        val duration = player.duration
                        if (currentPosition + 10000 < duration) {
                            player.seekTo(currentPosition + 10000) // Seek forward 10 seconds
                        } else {
                            player.seekTo(duration) // Seek to end
                        }
                    }
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = accentColor,
                    trackColor = accentColor.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Duration text
            Text(
                text = formatMillis(if (isPlaying) player.currentPosition else player.duration),
                fontSize = 12.sp,
                color = if (isOwnMessage) accentColor else Color.Gray,
                modifier = Modifier.semantics {
                    contentDescription = "Audio duration: ${
                        formatMillis(if (isPlaying) player.currentPosition else player.duration)
                    }"
                }
            )
        }
    }
}

private fun formatMillis(millis: Long): String {
    if (millis == Long.MAX_VALUE || millis < 0) return "--:--"

    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

