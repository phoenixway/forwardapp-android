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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemColors
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemSurfaceLayout
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedItemCheckbox
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedItemState
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemSurface
import com.romankozak.forwardappmobile.ui.components.listitem.unifiedCheckboxColors
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedListItemTokens
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedStatusChipSpec
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedStatusRow
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedTrailingActionButton
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
private data class TacticalMissionCardLookups(
    val projectNameById: Map<String, String>,
    val attachmentNameById: Map<String, String>,
)

private data class TacticalMissionCardActions(
    val onMissionToggled: () -> Unit,
    val onMissionSelectionToggle: () -> Unit,
    val onMissionClick: () -> Unit,
    val onMissionLongPress: () -> Unit,
    val onMissionMoreClick: () -> Unit,
    val onLinkedContextClick: (String) -> Unit,
    val onLinkedAttachmentClick: (String) -> Unit,
)

private data class TacticalMissionVisualState(
    val itemState: UnifiedItemState,
    val isInactive: Boolean,
    val isPaused: Boolean,
    val overdue: Boolean,
    val containerColor: Color,
    val borderColor: Color,
    val titleColor: Color,
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TacticalMissionList(
    missions: List<TacticalMission>,
    lookups: TacticalMissionListLookups,
    selectionState: TacticalMissionSelectionState,
    callbacks: TacticalMissionListCallbacks,
    listState: LazyListState? = null,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    var internalMissions by remember(missions) { mutableStateOf(sectionOrderedMissions(missions)) }
    val projectNameById =
        remember(lookups.projectOptions) {
            lookups.projectOptions.associate { it.id to it.name }
        }
    val attachmentNameById =
        remember(lookups.attachmentOptions) {
            lookups.attachmentOptions.associate { it.id to it.name }
        }
    val lazyListState = listState ?: rememberLazyListState()
    val reorderableState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            val fromMission = internalMissions.getOrNull(from.index) ?: return@rememberReorderableLazyListState
            val toMission = internalMissions.getOrNull(to.index) ?: return@rememberReorderableLazyListState
            if (missionSection(fromMission) != missionSection(toMission)) {
                return@rememberReorderableLazyListState
            }

            internalMissions =
                internalMissions.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
            callbacks.onMissionsReordered(internalMissions)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(UnifiedListItemTokens.OuterVerticalSpacing * 2),
    ) {
        itemsIndexed(internalMissions, key = { _, mission -> mission.id }) { index, mission ->
            val previousMission = internalMissions.getOrNull(index - 1)
            val headerTitle =
                if (previousMission == null || missionSection(previousMission) != missionSection(mission)) {
                    missionSectionTitle(mission)
                } else {
                    null
                }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (headerTitle != null) {
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                    )
                }

                ReorderableItem(reorderableState, key = mission.id) {
                    TacticalMissionCard(
                        mission = mission,
                        selectionState = selectionState,
                        lookups =
                            TacticalMissionCardLookups(
                                projectNameById = projectNameById,
                                attachmentNameById = attachmentNameById,
                            ),
                        actions =
                            TacticalMissionCardActions(
                                onMissionToggled = { callbacks.onMissionToggled(mission) },
                                onMissionSelectionToggle = { callbacks.onMissionSelectionToggle(mission) },
                                onMissionClick = { callbacks.onMissionClick(mission) },
                                onMissionLongPress = { callbacks.onMissionLongPress(mission) },
                                onMissionMoreClick = { callbacks.onMissionMoreClick(mission) },
                                onLinkedContextClick = callbacks.onLinkedContextClick,
                                onLinkedAttachmentClick = callbacks.onLinkedAttachmentClick,
                            ),
                        checkboxDragHandleModifier =
                            with(this@ReorderableItem) {
                                Modifier.longPressDraggableHandle(
                                    onDragStarted = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                )
                            },
                    )
                }
            }
        }
    }
}

private enum class TacticalMissionSection {
    ACTIVE,
    INACTIVE,
    COMPLETED,
}

private fun missionSection(mission: TacticalMission): TacticalMissionSection =
    when (mission.status) {
        MissionStatus.ACTIVE -> TacticalMissionSection.ACTIVE
        MissionStatus.COMPLETED -> TacticalMissionSection.COMPLETED
        else -> TacticalMissionSection.INACTIVE
    }

private fun missionSectionTitle(mission: TacticalMission): String =
    when (missionSection(mission)) {
        TacticalMissionSection.ACTIVE -> "Активні"
        TacticalMissionSection.INACTIVE -> "Неактивні"
        TacticalMissionSection.COMPLETED -> "Завершені"
    }

private fun sectionOrderedMissions(missions: List<TacticalMission>): List<TacticalMission> {
    val active = mutableListOf<TacticalMission>()
    val inactive = mutableListOf<TacticalMission>()
    val completed = mutableListOf<TacticalMission>()

    missions.forEach { mission ->
        when (missionSection(mission)) {
            TacticalMissionSection.ACTIVE -> active += mission
            TacticalMissionSection.INACTIVE -> inactive += mission
            TacticalMissionSection.COMPLETED -> completed += mission
        }
    }

    return active + inactive + completed
}

@Composable
private fun TacticalMissionCard(
    mission: TacticalMission,
    selectionState: TacticalMissionSelectionState,
    lookups: TacticalMissionCardLookups,
    actions: TacticalMissionCardActions,
    checkboxDragHandleModifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val visualState = rememberTacticalMissionVisualState(mission = mission, onSurface = onSurface)

    UnifiedListItemSurface(
        isSelected = mission.id in selectionState.selectedMissionIds,
        state = visualState.itemState,
        layout =
            UnifiedListItemSurfaceLayout(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            ),
        colors =
            UnifiedListItemColors(
                container = visualState.containerColor,
                border = visualState.borderColor,
            ),
    ) {
        Row(
            modifier =
                Modifier.combinedClickable(
                    onClick = {
                        if (selectionState.selectionMode) {
                            actions.onMissionSelectionToggle()
                        } else {
                            actions.onMissionClick()
                        }
                    },
                    onLongClick = actions.onMissionLongPress,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TacticalMissionSelectionCheckbox(
                selectionState = selectionState,
                mission = mission,
                visualState = visualState,
                onSurface = onSurface,
                actions = actions,
                checkboxDragHandleModifier = checkboxDragHandleModifier,
            )

            Spacer(Modifier.width(8.dp))

            TacticalMissionContent(
                mission = mission,
                visualState = visualState,
                lookups = lookups,
                actions = actions,
                modifier = Modifier.weight(1f),
            )

            if (!selectionState.selectionMode) {
                TacticalMissionMoreButton(
                    onMissionMoreClick = actions.onMissionMoreClick,
                    checkboxDragHandleModifier = checkboxDragHandleModifier,
                )
            }
        }
    }
}

@Composable
private fun TacticalMissionSelectionCheckbox(
    selectionState: TacticalMissionSelectionState,
    mission: TacticalMission,
    visualState: TacticalMissionVisualState,
    onSurface: Color,
    actions: TacticalMissionCardActions,
    checkboxDragHandleModifier: Modifier,
) {
    UnifiedItemCheckbox(
        checked =
            if (selectionState.selectionMode) {
                mission.id in selectionState.selectedMissionIds
            } else {
                visualState.itemState == UnifiedItemState.COMPLETED
            },
        onCheckedChange = {
            if (selectionState.selectionMode) {
                actions.onMissionSelectionToggle()
            } else {
                actions.onMissionToggled()
            }
        },
        style = UnifiedCheckboxStyle.Round,
        modifier = checkboxDragHandleModifier,
        colors =
            unifiedCheckboxColors(
                checked = MaterialTheme.colorScheme.primary,
                uncheckedBorder = onSurface.copy(alpha = 0.7f),
            ),
    )
}

@Composable
private fun rememberTacticalMissionVisualState(
    mission: TacticalMission,
    onSurface: Color,
): TacticalMissionVisualState {
    val isInactive = mission.status == MissionStatus.INACTIVE
    val isPaused = mission.status == MissionStatus.PAUSED
    val overdue = mission.status == MissionStatus.ACTIVE && System.currentTimeMillis() > mission.deadline
    val itemState = missionItemState(mission.status, overdue)
    val containerColor = missionContainerColor(itemState = itemState, isInactive = isInactive, isPaused = isPaused)
    val borderColor = missionBorderColor(itemState = itemState, isInactive = isInactive, isPaused = isPaused)
    val targetTitleColor =
        when {
            mission.status == MissionStatus.COMPLETED -> onSurface.copy(alpha = 0.4f)
            isInactive -> onSurface.copy(alpha = 0.58f)
            isPaused -> onSurface.copy(alpha = 0.74f)
            overdue -> MissionTitleOverdueColor
            else -> onSurface
        }
    val titleColor by animateColorAsState(targetTitleColor, label = "mission_title_color")

    return TacticalMissionVisualState(
        itemState = itemState,
        isInactive = isInactive,
        isPaused = isPaused,
        overdue = overdue,
        containerColor = containerColor,
        borderColor = borderColor,
        titleColor = titleColor,
    )
}

@Composable
private fun TacticalMissionContent(
    mission: TacticalMission,
    visualState: TacticalMissionVisualState,
    lookups: TacticalMissionCardLookups,
    actions: TacticalMissionCardActions,
    modifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier) {
        TacticalMissionTitle(
            mission = mission,
            titleColor = visualState.titleColor,
            onSurface = onSurface,
        )
        UnifiedStatusRow(
            items = missionStatusItems(mission = mission, visualState = visualState, onSurface = onSurface),
            modifier = Modifier.padding(top = 2.dp),
        )
        missionLinkedItems(mission = mission, lookups = lookups, actions = actions, onSurface = onSurface)
            .takeIf { it.isNotEmpty() }
            ?.let { linkedItems ->
                UnifiedStatusRow(items = linkedItems, modifier = Modifier.padding(top = 2.dp))
            }
    }
}

@Composable
private fun TacticalMissionTitle(
    mission: TacticalMission,
    titleColor: Color,
    onSurface: Color,
) {
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
}

@Composable
private fun missionStatusItems(
    mission: TacticalMission,
    visualState: TacticalMissionVisualState,
    onSurface: Color,
): List<UnifiedStatusChipSpec> =
    buildList {
        if (visualState.isInactive) {
            add(
                UnifiedStatusChipSpec(
                    text = "Неактивна",
                    contentColor = onSurface.copy(alpha = 0.72f),
                ),
            )
        }
        if (visualState.isPaused) {
            add(
                UnifiedStatusChipSpec(
                    text = "На паузі",
                    contentColor = missionPausedColor(),
                ),
            )
        }
        add(
            UnifiedStatusChipSpec(
                icon = if (visualState.overdue) Icons.Outlined.Warning else Icons.Outlined.Schedule,
                text = formatMissionDate(mission.deadline),
                contentColor = if (visualState.overdue) missionOverdueChipColor() else onSurface.copy(alpha = 0.75f),
            ),
        )
    }

private fun missionLinkedItems(
    mission: TacticalMission,
    lookups: TacticalMissionCardLookups,
    actions: TacticalMissionCardActions,
    onSurface: Color,
): List<UnifiedStatusChipSpec> =
    buildList {
        mission.linkedProjectIds.orEmpty().normalizedLinkedIds().forEach { id ->
            add(
                UnifiedStatusChipSpec(
                    icon = Icons.Outlined.AccountTree,
                    text = lookups.projectNameById[id] ?: id,
                    contentColor = onSurface.copy(alpha = 0.78f),
                    onClick = { actions.onLinkedContextClick(id) },
                ),
            )
        }
        mission.linkedAttachmentIds.orEmpty().normalizedLinkedIds().forEach { id ->
            add(
                UnifiedStatusChipSpec(
                    icon = Icons.Outlined.AttachFile,
                    text = lookups.attachmentNameById[id] ?: id,
                    contentColor = onSurface.copy(alpha = 0.78f),
                    onClick = { actions.onLinkedAttachmentClick(id) },
                ),
            )
        }
    }

@Composable
private fun TacticalMissionMoreButton(
    onMissionMoreClick: () -> Unit,
    checkboxDragHandleModifier: Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.End,
    ) {
        UnifiedTrailingActionButton(
            icon = Icons.Default.MoreVert,
            contentDescription = "Дії",
            onClick = onMissionMoreClick,
            modifier = checkboxDragHandleModifier,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
        )
    }
}
