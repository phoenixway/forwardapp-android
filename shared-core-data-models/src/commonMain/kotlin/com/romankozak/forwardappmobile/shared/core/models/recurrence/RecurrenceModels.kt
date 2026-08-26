package com.romankozak.forwardappmobile.shared.core.models.recurrence

import com.romankozak.forwardappmobile.shared.core.models.day.TaskPriority
import com.romankozak.forwardappmobile.shared.core.models.link.CanonicalRelatedLink
import com.romankozak.forwardappmobile.shared.core.models.sync.SyncEntityMeta
import kotlin.js.JsExport

/** Calendar-day identity. Expected representation: YYYY-MM-DD. */
typealias LocalDayKey = String

@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}

@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
enum class RecurrenceDayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY,
}

/** interval must be >= 1 in valid canonical domain state. */
@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
data class RecurrenceRule(
    val frequency: RecurrenceFrequency,
    val interval: Int,
    val daysOfWeek: List<RecurrenceDayOfWeek>?,
)

@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
enum class RecurringSeriesKind {
    TASK,
    FOCUS,
    RESPONSIBILITY,
}

/** Content/defaults copied into a materialized DayTask. */
@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
data class RecurringTaskTemplate(
    val title: String,
    val description: String?,
    val goalId: String?,
    val linkedProjectIds: List<String>,
    val linkedAttachmentIds: List<String>,
    val priority: TaskPriority,
    val estimatedDurationMinutes: Long?,
    val points: Int,
    val projectId: String? = null,
    val taskType: String? = null,
    val executionStrictness: String? = null,
)

/** Content/defaults copied into a FOCUS or RESPONSIBILITY DayFocusItem. */
@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
data class RecurringFocusTemplate(
    val title: String,
    val notes: String?,
    val relatedLinks: List<CanonicalRelatedLink>,
    val budgetPercent: Int?,
)

/**
 * Canonical source of truth for recurrence scheduling.
 *
 * A series owns what should be materialized and on which calendar days. It does
 * not own per-day completion or deletion state.
 */
@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
sealed interface RecurringSeries : SyncEntityMeta {
    val kind: RecurringSeriesKind
    val rule: RecurrenceRule
    val startDayKey: LocalDayKey
    val endDayKey: LocalDayKey?
}

@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
data class RecurringTaskSeries(
    override val id: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val syncedAt: Long?,
    override val isDeleted: Boolean,
    override val version: Long,
    override val rule: RecurrenceRule,
    override val startDayKey: LocalDayKey,
    override val endDayKey: LocalDayKey?,
    val template: RecurringTaskTemplate,
) : RecurringSeries {
    override val kind: RecurringSeriesKind
        get() = RecurringSeriesKind.TASK
}

@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
data class RecurringFocusSeries(
    override val id: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val syncedAt: Long?,
    override val isDeleted: Boolean,
    override val version: Long,
    override val rule: RecurrenceRule,
    override val startDayKey: LocalDayKey,
    override val endDayKey: LocalDayKey?,
    val template: RecurringFocusTemplate,
) : RecurringSeries {
    override val kind: RecurringSeriesKind
        get() = RecurringSeriesKind.FOCUS
}

@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
data class RecurringResponsibilitySeries(
    override val id: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val syncedAt: Long?,
    override val isDeleted: Boolean,
    override val version: Long,
    override val rule: RecurrenceRule,
    override val startDayKey: LocalDayKey,
    override val endDayKey: LocalDayKey?,
    val template: RecurringFocusTemplate,
) : RecurringSeries {
    override val kind: RecurringSeriesKind
        get() = RecurringSeriesKind.RESPONSIBILITY
}

/** Provenance carried by the materialized DayTask or DayFocusItem itself. */
@OptIn(kotlin.js.ExperimentalJsExport::class)
@JsExport
data class RecurrenceOrigin(
    val seriesId: String,
    val occurrenceDayKey: LocalDayKey,
    val sourceSeriesVersion: Long,
)

const val RECURRENCE_MODEL_VERSION: Int = 2
