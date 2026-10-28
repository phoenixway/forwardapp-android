package com.romankozak.forwardappmobile.features.reminders.components

import java.util.Calendar
import java.util.TimeZone

/** Material DatePicker encodes civil dates at UTC midnight, not local midnight. */
internal fun localDateToPickerMillis(timestamp: Long, zone: TimeZone = TimeZone.getDefault()): Long =
    copyCivilDate(timestamp, from = zone, to = TimeZone.getTimeZone("UTC"))

internal fun pickerMillisToLocalDate(timestamp: Long, zone: TimeZone = TimeZone.getDefault()): Long =
    copyCivilDate(timestamp, from = TimeZone.getTimeZone("UTC"), to = zone)

private fun copyCivilDate(timestamp: Long, from: TimeZone, to: TimeZone): Long {
    val source = Calendar.getInstance(from).apply { timeInMillis = timestamp }
    return Calendar.getInstance(to).apply {
        clear()
        set(source.get(Calendar.YEAR), source.get(Calendar.MONTH), source.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
