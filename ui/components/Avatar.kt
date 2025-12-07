// File: ContactAvatar.kt - SELF-CONTAINED VERSION
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
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import coil.request.ImageRequest

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

    val context = LocalContext.current

    // Create ImageLoader with SVG support
    val imageLoader = remember {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }

    if (!photoUrl.isNullOrEmpty()) {
        // Create ImageRequest
        val imageRequest = remember(photoUrl) {
            ImageRequest.Builder(context)
                .data(photoUrl)
                .crossfade(true)
                .build()
        }

        // Use rememberAsyncImagePainter to check state
        val painter = rememberAsyncImagePainter(
            model = imageRequest,
            imageLoader = imageLoader
        )

        // Check painter state and show appropriate UI
        when (painter.state) {
            is AsyncImagePainter.State.Loading,
            is AsyncImagePainter.State.Error -> {
                // Show fallback initials when loading or on error
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
            else -> {
                // Show the image when successfully loaded
                AsyncImage(
                    model = imageRequest,
                    contentDescription = "Profile picture of $name",
                    modifier = Modifier
                        .size(size)
                        .clip(shape),
                    contentScale = ContentScale.Crop,
                    imageLoader = imageLoader
                )
            }
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