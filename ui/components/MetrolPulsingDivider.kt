// File: MetroPulsingDivider.kt
package com.metromessages.ui.components


import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metromessages.data.settingsscreen.SettingsPreferences
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun MetroPulsingDivider(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    initialWidth: Dp = 90.dp,
    maxTravel: Dp = 8.dp
) {
    // Get accent color from settings
    val context = LocalContext.current
    val settingsPrefs = remember { SettingsPreferences(context) }
    val accentColor by settingsPrefs.accentColorFlow.collectAsState(
        initial = Color(0xFF0063B1) // ← Add initial value (your default Metro accent)
    )

    // Physics state
    var lastOffset by remember { mutableFloatStateOf(0f) }
    var lastUpdateTime by remember { mutableLongStateOf(0L) }
    val velocity = remember { Animatable(0f) }
    var isPressed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Motion calculations
    val directionProgress by remember(pagerState.currentPage) {
        derivedStateOf {
            val offset = pagerState.currentPageOffsetFraction
            val currentTime = System.currentTimeMillis()

            // Calculate velocity if within valid time window
            if (lastUpdateTime > 0 && currentTime - lastUpdateTime < 100) {
                val deltaTime = (currentTime - lastUpdateTime).coerceAtLeast(1)
                val deltaOffset = offset - lastOffset
                coroutineScope.launch {
                    velocity.snapTo(deltaOffset / deltaTime * 1000) // px/sec
                }
            }

            lastOffset = offset
            lastUpdateTime = currentTime

            Pair(
                if (offset < 0) 1 else -1,  // direction
                abs(offset).coerceIn(0f, 1f) // progress
            )
        }
    }

    val direction = directionProgress.first
    val progress = directionProgress.second

    // Animated properties
    val animatedWidth by animateDpAsState(
        targetValue = initialWidth * (1f - progress * 0.3f),
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = 300f
        )
    )

    val animatedOffset by animateDpAsState(
        targetValue = with(LocalDensity.current) {
            (direction * progress * maxTravel.toPx() +
                    velocity.value * 0.2f).toDp()
        },
        animationSpec = tween(durationMillis = 300)
    )

    val pressModifier = if (isPressed) {
        Modifier.scale(0.97f)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            }
            .then(pressModifier)
            .width(animatedWidth)
            .height(2.dp)
            .offset(x = animatedOffset)
            .background(
                color = accentColor.copy(alpha = 0.7f + progress * 0.3f),
                shape = RoundedCornerShape(50)
            )
    )
}

