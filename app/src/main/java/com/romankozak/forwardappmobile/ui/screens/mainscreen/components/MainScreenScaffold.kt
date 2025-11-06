package com.romankozak.forwardappmobile.ui.screens.mainscreen.components

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.shared.features.reminders.data.model.Reminder
import com.romankozak.forwardappmobile.shared.features.reminders.data.repository.uuid4
import com.romankozak.forwardappmobile.ui.components.NewRecentListsSheet
import com.romankozak.forwardappmobile.ui.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.ui.reminders.dialogs.ReminderPropertiesDialog
import com.romankozak.forwardappmobile.ui.screens.mainscreen.MainScreenContent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.MainScreenViewModel
import com.romankozak.forwardappmobile.ui.screens.mainscreen.SearchBottomBar
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenUiState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainSubState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.OptimizedExpandingBottomNav
import com.romankozak.forwardappmobile.ui.screens.mainscreen.components.HandleDialogs
import com.romankozak.forwardappmobile.ui.shared.InProgressIndicator


private const val UI_TAG = "MainScreenUI_DEBUG"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreenScaffold(
    uiState: MainScreenUiState,
    onEvent: (MainScreenEvent) -> Unit,
    enhancedNavigationManager: EnhancedNavigationManager,
    lastOngoingActivity: com.romankozak.forwardappmobile.data.database.models.ActivityRecord?,
    viewModel: MainScreenViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val listState = rememberLazyListState()
    var showContextSheet by remember { mutableStateOf(false) }
    var showSearchHistorySheet by remember { mutableStateOf(false) }

    val importLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
            uri?.let { onEvent(MainScreenEvent.ImportFromFileRequest(it)) }
        }

    val backHandlerEnabled by remember(uiState.subStateStack, uiState.currentBreadcrumbs, uiState.areAnyProjectsExpanded) {
        derivedStateOf {
            val enabled =
                uiState.subStateStack.size > 1 ||
                        uiState.currentBreadcrumbs.isNotEmpty() ||
                        uiState.areAnyProjectsExpanded
            Log.d(UI_TAG, "BackHandler enabled = $enabled")
            enabled
        }
    }

    BackHandler(enabled = backHandlerEnabled) {
        Log.i(UI_TAG, "Custom BackHandler INVOKED")
        onEvent(MainScreenEvent.BackClick)
    }

    val indicatorState = remember { com.romankozak.forwardappmobile.ui.shared.InProgressIndicatorState(isInitiallyExpanded = false) }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            val isSearchActive = uiState.subStateStack.any { it is MainSubState.LocalSearch }
            val isFocusMode = uiState.currentBreadcrumbs.isNotEmpty()

            MainScreenTopAppBar(
                isSearchActive = isSearchActive,
                isFocusMode = isFocusMode,
                canGoBack = uiState.canGoBack,
                canGoForward = uiState.canGoForward,
                onGoBack = { onEvent(MainScreenEvent.BackClick) },
                onGoForward = { onEvent(MainScreenEvent.ForwardClick) },
                onShowHistory = { onEvent(MainScreenEvent.HistoryClick) },
                onAddNewProject = { onEvent(MainScreenEvent.AddNewProjectRequest) },
                onShowWifiServer = { onEvent(MainScreenEvent.ShowWifiServerDialog) },
                onShowWifiImport = { onEvent(MainScreenEvent.ShowWifiImportDialog) },
                onExportToFile = { onEvent(MainScreenEvent.ExportToFile) },
                onImportFromFile = { importLauncher.launch("application/json") },
                onShowSettings = { onEvent(MainScreenEvent.GoToSettings) },
                onShowAbout = { onEvent(MainScreenEvent.ShowAboutDialog) },
                onShowReminders = { onEvent(MainScreenEvent.GoToReminders) },
                onShowAttachmentsLibrary = { onEvent(MainScreenEvent.OpenAttachmentsLibrary) },
            )
        },
        bottomBar = {
            Column {
                InProgressIndicator(
                    ongoingActivity = lastOngoingActivity,
                    onStopClick = { viewModel.stopOngoingActivity() },
                    onReminderClick = { viewModel.setReminderForOngoingActivity() },
                    onIndicatorClick = { onEvent(MainScreenEvent.NavigateToActivityTracker) },
                    indicatorState = indicatorState
                )
                Spacer(modifier = Modifier.height(4.dp))
                val isSearchActive = uiState.subStateStack.any { it is MainSubState.LocalSearch }

                if (isSearchActive) {
                    SearchBottomBar(
                        searchQuery = uiState.searchQuery,
                        onQueryChange = { onEvent(MainScreenEvent.SearchQueryChanged(it)) },
                        onCloseSearch = {
                            onEvent(MainScreenEvent.CloseSearch)
                        },
                        onPerformGlobalSearch = { onEvent(MainScreenEvent.GlobalSearchPerform(it)) },
                        onShowSearchHistory = { showSearchHistorySheet = true },
                    )
                } else {
                    OptimizedExpandingBottomNav(
                        onToggleSearch = {
                            onEvent(MainScreenEvent.SearchQueryChanged(TextFieldValue("")))
                        },
                        onGlobalSearchClick = { onEvent(MainScreenEvent.ShowSearchDialog) },
                        currentMode = uiState.planningMode,
                        onPlanningModeChange = { onEvent(MainScreenEvent.PlanningModeChange(it)) },
                        onContextsClick = { showContextSheet = true },
                        onRecentsClick = { onEvent(MainScreenEvent.ShowRecentLists) },
                        onDayPlanClick = { onEvent(MainScreenEvent.DayPlanClick) },
                        onHomeClick = { onEvent(MainScreenEvent.HomeClick) },
                        onStrManagementClick = { onEvent(MainScreenEvent.NavigateToStrategicManagement) },
                        isExpanded = uiState.isBottomNavExpanded,
                        onExpandedChange = { onEvent(MainScreenEvent.BottomNavExpandedChange(it)) },
                        onAiChatClick = { onEvent(MainScreenEvent.NavigateToChat) },
                        onActivityTrackerClick = { onEvent(MainScreenEvent.NavigateToActivityTracker) },
                        onInsightsClick = { onEvent(MainScreenEvent.NavigateToAiInsights) },
                        onShowReminders = { onEvent(MainScreenEvent.GoToReminders) },
                        onEvent = onEvent,
                    )
                }
            }
        },
    ) { paddingValues ->
        MainScreenContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onEvent = onEvent,
            listState = listState,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope
        )
    }

    if (uiState.showNavigationMenu) {
        com.romankozak.forwardappmobile.ui.navigation.NavigationHistoryMenu(
            navManager = enhancedNavigationManager,
            onDismiss = { onEvent(MainScreenEvent.HideHistory) },
        )
    }

    ContextBottomSheet(
        showSheet = showContextSheet,
        onDismiss = { showContextSheet = false },
        contexts = uiState.allContexts,
        contextMarkerToEmojiMap = uiState.contextMarkerToEmojiMap,
        onContextSelected = {
            onEvent(MainScreenEvent.ContextSelected(it))
            showContextSheet = false
        },
    )

    SearchHistoryBottomSheet(
        showSheet = showSearchHistorySheet,
        onDismiss = { showSearchHistorySheet = false },
        searchHistory = uiState.searchHistory,
        onHistoryClick = {
            onEvent(MainScreenEvent.SearchFromHistory(it))
            showSearchHistorySheet = false
        },
    )

    NewRecentListsSheet(
        showSheet = uiState.showRecentListsSheet,
        recentItems = uiState.recentItems,
        onDismiss = { onEvent(MainScreenEvent.DismissRecentLists) },
        onItemClick = { onEvent(MainScreenEvent.RecentItemSelected(it)) },
        onPinClick = { onEvent(MainScreenEvent.RecentItemPinClick(it)) }
    )

    HandleDialogs(
        uiState = uiState,
        onEvent = onEvent,
    )

    uiState.recordForReminderDialog?.let { record ->
        ReminderPropertiesDialog(
            onDismiss = { viewModel.onReminderDialogDismiss() },
            onSetReminder = { timestamp -> viewModel.onSetReminder(timestamp) },
            onRemoveReminder = if (record.reminderTime != null) { { viewModel.onClearReminder() } } else null,
            currentReminders =
                listOfNotNull(record.reminderTime).map {
                    Reminder(
                        id = uuid4(),
                        entityId = record.id,
                        entityType = "TASK",
                        reminderTime = it,
                        status = "SCHEDULED",
                        creationTime = System.currentTimeMillis(),
                        snoozeUntil = null,
                    )
                },
        )
    }


}
