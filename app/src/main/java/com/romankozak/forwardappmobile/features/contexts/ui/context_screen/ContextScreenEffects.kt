package com.romankozak.forwardappmobile.features.contexts.ui.context_screen

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.NavTargetRouter
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.utils.handleRelatedLinkClick
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.UiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

private const val TAG = "SendDebug"
private const val HIGHLIGHT_RESET_DELAY_MS = 2500L

data class ContextScreenEffectDependencies(
    val navController: NavController,
    val snackbarHostState: SnackbarHostState,
    val listState: LazyListState,
    val inboxListState: LazyListState,
    val coroutineScope: CoroutineScope,
)

private data class ContextScreenUiEventContext(
    val currentContextId: String?,
    val inboxRecordCount: Int,
    val obsidianVaultName: String?,
    val localContext: android.content.Context,
)

private data class ContextScreenGoalHighlightState(
    val uiState: com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextUiState,
    val displayList: List<BacklogItemContent>,
    val listContent: List<BacklogItemContent>,
    val areAttachmentsExpanded: Boolean?,
)

private data class GoalDetailEffectState(
    val uiState: com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextUiState,
    val listContent: List<BacklogItemContent>,
    val currentContextId: String?,
    val areAttachmentsExpanded: Boolean?,
    val inboxRecords: List<com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord>,
    val localContext: android.content.Context,
    val obsidianVaultName: String?,
    val lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    val savedStateHandle: androidx.lifecycle.SavedStateHandle?,
    val displayList: List<BacklogItemContent>,
)

@Composable
fun GoalDetailEffects(
    viewModel: ContextScreenViewModel,
    dependencies: ContextScreenEffectDependencies,
) {
    val state = rememberGoalDetailEffectState(viewModel, dependencies)

    ContextScreenUiEventsEffect(
        viewModel = viewModel,
        dependencies = dependencies,
        context =
            ContextScreenUiEventContext(
                currentContextId = state.currentContextId,
                inboxRecordCount = state.inboxRecords.size,
                obsidianVaultName = state.obsidianVaultName,
                localContext = state.localContext,
            ),
    )
    ContextScreenChooserResumeEffect(
        viewModel = viewModel,
        lifecycleOwner = state.lifecycleOwner,
        savedStateHandle = state.savedStateHandle,
    )
    ContextScreenRefreshOnResumeEffect(
        viewModel = viewModel,
        lifecycleOwner = state.lifecycleOwner,
    )
    ContextScreenGoalHighlightEffect(
        viewModel = viewModel,
        listState = dependencies.listState,
        state =
            ContextScreenGoalHighlightState(
                uiState = state.uiState,
                displayList = state.displayList,
                listContent = state.listContent,
                areAttachmentsExpanded = state.areAttachmentsExpanded,
            ),
    )
    ContextScreenInboxHighlightEffect(
        viewModel = viewModel,
        inboxListState = dependencies.inboxListState,
        uiState = state.uiState,
        inboxRecords = state.inboxRecords,
    )
    ContextScreenNewItemScrollEffect(
        viewModel = viewModel,
        listState = dependencies.listState,
        uiState = state.uiState,
        displayList = state.displayList,
        listContent = state.listContent,
    )
}

@Composable
private fun rememberGoalDetailEffectState(
    viewModel: ContextScreenViewModel,
    dependencies: ContextScreenEffectDependencies,
): GoalDetailEffectState {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listContent by viewModel.listContent.collectAsStateWithLifecycle()
    val list by viewModel.project.collectAsStateWithLifecycle()
    val inboxRecords by viewModel.inboxHandler.inboxRecords.collectAsStateWithLifecycle()
    val localContext = LocalContext.current
    val obsidianVaultName by viewModel.obsidianVaultName.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val savedStateHandle = dependencies.navController.currentBackStackEntry?.savedStateHandle
    val displayList =
        remember(listContent, list?.isAttachmentsExpanded) {
            val attachmentItems = listContent.filterIsInstance<BacklogItemContent.LinkItem>()
            val draggableItems = listContent.filterNot { it is BacklogItemContent.LinkItem }
            if (list?.isAttachmentsExpanded == true) attachmentItems + draggableItems else draggableItems
        }

    return GoalDetailEffectState(
        uiState = uiState,
        listContent = listContent,
        currentContextId = list?.id,
        areAttachmentsExpanded = list?.isAttachmentsExpanded,
        inboxRecords = inboxRecords,
        localContext = localContext,
        obsidianVaultName = obsidianVaultName,
        lifecycleOwner = lifecycleOwner,
        savedStateHandle = savedStateHandle,
        displayList = displayList,
    )
}

@Composable
private fun ContextScreenUiEventsEffect(
    viewModel: ContextScreenViewModel,
    dependencies: ContextScreenEffectDependencies,
    context: ContextScreenUiEventContext,
) {
    LaunchedEffect(Unit) {
        viewModel.uiEventFlow.collect { event ->
            when (event) {
                is UiEvent.Navigate ->
                    handleNavigateEvent(
                        event = event,
                        viewModel = viewModel,
                        navController = dependencies.navController,
                        currentContextId = context.currentContextId,
                    )

                is UiEvent.NavigateBack -> dependencies.navController.popBackStack()
                is UiEvent.ShowSnackbar ->
                    dependencies.coroutineScope.launch {
                        val result =
                            dependencies.snackbarHostState.showSnackbar(
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

                is UiEvent.NavigateBackAndReveal -> {
                    dependencies.navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("context_to_reveal", event.contextId)
                    dependencies.navController.popBackStack()
                }

                is UiEvent.HandleLinkClick ->
                    handleRelatedLinkClick(
                        event.link,
                        context.obsidianVaultName.orEmpty(),
                        context.localContext,
                        dependencies.navController,
                    )

                is UiEvent.OpenUri -> {
                    val intent = Intent(Intent.ACTION_VIEW, event.uri.toUri())
                    context.localContext.startActivity(intent)
                }

                is UiEvent.ScrollTo -> dependencies.listState.animateScrollToItem(event.index)
                is UiEvent.ScrollToLatestInboxRecord ->
                    dependencies.coroutineScope.launch {
                        if (context.inboxRecordCount > 0) {
                            dependencies.inboxListState.animateScrollToItem(context.inboxRecordCount - 1)
                        }
                    }
            }
        }
    }
}

private fun handleNavigateEvent(
    event: UiEvent.Navigate,
    viewModel: ContextScreenViewModel,
    navController: NavController,
    currentContextId: String?,
) {
    Log.d(TAG, "GoalDetailEffects: Отримано подію Navigate.")
    val route = NavTargetRouter.routeOf(event.target)
    val navigationManager = viewModel.enhancedNavigationManager
    val isCurrentContextRoute =
        event.target is NavTarget.ContextDetail &&
            currentContextId == event.target.contextId
    if (isCurrentContextRoute) return

    val isContextRoute = route.startsWith("goal_detail_screen/")
    val shouldRecordHistory =
        event.target is NavTarget.ContextDetail ||
            event.target is NavTarget.GlobalSearch ||
            event.target is NavTarget.ContextHierarchy

    when {
        isContextRoute -> {
            val currentDestId = navController.currentBackStackEntry?.destination?.id
            navigationManager.navigate(
                target = event.target,
                recordInHistory = shouldRecordHistory,
                builder = {
                    if (currentDestId != null) {
                        popUpTo(currentDestId) { inclusive = true }
                    }
                    launchSingleTop = true
                    restoreState = false
                },
            )
            viewModel.consumeLinkedContextReplace()
        }

        viewModel.consumeLinkedContextReplace() ->
            navigationManager.navigate(
                target = event.target,
                recordInHistory = shouldRecordHistory,
                builder = {
                    launchSingleTop = true
                    restoreState = false
                },
            )

        else ->
            navigationManager.navigate(
                target = event.target,
                recordInHistory = shouldRecordHistory,
            )
    }
}

@Composable
private fun ContextScreenChooserResumeEffect(
    viewModel: ContextScreenViewModel,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    savedStateHandle: androidx.lifecycle.SavedStateHandle?,
) {
    DisposableEffect(savedStateHandle, lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    handleChooserResume(viewModel, savedStateHandle)
                }
            }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

private fun handleChooserResume(
    viewModel: ContextScreenViewModel,
    savedStateHandle: androidx.lifecycle.SavedStateHandle?,
) {
    if (savedStateHandle?.contains("list_chooser_result") == true) {
        val result = savedStateHandle.get<String>("list_chooser_result")
        if (result != null) {
            Log.d("AddSublistDebug", "BacklogScreen: Received result from chooser: '$result'")
            viewModel.onListChooserResult(result)
        }
        savedStateHandle.remove<String>("list_chooser_result")
    } else if (
        savedStateHandle?.get<Boolean>("pendingDirectionLink") == true ||
        savedStateHandle?.get<Boolean>("pendingAddDirectionFromContextChooser") == true
    ) {
        viewModel.clearPendingDirectionLink()
    }
}

@Composable
private fun ContextScreenRefreshOnResumeEffect(
    viewModel: ContextScreenViewModel,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
) {
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.forceRefresh()
                }
            }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@Composable
private fun ContextScreenGoalHighlightEffect(
    viewModel: ContextScreenViewModel,
    listState: LazyListState,
    state: ContextScreenGoalHighlightState,
) {
    LaunchedEffect(
        state.uiState.goalToHighlight,
        state.uiState.itemToHighlight,
        state.displayList,
        state.areAttachmentsExpanded,
    ) {
        val goalId = state.uiState.goalToHighlight
        val itemId = state.uiState.itemToHighlight
        if ((goalId == null && itemId == null) || state.displayList.isEmpty()) return@LaunchedEffect

        val displayIndex =
            when {
                goalId != null ->
                    state.displayList
                        .indexOfFirst { it is BacklogItemContent.GoalItem && it.goal.id == goalId }
                        .takeIf { it != -1 }

                itemId != null ->
                    state.displayList
                        .indexOfFirst { it.backlogItem.id == itemId }
                        .takeIf { it != -1 }

                else -> null
        }

        if (displayIndex != null) {
            val targetItem = state.displayList.getOrNull(displayIndex)
            val actualIndex =
                targetItem?.let { item ->
                    state.listContent.indexOfFirst { it.backlogItem.id == item.backlogItem.id }
                } ?: -1
            if (actualIndex != -1) {
                listState.animateScrollToItem(actualIndex)
                delay(HIGHLIGHT_RESET_DELAY_MS)
            } else {
                Log.w(TAG, "Highlight requested item not found in listContent.")
            }
        }
        viewModel.onHighlightShown()
    }
}

@Composable
private fun ContextScreenInboxHighlightEffect(
    viewModel: ContextScreenViewModel,
    inboxListState: LazyListState,
    uiState: com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextUiState,
    inboxRecords: List<com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord>,
) {
    LaunchedEffect(uiState.inboxRecordToHighlight, inboxRecords.isNotEmpty()) {
        val recordId = uiState.inboxRecordToHighlight
        val recordsAreLoaded = inboxRecords.isNotEmpty()
        if (recordId != null && recordsAreLoaded && uiState.currentViewMode != ContextViewMode.INBOX) {
            if (inboxRecords.any { it.id == recordId } && viewModel.hasCapability(CapabilityId("inbox"))) {
                Log.d(TAG, "Highlight requested. Switching to INBOX view.")
                viewModel.onProjectViewChange(ContextViewMode.INBOX)
            }
        }
    }

    LaunchedEffect(uiState.inboxRecordToHighlight, uiState.currentViewMode, inboxRecords) {
        val recordId = uiState.inboxRecordToHighlight
        if (recordId != null && uiState.currentViewMode == ContextViewMode.INBOX && inboxRecords.isNotEmpty()) {
            val indexToScroll = inboxRecords.indexOfFirst { it.id == recordId }
            Log.d(TAG, "INBOX view is active. Searching for record. Found index: $indexToScroll")
            if (indexToScroll != -1) {
                Log.d(TAG, "Scrolling to index: $indexToScroll")
                inboxListState.animateScrollToItem(indexToScroll)
                Log.d(TAG, "Waiting for highlight to finish...")
                delay(HIGHLIGHT_RESET_DELAY_MS)
                Log.d(TAG, "Highlight duration passed. Resetting state.")
                viewModel.onInboxHighlightShown()
            } else {
                Log.w(TAG, "Record ID $recordId not found yet. Waiting for inboxRecords update.")
            }
        }
    }
}

@Composable
private fun ContextScreenNewItemScrollEffect(
    viewModel: ContextScreenViewModel,
    listState: LazyListState,
    uiState: com.romankozak.forwardappmobile.features.contexts.ui.context_screen.state.ContextUiState,
    displayList: List<BacklogItemContent>,
    listContent: List<BacklogItemContent>,
) {
    LaunchedEffect(uiState.newlyAddedItemId, displayList) {
        val itemId = uiState.newlyAddedItemId
        Log.d("AutoScrollDebug", "newlyAddedItemId: $itemId, displayList size: ${displayList.size}")
        if (itemId != null && displayList.isNotEmpty()) {
            var index = displayList.indexOfFirst { it.backlogItem.id == itemId }
            if (index == -1) {
                index = displayList.indexOfFirst { it is BacklogItemContent.GoalItem && it.goal.id == itemId }
                Log.d("AutoScrollDebug", "Trying goal.id search, found index: $index")
            }
            Log.d("AutoScrollDebug", "Final index: $index for itemId: $itemId")
            if (index != -1) {
                val targetItem = displayList.getOrNull(index)
                val actualIndex =
                    targetItem?.let { item ->
                        listContent.indexOfFirst { it.backlogItem.id == item.backlogItem.id }
                    } ?: -1
                if (actualIndex != -1) {
                    yield()
                    listState.animateScrollToItem(actualIndex)
                    if (listState.firstVisibleItemIndex != actualIndex || listState.firstVisibleItemScrollOffset != 0) {
                        listState.scrollToItem(actualIndex)
                    }
                    viewModel.onScrolledToNewItem()
                } else {
                    Log.w("AutoScrollDebug", "Item $itemId found in displayList but missing in listContent!")
                }
            } else {
                Log.w("AutoScrollDebug", "Item not found in displayList by any ID!")
            }
        }
    }
}
