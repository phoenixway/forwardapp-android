package com.romankozak.forwardappmobile.features.missions.presentation

import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStream
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIteration
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIterationStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission

internal fun buildVisibleTacticalMissions(
    allMissions: List<TacticalMission>,
    requestedMode: TacticsWorkspaceMode,
    selectedStreamId: String,
    streams: List<MissionStream>,
    activeIteration: TacticalIteration?,
    currentWeekKey: String,
): List<TacticalMission> {
    val iterationMissions =
        allMissions
            .filter { it.isInCurrentIteration(activeIteration?.id, currentWeekKey) }
            .filter { it.sourceBacklogItemId == null }
    val effectiveMode = requestedMode.forIteration(activeIteration)

    return when (effectiveMode) {
        TacticsWorkspaceMode.STREAMS ->
            iterationMissions
                .filter { it.normalizedMissionStreamId() == selectedStreamId }
                .sortedByMissionOrder()
        TacticsWorkspaceMode.ALL -> {
            val streamOrderById = streams.mapIndexed { index, stream -> stream.id to index }.toMap()
            iterationMissions.sortedWith(
                compareBy<TacticalMission> {
                    streamOrderById[it.normalizedMissionStreamId()] ?: Int.MAX_VALUE
                }
                    .thenBy { it.orderInWeek }
                    .thenBy { it.createdAt },
            )
        }
        TacticsWorkspaceMode.PLAN -> iterationMissions.sortedByMissionOrder()
    }
}

private fun TacticsWorkspaceMode.forIteration(iteration: TacticalIteration?): TacticsWorkspaceMode =
    when {
        iteration?.status == TacticalIterationStatus.DRAFT -> TacticsWorkspaceMode.PLAN
        this == TacticsWorkspaceMode.PLAN && iteration?.status == TacticalIterationStatus.ACTIVE ->
            TacticsWorkspaceMode.STREAMS
        else -> this
    }

private fun List<TacticalMission>.sortedByMissionOrder(): List<TacticalMission> =
    sortedWith(compareBy<TacticalMission> { it.orderInWeek }.thenBy { it.createdAt })

internal fun List<TacticalMission>.debugMissionIds(): String =
    take(DEBUG_ID_LIMIT).joinToString(
        prefix = "[",
        postfix = if (size > DEBUG_ID_LIMIT) ",…]" else "]",
    ) { mission ->
        "${mission.id}:${mission.iterationId}:${mission.normalizedMissionStreamId()}"
    }

private const val DEBUG_ID_LIMIT = 12
