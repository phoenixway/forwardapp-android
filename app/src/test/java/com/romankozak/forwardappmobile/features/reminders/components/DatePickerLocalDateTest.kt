package com.romankozak.forwardappmobile.features.reminders.components

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class DatePickerLocalDateTest {
    private val zones = listOf("UTC", "Europe/Kyiv", "America/Los_Angeles").map(TimeZone::getTimeZone)
    private fun day(zone: TimeZone, month: Int = Calendar.OCTOBER, date: Int = 27, hour: Int = 1) =
        Calendar.getInstance(zone).apply { clear(); set(2026, month, date, hour, 30) }

    @Test fun `picker initialization preserves local day east and west of UTC`() {
        zones.forEach { zone ->
            val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = localDateToPickerMillis(day(zone).timeInMillis, zone)
            }
            assertEquals(27, picked.get(Calendar.DAY_OF_MONTH))
            assertEquals(Calendar.OCTOBER, picked.get(Calendar.MONTH))
            assertEquals(0, picked.get(Calendar.HOUR_OF_DAY))
        }
    }

    @Test fun `selected UTC picker day becomes the same local date in every zone`() {
        val selected = day(TimeZone.getTimeZone("UTC"), date = 23, hour = 0).apply { set(Calendar.MINUTE, 0) }.timeInMillis
        zones.forEach { zone ->
            val local = Calendar.getInstance(zone).apply { timeInMillis = pickerMillisToLocalDate(selected, zone) }
            assertEquals(23, local.get(Calendar.DAY_OF_MONTH))
            assertEquals(Calendar.OCTOBER, local.get(Calendar.MONTH))
            assertEquals(0, local.get(Calendar.HOUR_OF_DAY))
        }
    }

    @Test fun `civil date round trip remains stable across daylight saving boundaries`() {
        zones.forEach { zone ->
            listOf(Calendar.MARCH to 29, Calendar.OCTOBER to 25).forEach { (month, date) ->
                val original = day(zone, month, date, 12)
                val roundTrip = Calendar.getInstance(zone).apply {
                    timeInMillis = pickerMillisToLocalDate(localDateToPickerMillis(original.timeInMillis, zone), zone)
                }
                assertEquals(original.get(Calendar.YEAR), roundTrip.get(Calendar.YEAR))
                assertEquals(month, roundTrip.get(Calendar.MONTH))
                assertEquals(date, roundTrip.get(Calendar.DAY_OF_MONTH))
            }
        }
    }
}
