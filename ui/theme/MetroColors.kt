package com.metromessages.ui.theme

import androidx.compose.ui.graphics.Color

sealed class MetroColors(val displayName: String, val color: Color) {
    object Blue : MetroColors("Blue", Color(0xFF0063B1))
    object Red : MetroColors("Red", Color(0xFFE81123))
    object Teal : MetroColors("Teal", Color(0xFF00B294))
    object Green : MetroColors("Green", Color(0xFF107C10))
    object Purple : MetroColors("Purple", Color(0xFF5C2D91))
    object Orange : MetroColors("Orange", Color(0xFFF7630C))
    object Pink : MetroColors("Pink", Color(0xFFE3008C))
    object Lime : MetroColors("Lime", Color(0xFFB4D455))
    object Indigo : MetroColors("Indigo", Color(0xFF6B69D6))
    object Gray : MetroColors("Gray", Color(0xFFA0AEB2))
    object Yellow : MetroColors("Yellow", Color(0xFFFFB900))
    object Brown : MetroColors("Brown", Color(0xFF8E562E))
    object Cyan : MetroColors("Cyan", Color(0xFF00B7C3))
    object White : MetroColors("White", Color.White)
    companion object {
        val allColors: List<MetroColors> = listOf(
            Blue, Red, Teal, Green, Purple, Orange,
            Pink, Lime, Indigo, White, Gray, Yellow, Brown, Cyan
        )

        fun fromName(name: String?): MetroColors = allColors.find {
            it.displayName.equals(name, ignoreCase = true)
        } ?: White
    }
}

