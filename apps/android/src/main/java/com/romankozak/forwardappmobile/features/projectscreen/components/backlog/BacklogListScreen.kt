package com.romankozak.forwardappmobile.features.projectscreen.components.backlog

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.romankozak.forwardappmobile.shared.features.projects.listitems.domain.model.ListItemContent
import com.romankozak.forwardappmobile.features.projectscreen.components.backlog.BacklogItemActionsBottomSheet
import com.romankozak.forwardappmobile.features.projectscreen.components.backlog.SwipeableBacklogItem
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun BacklogListScreen(
    items: List<ListItemContent>,
    modifier: Modifier = Modifier,
    listState: LazyListState,
    showCheckboxes: Boolean,
    selectedItemIds: Set<String>,
    contextMarkerToEmojiMap: Map<String, String>,
    onMove: (from: Int, to: Int) -> Unit,
    onItemClick: (ListItemContent) -> Unit,
    onLongClick: (ListItemContent) -> Unit,
    onCheckedChange: (ListItemContent, Boolean) -> Unit,
    onDelete: (ListItemContent) -> Unit,
    onDeleteEverywhere: (ListItemContent) -> Unit,
    onMoveToTop: (ListItemContent) -> Unit,
    onAddToDayPlan: (ListItemContent) -> Unit,
    onStartTracking: (ListItemContent) -> Unit,
    onShowGoalTransportMenu: (ListItemContent) -> Unit,
    onRelatedLinkClick: (com.romankozak.forwardappmobile.shared.data.models.RelatedLink) -> Unit,
    onRemindersClick: (ListItemContent) -> Unit,
    onCopyContent: (ListItemContent) -> Unit,
) {
    val reorderableState = rememberReorderableLazyListState(listState) { from, to -> onMove(from.index, to.index) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedItemForActions by remember { mutableStateOf<ListItemContent?>(null) }

    if (showBottomSheet && selectedItemForActions != null) {
        BacklogItemActionsBottomSheet(
            onDismiss = { showBottomSheet = false },
            onCopyContent = { onCopyContent(selectedItemForActions!!) },
            onRemindersClick = { onRemindersClick(selectedItemForActions!!) },
        )
    }

    LazyColumn(
        state = listState,
        modifier = modifier
    ) {
        items(items, key = { it.listItem.id }) { item ->
            ReorderableItem(reorderableState, key = item.listItem.id) { isDragging ->
                val isSelected = item.listItem.id in selectedItemIds
                SwipeableBacklogItem(
                    item = item,
                    reorderableScope = this,
                    showCheckboxes = showCheckboxes,
                    isDragging = isDragging,
                    isSelected = isSelected,
                    contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                    onItemClick = { onItemClick(item) },
                    onLongClick = { onLongClick(item) },
                    onMoreClick = {
                        selectedItemForActions = item
                        showBottomSheet = true
                    },
                    onCheckedChange = onCheckedChange,
                    onDelete = { onDelete(item) },
                    onDeleteEverywhere = { onDeleteEverywhere(item) },
                    onMoveToTop = { onMoveToTop(item) },
                    onAddToDayPlan = { onAddToDayPlan(item) },
                    onStartTracking = { onStartTracking(item) },
                    onShowGoalTransportMenu = { onShowGoalTransportMenu(item) },
                    onRelatedLinkClick = onRelatedLinkClick
                )
            }
        }
    }
}
