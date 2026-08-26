@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package com.romankozak.forwardappmobile.shared.core.domain.recurrence

import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceDayOfWeek
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceFrequency
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceOrigin
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceRule
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeriesKind
import kotlin.js.JsExport
import kotlin.js.JsName

/**
 * JavaScript interop helpers for the canonical recurrence model/domain.
 *
 * Canonical domain functions are exported directly when their model types have
 * a usable JS boundary. Helpers here only translate platform-friendly values
 * where Kotlin collection or enum construction would otherwise leak runtime
 * implementation details into Desktop.
 */
@JsExport
@JsName("recurrenceOccurrenceKey")
fun recurrenceOccurrenceKeyForJs(
    seriesId: String,
    dayKey: String,
): String =
    recurrenceOccurrenceKey(
        seriesId = seriesId,
        dayKey = dayKey,
    )

@JsExport
@JsName("recurrenceOccurrenceId")
fun recurrenceOccurrenceIdForJs(
    kind: String,
    seriesId: String,
    dayKey: String,
): String =
    recurrenceOccurrenceId(
        kind = parseRecurringSeriesKind(kind),
        seriesId = seriesId,
        dayKey = dayKey,
    )

/**
 * JavaScript interop constructor for the canonical KMP RecurrenceRule.
 *
 * Desktop passes only its serializable platform-boundary values. Conversion to
 * Kotlin collections and canonical enums happens inside the KMP runtime so the
 * caller never depends on kotlin-kotlin-stdlib implementation details.
 *
 * null or an empty weekday array means no explicit weekday selection.
 */
@JsExport
@JsName("createRecurrenceRule")
fun createRecurrenceRuleForJs(
    frequency: String,
    interval: Int,
    daysOfWeek: Array<String>?,
): RecurrenceRule =
    RecurrenceRule(
        frequency = parseRecurrenceFrequency(frequency),
        interval = interval,
        daysOfWeek = parseDaysOfWeekArray(daysOfWeek),
    )

/**
 * JavaScript interop constructor for canonical recurrence provenance.
 *
 * Desktop persistence uses JS numbers. Canonical KMP timestamps/versions use
 * Long. Conversion is accepted only for finite integer values that JavaScript
 * can represent exactly.
 */
@JsExport
@JsName("createRecurrenceOrigin")
fun createRecurrenceOriginForJs(
    seriesId: String,
    occurrenceDayKey: String,
    sourceSeriesVersion: Double,
): RecurrenceOrigin =
    RecurrenceOrigin(
        seriesId = seriesId,
        occurrenceDayKey = requireLocalDayKey(occurrenceDayKey),
        sourceSeriesVersion =
            requireJsSafeIntegerLong(
                sourceSeriesVersion,
                "sourceSeriesVersion",
            ),
    )

@JsExport
@JsName("requireLocalDayKey")
fun requireLocalDayKeyForJs(dayKey: String): String =
    requireLocalDayKey(dayKey)

private fun parseRecurrenceFrequency(value: String): RecurrenceFrequency =
    enumValueOf(value.trim().uppercase())

private fun parseRecurringSeriesKind(value: String): RecurringSeriesKind =
    enumValueOf(value.trim().uppercase())

private fun parseDaysOfWeekArray(value: Array<String>?): List<RecurrenceDayOfWeek>? {
    if (value.isNullOrEmpty()) return null

    return value.map { token ->
        enumValueOf<RecurrenceDayOfWeek>(token.trim().uppercase())
    }
}

