package com.romankozak.forwardappmobile.ui.utils

import java.util.Locale
import java.util.concurrent.TimeUnit

fun formatDurationForUi(millis: Long): String {
    if (millis < 0) return "0 с"
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60

    if (hours > 0) {
        return String.format(Locale.ROOT, "%d год %d хв", hours, minutes)
    }
    return String.format(Locale.ROOT, "%d хв", minutes)
}