package com.romankozak.forwardappmobile.features.missions.presentation.missionlist

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.GENERAL_MISSION_STREAM_ID
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.hasDeadline
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog.backlogitems.InlineTagChip
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.utils.TagUtils
import com.romankozak.forwardappmobile.features.missions.presentation.TACTICAL_MISSION_DEBUG_TAG
import com.romankozak.forwardappmobile.features.missions.presentation.debugMissionIds
import com.romankozak.forwardappmobile.features.missions.presentation.rememberTacticalOrientationSources
import com.romankozak.forwardappmobile.features.missions.presentation.TacticalMissionOrientationBadge
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary
import com.romankozak.forwardappmobile.features.planning.PlanningOrientationSourceBadge
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedCheckboxStyle
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedItemCheckbox
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedItemState
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedMetaChip
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedStatusChipSpec
import com.romankozak.forwardappmobile.ui.components.listitem.UnifiedStatusRow
import com.romankozak.forwardappmobile.ui.components.listitem.unifiedCheckboxColors
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
private data class TacticalMissionCardLookups(
    val orientationSource: CanonicalEntitySummary?,
    val relatedOrientations: List<CanonicalEntitySummary>,
    val projectNameById: Map<String, String>,
    val attachmentNameById: Map<String, String>,
    val missionStreamTitleById: Map<String, String>,
)

private data class TacticalMissionCardActions(
    val onOpenOrientation: (String) -> Unit,
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
    showStatusSections: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val orientationSources = rememberTacticalOrientationSources()
    val hapticFeedback = LocalHapticFeedback.current
    val internalMissions =
        remember {
            mutableStateListOf<TacticalMission>().apply {
                addAll(orderedMissions(missions, showStatusSections))
            }
        }
    var isDragInProgress by remember { mutableStateOf(false) }
    var hasPendingReorder by remember { mutableStateOf(false) }
    LaunchedEffect(missions, showStatusSections) {
        Log.d(
            TACTICAL_MISSION_DEBUG_TAG,
            "compose list input count=${missions.size} dragging=$isDragInProgress " +
                "ids=${missions.debugMissionIds()}",
        )
        if (!isDragInProgress) {
            internalMissions.clear()
            internalMissions.addAll(orderedMissions(missions, showStatusSections))
            Log.d(
                TACTICAL_MISSION_DEBUG_TAG,
                "compose list applied count=${internalMissions.size} ids=${internalMissions.debugMissionIds()}",
            )
        }
    }
    val projectNameById =
        remember(lookups.projectOptions) {
            lookups.projectOptions.associate { it.id to it.name }
        }
    val attachmentNameById =
        remember(lookups.attachmentOptions) {
            lookups.attachmentOptions.associate { it.id to it.name }
        }
    val lazyListState = listState ?: rememberLazyListState()
    val onDragStopped = {
        if (hasPendingReorder) {
            callbacks.onMissionsReordered(internalMissions.toList())
        }
        isDragInProgress = false
        hasPendingReorder = false
    }
    val reorderableState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            val fromMission = internalMissions.getOrNull(from.index) ?: return@rememberReorderableLazyListState
            val toMission = internalMissions.getOrNull(to.index) ?: return@rememberReorderableLazyListState
            if (showStatusSections && missionSection(fromMission) != missionSection(toMission)) {
                return@rememberReorderableLazyListState
            }

            isDragInProgress = true
            hasPendingReorder = true
            internalMissions.add(to.index, internalMissions.removeAt(from.index))
        }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        itemsIndexed(internalMissions, key = { _, mission -> mission.id }) { index, mission ->
            val previousMission = internalMissions.getOrNull(index - 1)
            val headerTitle =
                if (showStatusSections && (previousMission == null || missionSection(previousMission) != missionSection(mission))) {
                    missionSectionTitle(mission)
                } else {
                    null
                }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (headerTitle != null) {
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    )
                }

                ReorderableItem(reorderableState, key = mission.id) { isDragging ->
                    TacticalMissionCard(
                        mission = mission,
                        selectionState = selectionState,
                        isDragging = isDragging,
                        lookups =
                            TacticalMissionCardLookups(
                                orientationSource = orientationSources.byMission[mission.id],
                                relatedOrientations = orientationSources.relatedByMission[mission.id].orEmpty(),
                                projectNameById = projectNameById,
                                attachmentNameById = attachmentNameById,
                                missionStreamTitleById = lookups.missionStreamTitleById,
                            ),
                        actions =
                            TacticalMissionCardActions(
                                onOpenOrientation = orientationSources.onOpen,
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
                                    onDragStopped = { onDragStopped() },
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

internal fun orderedMissions(
    missions: List<TacticalMission>,
    showStatusSections: Boolean,
): List<TacticalMission> =
    if (showStatusSections) {
        sectionOrderedMissions(missions)
    } else {
        missions
    }

@Composable
private fun TacticalMissionCard(
    mission: TacticalMission,
    selectionState: TacticalMissionSelectionState,
    isDragging: Boolean,
    lookups: TacticalMissionCardLookups,
    actions: TacticalMissionCardActions,
    checkboxDragHandleModifier: Modifier = Modifier,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val visualState = rememberTacticalMissionVisualState(mission = mission, onSurface = onSurface)

    Column {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = when {
                mission.id in selectionState.selectedMissionIds -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                isDragging -> MaterialTheme.colorScheme.surfaceContainerHigh
                else -> Color.Transparent
            },
            shadowElevation = if (isDragging) 8.dp else 0.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
                    .combinedClickable(
                        onClick = {
                            if (selectionState.selectionMode) {
                                actions.onMissionSelectionToggle()
                            } else {
                                actions.onMissionClick()
                            }
                        },
                        onLongClick = actions.onMissionLongPress,
                    ).padding(vertical = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                TacticalMissionSelectionCheckbox(
                    selectionState = selectionState,
                    mission = mission,
                    visualState = visualState,
                    actions = actions,
                    checkboxDragHandleModifier = checkboxDragHandleModifier,
                )

                Spacer(Modifier.width(6.dp))

                TacticalMissionContent(
                    mission = mission,
                    visualState = visualState,
                    lookups = lookups,
                    actions = actions,
                    modifier = Modifier.weight(1f).padding(top = 5.dp),
                )

                TacticalMissionPriorityIndicator(mission.priority, completed = mission.status == MissionStatus.COMPLETED)

                if (!selectionState.selectionMode) {
                    TacticalMissionMoreButton(
                        onMissionMoreClick = actions.onMissionMoreClick,
                        checkboxDragHandleModifier = checkboxDragHandleModifier,
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }
}

@Composable
private fun TacticalMissionSelectionCheckbox(
    selectionState: TacticalMissionSelectionState,
    mission: TacticalMission,
    visualState: TacticalMissionVisualState,
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
        style = UnifiedCheckboxStyle.Square,
        modifier = checkboxDragHandleModifier,
        colors =
            unifiedCheckboxColors(
                checked = MaterialTheme.colorScheme.primary,
                uncheckedBorder = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
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
    val overdue = mission.status == MissionStatus.ACTIVE && mission.hasDeadline() && System.currentTimeMillis() > mission.deadline
    val itemState = missionItemState(mission.status, overdue)
    val targetTitleColor =
        when {
            mission.status == MissionStatus.COMPLETED -> onSurface.copy(alpha = 0.4f)
            isInactive -> onSurface.copy(alpha = 0.58f)
            isPaused -> onSurface.copy(alpha = 0.74f)
            else -> onSurface
        }
    val titleColor by animateColorAsState(targetTitleColor, label = "mission_title_color")

    return TacticalMissionVisualState(
        itemState = itemState,
        isInactive = isInactive,
        isPaused = isPaused,
        overdue = overdue,
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
    val statusItems = missionStatusItems(mission = mission, visualState = visualState, onSurface = onSurface)
    val tags = missionTextTags(mission)
    val linkedItems = missionLinkedItems(mission = mission, lookups = lookups, actions = actions, onSurface = onSurface)

    Column(modifier = modifier) {
        TacticalMissionTitle(
            mission = mission,
            titleColor = visualState.titleColor,
            onSurface = onSurface,
        )
        if (statusItems.isNotEmpty() || tags.isNotEmpty() || linkedItems.isNotEmpty() || lookups.orientationSource != null || lookups.relatedOrientations.isNotEmpty()) {
            UnifiedStatusRow(modifier = Modifier.padding(top = 4.dp)) {
                lookups.orientationSource?.let { source ->
                    TacticalMissionOrientationBadge(source) { actions.onOpenOrientation(source.ref.id) }
                }
                lookups.relatedOrientations.forEach { related ->
                    PlanningOrientationSourceBadge(related, related = true) { actions.onOpenOrientation(related.ref.id) }
                }
                statusItems.forEach { item ->
                    UnifiedMetaChip(
                        text = item.text,
                        icon = item.icon,
                        contentColor = item.contentColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = item.onClick,
                    )
                }
                tags.forEach { tag ->
                    InlineTagChip(
                        text = tag.fullTag,
                        tagType = tag.type,
                    )
                }
                linkedItems.forEach { item ->
                    UnifiedMetaChip(
                        text = item.text,
                        icon = item.icon,
                        contentColor = item.contentColor ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = item.onClick,
                    )
                }
            }
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
        style =
            MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight(450),
            ),
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
        if (visualState.overdue) {
            add(
                UnifiedStatusChipSpec(
                    text = "Прострочено",
                    contentColor = missionOverdueChipColor(),
                ),
            )
        }
        missionDeadlineLabel(mission)?.let { deadlineLabel ->
            add(
                UnifiedStatusChipSpec(
                    icon = Icons.Outlined.Schedule,
                    text = deadlineLabel,
                    contentColor = onSurface.copy(alpha = 0.75f),
                ),
            )
        }
    }

private fun missionTextTags(mission: TacticalMission) =
    buildString {
        append(mission.title)
        mission.description?.takeIf { it.isNotBlank() }?.let {
            append(' ')
            append(it)
        }
    }.let(TagUtils::extractTags)
        .distinctBy { it.fullTag }

private fun missionLinkedItems(
    mission: TacticalMission,
    lookups: TacticalMissionCardLookups,
    actions: TacticalMissionCardActions,
    onSurface: Color,
): List<UnifiedStatusChipSpec> =
    buildList {
        missionStreamItem(mission = mission, lookups = lookups, onSurface = onSurface)?.let(::add)
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

private fun missionStreamItem(
    mission: TacticalMission,
    lookups: TacticalMissionCardLookups,
    onSurface: Color,
): UnifiedStatusChipSpec? {
    if (lookups.missionStreamTitleById.isEmpty()) return null
    val streamId = mission.missionStreamId ?: GENERAL_MISSION_STREAM_ID
    val title = lookups.missionStreamTitleById[streamId] ?: return null
    return UnifiedStatusChipSpec(
        icon = Icons.Outlined.AccountTree,
        text = title,
        contentColor = onSurface.copy(alpha = 0.82f),
    )
}

@Composable
private fun TacticalMissionMoreButton(
    onMissionMoreClick: () -> Unit,
    checkboxDragHandleModifier: Modifier,
) {
    IconButton(
        onClick = onMissionMoreClick,
        modifier = checkboxDragHandleModifier.size(34.dp),
    ) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Дії",
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
