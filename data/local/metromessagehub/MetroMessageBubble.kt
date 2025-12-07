// File: com.metromessages.data.local.metromessagehub.MetroMessageBubble.kt
package com.metromessages.data.local.metromessagehub

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.metromessages.ui.components.MetroFont
import com.metromessages.ui.theme.MetroTypography
import com.metromessages.ui.theme.formatFriendlyTimestamp

/**
 * FOSSIFY-COMPLIANT: Direct MetroMessage display component
 * No wrapper models, no unnecessary abstraction
 * Uses MetroMessage directly like Fossify uses Message
 */
@Composable
fun MetroMessageBubble(
    message: MetroMessage,
    modifier: Modifier = Modifier,
    metroFont: MetroFont = MetroFont.Segoe,
    onMediaClick: ((Uri) -> Unit)? = null // Simplified: No bounds tracking like Fossify
) {
    val context = LocalContext.current

    // ✅ FOSSIFY PATTERN: Direct property usage
    val isIncoming = message.isIncoming
    val bubbleColor = if (isIncoming) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
    } else {
        Color(0xFF0063B1) // Metro accent color
    }

    val textColor = if (isIncoming) {
        MaterialTheme.colorScheme.onSurface
    } else {
        Color.White
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = if (isIncoming) Alignment.Start else Alignment.End
    ) {
        // Message bubble
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(bubbleColor)
                .widthIn(max = 280.dp)
        ) {
            when {
                // ✅ FOSSIFY PATTERN: Handle media directly
                message.hasMedia && message.mediaUri != null -> {
                    MediaMessageContent(
                        message = message,
                        onMediaClick = onMediaClick,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                // ✅ FOSSIFY PATTERN: OTP messages with security styling
                message.isOTP -> {
                    OTPMessageContent(
                        message = message,
                        textColor = textColor,
                        metroFont = metroFont,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // ✅ FOSSIFY PATTERN: Regular text message
                else -> {
                    TextMessageContent(
                        message = message,
                        textColor = textColor,
                        metroFont = metroFont,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Timestamp - ✅ FOSSIFY PATTERN: Simple, unobtrusive
        Text(
            text = formatFriendlyTimestamp(message.date),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 2.dp),
            style = MetroTypography.MetroCaption(metroFont),
        )
    }
}

/**
 * Media message content - handles images, videos, files
 * ✅ FOSSIFY PATTERN: Direct media handling without abstraction
 */
@Composable
private fun MediaMessageContent(
    message: MetroMessage,
    onMediaClick: ((Uri) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier) {
        when (message.messageType) {
            MessageType.IMAGE -> {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            enabled = onMediaClick != null,
                            onClick = { onMediaClick?.invoke(message.mediaUri!!) }
                        )
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(message.mediaUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Message image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(200.dp)
                    )
                }
            }

            MessageType.VIDEO -> {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(
                            enabled = onMediaClick != null,
                            onClick = { onMediaClick?.invoke(message.mediaUri!!) }
                        )
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(message.mediaUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Video thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(200.dp)
                    )

                    // Play indicator overlay
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .background(Color.Black.copy(alpha = 0.3f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play video",
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(36.dp)
                        )
                    }
                }
            }

            // ✅ FOSSIFY PATTERN: File and other media types
            MessageType.FILE, MessageType.AUDIO -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = when (message.messageType) {
                            MessageType.FILE -> "📄 File"
                            MessageType.AUDIO -> "🎵 Audio"
                            else -> "📎 Attachment"
                        },
                        color = Color.White,
                        style = MetroTypography.MetroBody2(MetroFont.Segoe),
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = message.fileName ?: "Unknown",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MetroTypography.MetroCaption(MetroFont.Segoe),
                        maxLines = 1
                    )
                }
            }

            else -> {
                Text(
                    text = "📎 Media content",
                    color = Color.White,
                    style = MetroTypography.MetroBody2(MetroFont.Segoe)
                )
            }
        }

        // Show text caption if available (like WhatsApp)
        message.body?.takeIf { it.isNotBlank() }?.let { caption ->
            Text(
                text = caption,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp),
                style = MetroTypography.MetroBody2(MetroFont.Segoe)
            )
        }
    }
}

/**
 * OTP message with security styling
 * ✅ FOSSIFY PATTERN: Security-first OTP handling
 */
@Composable
private fun OTPMessageContent(
    message: MetroMessage,
    textColor: Color,
    metroFont: MetroFont,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // OTP badge
        Text(
            text = "🔐 OTP CODE",
            color = textColor.copy(alpha = 0.7f),
            style = MetroTypography.MetroCaption(metroFont),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.padding(top = 4.dp))

        // OTP content (masked for security)
        Text(
            text = message.displayBody, // Already masked in MetroMessage
            color = textColor,
            style = MetroTypography.MetroBody2(metroFont)
        )

        // Expiry notice for expired OTPs
        if (message.isExpiredOTP) {
            Spacer(modifier = Modifier.padding(top = 4.dp))
            Text(
                text = "Expired",
                color = Color(0xFFFF6B6B),
                style = MetroTypography.MetroCaption(metroFont),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Regular text message content
 * ✅ FOSSIFY PATTERN: Simple text display
 */
@Composable
private fun TextMessageContent(
    message: MetroMessage,
    textColor: Color,
    metroFont: MetroFont,
    modifier: Modifier = Modifier
) {
    Text(
        text = message.displayBody,
        color = textColor,
        style = MetroTypography.MetroBody2(metroFont),
        modifier = modifier
    )
}
