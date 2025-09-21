package com.romankozak.forwardappmobile.ui.screens.projectscreen.views

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.ui.screens.projectscreen.BacklogViewModel
import com.romankozak.forwardappmobile.ui.screens.projectscreen.GoalActionType
import com.romankozak.forwardappmobile.ui.screens.projectscreen.UiState
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.attachments.AttachmentsSection
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems.GoalItem
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems.SublistItemRow
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.dnd.InteractiveListItem
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.dnd.SimpleDragDropState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

@Composable
fun BacklogView(
    modifier: Modifier = Modifier,
    viewModel: BacklogViewModel,
    uiState: UiState,
    listState: LazyListState,
    dragDropState: SimpleDragDropState,
    listContent: List<ListItemContent>,
    isAttachmentsExpanded: Boolean,
    swipeEnabled: Boolean
) {
    Log.d("ATTACHMENT_DEBUG", "UI: BacklogView recomposing with isAttachmentsExpanded = $isAttachmentsExpanded")

    val obsidianVaultName by viewModel.obsidianVaultName.collectAsStateWithLifecycle()
    val contextMarkerToEmojiMap by viewModel.contextMarkerToEmojiMap.collectAsStateWithLifecycle()
    val currentListContextEmojiToHide by viewModel.currentProjectContextEmojiToHide.collectAsStateWithLifecycle()

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        flow {
            while (true) {
                emit(System.currentTimeMillis())
                delay(60_000L)
            }
        }.collect {
            currentTime = it
        }
    }

    val attachmentItems = remember(listContent) {
        listContent.filterIsInstance<ListItemContent.LinkItem>()
    }
    val draggableItems = remember(listContent) {
        listContent.filterNot { it is ListItemContent.LinkItem }
    }

    Column(modifier = modifier.fillMaxSize()) {
        AttachmentsSection(
            attachments = attachmentItems,
            isExpanded = isAttachmentsExpanded,
            onAddAttachment = { viewModel.onAddAttachment(it) },
            onDeleteItem = { viewModel.itemActionHandler.deleteItem(it) },
            onItemClick = { viewModel.itemActionHandler.onItemClick(it) },
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            itemsIndexed(
                items = draggableItems,
                key = { _, item -> item.listItem.id }, // ВИПРАВЛЕНО
            ) { index, content ->
                val isSelected = content.listItem.id in uiState.selectedItemIds // ВИПРАВЛЕНО
                val isHighlighted =
                    (uiState.itemToHighlight == content.listItem.id) || // ВИПРАВЛЕНО
                            (content is ListItemContent.GoalItem && content.goal.id == uiState.goalToHighlight)

                InteractiveListItem(
                    item = content,
                    index = index,
                    dragDropState = dragDropState,
                    isSelected = isSelected,
                    isHighlighted = isHighlighted,
                    swipeEnabled = swipeEnabled,
                    isAnotherItemSwiped = (uiState.swipedItemId != null) && (uiState.swipedItemId != content.listItem.id), // ВИПРАВЛЕНО
                    resetTrigger = uiState.resetTriggers[content.listItem.id] ?: 0, // ВИПРАВЛЕНО
                    onSwipeStart = { viewModel.onSwipeStart(content.listItem.id) }, // ВИПРАВЛЕНО
                    onDelete = { viewModel.itemActionHandler.deleteItem(content) },
                    onMoreActionsRequest = { viewModel.itemActionHandler.onGoalActionInitiated(content) },
                    onAddToDayPlanRequest = { viewModel.addItemToDailyPlan(content) },
                    onCreateInstanceRequest = {
                        viewModel.itemActionHandler.onGoalActionSelected(
                            GoalActionType.CreateInstance,
                            content,
                        )
                    },
                    onMoveInstanceRequest = {
                        viewModel.itemActionHandler.onGoalActionSelected(
                            GoalActionType.MoveInstance,
                            content,
                        )
                    },
                    onCopyGoalRequest = {
                        viewModel.itemActionHandler.onGoalActionSelected(
                            GoalActionType.CopyGoal,
                            content,
                        )
                    },
                    modifier = Modifier,
                    onGoalTransportRequest = { viewModel.itemActionHandler.onGoalTransportInitiated(content) },
                    onCopyContentRequest = { viewModel.itemActionHandler.copyContentRequest(content) },
                    onStartTrackingRequest = { viewModel.onStartTrackingRequest(content) },
                ) {
                    when (content) {
                        is ListItemContent.GoalItem -> {
                            GoalItem(
                                goal = content.goal,
                                obsidianVaultName = obsidianVaultName,
                                onToggle = { isChecked ->
                                    viewModel.itemActionHandler.toggleGoalCompletedWithState(
                                        content.goal,
                                        isChecked,
                                    )
                                },
                                onItemClick = { viewModel.itemActionHandler.onItemClick(content) },
                                onLongClick = { viewModel.toggleSelection(content.listItem.id) }, // ВИПРАВЛЕНО
                                onTagClick = { tag -> viewModel.onTagClicked(tag) },
                                onRelatedLinkClick = { link -> viewModel.onLinkItemClick(link) },
                                contextMarkerToEmojiMap = contextMarkerToEmojiMap,
                                emojiToHide = currentListContextEmojiToHide,
                                currentTimeMillis = currentTime,
                            )
                        }
                        is ListItemContent.SublistItem -> {
                            SublistItemRow(
                                sublistContent = content,
                                isSelected = isSelected,
                                onClick = { viewModel.itemActionHandler.onItemClick(content) },
                                onLongClick = { viewModel.toggleSelection(content.listItem.id) }, // ВИПРАВЛЕНО
                                onCheckedChange = { isCompleted ->
                                    viewModel.onSubprojectCompletedChanged(content.project, isCompleted) // ВИПРАВЛЕНО
                                },
                                currentTimeMillis = currentTime,
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