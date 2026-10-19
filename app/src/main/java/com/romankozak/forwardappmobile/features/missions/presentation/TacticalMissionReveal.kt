package com.romankozak.forwardappmobile.features.missions.presentation

import com.romankozak.forwardappmobile.core.data.models.entities.tactical.*
import com.romankozak.forwardappmobile.features.missions.presentation.missionlist.orderedMissions

internal fun tacticalRenderedMissionIds(rows: List<TacticalMission>, status: TacticalIterationStatus?): List<Long> = when (status) {
    TacticalIterationStatus.DRAFT -> rows.filterNot { it.status == MissionStatus.COMPLETED }
    TacticalIterationStatus.ACTIVE -> rows
    else -> orderedMissions(rows, showStatusSections = true)
}.map { it.id }
