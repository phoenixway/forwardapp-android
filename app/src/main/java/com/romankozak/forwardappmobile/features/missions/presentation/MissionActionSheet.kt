package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.*
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MissionActionSheet(
    mission: TacticalMission,
    missionStreams: List<MissionStream>,
    selectedMissionStreamId: String,
    activitySlotContexts: List<ContextPresentation>,
    selectedActivitySlotContextId: String?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onToggleCompleted: () -> Unit,
    onAddToToday: () -> Unit,
    onAddToArc: () -> Unit,
    onAssignMissionStream: (String) -> Unit,
    onOpenStreamPicker: () -> Unit,
    onAssignActivitySlot: (String?) -> Unit,
    onOpenSlotPicker: () -> Unit,
    onPostpone: () -> Unit,
    onContinue: () -> Unit,
    onCopyMission: () -> Unit,
    onCutMission: () -> Unit,
    onDeleteMission: () -> Unit,
    semanticWorkspaceAction: @Composable () -> Unit = {},
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = mission.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
            )
            HorizontalDivider()

            semanticWorkspaceAction()

            MissionActionSection(title = "Status") {
                MissionActionSheetItem(
                    icon = Icons.Outlined.CheckCircle,
                    text =
                        if (mission.status == MissionStatus.COMPLETED) {
                            "Mark open"
                        } else {
                            "Mark done"
                        },
                    onClick = onToggleCompleted,
                )
            }

            MissionActionSection(title = "Plan") {
                MissionActionSheetItem(
                    icon = Icons.Outlined.Today,
                    text = "Add to today",
                    onClick = onAddToToday,
                )
                SubtleActionDivider()
                MissionActionSheetItem(
                    icon = Icons.Outlined.AccountTree,
                    text = "Add to arc",
                    onClick = onAddToArc,
                )
                SubtleActionDivider()
                MissionActionSheetItem(
                    icon = Icons.Outlined.Today,
                    text = "Postpone",
                    onClick = onPostpone,
                )
                SubtleActionDivider()
                MissionActionSheetItem(
                    icon = Icons.Outlined.Today,
                    text = "Extend",
                    onClick = onContinue,
                )
            }

            MissionActionSection(title = "Organize") {
                if (mission.normalizedMissionStreamId() != selectedMissionStreamId) {
                    MissionActionSheetItem(
                        icon = Icons.Outlined.AccountTree,
                        text = "Current stream",
                        onClick = { onAssignMissionStream(selectedMissionStreamId) },
                    )
                    SubtleActionDivider()
                }
                MissionActionSheetItem(
                    icon = Icons.Outlined.ChevronRight,
                    text = "Move stream",
                    enabled = missionStreams.isNotEmpty(),
                    onClick = onOpenStreamPicker,
                )
                SubtleActionDivider()
                if (
                    selectedActivitySlotContextId != null &&
                    mission.activitySlotContextId != selectedActivitySlotContextId
                ) {
                    MissionActionSheetItem(
                        icon = Icons.Outlined.AccountTree,
                        text = "Current slot",
                        onClick = { onAssignActivitySlot(selectedActivitySlotContextId) },
                    )
                    SubtleActionDivider()
                }
                MissionActionSheetItem(
                    icon = Icons.Outlined.ChevronRight,
                    text =
                        if (mission.activitySlotContextId == null) {
                            "Add slot"
                        } else {
                            "Change slot"
                        },
                    enabled =
                        activitySlotContexts.isNotEmpty() ||
                            mission.activitySlotContextId != null,
                    onClick = onOpenSlotPicker,
                )
            }

            MissionActionSection(title = "Edit") {
                MissionActionSheetItem(
                    icon = Icons.Outlined.Edit,
                    text = "Edit",
                    onClick = onEdit,
                )
                SubtleActionDivider()
                MissionActionSheetItem(
                    icon = Icons.Outlined.ContentCopy,
                    text = "Copy",
                    onClick = onCopyMission,
                )
                SubtleActionDivider()
                MissionActionSheetItem(
                    icon = Icons.Outlined.ContentCut,
                    text = "Cut",
                    onClick = onCutMission,
                )
                SubtleActionDivider()
                MissionActionSheetItem(
                    icon = Icons.Outlined.DeleteOutline,
                    text = "Delete",
                    textColor = MaterialTheme.colorScheme.error,
                    onClick = onDeleteMission,
                )
            }

            HorizontalDivider()
            MissionActionSheetItem(
                icon = Icons.Outlined.Close,
                text = "Cancel",
                onClick = onDismiss,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MissionActionSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
        )
        content()
    }
}
