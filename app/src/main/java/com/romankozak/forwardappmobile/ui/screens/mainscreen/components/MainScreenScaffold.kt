package com.romankozak.forwardappmobile.ui.screens.mainscreen.components

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Overlay
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Button
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenuItem
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.rememberHoldMenu2
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
import com.romankozak.forwardappmobile.config.FeatureFlag
import com.romankozak.forwardappmobile.config.FeatureToggles


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
    val holdMenuController = rememberHoldMenu2()
    val listState = rememberLazyListState()
    var showContextSheet by remember { mutableStateOf(false) }
    var showSearchHistorySheet by remember { mutableStateOf(false) }

    val importLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
            uri?.let { onEvent(MainScreenEvent.ImportFromFileRequest(it)) }
        }

    val importAttachmentsLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
            uri?.let { onEvent(MainScreenEvent.ImportAttachmentsFromFile(it)) }
        }

    val selectiveImportLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
            uri?.let { onEvent(MainScreenEvent.SelectiveImportFromFileRequest(it)) }
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
    var showImportExportSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            val isSearchActive = uiState.subStateStack.any { it is MainSubState.LocalSearch }
            val isFocusMode = uiState.currentBreadcrumbs.isNotEmpty()
            val focusedProjectId = (uiState.currentSubState as? MainSubState.ProjectFocused)?.projectId
            val focusedProjectTitle =
                focusedProjectId?.let { id ->
                    uiState.projectHierarchy.allProjects.find { it.id == id }?.name
                        ?: uiState.currentBreadcrumbs.lastOrNull()?.name
                }

            MainScreenTopAppBar(
                isSearchActive = isSearchActive,
                isFocusMode = isFocusMode,
                focusedProjectTitle = focusedProjectTitle,
                focusedProjectMenuClick = focusedProjectId?.let { id ->
                    {
                        uiState.projectHierarchy.allProjects.find { it.id == id }?.let {
                            onEvent(MainScreenEvent.ProjectMenuRequest(it))
                        }
                    }
                },
                focusedProjectOpenClick = focusedProjectId?.let { id ->
                    { onEvent(MainScreenEvent.ProjectClick(id)) }
                },
                canGoBack = uiState.canGoBack,
                canGoForward = uiState.canGoForward,
                onGoBack = { onEvent(MainScreenEvent.BackClick) },
                onGoForward = { onEvent(MainScreenEvent.ForwardClick) },
                onShowHistory = { onEvent(MainScreenEvent.HistoryClick) },
                onShowWifiServer = { onEvent(MainScreenEvent.ShowWifiServerDialog) },
                onShowWifiImport = { onEvent(MainScreenEvent.ShowWifiImportDialog) },
                onShowImportExportSheet = { showImportExportSheet = true },
                onShowSettings = { onEvent(MainScreenEvent.GoToSettings) },
                onShowAbout = { onEvent(MainScreenEvent.ShowAboutDialog) },
                onShowReminders = { onEvent(MainScreenEvent.GoToReminders) },
                onShowAttachmentsLibrary = { onEvent(MainScreenEvent.OpenAttachmentsLibrary) },
                onShowScriptsLibrary = { onEvent(MainScreenEvent.OpenScriptsLibrary) },
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
                        planningModesEnabled = com.romankozak.forwardappmobile.config.FeatureToggles.isEnabled(com.romankozak.forwardappmobile.config.FeatureFlag.PlanningModes),
                        onPlanningModeChange = { onEvent(MainScreenEvent.PlanningModeChange(it)) },
                        onContextsClick = { showContextSheet = true },
                        onRecentsClick = { onEvent(MainScreenEvent.ShowRecentLists) },
                        onDayPlanClick = { onEvent(MainScreenEvent.DayPlanClick) },
                        onHomeClick = { onEvent(MainScreenEvent.HomeClick) },
                        onStrManagementClick = { onEvent(MainScreenEvent.NavigateToStrategicManagement) },
                        strategicManagementEnabled = com.romankozak.forwardappmobile.config.FeatureToggles.isEnabled(com.romankozak.forwardappmobile.config.FeatureFlag.StrategicManagement),
                        aiChatEnabled = com.romankozak.forwardappmobile.config.FeatureToggles.isEnabled(com.romankozak.forwardappmobile.config.FeatureFlag.AiChat),
                        aiInsightsEnabled = com.romankozak.forwardappmobile.config.FeatureToggles.isEnabled(com.romankozak.forwardappmobile.config.FeatureFlag.AiInsights),
                        aiLifeManagementEnabled = com.romankozak.forwardappmobile.config.FeatureToggles.isEnabled(com.romankozak.forwardappmobile.config.FeatureFlag.AiLifeManagement),
                        isExpanded = uiState.isBottomNavExpanded,
                        onExpandedChange = { onEvent(MainScreenEvent.BottomNavExpandedChange(it)) },
                        onAiChatClick = { onEvent(MainScreenEvent.NavigateToChat) },
                        onActivityTrackerClick = { onEvent(MainScreenEvent.NavigateToActivityTracker) },
                        onInsightsClick = { onEvent(MainScreenEvent.NavigateToAiInsights) },
                        onShowReminders = { onEvent(MainScreenEvent.GoToReminders) },
                        onLifeStateClick = { onEvent(MainScreenEvent.NavigateToLifeState) },
                        onEvent = onEvent,
                    )
                }
            }
        },
        floatingActionButton = {
            val isSearchActiveFab = uiState.subStateStack.any { it is MainSubState.LocalSearch }
            var showAddMenu by remember { mutableStateOf(false) }

            AnimatedVisibility(visible = !isSearchActiveFab) {
                val scriptsEnabled = FeatureToggles.isEnabled(FeatureFlag.ScriptsLibrary)
                val menuItems =
                    buildList {
                        add(
                            HoldMenuItem(
                                label = stringResource(id = com.romankozak.forwardappmobile.R.string.add_action_project),
                                icon = Icons.Default.FolderOpen,
                            ),
                        )
                        add(
                            HoldMenuItem(
                                label = stringResource(id = com.romankozak.forwardappmobile.R.string.add_action_note),
                                icon = Icons.Default.Description,
                            ),
                        )
                        add(
                            HoldMenuItem(
                                label = stringResource(id = com.romankozak.forwardappmobile.R.string.add_action_checklist),
                                icon = Icons.Default.FormatListBulleted,
                            ),
                        )
                        if (scriptsEnabled) {
                            add(
                                HoldMenuItem(
                                    label = "Скрипт",
                                    icon = Icons.Default.Code,
                                ),
                            )
                        }
                    }
                HoldMenu2Button(
                    items = menuItems,
                    controller = holdMenuController,
                    onSelect = { index ->
                        when (index) {
                            0 -> onEvent(MainScreenEvent.AddNewProjectRequest)
                            1 -> onEvent(MainScreenEvent.AddNoteDocumentRequest)
                            2 -> onEvent(MainScreenEvent.AddChecklistRequest)
                            3 -> onEvent(MainScreenEvent.AddScriptRequest)
                        }
                    },
                    onTap = { showAddMenu = !showAddMenu },
                    menuAlignment = com.romankozak.forwardappmobile.features.common.components.holdmenu2.MenuAlignment.END,
                    iconPosition = com.romankozak.forwardappmobile.features.common.components.holdmenu2.IconPosition.END,
                ) {
                    Box {
                        FloatingActionButton(onClick = { showAddMenu = !showAddMenu }) {
                            Icon(Icons.Default.Add, contentDescription = "Додати")
                        }
                        DropdownMenu(
                            expanded = showAddMenu,
                            onDismissRequest = { showAddMenu = false },
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                                text = { Text(text = stringResource(id = com.romankozak.forwardappmobile.R.string.add_action_project)) },
                                onClick = {
                                    showAddMenu = false
                                    onEvent(MainScreenEvent.AddNewProjectRequest)
                                },
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                                text = { Text(text = stringResource(id = com.romankozak.forwardappmobile.R.string.add_action_note)) },
                                onClick = {
                                    showAddMenu = false
                                    onEvent(MainScreenEvent.AddNoteDocumentRequest)
                                },
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.FormatListBulleted, contentDescription = null) },
                                text = { Text(text = stringResource(id = com.romankozak.forwardappmobile.R.string.add_action_checklist)) },
                                onClick = {
                                    showAddMenu = false
                                    onEvent(MainScreenEvent.AddChecklistRequest)
                                },
                            )
                            if (scriptsEnabled) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                                    text = { Text(text = "Скрипт") },
                                    onClick = {
                                        showAddMenu = false
                                        onEvent(MainScreenEvent.AddScriptRequest)
                                    },
                                )
                            }
                        }
                    }
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

    if (showImportExportSheet) {
        ModalBottomSheet(onDismissRequest = { showImportExportSheet = false }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = "Імпорт / Експорт",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                ImportExportItem(
                    icon = Icons.Default.CloudUpload,
                    title = "Експорт повного бекапу",
                    subtitle = "Зберегти JSON у файлі",
                    onClick = {
                        showImportExportSheet = false
                        onEvent(MainScreenEvent.ExportToFile)
                    }
                )
                ImportExportItem(
                    icon = Icons.Default.CloudDownload,
                    title = "Повний імпорт з файлу",
                    subtitle = "Замінити поточні дані бекапом",
                    onClick = {
                        showImportExportSheet = false
                        importLauncher.launch("application/json")
                    }
                )
                ImportExportItem(
                    icon = Icons.Default.FolderOpen,
                    title = "Вибірковий імпорт",
                    subtitle = "Обрати сутності для імпорту",
                    onClick = {
                        showImportExportSheet = false
                        selectiveImportLauncher.launch("application/json")
                    }
                )
                ImportExportItem(
                    icon = Icons.Default.Description,
                    title = "Експорт вкладень",
                    subtitle = "Зберегти JSON вкладень",
                    onClick = {
                        showImportExportSheet = false
                        onEvent(MainScreenEvent.ExportAttachments)
                    }
                )
                ImportExportItem(
                    icon = Icons.Default.FolderOpen,
                    title = "Імпорт вкладень",
                    subtitle = "Додати вкладення з файлу",
                    onClick = {
                        showImportExportSheet = false
                        importAttachmentsLauncher.launch("application/json")
                    }
                )
            }
        }
    }

    HandleDialogs(
        uiState = uiState,
        onEvent = onEvent,
    )

    uiState.recordForReminderDialog?.let { record ->
        ReminderPropertiesDialog(
            onDismiss = { viewModel.onReminderDialogDismiss() },
            onSetReminder = { timestamp -> viewModel.onSetReminder(timestamp) },
            onRemoveReminder = if (record.reminderTime != null) { { viewModel.onClearReminder() } } else null,
            currentReminders = listOfNotNull(record.reminderTime).map { com.romankozak.forwardappmobile.data.database.models.Reminder(entityId = record.id, entityType = "TASK", reminderTime = it, status = "SCHEDULED", creationTime = System.currentTimeMillis()) },
        )
    }

    HoldMenu2Overlay(controller = holdMenuController)
}

@Composable
private fun ImportExportItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        leadingIcon = { Icon(icon, contentDescription = null) },
        text = {
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        onClick = onClick
    )
}
