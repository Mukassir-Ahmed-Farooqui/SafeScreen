package com.example.safescreen.ui.components

/** "0m", "<1m", "12m", "1h 05m". */
fun formatDuration(millis: Long): String {
    val totalMinutes = millis / 60_000
    return when {
        millis <= 0L -> "0m"
        totalMinutes < 1 -> "<1m"
        totalMinutes < 60 -> "${totalMinutes}m"
        else -> "${totalMinutes / 60}h ${(totalMinutes % 60).toString().padStart(2, '0')}m"
    }
}

/** "No limit", "15m", "1h", "1h 30m". */
fun formatLimit(minutes: Int): String = when {
    minutes <= 0 -> "No limit"
    minutes < 60 -> "${minutes}m"
    minutes % 60 == 0 -> "${minutes / 60}h"
    else -> "${minutes / 60}h ${minutes % 60}m"
}
