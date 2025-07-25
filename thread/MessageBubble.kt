// MessageBubble.kt
package com.metromessages.ui.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessageBubble(
    message: String,
    timestamp: Long,
    isOwnMessage: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    // Metro timestamp formatter
    fun formatMetroTimestamp(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    // Using built-in RectangleShape instead of custom shape
    val bubbleShape = RectangleShape

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .clip(bubbleShape)
                .background(
                    color = if (isOwnMessage) accentColor
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message,
                fontSize = 14.sp,
                color = if (isOwnMessage) Color.White
                else MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp,
                modifier = Modifier.widthIn(max = 280.dp)
            )
        }

        Text(
            text = formatMetroTimestamp(timestamp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier
                .padding(top = 2.dp)
                .align(Alignment.End),
            maxLines = 1,
            overflow = TextOverflow.Visible
        )
    }
}