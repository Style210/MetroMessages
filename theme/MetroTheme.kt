package com.metromessages.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metromessages.util.FontProvider

@Composable
fun MetroTheme(
    accentColor: Color,
    fontFamily: FontFamily = FontProvider.SegoeUI,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = accentColor,
        onPrimary = Color.Black,
        background = Color.Black,
        onBackground = Color.White,
        surface = Color.Black,
        onSurface = Color.White
    )

    val typography = Typography(
        displayLarge = MaterialTheme.typography.displayLarge.copy(fontFamily = fontFamily, fontSize = 36.sp),
        titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = fontFamily),
        bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = MaterialTheme.typography.bodySmall.copy(fontFamily = fontFamily)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = Shapes(
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(0.dp)
        ),
        content = content
    )
}

