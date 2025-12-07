package com.metromessages.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlin.random.Random

/**
 * 🌌 Cosmic Background Layer with Ken Burns Effect
 *
 * A reusable composable that displays a pure grayscale contact photo with subtle Ken Burns animation.
 * Creates cinematic movement by slowly panning and zooming across the image.
 *
 * @param contactPhotoUri The URI of the contact photo to display (null for fallback)
 * @param kenBurnsEnabled Whether to enable the Ken Burns animation (default: true)
 * @param animationCycleMs Duration of one complete animation cycle in milliseconds (default: 20000ms)
 * @param zoomRange How much the zoom changes during animation (default: 1.1f = 10% zoom)
 * @param modifier Modifier for the background layer
 */
@Composable
fun CosmicBackground(
    contactPhotoUri: String?,
    kenBurnsEnabled: Boolean = true,
    animationCycleMs: Int = 20000,
    zoomRange: Float = 1.1f,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier
) {
    val (scale, translation) = if (kenBurnsEnabled) {
        rememberKenBurnsAnimation(animationCycleMs, zoomRange)
    } else {
        Pair(1.0f, Offset.Zero)
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (contactPhotoUri != null) {
            AsyncImage(
                model = contactPhotoUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = translation.x
                        translationY = translation.y
                    },
                colorFilter = ColorFilter.colorMatrix(
                    ColorMatrix().apply {
                        // Pure grayscale - removes all color for monochromatic look
                        setToSaturation(0f)
                    }
                )
            )
        }
        // If no contact photo, your beautiful gradient shows through naturally
    }
}

/**
 * Remember the Ken Burns animation state with coordinated zoom and pan
 *
 * @param cycleMs Duration of one complete animation cycle
 * @param zoomRange How much the scale changes (1.0f = no change, 1.1f = 10% scale change)
 */
@Composable
private fun rememberKenBurnsAnimation(
    cycleMs: Int = 20000,
    zoomRange: Float = 1.1f
): Pair<Float, Offset> {
    val infiniteTransition = rememberInfiniteTransition()

    // Animate scale (zoom)
    val scale = infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = zoomRange,
        animationSpec = infiniteRepeatable(
            animation = tween(cycleMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    ).value

    // Animate X translation (horizontal pan)
    val translateX = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 50f, // Adjust this value based on your desired pan distance
        animationSpec = infiniteRepeatable(
            animation = tween(cycleMs * 3 / 2, easing = LinearEasing), // Different timing for more organic movement
            repeatMode = RepeatMode.Reverse
        )
    ).value

    // Animate Y translation (vertical pan) - slightly different timing
    val translateY = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 30f, // Adjust this value based on your desired pan distance
        animationSpec = infiniteRepeatable(
            animation = tween(cycleMs * 4 / 3, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    ).value

    return Pair(scale, Offset(translateX, translateY))
}

/**
 * Advanced version with multiple animation paths for more variety
 */
@Composable
fun rememberAdvancedKenBurnsAnimation(
    cycleMs: Int = 25000,
    zoomRange: Float = 1.15f
): Pair<Float, Offset> {
    val infiniteTransition = rememberInfiniteTransition()

    // Pre-calculate a seed based on cycleMs for consistent but varied animations
    val seed = (cycleMs / 1000).toLong()
    val random = Random(seed)

    val scale = infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = zoomRange,
        animationSpec = infiniteRepeatable(
            animation = tween(cycleMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    ).value

    // Create more organic movement with different pan distances and timings
    // Using the built-in nextDouble(from, until) method
    val translateX = infiniteTransition.animateFloat(
        initialValue = random.nextFloat() * 20f,
        targetValue = random.nextFloat() * 60f + 20f,
        animationSpec = infiniteRepeatable(
            animation = tween((cycleMs * random.nextDouble(1.3, 1.8)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    ).value

    val translateY = infiniteTransition.animateFloat(
        initialValue = random.nextFloat() * 15f,
        targetValue = random.nextFloat() * 40f + 10f,
        animationSpec = infiniteRepeatable(
            animation = tween((cycleMs * random.nextDouble(1.1, 1.5)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    ).value

    return Pair(scale, Offset(translateX, translateY))
}