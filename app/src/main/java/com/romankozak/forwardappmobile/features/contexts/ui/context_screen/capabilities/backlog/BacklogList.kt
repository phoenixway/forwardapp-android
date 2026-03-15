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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    val reorderableState =
        rememberReorderableLazyListState(state.listState) { from, to ->
            if (sortedItems.isEmpty()) return@rememberReorderableLazyListState
            val safeFromIndex = from.index.coerceIn(0, sortedItems.lastIndex)
            val safeToIndex = to.index.coerceIn(0, sortedItems.lastIndex)
            if (safeFromIndex == safeToIndex) return@rememberReorderableLazyListState
            val fromItem = sortedItems[safeFromIndex]
            val toItem = sortedItems[safeToIndex]
            val originalFrom = state.items.indexOfFirst { it.backlogItem.id == fromItem.backlogItem.id }
            val originalTo = state.items.indexOfFirst { it.backlogItem.id == toItem.backlogItem.id }
            if (originalFrom >= 0 && originalTo >= 0 && originalFrom != originalTo) {
                actions.onMove(originalFrom, originalTo)
            }
        }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedItemForActions by remember { mutableStateOf<BacklogItemContent?>(null) }
    val completedStartIndex = remember(sortedItems) { sortedItems.indexOfFirst { it.isCompleted() } }
    val completedCount =
        remember(sortedItems) {
            if (completedStartIndex == -1) 0 else sortedItems.size - completedStartIndex
        }

    if (showBottomSheet && selectedItemForActions != null) {
        BacklogItemActionsBottomSheet(
            onDismiss = { showBottomSheet = false },
            onCopyContent = { actions.onCopyContent(selectedItemForActions!!) },
            onRemindersClick = { actions.onRemindersClick(selectedItemForActions!!) },
            onDeleteEverywhere = { actions.onDeleteEverywhere(selectedItemForActions!!) },
            onOpenGoalProperties =
                if (selectedItemForActions is BacklogItemContent.GoalItem) {
                    { actions.onOpenGoalProperties(selectedItemForActions!!) }
                } else {
                    null
                },
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
                reorderableState = reorderableState,
                completedStartIndex = completedStartIndex,
                completedCount = completedCount,
            ),
        onShowItemActions = { item ->
            selectedItemForActions = item
            showBottomSheet = true
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
) {
    LazyColumn(
        state = state.listState,
        modifier = modifier,
    ) {
        itemsIndexed(contentState.sortedItems, key = { _, item -> item.backlogItem.id }) { index, item ->
            val showCompletedHeader =
                contentState.completedStartIndex != -1 && index == contentState.completedStartIndex
            Column {
                if (showCompletedHeader) {
                    CompletedSectionHeader(completedCount = contentState.completedCount)
                }
                ReorderableItem(contentState.reorderableState, key = item.backlogItem.id) { isDragging ->
                    val isSelected = item.backlogItem.id in state.selectedItemIds
                    SwipeableBacklogItem(
                        item = item,
                        reorderableScope = this,
                        showCheckboxes = state.showCheckboxes,
                        isDragging = isDragging,
                        isSelected = isSelected,
                        contextMarkerToEmojiMap = state.contextMarkerToEmojiMap,
                        isInlineEditing = (item as? BacklogItemContent.GoalItem)?.goal?.id == state.editingGoalId,
                        onRequestCloseOthers = { actions.onResetSwipe(item.backlogItem.id) },
                        swipedItemId = state.swipedItemId,
                        resetCounter = state.swipeResetCounter,
                        onItemClick = { actions.onItemClick(item) },
                        onLongClick = { actions.onLongClick(item) },
                        onMoreClick = { onShowItemActions(item) },
                        onCheckedChange = actions.onCheckedChange,
                        onDelete = { actions.onDelete(item) },
                        onRemindersClick = { actions.onRemindersClick(item) },
                        onAddToDayPlan = { actions.onAddToDayPlan(item) },
                        onStartTracking = { actions.onStartTracking(item) },
                        onShowGoalTransportMenu = { actions.onShowGoalTransportMenu(item) },
                        onRelatedLinkClick = actions.onRelatedLinkClick,
                        onInlineEditSave = actions.onGoalInlineEditSave,
                        onInlineEditCancel = actions.onGoalInlineEditCancel,
                        onDragStopped = actions.onDragStopped,
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
            text = "Виконані",
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
    val swipedItemId: String?,
    val swipeResetCounter: Int,
    val editingGoalId: String?,
)

data class BacklogListActions(
    val onMove: (from: Int, to: Int) -> Unit,
    val onItemClick: (BacklogItemContent) -> Unit,
    val onLongClick: (BacklogItemContent) -> Unit,
    val onCheckedChange: (BacklogItemContent, Boolean) -> Unit,
    val onDelete: (BacklogItemContent) -> Unit,
    val onDeleteEverywhere: (BacklogItemContent) -> Unit,
    val onAddToDayPlan: (BacklogItemContent) -> Unit,
    val onStartTracking: (BacklogItemContent) -> Unit,
    val onShowGoalTransportMenu: (BacklogItemContent) -> Unit,
    val onRelatedLinkClick: (RelatedLink) -> Unit,
    val onRemindersClick: (BacklogItemContent) -> Unit,
    val onCopyContent: (BacklogItemContent) -> Unit,
    val onOpenGoalProperties: (BacklogItemContent) -> Unit,
    val onGoalInlineEditSave: (String) -> Unit,
    val onGoalInlineEditCancel: () -> Unit,
    val onResetSwipe: (String) -> Unit,
    val onDragStopped: () -> Unit,
)

private data class BacklogListContentState(
    val sortedItems: List<BacklogItemContent>,
    val reorderableState: sh.calvin.reorderable.ReorderableLazyListState,
    val completedStartIndex: Int,
    val completedCount: Int,
)
