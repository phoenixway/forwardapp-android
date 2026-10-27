package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStream
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIterationStatus
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.AutocompleteSuggestions
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelComposer
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.ContextualActionsButton
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.ContextualComposerSurface
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption
import com.romankozak.forwardappmobile.features.missions.presentation.TacticsWorkspaceMode

@Composable
internal fun TacticsBottomPanelContent(
    inputValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    placeholder: String,
    autocompleteSuggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    onAddMissionFromContext: () -> Unit,
    onToggleScopeLinksSheet: () -> Unit,
    selectedMode: TacticsWorkspaceMode,
    missionStreams: List<MissionStream>,
    allMissionStreams: List<MissionStream>,
    selectedMissionStreamId: String,
    missionStreamCounts: Map<String, Int>,
    iterationStatus: TacticalIterationStatus?,
    iterationDurationDays: Int?,
    iterationDurationHours: Int?,
    activitySlotContexts: List<ContextPresentation>,
    selectedPlanningContextId: String?,
    projectOptions: List<ProjectOption>,
    canPasteAsMissions: Boolean,
    onModeSelected: (TacticsWorkspaceMode) -> Unit,
    onMissionStreamSelected: (String) -> Unit,
    onPlanningContextSelected: (String?) -> Unit,
    onOpenMissionStreamsSheet: () -> Unit,
    onBrowseBacklogSource: () -> Unit,
    onPasteMissions: () -> Unit,
) {
    ContextualComposerSurface {
        AutocompleteSuggestions(
            suggestions = autocompleteSuggestions, onSuggestionClick = onSuggestionClick,
            modifier = Modifier.fillMaxWidth(),
        )
        BottomPanelComposer(
            inputValue = inputValue, onValueChange = onValueChange, onSubmit = onSubmit,
            placeholderText = placeholder, sendContentDescription = "Створити місію", compact = true,
            trailingContent = {
                TacticsContextualTools(
                    selectedMode, missionStreams, allMissionStreams, selectedMissionStreamId,
                    missionStreamCounts, iterationDurationDays, iterationDurationHours,
                    activitySlotContexts, selectedPlanningContextId, projectOptions, canPasteAsMissions,
                    onModeSelected, onMissionStreamSelected, onPlanningContextSelected,
                    onOpenMissionStreamsSheet, onBrowseBacklogSource, onPasteMissions,
                    onAddMissionFromContext, onToggleScopeLinksSheet, iterationStatus, execution = false,
                )
            },
        )
    }
}

@Composable
internal fun TacticsExecutionStreamPanel(
    selectedMode: TacticsWorkspaceMode,
    missionStreams: List<MissionStream>,
    allMissionStreams: List<MissionStream>,
    selectedMissionStreamId: String,
    missionStreamCounts: Map<String, Int>,
    iterationDurationDays: Int?,
    iterationDurationHours: Int?,
    onModeSelected: (TacticsWorkspaceMode) -> Unit,
    onMissionStreamSelected: (String) -> Unit,
    onOpenMissionStreamsSheet: () -> Unit,
) {
    ContextualComposerSurface {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Тактики · ${if (selectedMode == TacticsWorkspaceMode.ALL) "Усі місії" else allMissionStreams.firstOrNull { it.id == selectedMissionStreamId }?.title ?: "Потоки"}",
                Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            TacticsContextualTools(
                if (selectedMode == TacticsWorkspaceMode.PLAN) TacticsWorkspaceMode.STREAMS else selectedMode,
                missionStreams, allMissionStreams, selectedMissionStreamId, missionStreamCounts,
                iterationDurationDays, iterationDurationHours, emptyList(), null, emptyList(), false,
                onModeSelected, onMissionStreamSelected, {}, onOpenMissionStreamsSheet, {}, {}, {}, {},
                TacticalIterationStatus.ACTIVE, execution = true,
            )
        }
    }
}

@Composable
private fun TacticsContextualTools(
    selectedMode: TacticsWorkspaceMode,
    missionStreams: List<MissionStream>,
    allMissionStreams: List<MissionStream>,
    selectedMissionStreamId: String,
    missionStreamCounts: Map<String, Int>,
    iterationDurationDays: Int?,
    iterationDurationHours: Int?,
    activitySlotContexts: List<ContextPresentation>,
    selectedPlanningContextId: String?,
    projectOptions: List<ProjectOption>,
    canPasteAsMissions: Boolean,
    onModeSelected: (TacticsWorkspaceMode) -> Unit,
    onMissionStreamSelected: (String) -> Unit,
    onPlanningContextSelected: (String?) -> Unit,
    onOpenMissionStreamsSheet: () -> Unit,
    onBrowseBacklogSource: () -> Unit,
    onPasteMissions: () -> Unit,
    onAddMissionFromContext: () -> Unit,
    onToggleScopeLinksSheet: () -> Unit,
    iterationStatus: TacticalIterationStatus?,
    execution: Boolean,
) {
    val title = "Тактики · ${when (selectedMode) {
        TacticsWorkspaceMode.STREAMS -> allMissionStreams.firstOrNull { it.id == selectedMissionStreamId }?.title ?: "Потоки"
        TacticsWorkspaceMode.ALL -> "Усі місії"
        TacticsWorkspaceMode.PLAN -> "План тижня"
    }}"
    ContextualActionsButton(
        title = title,
        actions = tacticsComposerActions(
            selectedMode, canPasteAsMissions, execution, onModeSelected, onOpenMissionStreamsSheet,
            onPasteMissions, onAddMissionFromContext, onToggleScopeLinksSheet,
        ),
        extraContent = { dismiss ->
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TacticalIterationDeadlineLine(iterationStatus, iterationDurationDays)
                when (selectedMode) {
                    TacticsWorkspaceMode.STREAMS -> MissionStreamChipRow(
                        missionStreams.map { MissionStreamChipUi(it.id, it.title, missionStreamCounts[it.id] ?: 0, it.budgetPercent) },
                        allMissionStreams.map { MissionStreamChipUi(it.id, it.title, missionStreamCounts[it.id] ?: 0, it.budgetPercent) },
                        selectedMissionStreamId, iterationDurationDays, iterationDurationHours,
                        allMissionStreams.sumOf { it.budgetPercent ?: 0 } > 100, onMissionStreamSelected,
                    )
                    TacticsWorkspaceMode.PLAN -> if (!execution) TacticsPlanControlRows(
                        missionStreams, selectedMissionStreamId, activitySlotContexts, selectedPlanningContextId,
                        projectOptions, onMissionStreamSelected, onPlanningContextSelected, { dismiss(); onBrowseBacklogSource() },
                    )
                    TacticsWorkspaceMode.ALL -> Unit
                }
            }
        },
    )
}
