// File: PanoramaCanvas.kt
package com.metromessages.data.local.homescreen

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PanoramaCanvas(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    panoramaFactor: Float = 3f,
    edgePeekDp: Dp = 32.dp,
    pageCount: Int = 3,
    snapThresholdRatio: Float = 0.4f,
    flickVelocityThreshold: Float = 1200f,
    animationSpec: AnimationSpec<Float> = tween(300),
    onProgressChange: (Float) -> Unit = {},
    onPageChanged: (Int) -> Unit = {},
    content: @Composable (pageIndex: Int, pageWidth: Dp, pageOpacity: Float) -> Unit // ← Added pageOpacity
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val viewportWidthState = remember { mutableStateOf<Dp?>(null) }
    var currentPage by remember { mutableIntStateOf(0) }

    // Calculate current scroll position in pages
    val currentPageOffset by remember(scrollState.value, scrollState.maxValue, pageCount) {
        derivedStateOf {
            if (scrollState.maxValue > 0) {
                scrollState.value.toFloat() / scrollState.maxValue.toFloat() * (pageCount - 1)
            } else {
                0f
            }
        }
    }
    // Notify progress and page changes
    LaunchedEffect(scrollState.value) {
        val progress = if (scrollState.maxValue > 0) {
            scrollState.value.toFloat() / scrollState.maxValue.toFloat()
        } else 0f
        onProgressChange(progress)
    }

    LaunchedEffect(currentPage) {
        onPageChanged(currentPage)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                viewportWidthState.value = with(density) { size.width.toDp() }
            }
    ) {
        val viewport = viewportWidthState.value
        if (viewport != null) {
            val widths: List<Dp> = List(pageCount) { index ->
                if (index < pageCount - 1) viewport - edgePeekDp else viewport
            }

            val totalWidth = widths.fold(0.dp) { acc, w -> acc + w }

            Row(
                modifier = Modifier
                    .width(totalWidth)
                    .horizontalScroll(scrollState, enabled = false)
                    .pointerInput(pageCount) {
                        val velocityTracker = VelocityTracker()
                        var dragDelta = 0f

                        detectDragGestures(
                            onDragStart = {
                                velocityTracker.resetTracking()
                                dragDelta = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragDelta += dragAmount.x
                                velocityTracker.addPointerInputChange(change)

                                coroutineScope.launch {
                                    val newValue = (scrollState.value - dragAmount.x.toInt())
                                        .coerceIn(0, scrollState.maxValue)
                                    scrollState.scrollTo(newValue)
                                }
                            },
                            onDragCancel = {
                                velocityTracker.resetTracking()
                                dragDelta = 0f
                            },
                            onDragEnd = {
                                val pxPerSecond = velocityTracker.calculateVelocity().x
                                val viewportPx = with(density) { viewport.roundToPx() }
                                val edgePeekPx = with(density) { edgePeekDp.roundToPx() }
                                val pageStep = (viewportPx - edgePeekPx).coerceAtLeast(1)

                                val currentOffset = scrollState.value.toFloat()
                                val distanceRatio = currentOffset / pageStep
                                var targetPage = distanceRatio.roundToInt().coerceIn(0, pageCount - 1)

                                // Flick override
                                if (abs(pxPerSecond) > flickVelocityThreshold) {
                                    targetPage = if (pxPerSecond < 0) {
                                        (currentPage + 1).coerceAtMost(pageCount - 1)
                                    } else {
                                        (currentPage - 1).coerceAtLeast(0)
                                    }
                                } else {
                                    // Snap threshold override (based on drag distance ratio)
                                    val dragRatio = abs(dragDelta) / pageStep
                                    if (dragDelta < 0 && dragRatio > snapThresholdRatio) {
                                        targetPage = (currentPage + 1).coerceAtMost(pageCount - 1)
                                    } else if (dragDelta > 0 && dragRatio > snapThresholdRatio) {
                                        targetPage = (currentPage - 1).coerceAtLeast(0)
                                    }
                                }

                                coroutineScope.launch {
                                    val targetScroll = (targetPage * pageStep)
                                        .coerceIn(0, scrollState.maxValue)
                                    scrollState.animateScrollTo(targetScroll, animationSpec)
                                    currentPage = targetPage
                                }
                            }
                        )
                    }
            ) {
                widths.forEachIndexed { index, width ->
                    // Calculate opacity based on distance from center (Windows Phone style)
                    val pageOpacity = calculatePageOpacity(currentPageOffset, index)
                    content(index, width, pageOpacity) // ← Pass opacity to content
                }
            }
        }
    }
}

// Authentic Windows Phone opacity calculation
private fun calculatePageOpacity(currentPageOffset: Float, pageIndex: Int): Float {
    val distanceFromCenter = abs(currentPageOffset - pageIndex)
    return when {
        distanceFromCenter < 0.5 -> 1.0f // Center page - full opacity
        distanceFromCenter < 1.5 -> 0.6f // Adjacent pages - slightly dimmed
        distanceFromCenter < 2.5 -> 0.4f // Next pages - more dimmed
        else -> 0.3f // Far edges - most dimmed
    }
}