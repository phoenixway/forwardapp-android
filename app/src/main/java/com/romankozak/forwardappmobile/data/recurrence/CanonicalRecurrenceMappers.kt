package com.romankozak.forwardappmobile.data.recurrence

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.CanonicalRecurringSeriesEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusItem
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceDayOfWeek
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceOrigin
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringFocusSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringResponsibilitySeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeriesKind
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskTemplate

private fun encodeDaysOfWeek(days: List<RecurrenceDayOfWeek>?): String? =
    days?.joinToString(",") { it.name }

private fun decodeDaysOfWeek(value: String?): List<RecurrenceDayOfWeek>? =
    value?.let { encoded ->
        if (encoded.isEmpty()) {
            emptyList()
        } else {
            encoded.split(',').map { RecurrenceDayOfWeek.valueOf(it) }
        }
    }

private fun CanonicalRecurringSeriesEntity.toRule(): RecurrenceRule =
    RecurrenceRule(
        frequency = RecurrenceFrequency.valueOf(ruleFrequency),
        interval = ruleInterval,
        daysOfWeek = decodeDaysOfWeek(ruleDaysOfWeekCsv),
    )

fun RecurringSeries.toAndroidEntity(): CanonicalRecurringSeriesEntity =
    CanonicalRecurringSeriesEntity(
        id = id,
        kind = kind.name,
        ruleFrequency = rule.frequency.name,
        ruleInterval = rule.interval,
        ruleDaysOfWeekCsv = encodeDaysOfWeek(rule.daysOfWeek),
        startDayKey = startDayKey,
        endDayKey = endDayKey,
        templateJson =
            when (this) {
                is RecurringTaskSeries -> encodeTaskRecurrenceTemplate(template)
                is RecurringFocusSeries -> encodeFocusRecurrenceTemplate(template)
                is RecurringResponsibilitySeries -> encodeFocusRecurrenceTemplate(template)
            },
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
    )

fun CanonicalRecurringSeriesEntity.toCanonicalSeries(): RecurringSeries {
    val rule = toRule()

    return when (RecurringSeriesKind.valueOf(kind)) {
        RecurringSeriesKind.TASK ->
            RecurringTaskSeries(
                id = id,
                createdAt = createdAt,
                updatedAt = updatedAt,
                syncedAt = syncedAt,
                isDeleted = isDeleted,
                version = version,
                rule = rule,
                startDayKey = startDayKey,
                endDayKey = endDayKey,
                template = decodeTaskRecurrenceTemplate(templateJson),
            )

        RecurringSeriesKind.FOCUS ->
            RecurringFocusSeries(
                id = id,
                createdAt = createdAt,
                updatedAt = updatedAt,
                syncedAt = syncedAt,
                isDeleted = isDeleted,
                version = version,
                rule = rule,
                startDayKey = startDayKey,
                endDayKey = endDayKey,
                template = decodeFocusRecurrenceTemplate(templateJson),
            )

        RecurringSeriesKind.RESPONSIBILITY ->
            RecurringResponsibilitySeries(
                id = id,
                createdAt = createdAt,
                updatedAt = updatedAt,
                syncedAt = syncedAt,
                isDeleted = isDeleted,
                version = version,
                rule = rule,
                startDayKey = startDayKey,
                endDayKey = endDayKey,
                template = decodeFocusRecurrenceTemplate(templateJson),
            )
    }
}

private fun canonicalRecurrenceOrigin(
    seriesId: String?,
    occurrenceDayKey: String?,
    sourceSeriesVersion: Long?,
): RecurrenceOrigin? {
    if (seriesId == null && occurrenceDayKey == null && sourceSeriesVersion == null) {
        return null
    }

    check(seriesId != null && occurrenceDayKey != null && sourceSeriesVersion != null) {
        "Partial canonical recurrence provenance: " +
            "seriesId=$seriesId occurrenceDayKey=$occurrenceDayKey " +
            "sourceSeriesVersion=$sourceSeriesVersion"
    }

    return RecurrenceOrigin(
        seriesId = seriesId,
        occurrenceDayKey = occurrenceDayKey,
        sourceSeriesVersion = sourceSeriesVersion,
    )
}

fun DayTask.toCanonicalRecurrenceOrigin(): RecurrenceOrigin? =
    canonicalRecurrenceOrigin(
        seriesId = recurrenceSeriesId,
        occurrenceDayKey = recurrenceOccurrenceDayKey,
        sourceSeriesVersion = recurrenceSourceSeriesVersion,
    )

fun DayFocusItem.toCanonicalRecurrenceOrigin(): RecurrenceOrigin? =
    canonicalRecurrenceOrigin(
        seriesId = recurrenceSeriesId,
        occurrenceDayKey = recurrenceOccurrenceDayKey,
        sourceSeriesVersion = recurrenceSourceSeriesVersion,
    )

fun DayTask.withCanonicalRecurrenceOrigin(origin: RecurrenceOrigin?): DayTask =
    copy(
        recurrenceSeriesId = origin?.seriesId,
        recurrenceOccurrenceDayKey = origin?.occurrenceDayKey,
        recurrenceSourceSeriesVersion = origin?.sourceSeriesVersion,
    )

fun DayFocusItem.withCanonicalRecurrenceOrigin(origin: RecurrenceOrigin?): DayFocusItem =
    copy(
        recurrenceSeriesId = origin?.seriesId,
        recurrenceOccurrenceDayKey = origin?.occurrenceDayKey,
        recurrenceSourceSeriesVersion = origin?.sourceSeriesVersion,
    )
