package com.metromessages.data.model.facebook

import android.graphics.Rect
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.metromessages.data.local.MessageType
import com.metromessages.ui.components.MetroFont
import com.metromessages.ui.theme.MetroTypography
import com.metromessages.ui.theme.formatFriendlyTimestamp
import com.metromessages.voicerecorder.MessageAudioPlayer
import kotlin.math.roundToInt

private val DefaultMetroAccent = Color(0xFF0063B1)

@Composable
fun FacebookMessageBubble(
    message: FacebookUiMessage,
    modifier: Modifier = Modifier,
    accentColor: Color = DefaultMetroAccent,
    metroFont: MetroFont = MetroFont.Segoe,
    onMediaClick: ((Uri, Rect) -> Unit)? = null // NEW: Media click handler
) {
    val context = LocalContext.current
    val localView = LocalView.current
    val density = LocalDensity.current

    // Track view bounds for smooth transitions
    var viewBounds by remember { mutableStateOf(Rect()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = if (message.isSentByUser) Alignment.End else Alignment.Start
    ) {
        when (message.messageType) {
            MessageType.TEXT -> {
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(RectangleShape)
                        .background(
                            color = if (message.isSentByUser) accentColor
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = message.body,
                        color = if (message.isSentByUser) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.widthIn(max = 280.dp),
                        style = MetroTypography.MetroBody2(metroFont),
                    )
                }
            }
            MessageType.AUDIO -> {
                MessageAudioPlayer(
                    audioPath = message.body,
                    isOwnMessage = message.isSentByUser,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            MessageType.IMAGE -> {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Gray.copy(alpha = 0.3f))
                        .onGloballyPositioned { coordinates ->
                            // Calculate screen bounds for smooth transition
                            val location = IntArray(2)
                            localView.getLocationOnScreen(location)
                            viewBounds = Rect(
                                location[0] + coordinates.positionInWindow().x.roundToInt(),
                                location[1] + coordinates.positionInWindow().y.roundToInt(),
                                location[0] + coordinates.positionInWindow().x.roundToInt() + coordinates.size.width,
                                location[1] + coordinates.positionInWindow().y.roundToInt() + coordinates.size.height
                            )
                        }
                        .clickable(
                            enabled = onMediaClick != null && message.mediaUri != null,
                            onClick = {
                                message.mediaUri?.let { uri ->
                                    onMediaClick?.invoke(uri, viewBounds)
                                }
                            }
                        )
                ) {
                    if (message.mediaUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(message.mediaUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Sent image",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = "📷 Image not available",
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
            MessageType.VIDEO -> {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .onGloballyPositioned { coordinates ->
                            // Calculate screen bounds for smooth transition
                            val location = IntArray(2)
                            localView.getLocationOnScreen(location)
                            viewBounds = Rect(
                                location[0] + coordinates.positionInWindow().x.roundToInt(),
                                location[1] + coordinates.positionInWindow().y.roundToInt(),
                                location[0] + coordinates.positionInWindow().x.roundToInt() + coordinates.size.width,
                                location[1] + coordinates.positionInWindow().y.roundToInt() + coordinates.size.height
                            )
                        }
                        .clickable(
                            enabled = onMediaClick != null && message.mediaUri != null,
                            onClick = {
                                message.mediaUri?.let { uri ->
                                    onMediaClick?.invoke(uri, viewBounds)
                                }
                            }
                        )
                ) {
                    if (message.mediaUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(message.mediaUri)
                                .decoderFactory(VideoFrameDecoder.Factory())
                                .crossfade(true)
                                .build(),
                            contentDescription = "Video thumbnail",
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play video",
                                tint = Color.White,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(48.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "🎥 Video not available",
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
            MessageType.FILE -> {
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(RectangleShape)
                        .background(
                            color = if (message.isSentByUser) accentColor
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "📄 ${message.body}",
                        color = if (message.isSentByUser) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.widthIn(max = 280.dp),
                        style = MetroTypography.MetroBody2(metroFont),
                    )
                }
            }
            MessageType.LINK -> {
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(RectangleShape)
                        .background(
                            color = if (message.isSentByUser) accentColor
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "🔗 ${message.body}",
                        color = if (message.isSentByUser) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.widthIn(max = 280.dp),
                        style = MetroTypography.MetroBody2(metroFont),
                    )
                }
            }
            MessageType.MEDIA -> {
                Box(
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(RectangleShape)
                        .background(
                            color = if (message.isSentByUser) accentColor
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "📦 ${message.body}",
                        color = if (message.isSentByUser) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.widthIn(max = 280.dp),
                        style = MetroTypography.MetroBody2(metroFont),
                    )
                }
            }
        }

        Text(
            text = formatFriendlyTimestamp(message.timestamp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier
                .padding(top = 2.dp)
                .align(if (message.isSentByUser) Alignment.End else Alignment.Start),
            maxLines = 1,
            overflow = TextOverflow.Visible,
            style = MetroTypography.MetroCaption(metroFont),
        )
    }
}


