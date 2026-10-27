package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStream
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.bottomPanelColors
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption
import com.romankozak.forwardappmobile.features.missions.presentation.TacticsWorkspaceMode

@Composable
internal fun TacticsPlanControlRows(
    missionStreams: List<MissionStream>,
    selectedMissionStreamId: String,
    activitySlotContexts: List<ContextPresentation>,
    selectedPlanningContextId: String?,
    projectOptions: List<ProjectOption>,
    onMissionStreamSelected: (String) -> Unit,
    onPlanningContextSelected: (String?) -> Unit,
    onBrowseBacklogSource: () -> Unit,
) {
    val streamOptions = missionStreams.map { PlanSelectorOption(it.id, it.title) }
    val sourceOptions =
        buildList {
            add(PlanSelectorOption(null, "Text input"))
            activitySlotContexts.forEach { context -> add(PlanSelectorOption(context.id, context.name)) }
            projectOptions.forEach { option -> add(PlanSelectorOption(option.id, option.name)) }
        }.distinctBy { it.id }
    val selectedStreamTitle =
        streamOptions.firstOrNull { it.id == selectedMissionStreamId }?.title ?: "Не вибрано"
    val selectedSourceTitle =
        sourceOptions.firstOrNull { it.id == selectedPlanningContextId }?.title ?: "Text input"
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        TacticsPlanSelectorRow(
            label = "У потік",
            value = selectedStreamTitle,
            options = streamOptions,
            onSelected = { option -> option.id?.let(onMissionStreamSelected) },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TacticsPlanSelectorRow(
                label = "З беклогу",
                value = selectedSourceTitle,
                options = sourceOptions,
                onSelected = { option -> onPlanningContextSelected(option.id) },
                modifier = Modifier.weight(1f),
            )
            TacticsBrowseBacklogButton(
                enabled = selectedPlanningContextId != null,
                onClick = onBrowseBacklogSource,
            )
        }
    }
}

internal data class PlanSelectorOption(
    val id: String?,
    val title: String,
)

internal fun resolvePlanningContextTitle(
    selectedPlanningContextId: String?,
    activitySlotContexts: List<ContextPresentation>,
    projectOptions: List<ProjectOption>,
): String =
    when {
        selectedPlanningContextId == null -> "Text input"
        else ->
            activitySlotContexts.firstOrNull { it.id == selectedPlanningContextId }?.name
                ?: projectOptions.firstOrNull { it.id == selectedPlanningContextId }?.name
                ?: "Backlog"
    }

internal fun buildCollapsedTacticsPanelSummary(
    selectedMode: TacticsWorkspaceMode,
    selectedStreamTitle: String?,
    selectedPlanningContextTitle: String?,
): String =
    when (selectedMode) {
        TacticsWorkspaceMode.STREAMS -> "Streams · ${selectedStreamTitle ?: "General"}"
        TacticsWorkspaceMode.PLAN -> "Plan · ${selectedPlanningContextTitle ?: "Text input"}"
        TacticsWorkspaceMode.ALL -> "All missions"
    }

@Composable
internal fun TacticsPlanSelectorRow(
    label: String,
    value: String,
    options: List<PlanSelectorOption>,
    onSelected: (PlanSelectorOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val colors = bottomPanelColors()
    Box(modifier = modifier) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(enabled = options.isNotEmpty()) { expanded = true },
            shape = RoundedCornerShape(14.dp),
            color = colors.inputContainer.copy(alpha = 0.68f),
            contentColor = colors.content,
            border = BorderStroke(1.dp, colors.border.copy(alpha = 0.24f)),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.mutedContent,
                )
                Text(
                    text = value,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.content,
                )
                Text(
                    text = ">",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.mutedContent,
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    },
                )
            }
        }
    }
}

@Composable
internal fun TacticsBrowseBacklogButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = bottomPanelColors()
    Surface(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color =
            if (enabled) {
                colors.selectedActionContainer.copy(alpha = 0.5f)
            } else {
                colors.inputContainer.copy(alpha = 0.38f)
            },
        contentColor =
            if (enabled) {
                colors.selectedActionContent
            } else {
                colors.mutedContent.copy(alpha = 0.6f)
            },
        border =
            BorderStroke(
                width = 1.dp,
                color = colors.border.copy(alpha = 0.24f),
            ),
    ) {
        Text(
            text = "Browse",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            maxLines = 1,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
