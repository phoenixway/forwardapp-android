// file: ui/screens/backlog/ProjectScreenEffects.kt

package com.romankozak.forwardappmobile.ui.screens.projectscreen

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.dnd.SimpleDragDropState
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.utils.handleRelatedLinkClick
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
// ВИДАЛЕНО: URLEncoder більше не потрібен тут
// import java.net.URLEncoder
// import java.nio.charset.StandardCharsets

private const val TAG = "SendDebug"
@Composable
fun GoalDetailEffects(
    navController: NavController,
    viewModel: BacklogViewModel,
    snackbarHostState: SnackbarHostState,
    listState: LazyListState,
    inboxListState: LazyListState,
    dragDropState: SimpleDragDropState,
    coroutineScope: CoroutineScope
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listContent by viewModel.listContent.collectAsStateWithLifecycle()
    val list by viewModel.project.collectAsStateWithLifecycle()

    val inboxRecords by viewModel.inboxHandler.inboxRecords.collectAsStateWithLifecycle()

    val localContext = LocalContext.current
    val obsidianVaultName by viewModel.obsidianVaultName.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle

    val displayList = remember(listContent, list?.isAttachmentsExpanded) {
        val attachmentItems = listContent.filterIsInstance<ListItemContent.LinkItem>()
        val draggableItems = listContent.filterNot { it is ListItemContent.LinkItem }
        if (list?.isAttachmentsExpanded == true) attachmentItems + draggableItems else draggableItems
    }

    // Обробка навігації, снек-барів та інших UI-подій
    LaunchedEffect(Unit) {
        viewModel.uiEventFlow.collect { event ->
            when (event) {
                // --- ЗМІНА: Видаляємо обробку NavigateToAuth ---
                // is UiEvent.NavigateToAuth -> { ... }
                is UiEvent.Navigate -> {
                    Log.d(TAG, "GoalDetailEffects: Отримано подію Navigate. Маршрут: '${event.route}'")
                    if (event.route == "back") {
                        navController.popBackStack()
                    } else {
                        navController.navigate(event.route)
                    }
                }
                is UiEvent.ShowSnackbar -> {
                    coroutineScope.launch {
                        val result =
                            snackbarHostState.showSnackbar(
                                message = event.message,
                                actionLabel = event.action,
                                duration = SnackbarDuration.Short,
                            )
                        if (result == SnackbarResult.ActionPerformed) {
                            when (event.action) {
                                "Обмежити в часі" -> viewModel.onLimitLastActivityRequested()
                                else -> viewModel.itemActionHandler.undoDelete()
                            }
                        }
                    }
                }
                is UiEvent.NavigateBackAndReveal -> {
                    // Set the result on the *previous* screen's saved state handle
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("project_to_reveal", event.projectId)
                    navController.popBackStack()
                }

                is UiEvent.HandleLinkClick -> {
                    handleRelatedLinkClick(event.link, obsidianVaultName, localContext, navController)
                }
                is UiEvent.ResetSwipeState -> viewModel.onSwipeStateReset(event.itemId)
                is UiEvent.ScrollTo -> listState.animateScrollToItem(event.index)
                is UiEvent.ScrollToLatestInboxRecord -> {
                    coroutineScope.launch {
                        if (inboxRecords.isNotEmpty()) {
                            inboxListState.animateScrollToItem(inboxRecords.lastIndex)
                        }
                    }
                }
            }
        }
    }

    // ... (решта коду в GoalDetailEffects залишається без змін) ...

    // Авто-скрол до нового елемента
    val newItemInList = uiState.newlyAddedItemId?.let { id -> displayList.find { it.listItem.id == id } }
    LaunchedEffect(newItemInList) {
        if (newItemInList != null) {
            listState.animateScrollToItem(0)
            viewModel.onScrolledToNewItem()
        }
    }

    // Обробка результату з екрана вибору списку
    DisposableEffect(savedStateHandle, lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (savedStateHandle?.contains("list_chooser_result") == true) {
                    val result = savedStateHandle.get<String>("list_chooser_result")
                    if (result != null) {
                        Log.d("AddSublistDebug", "BacklogScreen: Received result from chooser: '$result'")
                        viewModel.onListChooserResult(result)
                    }
                    savedStateHandle.remove<String>("list_chooser_result")
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Примусове оновлення при поверненні на екран
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.forceRefresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Скидання стану drag-and-drop при потребі
    LaunchedEffect(uiState.needsStateRefresh) {
        if (uiState.needsStateRefresh) {
            dragDropState.reset()
            viewModel.onStateRefreshed()
        }
    }



    // Підсвічування цілі або елемента
    LaunchedEffect(uiState.goalToHighlight, uiState.itemToHighlight, displayList, list?.isAttachmentsExpanded) {
        val goalId = uiState.goalToHighlight
        val itemId = uiState.itemToHighlight
        if ((goalId == null && itemId == null) || displayList.isEmpty()) return@LaunchedEffect

        val indexToScroll = when {
            goalId != null -> displayList.indexOfFirst { it is ListItemContent.GoalItem && it.goal.id == goalId }.takeIf { it != -1 }
            itemId != null -> displayList.indexOfFirst { it.listItem.id == itemId }.takeIf { it != -1 }
            else -> null
        }

        if (indexToScroll != null) {
            listState.animateScrollToItem(indexToScroll)
            delay(2500L)
        }
        viewModel.onHighlightShown()
    }

    // Переключення на INBOX для підсвічування запису
    LaunchedEffect(uiState.inboxRecordToHighlight, inboxRecords.isNotEmpty()) {
        val recordId = uiState.inboxRecordToHighlight
        val recordsAreLoaded = inboxRecords.isNotEmpty()
        if (recordId != null && recordsAreLoaded && uiState.currentView != ProjectViewMode.INBOX) {
            if (inboxRecords.any { it.id == recordId }) {
                Log.d(TAG, "Highlight requested. Switching to INBOX view.")
                viewModel.onProjectViewChange(ProjectViewMode.INBOX)
            }
        }
    }

    // Скрол та підсвічування запису в INBOX
    LaunchedEffect(uiState.inboxRecordToHighlight, uiState.currentView, inboxRecords) {
        val recordId = uiState.inboxRecordToHighlight
        if (recordId != null && uiState.currentView == ProjectViewMode.INBOX && inboxRecords.isNotEmpty()) {
            val indexToScroll = inboxRecords.indexOfFirst { it.id == recordId }
            Log.d(TAG, "INBOX view is active. Searching for record. Found index: $indexToScroll")
            if (indexToScroll != -1) {
                Log.d(TAG, "Scrolling to index: $indexToScroll")
                inboxListState.animateScrollToItem(indexToScroll)
                Log.d(TAG, "Waiting for highlight to finish...")
                delay(2500L)
                Log.d(TAG, "Highlight duration passed. Resetting state.")
                viewModel.onInboxHighlightShown()
            } else {
                Log.w(TAG, "Record ID $recordId not found. Clearing highlight state.")
                viewModel.onInboxHighlightShown()
            }
        }
    }

    // Скрол до новоствореного елемента
    LaunchedEffect(uiState.newlyAddedItemId, displayList) {
        val itemId = uiState.newlyAddedItemId
        Log.d("AutoScrollDebug", "newlyAddedItemId: $itemId, displayList size: ${displayList.size}")
        if (itemId != null) {
            var index = displayList.indexOfFirst { it.listItem.id == itemId }
            if (index == -1) {
                index = displayList.indexOfFirst { it is ListItemContent.GoalItem && it.goal.id == itemId }
                Log.d("AutoScrollDebug", "Trying goal.id search, found index: $index")
            }
            Log.d("AutoScrollDebug", "Final index: $index for itemId: $itemId")
            if (index != -1) {
                listState.animateScrollToItem(index)
                viewModel.onScrolledToNewItem()
            } else {
                Log.w("AutoScrollDebug", "Item not found in displayList by any ID!")
            }
        }
    }
}