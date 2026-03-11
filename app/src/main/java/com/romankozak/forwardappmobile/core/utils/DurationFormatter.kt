package com.romankozak.forwardappmobile.core.utils

import java.util.Locale
import java.util.concurrent.TimeUnit

private const val MINUTES_PER_HOUR = 60L

fun formatDurationForUi(millis: Long): String {
    val safeMillis = millis.coerceAtLeast(0)
    val hours = TimeUnit.MILLISECONDS.toHours(safeMillis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(safeMillis) % MINUTES_PER_HOUR
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d год %d хв", hours, minutes)
    } else {
        String.format(Locale.ROOT, "%d хв", minutes)
    }
}
