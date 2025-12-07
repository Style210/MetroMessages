// File: MetroTypography.kt
package com.metromessages.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.metromessages.ui.components.MetroFont

object MetroTypography {

    // Large page headers (e.g., pivot titles)
    fun MetroHeader(metroFont: MetroFont): TextStyle = TextStyle(
        fontSize = 120.sp,
        lineHeight = 50.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (0).sp,
        fontFamily = metroFont.fontFamily
    )

    // Section headers / subheadings
    fun MetroSubhead(metroFont: MetroFont): TextStyle = TextStyle(
        fontSize = 42.sp,
        lineHeight = 48.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (-1).sp,
        fontFamily = metroFont.fontFamily
    )

    // Main content text (e.g., contact names)
    fun MetroBody1(metroFont: MetroFont): TextStyle = TextStyle(
        fontSize = 24.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (-0.5).sp,
        fontFamily = metroFont.fontFamily
    )

    // Secondary text (e.g., message previews)
    fun MetroBody2(metroFont: MetroFont): TextStyle = TextStyle(
        fontSize = 20.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (-0.25).sp,
        fontFamily = metroFont.fontFamily
    )

    // Small text (timestamps, metadata)
    fun MetroCaption(metroFont: MetroFont): TextStyle = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Light,
        letterSpacing = (0).sp,
        fontFamily = metroFont.fontFamily
    )
}

// NEW: Animation data class
data class MetroAnimationProfile(
    val enterDelay: Long,    // milliseconds
    val slideDuration: Long, // milliseconds
    val slideOffset: Int     // dp
)

// NEW: Extension properties for animation profiles
val TextStyle.metroAnimationProfile: MetroAnimationProfile
    get() = when (this.fontSize) {
        120.sp -> MetroAnimationProfile(
            enterDelay = 0L, slideDuration = 320L, slideOffset = 500
        )
        42.sp -> MetroAnimationProfile(
            enterDelay = 50L, slideDuration = 280L, slideOffset = 400
        )
        24.sp -> MetroAnimationProfile(
            enterDelay = 100L, slideDuration = 240L, slideOffset = 300
        )
        20.sp -> MetroAnimationProfile(
            enterDelay = 150L, slideDuration = 200L, slideOffset = 250
        )
        14.sp -> MetroAnimationProfile(
            enterDelay = 200L, slideDuration = 160L, slideOffset = 200
        )
        else -> MetroAnimationProfile(100L, 250L, 300)
    }
