package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayTaskSnapshot

/** Old peers cannot implicitly clear a field they do not transport. */
internal fun mergeDayTaskRealizations(local: DayTask?, incoming: DayTaskSnapshot, entity: DayTask): DayTask {
    if (local == null) return entity
    if (incoming.version < local.version || (local.isDeleted && incoming.version == local.version)) return local
    val refs = incoming.realizedDayFocusItemIds
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
