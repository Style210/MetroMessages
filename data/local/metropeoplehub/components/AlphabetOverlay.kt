// File: AlphabetOverlay.kt
package com.metromessages.data.local.metropeoplehub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.metromessages.ui.components.MetroFont
import com.metromessages.ui.theme.MetroTypography

@Composable
fun AlphabetOverlay(
    letters: List<Char>,
    availableLetters: Set<Char>,
    onLetterSelected: (Char) -> Unit,
    onDismiss: () -> Unit,
    metroFont: MetroFont,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // Responsive letter size - scales with screen but has reasonable bounds
    val letterSize: Dp = when {
        screenWidth < 360.dp -> 60.dp  // Small phones
        screenWidth < 480.dp -> 70.dp  // Medium phones
        screenWidth < 600.dp -> 80.dp  // Large phones
        else -> 90.dp  // Tablets
    }

    // Responsive column count
    val columns = when {
        screenWidth < 360.dp -> 4  // Small phones
        screenWidth < 480.dp -> if (isLandscape) 6 else 4  // Medium phones
        screenWidth < 600.dp -> if (isLandscape) 7 else 5  // Large phones
        else -> if (isLandscape) 8 else 6  // Tablets
    }

    // Responsive padding - percentage based
    val horizontalPadding = (screenWidth * 0.04f).coerceAtLeast(16.dp).coerceAtMost(48.dp)
    val verticalPadding = (configuration.screenHeightDp.dp * 0.02f).coerceAtLeast(16.dp).coerceAtMost(64.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier
                .padding(
                    horizontal = horizontalPadding,
                    vertical = verticalPadding
                )
        ) {
            items(letters) { letter ->
                val hasContacts = availableLetters.contains(letter)
                val opacity = if (hasContacts) 0.9f else 0.3f
                val letterColor = if (hasContacts) accentColor else accentColor.copy(alpha = 0.3f)

                Box(
                    modifier = Modifier
                        .size(letterSize)
                        .padding(8.dp)
                        .clickable(
                            enabled = hasContacts,
                            onClick = {
                                onLetterSelected(letter)
                                onDismiss()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter.toString(),
                        style = MetroTypography.MetroSubhead(metroFont).copy(
                            color = letterColor
                        )
                    )
                }
            }
        }
    }
}