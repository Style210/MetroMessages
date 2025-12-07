// File: ui/components/FadingEdgesContent.kt
package com.metromessages.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FadingEdgesContent(
    scrollState: LazyListState,
    topEdgeHeight: Dp = 120.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()

                val scrollOffset = scrollState.firstVisibleItemScrollOffset.toFloat()
                if (scrollOffset > 0) {
                    val fadeColors = listOf(Color.Transparent, Color.Black)

                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = fadeColors,
                            startY = 0f,
                            endY = topEdgeHeight.toPx()
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
            }
    ) {
        content()
    }
}