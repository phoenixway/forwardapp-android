package com.romankozak.forwardappmobile.ui.screens.projectscreen.views

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.ui.dnd.dragHandle
import com.romankozak.forwardappmobile.ui.screens.projectscreen.BacklogViewModel
import com.romankozak.forwardappmobile.ui.screens.projectscreen.UiState
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.attachments.AttachmentsSection
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems.GoalItem
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems.SubprojectItemRow
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.dnd.InteractiveListItem
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.dnd.MoreActionsButton
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BacklogView(
    modifier: Modifier = Modifier,
    viewModel: BacklogViewModel,
    uiState: UiState,
    listState: LazyListState,
    listContent: List<ListItemContent>,
    isAttachmentsExpanded: Boolean,
    swipeEnabled: Boolean,
    dragDropManager: com.romankozak.forwardappmobile.ui.dnd.DragDropManager,
    dndVisualState: StateFlow<com.romankozak.forwardappmobile.ui.dnd.DnDVisualState>,
) {
    Log.d("ATTACHMENT_DEBUG", "UI: BacklogView recomposing with isAttachmentsExpanded = $isAttachmentsExpanded")

    val obsidianVaultName by viewModel.obsidianVaultName.collectAsStateWithLifecycle()
    val contextMarkerToEmojiMap by viewModel.contextMarkerToEmojiMap.collectAsStateWithLifecycle()
    val currentListContextEmojiToHide by viewModel.currentProjectContextEmojiToHide.collectAsStateWithLifecycle()
    val subprojectChildren by viewModel.subprojectChildren.collectAsStateWithLifecycle()
    val dragState by viewModel.dragState.collectAsState()

    LaunchedEffect(listState) {
        viewModel.setLazyListState(listState)
    }

    LaunchedEffect(viewModel.uiEventFlow) {
        viewModel.uiEventFlow.collect { event ->
            when (event) {
                is com.romankozak.forwardappmobile.ui.screens.projectscreen.UiEvent.ScrollTo -> {
                    listState.animateScrollToItem(event.index)
                }
                else -> {}
            }
        }
    }

    val attachmentItems =
        remember(listContent) {
            listContent.filter { it is ListItemContent.LinkItem || it is ListItemContent.NoteItem || it is ListItemContent.CustomListItem }
        }
    val draggableItems =
        remember(listContent) {
            listContent.filterNot { it is ListItemContent.LinkItem || it is ListItemContent.NoteItem || it is ListItemContent.CustomListItem }
        }

    val scope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        AttachmentsSection(
            attachments = attachmentItems,
            isExpanded = isAttachmentsExpanded,
            onAddAttachment = { viewModel.onAddAttachment(it) },
            onDeleteItem = { viewModel.itemActionHandler.deleteItem(it) },
            onItemClick = { viewModel.itemActionHandler.onItemClick(it) },
            onCopyContentRequest = { viewModel.itemActionHandler.copyContentRequest(it) },
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            itemsIndexed(
                items = draggableItems,
                key = { _, item -> item.listItem.id },
            ) { index, content ->
                val isSelected = content.listItem.id in uiState.selectedItemIds
                val isHighlighted =
                    (uiState.itemToHighlight == content.listItem.id) ||
                        (content is ListItemContent.GoalItem && content.goal.id == uiState.goalToHighlight)
                
                var isDragHandleActive by remember { mutableStateOf(false) }

                InteractiveListItem(
                    item = content,
                    index = index,
                    dragAndDropState = dragState,
                    dndVisualState = dndVisualState,
                    listState = listState,
                    isSelected = isSelected,
                    isHighlighted = isHighlighted,
                    swipeEnabled = swipeEnabled,
                    isDragHandleActive = isDragHandleActive,
                    isAnotherItemSwiped = (uiState.swipedItemId != null) && (uiState.swipedItemId != content.listItem.id),
                    resetTrigger = uiState.resetTriggers[content.listItem.id] ?: 0,
                    onSwipeStart = { viewModel.onSwipeStart(content.listItem.id) },
                    onDelete = { viewModel.itemActionHandler.deleteItem(content) },
                    onMoreActionsRequest = { viewModel.itemActionHandler.onGoalActionInitiated(content) },
                    onMoveToTopRequest = { viewModel.onMoveToTop(content) },
                    onAddToDayPlanRequest = { viewModel.addItemToDailyPlan(content) },
                    onShowGoalTransportMenu = { itemToTransport ->
                        viewModel.itemActionHandler.onGoalTransportInitiated(
                            itemToTransport,
                            onCopyContentToClipboard = {
                                viewModel.itemActionHandler.copyContentRequest(itemToTransport)
                            },
                        )
                    },
                    onStartTrackingRequest = { viewModel.onStartTrackingRequest(content) },
                    onCopyContentRequest = { viewModel.itemActionHandler.copyContentRequest(content) },
                    onToggleCompleted = {
                        when (content) {
                            is ListItemContent.GoalItem -> {
                                viewModel.itemActionHandler.toggleGoalCompletedWithState(content.goal, !content.goal.completed)
                            }
                            is ListItemContent.SublistItem -> {
                                viewModel.onSubprojectCompletedChanged(content.project, !content.project.isCompleted)
                            }
                            else -> {}
                        }
                    },
                    modifier = Modifier.animateItem()
                ) { isDragging ->
                    when (content) {
                        is ListItemContent.GoalItem -> {
                            GoalItem(
                                goal = content.goal,
                                reminders = content.reminders,
                                obsidianVaultName = obsidianVaultName,
                                showCheckbox = uiState.showCheckboxes,
                                onCheckedChange = { isChecked ->
                                    viewModel.itemActionHandler.toggleGoalCompletedWithState(
                                        content.goal,
                                        isChecked,
                                    )
                                },
                                onItemClick = { viewModel.itemActionHandler.onItemClick(content) },
                                onLongClick = { viewModel.toggleSelection(content.listItem.id) },
                                onTagClick = { tag -> viewModel.onTagClicked(tag) },
                                onRelatedLinkClick = { link -> viewModel.onLinkItemClick(link) },
                                contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                                emojiToHide = currentListContextEmojiToHide,
                                isSelected = isSelected,
                                endAction = {
                                    MoreActionsButton(
                                        isDragging = isDragging,
                                        onMoreClick = { viewModel.itemActionHandler.onGoalActionInitiated(content) },
                                        dragHandleModifier = Modifier.dragHandle(
                                            dragDropManager = dragDropManager,
                                            itemIndex = index,
                                            lazyListState = listState,
                                            scope = scope,
                                            onDragStateChanged = { isDragHandleActive = it }
                                        )
                                    )
                                }
                            )
                        }
                        is ListItemContent.SublistItem -> {
                            val children = subprojectChildren[content.project.id] ?: emptyList()
                            SubprojectItemRow(
                                subprojectContent = content,
                                reminders = content.reminders,
                                isSelected = isSelected,
                                showCheckbox = uiState.showCheckboxes,
                                onClick = { viewModel.itemActionHandler.onItemClick(content) },
                                onLongClick = { viewModel.toggleSelection(content.listItem.id) },
                                onCheckedChange = { isCompleted ->
                                    viewModel.onSubprojectCompletedChanged(content.project, isCompleted)
                                },
                                childProjects = children,
                                onChildProjectClick = { child -> viewModel.onChildProjectClick(child) },
                                contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                                emojiToHide = currentListContextEmojiToHide,
                                endAction = {
                                    MoreActionsButton(
                                        isDragging = isDragging,
                                        onMoreClick = { viewModel.itemActionHandler.onGoalActionInitiated(content) },
                                        dragHandleModifier = Modifier.dragHandle(
                                            dragDropManager = dragDropManager,
                                            itemIndex = index,
                                            lazyListState = listState,
                                            scope = scope,
                                            onDragStateChanged = { isDragHandleActive = it }
                                        )
                                    )
                                }
                            )
                        }
                        else -> {
                            Log.w(
                                "BacklogScreen",
                                "Unsupported type in draggableItems list: ${content::class.simpleName}",
                            )
                        }
                    }
                }
            }
        }
    }
}