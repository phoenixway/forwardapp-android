package com.romankozak.forwardappmobile.shared.core.domain.recurrence

import com.romankozak.forwardappmobile.shared.core.models.day.CanonicalDayDatabase
import com.romankozak.forwardappmobile.shared.core.models.day.DayFocusItem
import com.romankozak.forwardappmobile.shared.core.models.day.DayFocusType
import com.romankozak.forwardappmobile.shared.core.models.day.DayPlan
import com.romankozak.forwardappmobile.shared.core.models.day.DayTask
import com.romankozak.forwardappmobile.shared.core.models.day.TaskStatus
import com.romankozak.forwardappmobile.shared.core.models.recurrence.LocalDayKey
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurrenceOrigin
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringFocusSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringResponsibilitySeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringSeries
import com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSeries

enum class RecurrenceMaterializationStatus {
    MATERIALIZED,
    PLAN_NOT_FOUND,
}

/**
 * Pure materialization result.
 *
 * The shared domain plans canonical entities but does not persist them. Platform
 * adapters apply tasksToCreate/focusItemsToCreate transactionally to their own
 * storage.
 */
data class RecurrenceMaterializationPlan(
    val status: RecurrenceMaterializationStatus,
    val dayKey: LocalDayKey,
    val dayPlanId: String?,
    val tasksToCreate: List<DayTask>,
    val focusItemsToCreate: List<DayFocusItem>,
    val skippedExistingOccurrenceKeys: List<String>,
) {
    val createdTaskIds: List<String>
        get() = tasksToCreate.map { it.id }

    val createdFocusItemIds: List<String>
        get() = focusItemsToCreate.map { it.id }
}

private fun findActiveDayPlan(
    database: CanonicalDayDatabase,
    dayKey: LocalDayKey,
): DayPlan? {
    val matchingPlans =
        database.dayPlans.filter { plan ->
            !plan.isDeleted && plan.dayKey == dayKey
        }

    check(matchingPlans.size <= 1) {
        "Canonical Day invariant violated: multiple active DayPlans for $dayKey"
    }

    return matchingPlans.firstOrNull()
}

private fun sameOccurrence(
    recurrence: RecurrenceOrigin?,
    seriesId: String,
    dayKey: LocalDayKey,
): Boolean =
    recurrence != null &&
        recurrence.seriesId == seriesId &&
        recurrence.occurrenceDayKey == dayKey

/**
 * Deliberately includes soft-deleted entities.
 *
 * A tombstone still occupies (seriesId, occurrenceDayKey), preventing a deleted
 * occurrence from being regenerated.
 */
private fun occurrenceAlreadyExists(
    database: CanonicalDayDatabase,
    series: RecurringSeries,
    dayKey: LocalDayKey,
): Boolean =
    when (series) {
        is RecurringTaskSeries ->
            database.dayTasks.any { task ->
                sameOccurrence(task.recurrence, series.id, dayKey)
            }

        is RecurringFocusSeries,
        is RecurringResponsibilitySeries ->
            database.dayFocusItems.any { item ->
                sameOccurrence(item.recurrence, series.id, dayKey)
            }
    }

private fun assertOccurrenceIdAvailable(
    database: CanonicalDayDatabase,
    plannedTasks: List<DayTask>,
    plannedFocusItems: List<DayFocusItem>,
    series: RecurringSeries,
    dayKey: LocalDayKey,
) {
    val id = recurrenceOccurrenceId(series.kind, series.id, dayKey)

    val collision =
        database.dayTasks.any { it.id == id } ||
            database.dayFocusItems.any { it.id == id } ||
            plannedTasks.any { it.id == id } ||
            plannedFocusItems.any { it.id == id }

    check(!collision) {
        "Canonical recurrence occurrence id collision: $id"
    }
}

private fun nextTaskOrder(
    database: CanonicalDayDatabase,
    plannedTasks: List<DayTask>,
    planId: String,
): Long {
    val orders =
        buildList {
            database.dayTasks
                .filter { task -> task.dayPlanId == planId && !task.isDeleted }
                .mapTo(this) { it.order }
            plannedTasks
                .filter { task -> task.dayPlanId == planId && !task.isDeleted }
                .mapTo(this) { it.order }
        }

    return if (orders.isEmpty()) 0L else orders.max() + 1L
}

private fun nextFocusOrder(
    database: CanonicalDayDatabase,
    plannedFocusItems: List<DayFocusItem>,
    planId: String,
    type: DayFocusType,
): Long {
    val orders =
        buildList {
            database.dayFocusItems
                .filter { item ->
                    item.dayPlanId == planId &&
                        item.type == type &&
                        !item.isDeleted
                }.mapTo(this) { it.order }
            plannedFocusItems
                .filter { item ->
                    item.dayPlanId == planId &&
                        item.type == type &&
                        !item.isDeleted
                }.mapTo(this) { it.order }
        }

    return if (orders.isEmpty()) 0L else orders.max() + 1L
}

private fun materializeTask(
    database: CanonicalDayDatabase,
    plannedTasks: List<DayTask>,
    plan: DayPlan,
    series: RecurringTaskSeries,
    dayKey: LocalDayKey,
    now: Long,
): DayTask =
    DayTask(
        id = recurrenceOccurrenceId(series.kind, series.id, dayKey),
        createdAt = now,
        updatedAt = now,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
        dayPlanId = plan.id,
        recurrence = recurrenceOrigin(series, dayKey),
        title = series.template.title,
        description = series.template.description,
        goalId = series.template.goalId,
        projectId = series.template.projectId,
        linkedProjectIds = series.template.linkedProjectIds.toList(),
        linkedAttachmentIds = series.template.linkedAttachmentIds.toList(),
        activityRecordId = null,
        taskType = series.template.taskType ?: if (series.template.goalId != null) "GOAL" else null,
        entityId = null,
        order = nextTaskOrder(database, plannedTasks, plan.id),
        priority = series.template.priority,
        status = TaskStatus.NOT_STARTED,
        completed = false,
        scheduledTime = null,
        estimatedDurationMinutes = series.template.estimatedDurationMinutes,
        actualDurationMinutes = null,
        dueTime = null,
        executionStrictness = series.template.executionStrictness ?: "NORMAL",
        valueImportance = 0f,
        valueImpact = 0f,
        effort = 0f,
        cost = 0f,
        risk = 0f,
        location = null,
        tags = emptyList(),
        notes = null,
        completedAt = null,
        points = series.template.points,
    )

private fun materializeFocus(
    database: CanonicalDayDatabase,
    plannedFocusItems: List<DayFocusItem>,
    plan: DayPlan,
    series: RecurringFocusSeries,
    dayKey: LocalDayKey,
    now: Long,
): DayFocusItem =
    DayFocusItem(
        id = recurrenceOccurrenceId(series.kind, series.id, dayKey),
        createdAt = now,
        updatedAt = now,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
        dayPlanId = plan.id,
        recurrence = recurrenceOrigin(series, dayKey),
        title = series.template.title,
        notes = series.template.notes,
        relatedLinks = series.template.relatedLinks.toList(),
        type = DayFocusType.FOCUS,
        budgetPercent = series.template.budgetPercent,
        order = nextFocusOrder(database, plannedFocusItems, plan.id, DayFocusType.FOCUS),
    )

private fun materializeResponsibility(
    database: CanonicalDayDatabase,
    plannedFocusItems: List<DayFocusItem>,
    plan: DayPlan,
    series: RecurringResponsibilitySeries,
    dayKey: LocalDayKey,
    now: Long,
): DayFocusItem =
    DayFocusItem(
        id = recurrenceOccurrenceId(series.kind, series.id, dayKey),
        createdAt = now,
        updatedAt = now,
        syncedAt = null,
        isDeleted = false,
        version = 1L,
        dayPlanId = plan.id,
        recurrence = recurrenceOrigin(series, dayKey),
        title = series.template.title,
        notes = series.template.notes,
        relatedLinks = series.template.relatedLinks.toList(),
        type = DayFocusType.RESPONSIBILITY,
        budgetPercent = series.template.budgetPercent,
        order = nextFocusOrder(
            database,
            plannedFocusItems,
            plan.id,
            DayFocusType.RESPONSIBILITY,
        ),
    )

/**
 * Plans missing canonical recurrence occurrences for one already-resolved working
 * calendar day.
 *
 * The caller owns wall-clock/day-boundary resolution. This function never derives
 * occurrence identity from now.
 *
 * It does not create DayPlans, resolve sync conflicts, revive existing entities,
 * or ignore tombstones.
 */
fun planRecurringSeriesForDay(
    database: CanonicalDayDatabase,
    dayKey: LocalDayKey,
    now: Long,
): RecurrenceMaterializationPlan {
    requireLocalDayKey(dayKey)

    val plan = findActiveDayPlan(database, dayKey)
        ?: return RecurrenceMaterializationPlan(
            status = RecurrenceMaterializationStatus.PLAN_NOT_FOUND,
            dayKey = dayKey,
            dayPlanId = null,
            tasksToCreate = emptyList(),
            focusItemsToCreate = emptyList(),
            skippedExistingOccurrenceKeys = emptyList(),
        )

    val plannedTasks = mutableListOf<DayTask>()
    val plannedFocusItems = mutableListOf<DayFocusItem>()
    val skippedExistingOccurrenceKeys = mutableListOf<String>()

    for (series in database.recurringSeries) {
        if (!recurringSeriesMatchesDay(series, dayKey)) continue

        val occurrenceKey = recurrenceOccurrenceKey(series.id, dayKey)

        if (occurrenceAlreadyExists(database, series, dayKey)) {
            skippedExistingOccurrenceKeys += occurrenceKey
            continue
        }

        assertOccurrenceIdAvailable(
            database = database,
            plannedTasks = plannedTasks,
            plannedFocusItems = plannedFocusItems,
            series = series,
            dayKey = dayKey,
        )

        when (series) {
            is RecurringTaskSeries ->
                plannedTasks +=
                    materializeTask(
                        database = database,
                        plannedTasks = plannedTasks,
                        plan = plan,
                        series = series,
                        dayKey = dayKey,
                        now = now,
                    )

            is RecurringFocusSeries ->
                plannedFocusItems +=
                    materializeFocus(
                        database = database,
                        plannedFocusItems = plannedFocusItems,
                        plan = plan,
                        series = series,
                        dayKey = dayKey,
                        now = now,
                    )

            is RecurringResponsibilitySeries ->
                plannedFocusItems +=
                    materializeResponsibility(
                        database = database,
                        plannedFocusItems = plannedFocusItems,
                        plan = plan,
                        series = series,
                        dayKey = dayKey,
                        now = now,
                    )
        }
    }

    return RecurrenceMaterializationPlan(
        status = RecurrenceMaterializationStatus.MATERIALIZED,
        dayKey = dayKey,
        dayPlanId = plan.id,
        tasksToCreate = plannedTasks,
        focusItemsToCreate = plannedFocusItems,
        skippedExistingOccurrenceKeys = skippedExistingOccurrenceKeys,
    )
}
