package com.romankozak.forwardappmobile.shared.core.models.recurrence

import com.romankozak.forwardappmobile.shared.core.models.day.TaskPriority
import com.romankozak.forwardappmobile.shared.core.models.sync.SyncEntityMeta

/** Calendar-day identity. Expected representation: YYYY-MM-DD. */
typealias LocalDayKey = String

enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
}

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
data class RecurrenceRule(
    val frequency: RecurrenceFrequency,
    val interval: Int,
    val daysOfWeek: List<RecurrenceDayOfWeek>?,
)

enum class RecurringSeriesKind {
    TASK,
    FOCUS,
    RESPONSIBILITY,
}

/** Content/defaults copied into a materialized DayTask. */
data class RecurringTaskTemplate(
    val title: String,
    val description: String?,
    val goalId: String?,
    val linkedProjectIds: List<String>,
    val linkedAttachmentIds: List<String>,
    val priority: TaskPriority,
    val estimatedDurationMinutes: Long?,
    val points: Int,
)

/** Content/defaults copied into a FOCUS or RESPONSIBILITY DayFocusItem. */
data class RecurringFocusTemplate(
    val title: String,
    val notes: String?,
    val relatedLinks: List<Any?>,
    val budgetPercent: Int?,
)

/**
 * Canonical source of truth for recurrence scheduling.
 *
 * A series owns what should be materialized and on which calendar days. It does
 * not own per-day completion or deletion state.
 */
sealed interface RecurringSeries : SyncEntityMeta {
    val kind: RecurringSeriesKind
    val rule: RecurrenceRule
    val startDayKey: LocalDayKey
    val endDayKey: LocalDayKey?
}

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
data class RecurrenceOrigin(
    val seriesId: String,
    val occurrenceDayKey: LocalDayKey,
    val sourceSeriesVersion: Long,
)

const val RECURRENCE_MODEL_VERSION: Int = 2
