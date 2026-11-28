package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskRealizesOverride
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskRealizesTargetOverrides
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayTaskSnapshot

/** Old peers cannot implicitly clear a field they do not transport. */
internal fun mergeDayTaskRealizations(local: DayTask?, incoming: DayTaskSnapshot, entity: DayTask): DayTask {
    if (local == null) return entity
    if (incoming.version < local.version || (local.isDeleted && incoming.version == local.version)) return local
    val refs = incoming.realizedDayFocusItemIds
    val incomingOverride = incoming.realizesOverride
    val localOverride = local.realizesOverride
    if (incoming.version == local.version && refs != null && localOverride != null && incomingOverride != null &&
        !incoming.isDeleted && local.dayPlanId == entity.dayPlanId && local.recurrenceSeriesId != null &&
        local.recurrenceSeriesId == entity.recurrenceSeriesId && local.recurrenceOccurrenceDayKey == entity.recurrenceOccurrenceDayKey) {
        val localExplicit = local.realizedDayFocusItemIds - localOverride.priorities.inheritedLinks.values.toSet()
        val incomingExplicit = refs - incomingOverride.priorities.inheritedLinks.values.toSet()
        require(localExplicit.toSet() == incomingExplicit.toSet()) { "Concurrent edits to occurrence priority links: ${incoming.id}" }
        val merged = TaskRealizesOverride(
            mergeAppliedLinks(localOverride.themes, incomingOverride.themes),
            mergeAppliedLinks(localOverride.priorities, incomingOverride.priorities),
        )
        // Endpoints can arrive in separate exchanges. Reconciliation checks this provenance
        // against live endpoints and the winning template in the same merge.
        return entity.copy(realizesOverride = merged, realizedDayFocusItemIds = (refs + local.realizedDayFocusItemIds).distinct().sorted())
    }
    require(refs == null || incoming.version != local.version || refs.toSet() == local.realizedDayFocusItemIds.toSet()) {
        "Equal-version divergent DayTask realization references: ${incoming.id}"
    }
    val override = incoming.realizesOverride
    require(override == null || local.realizesOverride == null || incoming.version != local.version || override == local.realizesOverride) {
        "Equal-version divergent DayTask Realizes overrides: ${incoming.id}"
    }
    return entity.copy(
        realizedDayFocusItemIds = refs?.distinct()?.sorted() ?: local.realizedDayFocusItemIds,
        realizesOverride = override ?: local.realizesOverride,
    )
}


private fun mergeAppliedLinks(a: TaskRealizesTargetOverrides, b: TaskRealizesTargetOverrides): TaskRealizesTargetOverrides {
    require(a.explicit == b.explicit && a.excluded == b.excluded) { "Concurrent edits to occurrence Realizes overrides" }
    require(a.inheritedLinks.all { (target, link) -> b.inheritedLinks[target]?.let { it == link } != false }) {
        "A realization target has different concrete IDs within one day"
    }
    return a.copy(inheritedLinks = a.inheritedLinks + b.inheritedLinks)
}
