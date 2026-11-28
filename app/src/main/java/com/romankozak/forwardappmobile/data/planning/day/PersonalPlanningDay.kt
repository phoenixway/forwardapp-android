package com.romankozak.forwardappmobile.data.planning.day

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/** The exact zone configured at creation, including an explicitly configured fixed-offset zone. */
internal data class PersonalPlanningDay(val date: LocalDate, val zone: ZoneId) {
    val scopeId: String
        get() = UUID.nameUUIDFromBytes("forwardapp:personal-day:v1:$date:${zone.id}".toByteArray(Charsets.UTF_8)).toString()
    val start: Long
        get() = date.atStartOfDay(zone).toInstant().toEpochMilli()

    companion object {
        fun at(instant: Long, zone: ZoneId = ZoneId.systemDefault()): PersonalPlanningDay =
            PersonalPlanningDay(Instant.ofEpochMilli(instant).atZone(zone).toLocalDate(), zone)
    }
}

internal fun verifyPersonalPlanningDay(
    scopeId: String,
    start: Long,
    localDate: String?,
    timeZoneId: String?,
): PersonalPlanningDay? {
    if (localDate == null && timeZoneId == null) return null
    val key = PersonalPlanningDay(LocalDate.parse(requireNotNull(localDate)), ZoneId.of(requireNotNull(timeZoneId)))
    require(key.date.toString() == localDate && key.start == start && key.scopeId == scopeId) {
        "Day planning identity does not match its persisted local date and time zone"
    }
    return key
}
