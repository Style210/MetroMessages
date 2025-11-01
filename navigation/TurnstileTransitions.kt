// app/src/main/java/com/metromessages/ui/navigation/TurnstileTransitions.kt
package com.metromessages.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

private const val TURNSTILE_DURATION = 450

/**
 * Forward Enter: Screen slides in from right with subtle overshoot
 */
fun turnstileEnter(): EnterTransition = slideInHorizontally(
    initialOffsetX = { fullWidth -> (fullWidth * 1.1).toInt() }, // Subtle overshoot
    animationSpec = tween(
        durationMillis = TURNSTILE_DURATION,
        easing = androidx.compose.animation.core.CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
    )
) + fadeIn(tween(300))

/**
 * Forward Exit: Screen slides out to left with subtle compression
 */
fun turnstileExit(): ExitTransition = slideOutHorizontally(
    targetOffsetX = { fullWidth -> (-fullWidth * 0.95).toInt() }, // Subtle compression
    animationSpec = tween(
        durationMillis = TURNSTILE_DURATION,
        easing = androidx.compose.animation.core.CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)
    )
) + fadeOut(tween(250))

/**
 * Backward Enter: Screen slides in from left (reverse of forward)
 */
fun turnstilePopEnter(): EnterTransition = slideInHorizontally(
    initialOffsetX = { fullWidth -> (-fullWidth * 1.1).toInt() }, // Subtle overshoot from left
    animationSpec = tween(
        durationMillis = TURNSTILE_DURATION,
        easing = androidx.compose.animation.core.CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
    )
) + fadeIn(tween(300))

/**
 * Backward Exit: Screen slides out to right (reverse of forward)
 */
fun turnstilePopExit(): ExitTransition = slideOutHorizontally(
    targetOffsetX = { fullWidth -> (fullWidth * 0.95).toInt() }, // Subtle compression to right
    animationSpec = tween(
        durationMillis = TURNSTILE_DURATION,
        easing = androidx.compose.animation.core.CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)
    )
) + fadeOut(tween(250))