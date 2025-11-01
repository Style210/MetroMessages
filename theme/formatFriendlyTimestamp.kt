package com.metromessages.ui.theme

import java.text.SimpleDateFormat
import java.util.*

fun formatFriendlyTimestamp(timestampMillis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestampMillis

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestampMillis

    val nowCalendar = Calendar.getInstance()

    return when {
        // Same day
        isSameDay(calendar, nowCalendar) -> {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestampMillis))
        }

        // Yesterday
        isYesterday(calendar, nowCalendar) -> {
            "Yesterday"
        }

        // Same week
        isSameWeek(calendar, nowCalendar) -> {
            SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestampMillis)) // e.g., "Mon"
        }

        // Same year
        isSameYear(calendar, nowCalendar) -> {
            SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestampMillis)) // e.g., "Apr 21"
        }

        else -> {
            SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date(timestampMillis)) // fallback
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(cal1: Calendar, cal2: Calendar): Boolean {
    val cal1Copy = cal1.clone() as Calendar
    cal1Copy.add(Calendar.DAY_OF_YEAR, 1)
    return isSameDay(cal1Copy, cal2)
}

private fun isSameWeek(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR) &&
            cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
}

private fun isSameYear(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
}
