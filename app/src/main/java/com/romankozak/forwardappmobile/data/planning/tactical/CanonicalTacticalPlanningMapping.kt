package com.romankozak.forwardappmobile.data.planning.tactical

import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIteration
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIterationStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIterationType
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScope
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeId
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeKind
import com.romankozak.forwardappmobile.shared.core.domain.planning.PlanningScopeLifecycle
import com.romankozak.forwardappmobile.shared.core.domain.planning.TacticalPlanningScopeExtension
import com.romankozak.forwardappmobile.shared.core.domain.planning.TacticalPlanningScopeType

/**
 * Pure deterministic V3.5a scope mapping.
 *
 * It establishes no persistence or runtime authority. Tactical mission
 * commitments remain gated on explicit canonical Orientation targets.
 */
internal fun TacticalIteration.toCanonicalTacticalPlanningScope(): PlanningScope {
    require(title.isNotBlank()) { "Tactical iteration $id has a blank title" }
    return PlanningScope(
        id = PlanningScopeId(id),
        kind = PlanningScopeKind.TACTICAL_CYCLE,
        lifecycle = status.toCanonicalTacticalScopeLifecycle(),
        title = title,
        startsAt = startedAt,
        endsAt = plannedEndAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
    )
}

internal fun TacticalIteration.toCanonicalTacticalPlanningScopeExtension(): TacticalPlanningScopeExtension =
    TacticalPlanningScopeExtension(
        scopeId = PlanningScopeId(id),
        type =
            when (type) {
                TacticalIterationType.TIMEBOXED -> TacticalPlanningScopeType.TIMEBOXED
                TacticalIterationType.OPEN_ENDED -> TacticalPlanningScopeType.OPEN_ENDED
            },
        weekKey = weekKey,
        closedAt = closedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
    )

internal fun TacticalIterationStatus.toCanonicalTacticalScopeLifecycle(): PlanningScopeLifecycle =
    when (this) {
        TacticalIterationStatus.DRAFT -> PlanningScopeLifecycle.DRAFT
        TacticalIterationStatus.ACTIVE -> PlanningScopeLifecycle.ACTIVE
        TacticalIterationStatus.CLOSED -> PlanningScopeLifecycle.CLOSED
        TacticalIterationStatus.ARCHIVED -> PlanningScopeLifecycle.ARCHIVED
    }
