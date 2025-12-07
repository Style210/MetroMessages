// File: AccentColor.kt
package com.metromessages.ui.components

import androidx.compose.ui.graphics.Color
import com.metromessages.ui.theme.MetroColors

enum class AccentColor(val displayName: String, val color: Color) {
    Blue("Blue", MetroColors.Blue.color),
    Red("Red", MetroColors.Red.color),
    Teal("Teal", MetroColors.Teal.color),
    Green("Green", MetroColors.Green.color),
    Purple("Purple", MetroColors.Purple.color),
    Orange("Orange", MetroColors.Orange.color),
    Pink("Pink", MetroColors.Pink.color),
    Lime("Lime", MetroColors.Lime.color),
    Indigo("Indigo", MetroColors.Indigo.color),
    Gray("Gray", MetroColors.Gray.color),
    Yellow("Yellow", MetroColors.Yellow.color),
    Brown("Brown", MetroColors.Brown.color),
    Cyan("Cyan", MetroColors.Cyan.color),
    White("White", MetroColors.White.color);


    companion object {
        fun fromColor(color: Color): AccentColor? {
            return entries.find { it.color == color }
        }

        fun fromName(name: String?): AccentColor {
            return entries.find { it.displayName.equals(name, ignoreCase = true) } ?: White
        }
    }
}

fun AccentColor.toColor(): Color = this.color

fun AccentColor.toMetroColor(): MetroColors {
    return MetroColors.allColors.find { it.displayName == this.displayName } ?: MetroColors.White
}
