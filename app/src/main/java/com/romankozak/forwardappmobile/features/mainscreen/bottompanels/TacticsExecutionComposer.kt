package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.features.missions.presentation.TacticalMissionViewModel

/** Keep execution lightweight: no planning/backlog/picker subscriptions just to render its controls. */
@Composable
internal fun TacticsExecutionComposer(viewModel: TacticalMissionViewModel) {
    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val recentStreams by viewModel.recentMissionStreams.collectAsStateWithLifecycle()
    val streams by viewModel.missionStreams.collectAsStateWithLifecycle()
    val selectedStream by viewModel.selectedMissionStreamId.collectAsStateWithLifecycle()
    val counts by viewModel.missionStreamCounts.collectAsStateWithLifecycle()
    val days by viewModel.iterationDurationDays.collectAsStateWithLifecycle()
    val hours by viewModel.iterationDurationHours.collectAsStateWithLifecycle()
    TacticsExecutionStreamPanel(
        selectedMode, recentStreams, streams, selectedStream, counts, days, hours,
        viewModel::selectMode, viewModel::selectMissionStream, viewModel::openMissionStreamsSheet,
    )
}
