// File: MetroEmptyState.kt
package com.metromessages.ui.thread

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MetroEmptyState(
    primaryText: String = "no messages", // NEW: Parameter for main message
    showActionPrompt: Boolean = true,    // NEW: Control if "tap to start" is shown
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    // Metro-style pulsing animation
    val infiniteTransition = rememberInfiniteTransition()
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Primary message (Metro lowercase) - Now uses parameter
            Text(
                text = primaryText, // CHANGED: Uses parameter
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Light,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )

            // NEW: Only show spacer and action prompt if needed
            if (showActionPrompt) {
                Spacer(Modifier.height(12.dp))

                // Action prompt (Metro uppercase with pulse)
                Text(
                    text = "tap to start", // You could also make this a parameter if needed
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = accentColor.copy(alpha = pulseAlpha),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
