// File: ContactAvatar.kt
package com.metromessages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.metromessages.mockdata.ImageLoader

@Composable
fun ContactAvatar(
    name: String,
    photoUrl: String?,
    size: Dp = 40.dp,
    isWindowsPhoneStyle: Boolean = false,
    isSquare: Boolean = false
) {
    val shape = when {
        isSquare -> RoundedCornerShape(0.dp)
        isWindowsPhoneStyle -> RoundedCornerShape(0.dp)
        else -> CircleShape
    }
    val bgColor = if (isWindowsPhoneStyle) Color(0xFF2D2D30) else Color.DarkGray
    val textWeight = if (isWindowsPhoneStyle) FontWeight.Normal else FontWeight.Medium

    // Calculate initials
    val initials = name.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2)
        .joinToString("")

    // Get context outside of remember block
    val context = LocalContext.current

    // Create SVG-capable ImageLoader
    val imageLoader = remember {
        ImageLoader.createWithSvgSupport(context)
    }

    if (!photoUrl.isNullOrEmpty()) {
        val imageState = remember(photoUrl) {
            ImageRequest.Builder(context)
                .data(photoUrl)
                .crossfade(true)
                .build()
        }

        val painter = rememberAsyncImagePainter(
            model = imageState,
            imageLoader = imageLoader,
            onError = { // This is called in a non-composable context
                // We can't show UI here, but we can handle the error
            }
        )

        // Check if the image failed to load
        if (painter.state is AsyncImagePainter.State.Error) {
            // Show fallback initials
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(shape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontSize = (size.value * 0.4).sp,
                    fontWeight = textWeight,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Show the image
            AsyncImage(
                model = imageState,
                contentDescription = "Profile picture of $name",
                modifier = Modifier
                    .size(size)
                    .clip(shape),
                contentScale = ContentScale.Crop,
                imageLoader = imageLoader
            )
        }
    } else {
        // No photo URL provided, show initials
        Box(
            modifier = Modifier
                .size(size)
                .clip(shape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = (size.value * 0.4).sp,
                fontWeight = textWeight,
                textAlign = TextAlign.Center
            )
        }
    }
}