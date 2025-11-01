// File: FontProvider.kt
package com.metromessages.data.local.font

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.metromessages.R

sealed class MetroFont(val displayName: String, val fontFamily: FontFamily) {
    object SegoeUI : MetroFont("segoe_ui", FontFamily(Font(R.font.segoe_ui)))
    object NotoSans : MetroFont("noto_sans", FontFamily(Font(R.font.noto_sans)))
    object OpenSans : MetroFont("open_sans", FontFamily(Font(R.font.open_sans)))
    object Lato : MetroFont("lato", FontFamily(Font(R.font.lato)))
    object Roboto : MetroFont("roboto", FontFamily(Font(R.font.roboto)))
}

object FontProvider {
    val SegoeUI = MetroFont.SegoeUI
    val NotoSans = MetroFont.NotoSans
    val OpenSans = MetroFont.OpenSans
    val Lato = MetroFont.Lato
    val Roboto = MetroFont.Roboto

    val allFonts = listOf(SegoeUI, NotoSans, OpenSans, Lato, Roboto)

    // Current font used app-wide; defaults to SegoeUI
    var currentFont: MetroFont = SegoeUI
        private set

    /** Updates the currentFont globally and can be observed in Compose UI */
    fun setFont(font: MetroFont) {
        currentFont = font
        // Future: could add LiveData/StateFlow if reactive recomposition needed across screens
    }
}
