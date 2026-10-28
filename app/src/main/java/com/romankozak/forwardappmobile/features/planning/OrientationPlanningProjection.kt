package com.romankozak.forwardappmobile.features.planning

import com.romankozak.forwardappmobile.data.planning.*
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.shared.core.domain.planning.*

internal fun PlanningCommitmentCompositionProjection.forOrientation(id: String) =
    byOrientation[id].orEmpty()

internal fun PlanningScopeKind.horizonLabel(): String = when (this) {
    PlanningScopeKind.STRATEGIC_ARC -> "Strategic"
    PlanningScopeKind.TACTICAL_CYCLE -> "Tactical"
    PlanningScopeKind.DAY -> "Day"
}

internal data class PlanningContextBadge(val participant: PlanningParticipantSummary, val label: String)

/** One batched projection for the entire backlog. Open scopes + unfinished participation only. */
internal fun PlanningCommitmentCompositionProjection.planningBadgesByOrientation(): Map<String, List<PlanningContextBadge>> {
    fun eligible(item: PlanningParticipantSummary) = item.scope.lifecycle in setOf(PlanningScopeLifecycle.ACTIVE, PlanningScopeLifecycle.DRAFT) &&
        item.commitment.status !in setOf(PlanningCommitmentStatus.COMPLETED, PlanningCommitmentStatus.DROPPED)
    return participants.filter(::eligible).groupBy { it.commitment.orientationId }.mapValues { (_, items) ->
        items.flatMap { item ->
            val parents = outgoing(item.commitment.id).map { it.participant }.filter(::eligible)
            if (parents.isNotEmpty()) parents.map { PlanningContextBadge(it, "${it.scope.kind.horizonLabel()} · ${it.entity.title}") }
            else listOf(PlanningContextBadge(item, "${item.scope.kind.horizonLabel()} · ${item.horizonLabel ?: item.scope.title ?: item.contextLabel}"))
        }.distinctBy { it.participant.commitment.id }
            .sortedWith(compareBy({ it.participant.scope.kind.ordinal }, { it.label }, { it.participant.commitment.id.value }))
    }
}

internal fun orientationPlanningTarget(ref: CanonicalEntityRef) =
    (ref as? CanonicalEntityRef.Orientation)?.let {
        com.romankozak.forwardappmobile.core.navigation.NavTarget.OrientationPlanning(it.id)
    }
