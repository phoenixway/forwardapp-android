// File: app/src/main/java/com/romankozak/forwardappmobile/ui/screens/backlogs/BacklogsHelper.kt

package com.romankozak.forwardappmobile.ui.screens.backlogs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.mohamedrejeb.compose.dnd.DragAndDropState
import com.mohamedrejeb.compose.dnd.drag.DraggableItem
import com.mohamedrejeb.compose.dnd.drop.dropTarget
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ListHierarchyData
import com.romankozak.forwardappmobile.ui.components.FilterableListChooser
import com.romankozak.forwardappmobile.ui.components.GoalListRow
import com.romankozak.forwardappmobile.ui.dialogs.AboutAppDialog
import com.romankozak.forwardappmobile.ui.dialogs.AddListDialog
import com.romankozak.forwardappmobile.ui.screens.backlogs.dialogs.ContextMenuDialog
import com.romankozak.forwardappmobile.ui.dialogs.GlobalSearchDialog
import com.romankozak.forwardappmobile.ui.dialogs.WifiImportDialog
import com.romankozak.forwardappmobile.ui.dialogs.WifiServerDialog
import java.util.UUID

// У BacklogsHelper.kt
// ПОВНІСТЮ ЗАМІНІТЬ ЦІЄЮ ВЕРСІЄЮ

fun LazyListScope.renderGoalList(
    lists: List<GoalList>,
    childMap: Map<String, List<GoalList>>,
    level: Int,
    dragAndDropState: DragAndDropState<GoalList>,
    viewModel: GoalListViewModel,
    allListsFlat: List<GoalList>,
    isSearchActive: Boolean,
    planningMode: PlanningMode,
    highlightedListId: String?,
) {
    lists.forEach { list ->
        item(key = list.id) {
            val draggedItemData = dragAndDropState.draggedItem?.data

            val isDropAllowed = remember(draggedItemData, list) {
                draggedItemData == null || draggedItemData.parentId == list.parentId
            }

            DraggableItem(
                state = dragAndDropState,
                key = list.id,
                data = list,
                dragAfterLongPress = true
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    GoalListRow(
                        list = list,
                        level = level,
                        hasChildren = childMap.containsKey(list.id),
                        onListClick = { viewModel.onListClicked(it) },
                        onToggleExpanded = { viewModel.onToggleExpanded(it) },
                        onMenuRequested = { viewModel.onMenuRequested(it) },
                        isCurrentlyDragging = isDragging,
                        isHighlighted = list.id == highlightedListId,
                        isHovered = isDropAllowed && (dragAndDropState.hoveredDropTargetKey == "before-${list.id}" ||
                                dragAndDropState.hoveredDropTargetKey == "after-${list.id}"),
                        isDraggingDown = false
                    )

                    Column(modifier = Modifier.matchParentSize()) {
                        val dropModifierBefore = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .then(
                                if (isDropAllowed) {
                                    Modifier.dropTarget(
                                        state = dragAndDropState,
                                        key = "before-${list.id}"
                                    ) { draggedItemState ->
                                        viewModel.onListReorder(
                                            fromId = draggedItemState.data.id,
                                            toId = list.id,
                                            position = DropPosition.BEFORE
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            )

                        val dropModifierAfter = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .then(
                                if (isDropAllowed) {
                                    Modifier.dropTarget(
                                        state = dragAndDropState,
                                        key = "after-${list.id}"
                                    ) { draggedItemState ->
                                        viewModel.onListReorder(
                                            fromId = draggedItemState.data.id,
                                            toId = list.id,
                                            position = DropPosition.AFTER
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            )
                        Box(modifier = dropModifierBefore)
                        Box(modifier = dropModifierAfter)
                    }
                }
            }
        }
        if (list.isExpanded) {
            val children = childMap[list.id]?.sortedBy { it.order } ?: emptyList()
            if (children.isNotEmpty()) {
                renderGoalList(
                    lists = children,
                    childMap = childMap,
                    level = level + 1,
                    dragAndDropState = dragAndDropState,
                    viewModel = viewModel,
                    allListsFlat = allListsFlat,
                    isSearchActive = isSearchActive,
                    planningMode = planningMode,
                    highlightedListId = highlightedListId,
                )
            }
        }
    }
}

fun getDescendantIds(listId: String, childMap: Map<String, List<GoalList>>): Set<String> {
    val descendants = mutableSetOf<String>()
    val queue = ArrayDeque<String>()
    queue.add(listId)
    while (queue.isNotEmpty()) {
        val currentId = queue.removeFirst()
        childMap[currentId]?.forEach { child ->
            descendants.add(child.id)
            queue.add(child.id)
        }
    }
    return descendants
}

@Composable
fun HandleDialogs(
    dialogState: DialogState,
    viewModel: GoalListViewModel,
    listChooserFilterText: String,
    listChooserExpandedIds: Set<String>,
    filteredListHierarchyForDialog: ListHierarchyData,
) {
    val stats by viewModel.appStatistics.collectAsState()
    val showWifiServerDialog by viewModel.showWifiServerDialog.collectAsState()
    val wifiServerAddress by viewModel.wifiServerAddress.collectAsState()
    val showWifiImportDialog by viewModel.showWifiImportDialog.collectAsState()
    val showSearchDialog by viewModel.showSearchDialog.collectAsState()

    when (val state = dialogState) {
        DialogState.Hidden -> {}
        is DialogState.AddList -> {
            AddListDialog(
                title = if (state.parentId == null) "Create new list" else "Create sublist",
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { name ->
                    val newId = UUID.randomUUID().toString()
                    viewModel.addNewList(newId, state.parentId, name)
                    viewModel.dismissDialog()
                },
            )
        }

        is DialogState.ContextMenu -> {
            ContextMenuDialog(
                list = state.list,
                onDismissRequest = { viewModel.dismissDialog() },
                onMoveRequest = { viewModel.onMoveListRequest(it) },
                onAddSublistRequest = { viewModel.onAddSublistRequest(it) },
                onDeleteRequest = { viewModel.onDeleteRequest(it) },
                onEditRequest = { viewModel.onEditRequest(it) },
            )
        }
        is DialogState.MoveList -> {
            val disabledIds = remember(state.list.id, filteredListHierarchyForDialog.childMap) {
                getDescendantIds(
                    state.list.id,
                    filteredListHierarchyForDialog.childMap,
                ) + state.list.id
            }
            FilterableListChooser(
                title = "Перемістити '${state.list.name}'",
                filterText = listChooserFilterText,
                onFilterTextChanged = viewModel::onListChooserFilterChanged,
                topLevelLists = filteredListHierarchyForDialog.topLevelLists,
                childMap = filteredListHierarchyForDialog.childMap,
                expandedIds = listChooserExpandedIds,
                onToggleExpanded = viewModel::onListChooserToggleExpanded,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { newParentId -> viewModel.onMoveListConfirmed(newParentId) },
                currentParentId = state.list.parentId,
                disabledIds = disabledIds,
                onAddNewList = viewModel::addNewList,
            )
        }

        is DialogState.ConfirmDelete -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                title = { Text("Delete list?") },
                text = { Text("Are you sure you want to delete '${state.list.name}' and all its sublists and goals? This action cannot be undone.") },
                confirmButton = { Button(onClick = { viewModel.onDeleteListConfirmed(state.list) }) { Text("Delete") } },
                dismissButton = { TextButton(onClick = { viewModel.dismissDialog() }) { Text("Cancel") } },
            )
        }
        is DialogState.EditList -> {
            // ЦЕЙ ДІАЛОГ БІЛЬШЕ НЕ ВИКОРИСТОВУЄТЬСЯ
            // onEditRequest -> GoalListViewModel -> NavigateToEditListScreen
        }
        is DialogState.AboutApp -> {
            AboutAppDialog(stats) { viewModel.dismissDialog() }
        }
    }

    if (showWifiServerDialog) {
        WifiServerDialog(wifiServerAddress) { viewModel.onDismissWifiServerDialog() }
    }
    if (showWifiImportDialog) {
        val desktopAddress by viewModel.desktopAddress.collectAsState()
        WifiImportDialog(
            desktopAddress = desktopAddress,
            onAddressChange = { viewModel.onDesktopAddressChange(it) },
            onDismiss = { viewModel.onDismissWifiImportDialog() },
            onConfirm = { address -> viewModel.performWifiImport(address) },
        )
    }
    if (showSearchDialog) {
        GlobalSearchDialog(
            onDismiss = { viewModel.onDismissSearchDialog() },
            onConfirm = { query -> viewModel.onPerformGlobalSearch(query) },
        )
    }
}