package com.metromessages.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import kotlin.math.abs
import kotlin.math.pow

/**
 * Pure math utility for parallax calculations - no state management
 */
object MetroParallax {
    // --- GLOBAL SETTINGS (Constants only) ---
    const val GlobalMultiplier = 80f
    const val MaxHeaderOffset = 500f
    const val DEBUG_ENABLED = true

    // --- CORE MATH FUNCTIONS (Stateless) ---

    /**
     * Pure function: calculates parallax offset given inputs
     */
    fun calculateOffset(
        rawProgress: Float,
        speedRatio: Float,
        weight: Float
    ): Float {
        val speedReducedProgress = rawProgress * speedRatio
        val weightedOffset = speedReducedProgress * weight
        val easedOffset = easeInOutCubic(weightedOffset)
        return (easedOffset * GlobalMultiplier).coerceIn(-MaxHeaderOffset, MaxHeaderOffset)
    }

    /**
     * Pure function: calculates relative offset for header
     */
    fun calculateRelativeOffset(
        rawProgress: Float,
        containerWidth: Dp,
        speedRatio: Float,
        weight: Float
    ): Dp {
        val contentScrollDistance = rawProgress * containerWidth.value
        val headerApparentMovement = contentScrollDistance * speedRatio * weight
        val actualHeaderMovement = contentScrollDistance
        val counterMovement = actualHeaderMovement - headerApparentMovement
        val limitedOffset = counterMovement.coerceIn(-MaxHeaderOffset, MaxHeaderOffset)
        return (-limitedOffset).dp
    }

    private fun easeInOutCubic(x: Float): Float {
        val clampedX = x.coerceIn(-1f, 1f)
        return if (abs(clampedX) < 0.5f) {
            4f * clampedX * clampedX * clampedX
        } else {
            val sign = if (clampedX > 0) 1f else -1f
            sign * (1f - (-2f * abs(clampedX) + 2f).pow(3) / 2f)
        }
    }

    // --- DEBUG UTILITIES ---
    fun debugValues(
        rawProgress: Float,
        speedRatio: Float,
        weight: Float,
        containerWidth: Dp? = null
    ) {
        if (DEBUG_ENABLED) {
            println("PARALLAX MATH DEBUG:")
            println("  Input Progress: $rawProgress")
            println("  Speed Ratio: $speedRatio")
            println("  Weight: $weight")
            println("  Effective Speed: ${(speedRatio * weight * 100).toInt()}%")

            if (containerWidth != null) {
                val relativeOffset = calculateRelativeOffset(rawProgress, containerWidth, speedRatio, weight)
                println("  Calculated Offset: ${relativeOffset.value}dp")
            }
        }
    }
}

