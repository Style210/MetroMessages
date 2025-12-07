// File: MetroFont.kt
package com.metromessages.ui.components

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.metromessages.R

enum class MetroFont(val displayName: String, val fontFamily: FontFamily) {
    Segoe("Segoe UI", FontFamily(Font(R.font.segoe_ui))),
    NotoSans("Noto Sans", FontFamily(Font(R.font.noto_sans))),
    OpenSans("Open Sans", FontFamily(Font(R.font.open_sans))),
    Lato("Lato", FontFamily(Font(R.font.lato))),
    Roboto("Roboto", FontFamily(Font(R.font.roboto)));


}
