// File: data/model/facebook/components/ConversationHelpers.kt
package com.metromessages.data.local.metromessagehub

import android.graphics.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Phone number formatting for unknown contacts
fun formatPhoneNumberForDisplay(phoneNumber: String): String {
    val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")

    return when {
        digitsOnly.length == 10 -> {
            "(${digitsOnly.substring(0, 3)}) ${digitsOnly.substring(3, 6)}-${digitsOnly.substring(6)}"
        }
        digitsOnly.length == 11 && digitsOnly.startsWith("1") -> {
            "+1 (${digitsOnly.substring(1, 4)}) ${digitsOnly.substring(4, 7)}-${digitsOnly.substring(7)}"
        }
        else -> {
            // Return the original with basic formatting
            if (phoneNumber.contains("-")) phoneNumber else {
                val cleaned = phoneNumber.replace(Regex("[^0-9]"), "")
                if (cleaned.length <= 3) cleaned
                else if (cleaned.length <= 6) "${cleaned.substring(0, 3)}-${cleaned.substring(3)}"
                else "${cleaned.substring(0, 3)}-${cleaned.substring(3, 6)}-${cleaned.substring(6)}"
            }
        }
    }
}

// Tab positioning calculations
fun calculateTabPosition(offset: Float, tabIndex: Int, totalTabs: Int, tabTravel: Float): Dp {
    return when {
        offset < tabIndex -> ((tabIndex - offset) * tabTravel).dp
        offset > tabIndex + 1 -> ((totalTabs - offset + tabIndex) * -tabTravel).dp
        else -> ((tabIndex - offset) * tabTravel).dp
    }
}

// Last activity formatting (used in MetroContactCard)
fun formatLastActivity(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60000 -> "just now"
        diff < 3600000 -> "${diff / 60000} minutes ago"
        diff < 86400000 -> "${diff / 3600000} hours ago"
        else -> "${diff / 86400000} days ago"
    }
}

// Helper for getting placeholder bounds (used in media items)
fun getPlaceholderBounds(): Rect {
    return Rect(0, 0, 100, 100)
}

