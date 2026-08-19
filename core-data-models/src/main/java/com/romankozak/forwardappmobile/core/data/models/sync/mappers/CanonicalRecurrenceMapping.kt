package com.romankozak.forwardappmobile.core.data.models.sync.mappers

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.CanonicalRecurringSeriesEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.CanonicalRecurrenceRuleSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.CanonicalRecurringSeriesSnapshot

private val canonicalRecurrenceSyncGson = Gson()

fun CanonicalRecurringSeriesEntity.toSnapshot(): CanonicalRecurringSeriesSnapshot =
    CanonicalRecurringSeriesSnapshot(
        id = id,
        kind = kind,
        rule =
            CanonicalRecurrenceRuleSnapshot(
                frequency = ruleFrequency,
                interval = ruleInterval,
                daysOfWeek =
                    ruleDaysOfWeekCsv?.let { encoded ->
                        if (encoded.isEmpty()) emptyList() else encoded.split(',')
                    },
            ),
        startDayKey = startDayKey,
        endDayKey = endDayKey,
        template = JsonParser.parseString(templateJson),
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
    )

fun CanonicalRecurringSeriesSnapshot.toEntity(): CanonicalRecurringSeriesEntity =
    CanonicalRecurringSeriesEntity(
        id = id,
        kind = kind,
        ruleFrequency = rule.frequency,
        ruleInterval = rule.interval,
        ruleDaysOfWeekCsv = rule.daysOfWeek?.joinToString(","),
        startDayKey = startDayKey,
        endDayKey = endDayKey,
        templateJson = canonicalRecurrenceSyncGson.toJson(template),
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
    )
