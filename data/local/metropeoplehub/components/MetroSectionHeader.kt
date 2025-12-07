// File: ui/components/peoplehub/MetroSectionHeader.kt
package com.metromessages.data.local.metropeoplehub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.metromessages.ui.components.MetroFont
import com.metromessages.ui.theme.MetroTypography
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight

@Composable
fun MetroSectionHeader(
    letter: String,
    onClick: () -> Unit,
    metroFont: MetroFont,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    // Responsive height - matches contact item height
    val headerHeight = 72.dp
    val badgeSize = 48.dp  // Square badge size
    val horizontalPadding = 0.dp


    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight)
            .clickable { onClick() }
            .padding(horizontal = horizontalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Square accent-colored badge ONLY (authentic WP8.1)
        Box(
            modifier = Modifier
                .size(badgeSize)
                .background(accentColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter,
                style = MetroTypography.MetroBody1(metroFont).copy(
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            )
        }
        // No duplicate letter text - just the badge like WP8.1
    }
}
