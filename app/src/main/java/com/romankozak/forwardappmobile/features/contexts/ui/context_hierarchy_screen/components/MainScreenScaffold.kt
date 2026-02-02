package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.Card
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavigationHistoryMenu
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Button
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Overlay
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenuItem
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.rememberHoldMenu2
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.ContextHierarchyScreenViewModel
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.OptimizedExpandingProjectHierarchyBottomNav
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.ProjectHierarchyScreenContent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.SearchProjectHierarchyBottomBar
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenSubState
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectHierarchyScreenUiState
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.features.reminders.dialogs.ReminderPropertiesDialog
import com.romankozak.forwardappmobile.ui.components.NewRecentListsSheet
import com.romankozak.forwardappmobile.ui.components.header.CommandDeckBackgroundModifier
import com.romankozak.forwardappmobile.ui.shared.InProgressIndicator

private const val UI_TAG = "ProjectHierarchyScreenUI_DEBUG"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ProjectHierarchyScreenScaffold(
    uiState: ProjectHierarchyScreenUiState,
    onEvent: (ContextHierarchyScreenEvent) -> Unit,
    enhancedNavigationManager: EnhancedNavigationManager,
    lastOngoingActivity: ActivityRecord?,
    viewModel: ContextHierarchyScreenViewModel,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val holdMenuController = rememberHoldMenu2()
    val listState = rememberLazyListState()
    var showContextSheet by remember { mutableStateOf(false) }
    var showSearchHistorySheet by remember { mutableStateOf(false) }

    val importLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
            uri?.let { onEvent(ContextHierarchyScreenEvent.ImportFromFileRequest(it)) }
        }

    val importAttachmentsLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
            uri?.let { onEvent(ContextHierarchyScreenEvent.ImportAttachmentsFromFile(it)) }
        }

    val selectiveImportLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
            uri?.let { onEvent(ContextHierarchyScreenEvent.SelectiveImportFromFileRequest(it)) }
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
        onEvent(ContextHierarchyScreenEvent.BackClick)
    }

    val indicatorState = remember { com.romankozak.forwardappmobile.ui.shared.InProgressIndicatorState(isInitiallyExpanded = false) }
    var showImportExportSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            val isSearchActive = uiState.subStateStack.any { it is ProjectHierarchyScreenSubState.LocalSearch }
            val isFocusMode = uiState.currentBreadcrumbs.isNotEmpty()
            val focusedProjectId = (uiState.currentSubState as? ProjectHierarchyScreenSubState.ProjectFocused)?.projectId
            val focusedProjectTitle =
                focusedProjectId?.let { id ->
                    uiState.projectHierarchy.allProjects.find { it.id == id }?.name
                        ?: uiState.currentBreadcrumbs.lastOrNull()?.name
                }

            ProjectHierarchyScreenTopAppBar(
                isSearchActive = isSearchActive,
                isFocusMode = isFocusMode,
                focusedProjectTitle = focusedProjectTitle,
                focusedProjectMenuClick =
                    focusedProjectId?.let { id ->
                        {
                            uiState.projectHierarchy.allProjects.find { it.id == id }?.let {
                                onEvent(ContextHierarchyScreenEvent.ContextMenuRequest(it))
                            }
                        }
                    },
                focusedProjectOpenClick =
                    focusedProjectId?.let { id ->
                        { onEvent(ContextHierarchyScreenEvent.ContextClick(id)) }
                    },
                canGoBack = uiState.canGoBack,
                canGoForward = uiState.canGoForward,
                onGoBack = { onEvent(ContextHierarchyScreenEvent.BackClick) },
                onGoForward = { onEvent(ContextHierarchyScreenEvent.ForwardClick) },
                onShowHistory = { onEvent(ContextHierarchyScreenEvent.HistoryClick) },
                onShowWifiServer = { onEvent(ContextHierarchyScreenEvent.ShowWifiServerDialog) },
                onShowWifiImport = { onEvent(ContextHierarchyScreenEvent.ShowWifiImportDialog) },
                onShowImportExportSheet = { showImportExportSheet = true },
                onShowSettings = { onEvent(ContextHierarchyScreenEvent.GoToSettings) },
                onShowAbout = { onEvent(ContextHierarchyScreenEvent.ShowAboutDialog) },
                onShowReminders = { onEvent(ContextHierarchyScreenEvent.GoToReminders) },
                onShowAttachmentsLibrary = { onEvent(ContextHierarchyScreenEvent.OpenAttachmentsLibrary) },
                onShowScriptsLibrary = { onEvent(ContextHierarchyScreenEvent.OpenScriptsLibrary) },
                onShowContextLab = { onEvent(ContextHierarchyScreenEvent.NavigateToContextLab) },
                syncStatus = uiState.syncStatus,
                onSyncIndicatorClick = { onEvent(ContextHierarchyScreenEvent.ShowWifiServerDialog) },
                featureToggles = uiState.featureToggles,
            )
        },
        bottomBar = {
            Column {
                InProgressIndicator(
                    ongoingActivity = lastOngoingActivity,
                    onStopClick = { viewModel.stopOngoingActivity() },
                    onReminderClick = { viewModel.setReminderForOngoingActivity() },
                    onIndicatorClick = { onEvent(ContextHierarchyScreenEvent.NavigateToActivityTracker) },
                    indicatorState = indicatorState,
                )
                val isSearchActive = uiState.subStateStack.any { it is ProjectHierarchyScreenSubState.LocalSearch }

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = if (isSearchActive) 8.dp else 16.dp,
                                vertical = if (isSearchActive) 8.dp else 12.dp,
                            )
                            .clip(RoundedCornerShape(20.dp))
                            .then(CommandDeckBackgroundModifier())
                            .padding(
                                horizontal = if (isSearchActive) 4.dp else 22.dp,
                                vertical = if (isSearchActive) 4.dp else 12.dp,
                            ),
                ) {
                    if (isSearchActive) {
                        SearchProjectHierarchyBottomBar(
                            searchQuery = uiState.searchQuery,
                            onQueryChange = { onEvent(ContextHierarchyScreenEvent.SearchQueryChanged(it)) },
                            onCloseSearch = {
                                onEvent(ContextHierarchyScreenEvent.CloseSearch)
                            },
                            onPerformGlobalSearch = { onEvent(ContextHierarchyScreenEvent.GlobalSearchPerform(it)) },
                            onShowSearchHistory = { showSearchHistorySheet = true },
                        )
                    } else {
                        OptimizedExpandingProjectHierarchyBottomNav(
                            onToggleSearch = { _ ->
                                onEvent(ContextHierarchyScreenEvent.SearchQueryChanged(TextFieldValue("")))
                            },
                            onGlobalSearchClick = { onEvent(ContextHierarchyScreenEvent.ShowSearchDialog) },
                            onShowCommandDeck = { onEvent(ContextHierarchyScreenEvent.CommandDeckClick) },
                            currentMode = uiState.planningMode,
                            planningModesEnabled = uiState.featureToggles[FeatureFlag.PlanningModes] == true,
                            onPlanningModeChange = { mode -> onEvent(ContextHierarchyScreenEvent.PlanningModeChange(mode)) },
                            onRecentsClick = { onEvent(ContextHierarchyScreenEvent.ShowRecentLists) },
                            onDayPlanClick = { onEvent(ContextHierarchyScreenEvent.DayPlanClick) },
                            onHomeClick = { onEvent(ContextHierarchyScreenEvent.HomeClick) },
                            onStrManagementClick = { onEvent(ContextHierarchyScreenEvent.NavigateToStrategicManagement) },
                            strategicManagementEnabled = uiState.featureToggles[FeatureFlag.StrategicManagement] == true,
                            aiChatEnabled = uiState.featureToggles[FeatureFlag.AiChat] == true,
                            aiInsightsEnabled = uiState.featureToggles[FeatureFlag.AiInsights] == true,
                            aiLifeManagementEnabled = uiState.featureToggles[FeatureFlag.AiLifeManagement] == true,
                            isExpanded = uiState.isBottomNavExpanded,
                            onExpandedChange = { expanded -> onEvent(ContextHierarchyScreenEvent.BottomNavExpandedChange(expanded)) },
                            onAiChatClick = { onEvent(ContextHierarchyScreenEvent.NavigateToChat) },
                            onActivityTrackerClick = { onEvent(ContextHierarchyScreenEvent.NavigateToActivityTracker) },
                            onInsightsClick = { onEvent(ContextHierarchyScreenEvent.NavigateToAiInsights) },
                            onShowReminders = { onEvent(ContextHierarchyScreenEvent.GoToReminders) },
                            onLifeStateClick = { onEvent(ContextHierarchyScreenEvent.NavigateToLifeState) },
                            onTacticsClick = { onEvent(ContextHierarchyScreenEvent.NavigateToTacticsScreen) },
                            onContextsClick = { showContextSheet = true },
                            onEvent = onEvent,
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            val isSearchActiveFab = uiState.subStateStack.any { it is ProjectHierarchyScreenSubState.LocalSearch }
            var showAddMenu by remember { mutableStateOf(false) }

            AnimatedVisibility(visible = !isSearchActiveFab) {
                val scriptsEnabled = uiState.featureToggles[FeatureFlag.ScriptsLibrary] == true
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
                            0 -> onEvent(ContextHierarchyScreenEvent.AddNewContextRequest)
                            1 -> onEvent(ContextHierarchyScreenEvent.AddNoteDocumentRequest)
                            2 -> onEvent(ContextHierarchyScreenEvent.AddChecklistRequest)
                            3 -> onEvent(ContextHierarchyScreenEvent.AddScriptRequest)
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
                                    onEvent(ContextHierarchyScreenEvent.AddNewContextRequest)
                                },
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                                text = { Text(text = stringResource(id = com.romankozak.forwardappmobile.R.string.add_action_note)) },
                                onClick = {
                                    showAddMenu = false
                                    onEvent(ContextHierarchyScreenEvent.AddNoteDocumentRequest)
                                },
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.FormatListBulleted, contentDescription = null) },
                                text = { Text(text = stringResource(id = com.romankozak.forwardappmobile.R.string.add_action_checklist)) },
                                onClick = {
                                    showAddMenu = false
                                    onEvent(ContextHierarchyScreenEvent.AddChecklistRequest)
                                },
                            )
                            if (scriptsEnabled) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) },
                                    text = { Text(text = "Скрипт") },
                                    onClick = {
                                        showAddMenu = false
                                        onEvent(ContextHierarchyScreenEvent.AddScriptRequest)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        ProjectHierarchyScreenContent(
            modifier = Modifier.padding(paddingValues),
            uiState = uiState,
            onEvent = onEvent,
            listState = listState,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
        )
    }

    if (uiState.showNavigationMenu) {
        NavigationHistoryMenu(
            navManager = enhancedNavigationManager,
            onDismiss = { onEvent(ContextHierarchyScreenEvent.HideHistory) },
        )
    }

    ContextBottomSheet(
        showSheet = showContextSheet,
        onDismiss = { showContextSheet = false },
        contexts = uiState.allContexts,
        contextMarkerToEmojiMap = uiState.contextMarkerToEmojiMap,
        onContextSelected = {
            onEvent(ContextHierarchyScreenEvent.ContextSelected(it))
            showContextSheet = false
        },
    )

    SearchHistoryBottomSheet(
        showSheet = showSearchHistorySheet,
        onDismiss = { showSearchHistorySheet = false },
        searchHistory = uiState.searchHistory,
        onHistoryClick = {
            onEvent(ContextHierarchyScreenEvent.SearchFromHistory(it))
            showSearchHistorySheet = false
        },
    )

    NewRecentListsSheet(
        showSheet = uiState.showRecentListsSheet,
        recentItems = uiState.recentItems,
        onDismiss = { onEvent(ContextHierarchyScreenEvent.DismissRecentLists) },
        onItemClick = { onEvent(ContextHierarchyScreenEvent.RecentItemSelected(it)) },
        onPinClick = { onEvent(ContextHierarchyScreenEvent.RecentItemPinClick(it)) },
    )

    if (showImportExportSheet) {
        ModalBottomSheet(onDismissRequest = { showImportExportSheet = false }) {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = "Імпорт / Експорт",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(160.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    item {
                        ImportExportTile(
                            icon = Icons.Default.CloudUpload,
                            title = "Експорт бекапу",
                            subtitle = "Зберегти JSON у файлі",
                            onClick = {
                                showImportExportSheet = false
                                onEvent(ContextHierarchyScreenEvent.ExportToFile)
                            },
                        )
                    }
                    item {
                        ImportExportTile(
                            icon = Icons.Default.CloudDownload,
                            title = "Повний імпорт",
                            subtitle = "Замінити поточні дані",
                            onClick = {
                                showImportExportSheet = false
                                importLauncher.launch("application/json")
                            },
                        )
                    }
                    item {
                        ImportExportTile(
                            icon = Icons.Default.FolderOpen,
                            title = "Вибірковий імпорт",
                            subtitle = "Обрати сутності",
                            onClick = {
                                showImportExportSheet = false
                                selectiveImportLauncher.launch("application/json")
                            },
                        )
                    }
                    item {
                        ImportExportTile(
                            icon = Icons.Default.Description,
                            title = "Експорт вкладень",
                            subtitle = "JSON вкладень",
                            onClick = {
                                showImportExportSheet = false
                                onEvent(ContextHierarchyScreenEvent.ExportAttachments)
                            },
                        )
                    }
                    item {
                        ImportExportTile(
                            icon = Icons.Default.FolderOpen,
                            title = "Імпорт вкладень",
                            subtitle = "Додати вкладення",
                            onClick = {
                                showImportExportSheet = false
                                importAttachmentsLauncher.launch("application/json")
                            },
                        )
                    }
                    item {
                        ImportExportTile(
                            icon = Icons.Default.CloudUpload,
                            title = "Push змін по Wi‑Fi",
                            subtitle = "Надіслати несинхронізоване",
                            onClick = {
                                showImportExportSheet = false
                                onEvent(ContextHierarchyScreenEvent.WifiPush("localhost:8080"))
                            },
                        )
                    }
                }
            }
        }
    }

    HandleProjectHierarchyDialogs(
        uiState = uiState,
        onEvent = onEvent,
    )

    uiState.recordForReminderDialog?.let { record ->
        ReminderPropertiesDialog(
            onDismiss = { viewModel.onReminderDialogDismiss() },
            onSetReminder = { timestamp -> viewModel.onSetReminder(timestamp) },
            onRemoveReminder =
                if (record.reminderTime != null) {
                    { _: String -> viewModel.onClearReminder() }
                } else {
                    null
                },
            currentReminders =
                listOfNotNull(record.reminderTime).map {
                    Reminder(
                        entityId = record.id,
                        entityType = "TASK",
                        reminderTime = it,
                        status = "SCHEDULED",
                        creationTime = System.currentTimeMillis(),
                    )
                },
        )
    }

    HoldMenu2Overlay(controller = holdMenuController)
}

@Composable
private fun ImportExportItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        leadingIcon = { Icon(icon, contentDescription = null) },
        text = {
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        onClick = onClick,
    )
}

@Composable
private fun ImportExportTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
