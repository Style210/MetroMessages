// File: ui/theme/MetroHeaderCanvas.kt
package com.metromessages.ui.theme

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.metromessages.R
import com.metromessages.ui.components.MetroFont

// Universal configuration for all Metro headers
object MetroHeaderConfig {
    var defaultParallaxSpeed: Float = 0.35f
    var defaultOpacity: Float = 1f
    var defaultTextColor: Color = Color.White
    var defaultHeight: Dp = 120.dp
}

// Primary Canvas-based MetroHeader implementation - GRADIENT REMOVED
@Composable
fun MetroHeaderCanvas(
    text: String,
    scrollState: ScrollState,
    metroFont: MetroFont,
    modifier: Modifier = Modifier,
    textColor: Color = MetroHeaderConfig.defaultTextColor,
    opacity: Float = MetroHeaderConfig.defaultOpacity,
    textSize: Dp = with(LocalDensity.current) { MetroTypography.MetroHeader(metroFont).fontSize.toDp() },
    parallaxSpeed: Float = MetroHeaderConfig.defaultParallaxSpeed,
    headerHeight: Dp = MetroHeaderConfig.defaultHeight
    // REMOVED: accentColor and gradientEnabled parameters
) {
    val density = LocalDensity.current
    val context = LocalContext.current

    Box(
        modifier = modifier.height(headerHeight)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            // REMOVED: Gradient background - now handled at root level

            // Use the exact same font files as defined in MetroFont
            val fontResource = when (metroFont) {
                MetroFont.Segoe -> R.font.segoe_ui_family
                MetroFont.NotoSans -> R.font.noto_sans_family
                MetroFont.OpenSans -> R.font.open_sans_family
                MetroFont.Lato -> R.font.lato_family
                MetroFont.Roboto -> R.font.roboto_family
            }

            // Load the exact same font file used in Compose Text components
            val typeface = ResourcesCompat.getFont(context, fontResource)

            val paint = Paint().apply {
                color = textColor.copy(alpha = opacity).toArgb()
                this.textSize = with(density) { textSize.toPx() }
                this.typeface = typeface
                textAlign = Paint.Align.LEFT
                isAntiAlias = true
                // Ensure no artificial boldness
                isFakeBoldText = false
                // Use the same text rendering as Compose
                isLinearText = true
                isSubpixelText = true
                strokeWidth = 0f
                style = Paint.Style.FILL
            }

            // Calculate X/Y for parallax scroll
            val textX = -scrollState.value * parallaxSpeed
            val textY = size.height * 0.7f

            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawText(
                    text,
                    textX,
                    textY,
                    paint
                )
            }
        }
    }
}

// Legacy Text-based implementation for SettingsScreen
@Composable
fun MetroHeader(
    text: String,
    metroFont: MetroFont,
    pageOffset: Float = 0f,
    verticalOffset: Dp = 0.dp,
    horizontalWeight: Float = 0.2f,
    modifier: Modifier = Modifier,
    textColor: Color = MetroHeaderConfig.defaultTextColor,
    opacity: Float = MetroHeaderConfig.defaultOpacity,
    maxLines: Int = 1
    // REMOVED: accentColor and gradientEnabled parameters
) {
    Text(
        text = text,
        modifier = modifier,
        color = textColor.copy(alpha = opacity),
        style = MetroTypography.MetroHeader(metroFont),
        maxLines = maxLines
    )
}

// Modifier extension for consistent header spacing
fun Modifier.metroHeaderPadding(): Modifier = this.padding(
    top = MetroHeaderConfig.defaultHeight,
    start = 24.dp
)