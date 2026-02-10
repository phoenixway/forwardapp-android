package com.romankozak.forwardappmobile.features.missions.presentation.missionlist

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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

@Composable
fun TacticalMissionList(
    missions: List<TacticalMission>,
    onMissionToggled: (TacticalMission) -> Unit,
    onMissionDeleted: (TacticalMission) -> Unit,
    onMissionEdited: (TacticalMission) -> Unit,
    onMissionsReordered: (List<TacticalMission>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    var internalMissions by remember(missions) { mutableStateOf(missions) }
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
                    onMissionToggled = { onMissionToggled(mission) },
                    onMissionDeleted = { onMissionDeleted(mission) },
                    onMissionEdited = { onMissionEdited(mission) },
                    checkboxDragHandleModifier = Modifier.draggableHandle(),
                )
            }
        }
    }
}

@Composable
fun TacticalMissionCard(
    mission: TacticalMission,
    onMissionToggled: () -> Unit,
    onMissionDeleted: () -> Unit,
    onMissionEdited: () -> Unit,
    checkboxDragHandleModifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val colorScheme = MaterialTheme.colorScheme

    val overdue = System.currentTimeMillis() > mission.deadline && mission.status != MissionStatus.COMPLETED
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
            UnifiedItemState.DEFAULT -> colorScheme.tertiaryContainer.copy(alpha = 0.40f)
            UnifiedItemState.SELECTED -> colorScheme.surfaceContainerHighest
            UnifiedItemState.DISABLED -> colorScheme.surfaceVariant.copy(alpha = 0.6f)
        }
    val missionBorderColor =
        when (itemState) {
            UnifiedItemState.COMPLETED -> colorScheme.secondary.copy(alpha = 0.35f)
            UnifiedItemState.OVERDUE -> colorScheme.error.copy(alpha = 0.50f)
            UnifiedItemState.DEFAULT -> colorScheme.tertiary.copy(alpha = 0.38f)
            UnifiedItemState.SELECTED -> colorScheme.primary.copy(alpha = 0.4f)
            UnifiedItemState.DISABLED -> colorScheme.outlineVariant.copy(alpha = 0.35f)
        }

    UnifiedListItemSurface(
        isSelected = false,
        state = itemState,
        containerColorOverride = missionContainerColor,
        borderColorOverride = missionBorderColor,
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UnifiedItemCheckbox(
                checked = mission.status == MissionStatus.COMPLETED,
                onCheckedChange = { onMissionToggled() },
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
                        overdue -> Color(0xFFFF6E6E)
                        else -> onSurface
                    },
                    label = "mission_title_color",
                )

                androidx.compose.material3.Text(
                    mission.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                    listOf(
                        UnifiedStatusChipSpec(
                            icon = if (overdue) Icons.Outlined.Warning else Icons.Outlined.Schedule,
                            text = formatMissionDate(mission.deadline),
                            contentColor = if (overdue) MaterialTheme.colorScheme.error else onSurface.copy(alpha = 0.75f),
                        ),
                    )
                UnifiedStatusRow(items = statusItems, modifier = Modifier.padding(top = 4.dp))
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.End,
            ) {
                UnifiedTrailingActionButton(
                    icon = Icons.Default.Edit,
                    contentDescription = "Edit",
                    onClick = onMissionEdited,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                )
                UnifiedTrailingActionButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "Delete",
                    onClick = onMissionDeleted,
                    tint = Color(0xFFFF5A5A),
                )
            }
        }
    }
}

private fun formatMissionDate(ts: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(ts))
}
