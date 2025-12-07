// File: MetroAnimatedText.kt
package com.metromessages.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.metromessages.ui.theme.MetroAnimationProfile
import com.metromessages.ui.theme.metroAnimationProfile
import kotlinx.coroutines.delay

@Composable
fun MetroAnimatedText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true,
    overflow: androidx.compose.ui.text.style.TextOverflow = androidx.compose.ui.text.style.TextOverflow.Clip
) {
    val animationProfile: MetroAnimationProfile = style.metroAnimationProfile
    val animatedOffset = remember { Animatable(animationProfile.slideOffset.toFloat()) }

    LaunchedEffect(Unit) {
        delay(animationProfile.enterDelay)
        animatedOffset.animateTo(
            targetValue = 0f,
            animationSpec = tween(
                durationMillis = animationProfile.slideDuration.toInt(),
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        )
    }

    Text(
        text = text,
        style = style,
        color = color,
        maxLines = maxLines,
        softWrap = softWrap,
        overflow = overflow,
        modifier = modifier
            .offset(x = animatedOffset.value.dp)
            .clipToBounds() // Prevent horizontal clipping during animation
    )
}