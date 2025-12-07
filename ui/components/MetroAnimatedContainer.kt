// File: MetroAnimatedContainer.kt
package com.metromessages.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Metro-style container that animates in with top-to-bottom hierarchy
 * @param enterDelay Milliseconds to delay before starting animation
 * @param slideDuration Duration of the slide animation
 * @param fadeDuration Duration of the fade animation
 * @param slideOffset Starting horizontal offset (positive = from right, negative = from left)
 */
@Composable
fun MetroAnimatedContainer(
    enterDelay: Long = 0L,
    slideDuration: Int = 400,
    fadeDuration: Int = 300,
    slideOffset: Int = 500,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val animatedOffset = remember { Animatable(slideOffset.toFloat()) }
    val animatedAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Wait for the specified delay
        delay(enterDelay)

        // Start both animations simultaneously
        launch {
            animatedOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = slideDuration,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            )
        }

        launch {
            animatedAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = fadeDuration)
            )
        }
    }

    Box(
        modifier = modifier
            .offset(x = animatedOffset.value.dp)
            .alpha(animatedAlpha.value)
    ) {
        content()
    }
}