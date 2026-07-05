package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.backlog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.GENERAL_MISSION_STREAM_ID
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStream
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun BacklogListScreen(
    state: BacklogListState,
    actions: BacklogListActions,
    modifier: Modifier = Modifier,
) {
    val sortedItems = remember(state.items) { state.items.withCompletedAtEnd() }
    val uiItems = remember { mutableStateListOf<BacklogItemContent>() }
    var isDragInProgress by remember { mutableStateOf(false) }

    LaunchedEffect(sortedItems) {
        if (!isDragInProgress) {
            uiItems.clear()
            uiItems.addAll(sortedItems)
        }
    }

    val reorderableState =
        rememberReorderableLazyListState(state.listState) { from, to ->
            if (uiItems.isEmpty()) return@rememberReorderableLazyListState
            val safeFromIndex = from.index.coerceIn(0, uiItems.lastIndex)
            val safeToIndex = to.index.coerceIn(0, uiItems.lastIndex)
            if (safeFromIndex == safeToIndex) return@rememberReorderableLazyListState
            isDragInProgress = true
            val movedItem = uiItems.removeAt(safeFromIndex)
            uiItems.add(safeToIndex, movedItem)
        }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedItemForActions by remember { mutableStateOf<BacklogItemContent?>(null) }
    var selectedMissionStreamId by remember { mutableStateOf(GENERAL_MISSION_STREAM_ID) }
    val completedStartIndex = remember(uiItems.toList()) { uiItems.indexOfFirst { it.isGroupedAtEnd() } }
    val completedCount =
        remember(uiItems.toList()) {
            if (completedStartIndex == -1) 0 else uiItems.size - completedStartIndex
        }

    if (showBottomSheet && selectedItemForActions != null) {
        val selectedItem = selectedItemForActions!!
        val isCompleted =
            when (selectedItem) {
                is BacklogItemContent.GoalItem -> selectedItem.goal.completed
                is BacklogItemContent.ContextLinkItem -> selectedItem.project.isCompleted
                else -> false
            }
        val itemTitle =
            when (selectedItem) {
                is BacklogItemContent.GoalItem -> selectedItem.goal.text
                is BacklogItemContent.ContextLinkItem -> selectedItem.project.name
                is BacklogItemContent.LinkItem -> selectedItem.link.linkData.displayName ?: selectedItem.link.linkData.target
                is BacklogItemContent.NoteItem -> selectedItem.note.title.ifBlank { selectedItem.note.content.take(48) }
                is BacklogItemContent.NoteDocumentItem -> selectedItem.document.name
                is BacklogItemContent.JournalDocumentItem -> selectedItem.document.name
                is BacklogItemContent.MusicNoteItem -> selectedItem.musicNote.name
                is BacklogItemContent.ChecklistItem -> selectedItem.checklist.name
            }
        val canTransport =
            selectedItem is BacklogItemContent.GoalItem || selectedItem is BacklogItemContent.ContextLinkItem
        BacklogItemActionsBottomSheet(
            onDismiss = { showBottomSheet = false },
            itemTitle = itemTitle,
            isCompleted = isCompleted,
            onOpenGoalProperties =
                if (selectedItem is BacklogItemContent.GoalItem) {
                    { actions.onOpenGoalProperties(selectedItem) }
                } else {
                    null
                },
            onRemindersClick = { actions.onRemindersClick(selectedItem) },
            onAddToDayPlan = { actions.onAddToDayPlan(selectedItem) },
            missionStreams = state.missionStreams,
            selectedMissionStreamId = selectedMissionStreamId,
            currentTacticalPriorityStreamId = state.tacticalPriorityStreamIdByItemId[selectedItem.backlogItem.id],
            onMissionStreamSelected = { selectedMissionStreamId = it },
            isTacticalPriority = actions.isTacticalPriority(selectedItem),
            onToggleTacticalPriority = { actions.onToggleTacticalPriority(selectedItem, selectedMissionStreamId) },
            onStartTracking = { actions.onStartTracking(selectedItem) },
            onCopyTransport = if (canTransport) ({ actions.onCopyTransport(selectedItem) }) else null,
            onCutTransport = if (canTransport) ({ actions.onCutTransport(selectedItem) }) else null,
            onToggleCompleted = { actions.onCheckedChange(selectedItem, !isCompleted) },
            onCopyContent = { actions.onCopyContent(selectedItem) },
            onDelete = { actions.onDelete(selectedItem) },
            onDeleteEverywhere = { actions.onDeleteEverywhere(selectedItem) },
        )
    }

    if (sortedItems.isEmpty()) {
        BacklogListEmptyState(modifier = modifier)
        return
    }

    BacklogListContent(
        state = state,
        actions = actions,
        modifier = modifier,
        contentState =
            BacklogListContentState(
                sortedItems = sortedItems,
                uiItems = uiItems,
                reorderableState = reorderableState,
                completedStartIndex = completedStartIndex,
                completedCount = completedCount,
            ),
        onShowItemActions = { item ->
            selectedItemForActions = item
            selectedMissionStreamId =
                state.tacticalPriorityStreamIdByItemId[item.backlogItem.id]
                    ?: state.missionStreams.firstOrNull()?.id
                    ?: GENERAL_MISSION_STREAM_ID
            showBottomSheet = true
        },
        onDragFinished = {
            isDragInProgress = false
        },
    )
}

@Composable
private fun BacklogListEmptyState(modifier: Modifier) {
    Box(
        modifier = modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = "Беклог порожній",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Додай першу ціль або посилання на контекст через панель вводу нижче",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BacklogListContent(
    state: BacklogListState,
    actions: BacklogListActions,
    modifier: Modifier,
    contentState: BacklogListContentState,
    onShowItemActions: (BacklogItemContent) -> Unit,
    onDragFinished: () -> Unit,
) {
    LazyColumn(
        state = state.listState,
        modifier = modifier,
    ) {
        itemsIndexed(contentState.uiItems, key = { _, item -> item.backlogItem.id }) { index, item ->
            val showCompletedHeader =
                contentState.completedStartIndex != -1 && index == contentState.completedStartIndex
            Column {
                if (showCompletedHeader) {
                    CompletedSectionHeader(completedCount = contentState.completedCount)
                }
                ReorderableItem(contentState.reorderableState, key = item.backlogItem.id) { isDragging ->
                    val isSelected = item.backlogItem.id in state.selectedItemIds
                    BacklogItem(
                        item = item,
                        reorderableScope = this,
                        modifier = Modifier,
                        onItemClick = { actions.onItemClick(item) },
                        onTagClick = actions.onTagClick,
                        onLongClick = { actions.onLongClick(item) },
                        onMoreClick = { onShowItemActions(item) },
                        onCheckedChange = { isChecked -> actions.onCheckedChange(item, isChecked) },
                        onRelatedLinkClick = actions.onRelatedLinkClick,
                        showCheckbox = state.showCheckboxes,
                        isSelected = isSelected,
                        isTacticalPriority = actions.isTacticalPriority(item),
                        tacticalPriorityStreamTitle = state.tacticalPriorityStreamTitleByItemId[item.backlogItem.id],
                        contextMarkerToEmojiMap = state.contextMarkerToEmojiMap,
                        isInlineEditing = (item as? BacklogItemContent.GoalItem)?.goal?.id == state.editingGoalId,
                        onInlineEditSave = actions.onGoalInlineEditSave,
                        onInlineEditCancel = actions.onGoalInlineEditCancel,
                        onDragStopped = {
                            onDragFinished()
                            actions.onDragStopped(contentState.uiItems.toList())
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedSectionHeader(completedCount: Int) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Завершені та неактивні",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        if (completedCount > 0) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = completedCount.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

data class BacklogListState(
    val items: List<BacklogItemContent>,
    val listState: LazyListState,
    val showCheckboxes: Boolean,
    val selectedItemIds: Set<String>,
    val contextMarkerToEmojiMap: Map<String, String>,
    val editingGoalId: String?,
    val missionStreams: List<MissionStream>,
    val tacticalPriorityStreamIdByItemId: Map<String, String>,
    val tacticalPriorityStreamTitleByItemId: Map<String, String>,
)

data class BacklogListActions(
    val onItemClick: (BacklogItemContent) -> Unit,
    val onTagClick: (String) -> Unit,
    val onLongClick: (BacklogItemContent) -> Unit,
    val onCheckedChange: (BacklogItemContent, Boolean) -> Unit,
    val onDelete: (BacklogItemContent) -> Unit,
    val onDeleteEverywhere: (BacklogItemContent) -> Unit,
    val onAddToDayPlan: (BacklogItemContent) -> Unit,
    val isTacticalPriority: (BacklogItemContent) -> Boolean,
    val onToggleTacticalPriority: (BacklogItemContent, String) -> Unit,
    val onStartTracking: (BacklogItemContent) -> Unit,
    val onCopyTransport: (BacklogItemContent) -> Unit,
    val onCutTransport: (BacklogItemContent) -> Unit,
    val onRelatedLinkClick: (RelatedLink) -> Unit,
    val onRemindersClick: (BacklogItemContent) -> Unit,
    val onCopyContent: (BacklogItemContent) -> Unit,
    val onOpenGoalProperties: (BacklogItemContent) -> Unit,
    val onGoalInlineEditSave: (String) -> Unit,
    val onGoalInlineEditCancel: () -> Unit,
    val onDragStopped: (List<BacklogItemContent>) -> Unit,
)

private data class BacklogListContentState(
    val sortedItems: List<BacklogItemContent>,
    val uiItems: List<BacklogItemContent>,
    val reorderableState: sh.calvin.reorderable.ReorderableLazyListState,
    val completedStartIndex: Int,
    val completedCount: Int,
)
