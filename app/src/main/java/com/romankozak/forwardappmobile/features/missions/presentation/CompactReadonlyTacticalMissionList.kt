package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.romankozak.forwardappmobile.features.missions.presentation.missionlist.TacticalMissionPriorityIndicator
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.features.planning.PlanningOrientationSourceBadge
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedStatusRow
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedCheckboxStyle
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedItemCheckbox
import com.romankozak.forwardappmobile.ui.components.listitem.unifiedCheckboxColors
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
internal fun CompactReadonlyTacticalMissionList(
    missions: List<TacticalMission>,
    missionStreamTitleById: Map<String, String>,
    showStream: Boolean,
    onMissionToggle: (TacticalMission) -> Unit,
    onMissionMoreClick: (TacticalMission) -> Unit,
    onMissionsReordered: (List<TacticalMission>) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier,
) {
    val orientationSources = rememberTacticalOrientationSources()
    val composition = com.romankozak.forwardappmobile.features.planning.rememberPlanningCompositionList()
    val hapticFeedback = LocalHapticFeedback.current
    val reorderableMissions =
        remember {
            mutableStateListOf<TacticalMission>().apply {
                addAll(missions)
            }
        }
    var isDragInProgress by remember { mutableStateOf(false) }
    var hasPendingReorder by remember { mutableStateOf(false) }
    var pendingOrderIds by remember { mutableStateOf<List<Long>?>(null) }
    LaunchedEffect(missions) {
        val incomingIds = missions.map(TacticalMission::id)
        val pendingIds = pendingOrderIds
        when {
            isDragInProgress -> Unit
            pendingIds == null -> {
                reorderableMissions.clear()
                reorderableMissions.addAll(missions)
            }
            incomingIds == pendingIds || incomingIds.toSet() != pendingIds.toSet() -> {
                pendingOrderIds = null
                reorderableMissions.clear()
                reorderableMissions.addAll(missions)
            }
            else -> {
                val incomingById = missions.associateBy(TacticalMission::id)
                reorderableMissions.indices.forEach { index ->
                    val mission = reorderableMissions[index]
                    reorderableMissions[index] = incomingById[mission.id] ?: mission
                }
            }
        }
    }
    val onDragStopped = {
        if (hasPendingReorder) {
            val reordered = reorderableMissions.toList()
            pendingOrderIds = reordered.map(TacticalMission::id)
            onMissionsReordered(reordered)
        }
        isDragInProgress = false
        hasPendingReorder = false
    }
    val reorderableState =
        rememberReorderableLazyListState(listState) { from, to ->
            val fromMission =
                reorderableMissions.getOrNull(from.index) ?: return@rememberReorderableLazyListState
            isDragInProgress = true
            hasPendingReorder = true
            reorderableMissions.removeAt(from.index)
            reorderableMissions.add(to.index, fromMission)
        }
    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = 12.dp),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        itemsIndexed(reorderableMissions, key = { _, mission -> mission.id }) { _, mission ->
            ReorderableItem(reorderableState, key = mission.id) { isDragging ->
                CompactReadonlyMissionRow(
                    orientationSource = orientationSources.byMission[mission.id],
                    relatedOrientations = orientationSources.relatedByMission[mission.id].orEmpty(),
                    onOpenRelated = orientationSources.onOpen,
                    onOpenSource = { orientationSources.byMission[mission.id]?.let { orientationSources.onOpen(it.ref.id) } },
                    mission = mission,
                    planningContext = {
                        com.romankozak.forwardappmobile.features.planning.PlanningCompositionPreview(
                            com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwner(
                                com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwnerKind.TACTICAL_MISSION,
                                mission.id.toString()), composition)
                    },
                    streamTitle = mission.missionStreamId?.let(missionStreamTitleById::get),
                    showStream = showStream,
                    isDragging = isDragging,
                    onToggle = { onMissionToggle(mission) },
                    onMoreClick = { onMissionMoreClick(mission) },
                    dragHandleModifier =
                        with(this@ReorderableItem) {
                            Modifier.longPressDraggableHandle(
                                onDragStarted = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragStopped = { onDragStopped() },
                            )
                        },
                )
            }
        }
    }
}

@Composable
private fun CompactReadonlyMissionRow(
    orientationSource: CanonicalEntitySummary?,
    relatedOrientations: List<CanonicalEntitySummary>,
    onOpenRelated: (String) -> Unit,
    onOpenSource: () -> Unit,
    mission: TacticalMission,
    streamTitle: String?,
    showStream: Boolean,
    isDragging: Boolean,
    onToggle: () -> Unit,
    onMoreClick: () -> Unit,
    dragHandleModifier: Modifier = Modifier,
    planningContext: @Composable () -> Unit = {},
) {
    val isDone = mission.status == MissionStatus.COMPLETED
    val isPaused = mission.status == MissionStatus.PAUSED
    val contentColor =
        if (isDone) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    Column {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(dragHandleModifier)
                    .clickable(onClick = onToggle),
            color = if (isDragging) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
            shadowElevation = if (isDragging) 8.dp else 0.dp,
        ) {
            Row(
                modifier = Modifier.heightIn(min = 60.dp).padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                UnifiedItemCheckbox(
                    checked = isDone,
                    onCheckedChange = { onToggle() },
                    style = UnifiedCheckboxStyle.Square,
                    modifier = dragHandleModifier,
                    colors = unifiedCheckboxColors(
                        checked = MaterialTheme.colorScheme.primary,
                        uncheckedBorder = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    ),
                )
                Column(modifier = Modifier.weight(1f).padding(top = 5.dp)) {
                    Text(
                        text = mission.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight(450)),
                        color = contentColor,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration =
                            if (isDone) {
                                TextDecoration.LineThrough
                            } else {
                                null
                            },
                    )
                    planningContext()
                    val meta = buildCompactMissionMeta(mission, streamTitle, showStream, isPaused)
                    if (orientationSource != null || relatedOrientations.isNotEmpty()) {
                        UnifiedStatusRow {
                            orientationSource?.let { source -> TacticalMissionOrientationBadge(source, onOpenSource) }
                            relatedOrientations.forEach { related ->
                                PlanningOrientationSourceBadge(related, related = true) { onOpenRelated(related.ref.id) }
                            }
                        }
                    }
                    if (meta.isNotBlank()) {
                        Text(
                            text = meta,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                TacticalMissionPriorityIndicator(mission.priority, completed = isDone)
                IconButton(onClick = onMoreClick, modifier = dragHandleModifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Mission actions",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}

internal fun buildCompactMissionMeta(
    mission: TacticalMission,
    streamTitle: String?,
    showStream: Boolean,
    isPaused: Boolean,
): String =
    buildList {
        if (showStream && !streamTitle.isNullOrBlank()) add(streamTitle)
        if (isPaused) add("Paused")
        if (mission.status == MissionStatus.INACTIVE) add("Inactive")
    }.joinToString(" · ")
