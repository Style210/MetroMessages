package com.metromessages.ui.theme

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue

@Composable
fun rememberContactFadeAlpha(
    listState: LazyListState,
    itemIndex: Int,
    fadeStartThreshold: Float = 80f,
    fadeEndThreshold: Float = 0f
): Float {
    // Use derivedStateOf to minimize recompositions during scrolling
    val alpha by remember(listState, itemIndex) {
        derivedStateOf {
            calculateContactFadeAlpha(
                listState = listState,
                itemIndex = itemIndex,
                fadeStartThreshold = fadeStartThreshold,
                fadeEndThreshold = fadeEndThreshold
            )
        }
    }
    return alpha
}

private fun calculateContactFadeAlpha(
    listState: LazyListState,
    itemIndex: Int,
    fadeStartThreshold: Float,
    fadeEndThreshold: Float
): Float {
    val layoutInfo = listState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo

    // Find if our item is currently visible
    val itemInfo = visibleItems.find { it.index == itemIndex }

    return if (itemInfo != null) {
        // Item is visible - calculate fade based on position in viewport
        val itemTop = itemInfo.offset.toFloat()
        val fadeProgress = when {
            itemTop > fadeStartThreshold -> 1.0f // Fully visible
            itemTop < fadeEndThreshold -> 0.0f   // Fully faded
            else -> {
                // Smooth quadratic fade between thresholds
                val progress = (itemTop - fadeEndThreshold) / (fadeStartThreshold - fadeEndThreshold)
                progress * progress
            }
        }
        fadeProgress
    } else {
        // Item not visible - determine if it's above or below viewport
        val firstVisibleIndex = visibleItems.firstOrNull()?.index ?: 0
        if (itemIndex < firstVisibleIndex) {
            0f // Above viewport - faded out
        } else {
            1f // Below viewport - fully visible
        }
    }
}
