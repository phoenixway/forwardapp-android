package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.usecases

import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.GENERAL_MISSION_STREAM_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

/** Both backlog badge facets consume one canonical Mission read, not two full frames. */
internal class WorkspaceBacklogTacticalProjection(source: Flow<List<TacticalMission>>, scope: CoroutineScope) {
    // The two StateFlows below own the five-second retention; do not add a
    // second timeout that keeps expensive reads alive after both release it.
    private val missions = source.shareIn(scope, SharingStarted.WhileSubscribed(), replay = 1)

    val itemIds: StateFlow<Set<String>> = missions
        .map { rows -> rows.mapNotNull(TacticalMission::sourceBacklogItemId).toSet() }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val streamIds: StateFlow<Map<String, String>> = missions
        .map { rows -> rows.mapNotNull { mission ->
            mission.sourceBacklogItemId?.let { it to (mission.missionStreamId ?: GENERAL_MISSION_STREAM_ID) }
        }.toMap() }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}
