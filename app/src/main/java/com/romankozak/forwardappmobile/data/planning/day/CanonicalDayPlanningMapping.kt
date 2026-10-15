package com.romankozak.forwardappmobile.data.planning.day

import com.romankozak.forwardappmobile.core.data.models.entities.DayStatus
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayPlan
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScope
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeLifecycle
import com.romankozak.forwardappmobile.shared.core.domain.planning.DayPlanningOutcome
import com.romankozak.forwardappmobile.shared.core.domain.planning.DayPlanningScopeExtension

/**
 * Pure V3.4a mapping foundation for the future Day authority cutover.
 *
 * [DayPlan.id] is already a durable synchronized record identity independent
 * of the calendar key, so migration preserves it as [PlanningScopeId]. The
 * calendar-bound interpretation of [DayPlan.date] remains Day-extension state;
 * it also supplies the shared scope's optional lower horizon bound.
 *
 * This mapper deliberately produces no PlanningCommitment. Day Theme, focus,
 * responsibility, and task participation require an existing live Orientation
 * and are handled only by an explicitly activated domain cutover.
 */
internal fun DayPlan.toCanonicalDayPlanningScope(): PlanningScope =
    PlanningScope(
        id = PlanningScopeId(id),
        kind = PlanningScopeKind.DAY,
        lifecycle = status.toCanonicalDayScopeLifecycle(),
        title = name?.trim()?.takeIf(String::isNotEmpty),
        startsAt = date,
        endsAt = null,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
    )

internal fun DayStatus.toCanonicalDayScopeLifecycle(): PlanningScopeLifecycle =
    when (this) {
        DayStatus.PLANNED -> PlanningScopeLifecycle.DRAFT
        DayStatus.IN_PROGRESS -> PlanningScopeLifecycle.ACTIVE
        DayStatus.COMPLETED,
        DayStatus.MISSED,
        -> PlanningScopeLifecycle.CLOSED

        DayStatus.ARCHIVED -> PlanningScopeLifecycle.ARCHIVED
    }

/** Pure V3.4b mapping proof. Persistence remains dormant until the Day cutover migration. */
internal fun DayPlan.toCanonicalDayPlanningScopeExtension(): DayPlanningScopeExtension =
    DayPlanningScopeExtension(
        scopeId = PlanningScopeId(id),
        calendarDayStartMillis = date,
        outcome =
            when (status) {
                DayStatus.COMPLETED -> DayPlanningOutcome.COMPLETED
                DayStatus.MISSED -> DayPlanningOutcome.MISSED
                DayStatus.PLANNED,
                DayStatus.IN_PROGRESS,
                DayStatus.ARCHIVED,
                -> null
            },
        plannedBudgetMinutes = predictedDurationMinutes,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
    )
