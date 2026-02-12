package com.romankozak.forwardappmobile.features.missions.presentation.missionlist

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentOption
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedCheckboxStyle
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedItemCheckbox
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedItemState
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemSurface
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemTokens
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedStatusChipSpec
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedStatusRow
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedTrailingActionButton
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TacticalMissionList(
    missions: List<TacticalMission>,
    projectOptions: List<ProjectOption>,
    attachmentOptions: List<AttachmentOption>,
    selectedMissionIds: Set<Long>,
    selectionMode: Boolean,
    onMissionToggled: (TacticalMission) -> Unit,
    onMissionSelectionToggle: (TacticalMission) -> Unit,
    onMissionClick: (TacticalMission) -> Unit,
    onMissionLongPress: (TacticalMission) -> Unit,
    onMissionMoreClick: (TacticalMission) -> Unit,
    onMissionsReordered: (List<TacticalMission>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    var internalMissions by remember(missions) { mutableStateOf(missions) }
    val projectNameById =
        remember(projectOptions) {
            projectOptions.associate { it.id to it.name }
        }
    val attachmentNameById =
        remember(attachmentOptions) {
            attachmentOptions.associate { it.id to it.name }
        }
    val lazyListState = rememberLazyListState()
    val reorderableState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            internalMissions =
                internalMissions.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
            onMissionsReordered(internalMissions)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(UnifiedListItemTokens.OuterVerticalSpacing * 2),
    ) {
        items(internalMissions, key = { it.id }) { mission ->
            ReorderableItem(reorderableState, key = mission.id) {
                TacticalMissionCard(
                    mission = mission,
                    isSelected = mission.id in selectedMissionIds,
                    selectionMode = selectionMode,
                    projectNameById = projectNameById,
                    attachmentNameById = attachmentNameById,
                    onMissionToggled = { onMissionToggled(mission) },
                    onMissionSelectionToggle = { onMissionSelectionToggle(mission) },
                    onMissionClick = { onMissionClick(mission) },
                    onMissionLongPress = { onMissionLongPress(mission) },
                    onMissionMoreClick = { onMissionMoreClick(mission) },
                    checkboxDragHandleModifier = Modifier.draggableHandle(),
                )
            }
        }
    }
}

@Composable
fun TacticalMissionCard(
    mission: TacticalMission,
    isSelected: Boolean,
    selectionMode: Boolean,
    projectNameById: Map<String, String>,
    attachmentNameById: Map<String, String>,
    onMissionToggled: () -> Unit,
    onMissionSelectionToggle: () -> Unit,
    onMissionClick: () -> Unit,
    onMissionLongPress: () -> Unit,
    onMissionMoreClick: () -> Unit,
    checkboxDragHandleModifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val colorScheme = MaterialTheme.colorScheme
    val isInactive = mission.status == MissionStatus.INACTIVE
    val isPaused = mission.status == MissionStatus.PAUSED

    val overdue = mission.status == MissionStatus.ACTIVE && System.currentTimeMillis() > mission.deadline
    val itemState =
        when {
            mission.status == MissionStatus.COMPLETED -> UnifiedItemState.COMPLETED
            overdue -> UnifiedItemState.OVERDUE
            else -> UnifiedItemState.DEFAULT
        }

    val missionContainerColor =
        when (itemState) {
            UnifiedItemState.COMPLETED -> colorScheme.secondaryContainer.copy(alpha = 0.36f)
            UnifiedItemState.OVERDUE -> colorScheme.errorContainer.copy(alpha = 0.58f)
            UnifiedItemState.DEFAULT ->
                when {
                    isInactive -> colorScheme.surfaceVariant.copy(alpha = 0.70f)
                    isPaused -> colorScheme.surfaceVariant.copy(alpha = 0.56f)
                    else -> colorScheme.tertiaryContainer.copy(alpha = 0.40f)
                }
            UnifiedItemState.SELECTED -> colorScheme.surfaceContainerHighest
            UnifiedItemState.DISABLED -> colorScheme.surfaceVariant.copy(alpha = 0.6f)
        }
    val missionBorderColor =
        when (itemState) {
            UnifiedItemState.COMPLETED -> colorScheme.secondary.copy(alpha = 0.35f)
            UnifiedItemState.OVERDUE -> colorScheme.error.copy(alpha = 0.50f)
            UnifiedItemState.DEFAULT ->
                when {
                    isInactive -> colorScheme.outline.copy(alpha = 0.50f)
                    isPaused -> colorScheme.outlineVariant.copy(alpha = 0.44f)
                    else -> colorScheme.tertiary.copy(alpha = 0.38f)
                }
            UnifiedItemState.SELECTED -> colorScheme.primary.copy(alpha = 0.4f)
            UnifiedItemState.DISABLED -> colorScheme.outlineVariant.copy(alpha = 0.35f)
        }

    UnifiedListItemSurface(
        isSelected = isSelected,
        state = itemState,
        containerColorOverride = missionContainerColor,
        borderColorOverride = missionBorderColor,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier.combinedClickable(
                    onClick = {
                        if (selectionMode) {
                            onMissionSelectionToggle()
                        } else {
                            onMissionClick()
                        }
                    },
                    onLongClick = { onMissionLongPress() },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UnifiedItemCheckbox(
                checked = if (selectionMode) isSelected else mission.status == MissionStatus.COMPLETED,
                onCheckedChange = {
                    if (selectionMode) {
                        onMissionSelectionToggle()
                    } else {
                        onMissionToggled()
                    }
                },
                style = UnifiedCheckboxStyle.Round,
                modifier = checkboxDragHandleModifier,
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedBorderColor = onSurface.copy(alpha = 0.7f),
            )

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                val titleColor by animateColorAsState(
                    when {
                        mission.status == MissionStatus.COMPLETED -> onSurface.copy(alpha = 0.4f)
                        isInactive -> onSurface.copy(alpha = 0.58f)
                        isPaused -> onSurface.copy(alpha = 0.74f)
                        overdue -> Color(0xFFFF6E6E)
                        else -> onSurface
                    },
                    label = "mission_title_color",
                )

                androidx.compose.material3.Text(
                    mission.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = titleColor,
                    maxLines = 4,
                    overflow = TextOverflow.Clip,
                    textDecoration = if (mission.status == MissionStatus.COMPLETED) TextDecoration.LineThrough else null,
                )

                mission.description?.takeIf { it.isNotBlank() }?.let { description ->
                    androidx.compose.material3.Text(
                        description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onSurface.copy(alpha = 0.7f),
                        textDecoration = if (mission.status == MissionStatus.COMPLETED) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }

                val statusItems =
                    buildList {
                        if (isInactive) {
                            add(
                                UnifiedStatusChipSpec(
                                    text = "Неактивна",
                                    contentColor = onSurface.copy(alpha = 0.72f),
                                ),
                            )
                        }
                        if (isPaused) {
                            add(
                                UnifiedStatusChipSpec(
                                    text = "На паузі",
                                    contentColor = colorScheme.tertiary,
                                ),
                            )
                        }
                        add(
                            UnifiedStatusChipSpec(
                                icon = if (overdue) Icons.Outlined.Warning else Icons.Outlined.Schedule,
                                text = formatMissionDate(mission.deadline),
                                contentColor = if (overdue) MaterialTheme.colorScheme.error else onSurface.copy(alpha = 0.75f),
                            ),
                        )
                    }
                UnifiedStatusRow(items = statusItems, modifier = Modifier.padding(top = 2.dp))

                val linkedContextIds =
                    mission.linkedProjectIds
                        .orEmpty()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                val linkedAttachmentIds =
                    mission.linkedAttachmentIds
                        .orEmpty()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()

                val linkedItems =
                    buildList {
                        linkedContextIds.forEach { id ->
                            add(
                                UnifiedStatusChipSpec(
                                    icon = Icons.Outlined.AccountTree,
                                    text = projectNameById[id] ?: id,
                                    contentColor = onSurface.copy(alpha = 0.78f),
                                ),
                            )
                        }
                        linkedAttachmentIds.forEach { id ->
                            add(
                                UnifiedStatusChipSpec(
                                    icon = Icons.Outlined.AttachFile,
                                    text = attachmentNameById[id] ?: id,
                                    contentColor = onSurface.copy(alpha = 0.78f),
                                ),
                            )
                        }
                    }
                if (linkedItems.isNotEmpty()) {
                    UnifiedStatusRow(items = linkedItems, modifier = Modifier.padding(top = 2.dp))
                }
            }

            if (!selectionMode) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    UnifiedTrailingActionButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = "Дії",
                        onClick = onMissionMoreClick,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    )
                }
            }
        }
    }
}

private fun formatMissionDate(ts: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(ts))
}
