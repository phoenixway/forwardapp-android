package com.romankozak.forwardappmobile.features.mainscreen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Today
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.navigateOrFallback
import com.romankozak.forwardappmobile.core.navigation.routes.STRATEGIC_MANAGEMENT_ROUTE
import com.romankozak.forwardappmobile.features.ai.insights.AiInsightsViewModel
import com.romankozak.forwardappmobile.features.activitytracker.ActivityTrackerViewModel
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.ContextHierarchyScreenViewModel
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components.ContextMarkersSheet
import com.romankozak.forwardappmobile.features.daymanagement.runtime.presentation.DayManagementRuntimeUiState
import com.romankozak.forwardappmobile.features.daymanagement.runtime.presentation.DayManagementRuntimeViewModel
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementScreen
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementViewModel
import com.romankozak.forwardappmobile.features.daymanagement.ui.defaultTodayTabForRuntimeState
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus.DayFocusesViewModel
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayPlanViewModel
import com.romankozak.forwardappmobile.features.globalsearch.GlobalCreateActionsHost
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.CoreBottomPanel
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.DashboardBottomPanel
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.StrategicArcBottomPanel
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.StrategyBottomPanel
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.TacticsBottomPanel
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.TodayBottomPanel
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelGlobalActions
import com.romankozak.forwardappmobile.features.missions.presentation.TacticalManagementScreen
import com.romankozak.forwardappmobile.features.missions.presentation.TacticalMissionViewModel
import com.romankozak.forwardappmobile.features.mainscreen.session.SessionModeState
import com.romankozak.forwardappmobile.features.recent.RecentViewModel
import com.romankozak.forwardappmobile.features.strategicmanagement.StrategicManagementScreen
import com.romankozak.forwardappmobile.features.userawareness.UserAwarenessHeaderBadge
import com.romankozak.forwardappmobile.features.userawareness.UserAwarenessQuickSwitchDialog
import com.romankozak.forwardappmobile.features.userawareness.UserAwarenessViewModel
import com.romankozak.forwardappmobile.ui.components.header.CommandDeckHeaderPreset
import com.romankozak.forwardappmobile.ui.components.header.CommandDeckBackgroundModifier
import com.romankozak.forwardappmobile.ui.components.header.FAHeader
import com.romankozak.forwardappmobile.ui.components.header.FAHeaderBackground
import com.romankozak.forwardappmobile.ui.dialogs.WifiImportDialog
import com.romankozak.forwardappmobile.ui.dialogs.WifiServerDialog
import kotlinx.coroutines.launch
import java.net.URLEncoder
import kotlin.math.abs

const val MAIN_SCREEN_DASHBOARD_ROUTE = "command_deck_dashboard"
const val MAIN_SCREEN_CORE_ROUTE = "command_deck_core"
const val MAIN_SCREEN_STRATEGIC_ARC_ROUTE = "command_deck_strategic_arc"
const val MAIN_SCREEN_TACTICS_ROUTE = "command_deck_tactics"
const val MAIN_SCREEN_TODAY_ROUTE = "command_deck_today"
private const val TAB_SWIPE_THRESHOLD_PX = 48f

@Composable
fun MainScreenLayout(
    navController: NavController,
    navigationManager: EnhancedNavigationManager? = null,
    onNavigateToProjectHierarchy: () -> Unit,
    onNavigateToManageContextMarkers: () -> Unit = {},
    onNavigateToPresets: () -> Unit,
    onNavigateToCharacter: () -> Unit,
    onNavigateToGlobalSearch: () -> Unit,
    onNavigateToQuickCatch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToTracker: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onNavigateToAiInsights: () -> Unit,
    onNavigateToAiLifeManagement: () -> Unit,
    // New Import/Export Callbacks
    onExportToFile: () -> Unit,
    onImportFromFileRequest: (Uri) -> Unit,
    onSelectiveImportFromFileRequest: (Uri) -> Unit,
    onExportAttachments: () -> Unit,
    onImportAttachmentsFromFileRequest: (Uri) -> Unit,
    onWifiPush: (String) -> Unit,
    onShowWifiServer: () -> Unit,
    onShowWifiImport: () -> Unit,
    onNavigateToSyncScreenWithData: (String) -> Unit,
    onNavigateToAttachments: () -> Unit,
    onNavigateToScripts: () -> Unit,
    onNavigateToRecentItem: (RecentItem) -> Unit,
    recentViewModel: RecentViewModel = hiltViewModel(),
    commandDeckViewModel: CommandDeckViewModel = hiltViewModel(),
    contextHierarchyViewModel: ContextHierarchyScreenViewModel = hiltViewModel(),
) {
    val tabs =
        listOf(
            CommandDeckTab.Dashboard,
            CommandDeckTab.Today,
            CommandDeckTab.Tactics,
            CommandDeckTab.StrategicArc,
            CommandDeckTab.Strategy,
            CommandDeckTab.Core,
        )
    val initialTab = commandDeckViewModel.getSelectedTab()
    val pagerState =
        rememberPagerState(
            initialPage = tabs.indexOf(initialTab).takeIf { it >= 0 } ?: 0,
        ) {
            tabs.size
        }
    val scope = rememberCoroutineScope()

    val currentRoute =
        when (tabs[pagerState.currentPage]) {
            CommandDeckTab.Dashboard -> MAIN_SCREEN_DASHBOARD_ROUTE
            CommandDeckTab.Core -> MAIN_SCREEN_CORE_ROUTE
            CommandDeckTab.Strategy -> STRATEGIC_MANAGEMENT_ROUTE
            CommandDeckTab.StrategicArc -> MAIN_SCREEN_STRATEGIC_ARC_ROUTE
            CommandDeckTab.Tactics -> MAIN_SCREEN_TACTICS_ROUTE
            CommandDeckTab.Today -> MAIN_SCREEN_TODAY_ROUTE
        }

    val snackbarHostState = remember { SnackbarHostState() }
    val dayManagementViewModel: DayManagementViewModel = hiltViewModel()
    val dayManagementRuntimeViewModel: DayManagementRuntimeViewModel = hiltViewModel()
    val dayPlanViewModel: DayPlanViewModel = hiltViewModel()
    val activityTrackerViewModel: ActivityTrackerViewModel = hiltViewModel()
    val dayFocusesViewModel: DayFocusesViewModel = hiltViewModel()
    val dayManagementUiState by dayManagementViewModel.uiState.collectAsStateWithLifecycle()
    val dayManagementRuntimeUiState by dayManagementRuntimeViewModel.uiState.collectAsStateWithLifecycle()
    val defaultTodayTab = defaultTodayTabForRuntimeState(dayManagementRuntimeUiState.runtimeState)
    val selectedTodayTab = commandDeckViewModel.getSelectedTodayTab(defaultTodayTab)
    val context = LocalContext.current
    val userAwarenessViewModel: UserAwarenessViewModel = hiltViewModel()
    val aiInsightsViewModel: AiInsightsViewModel = hiltViewModel()
    val focusContextsViewModel: FocusContextsViewModel = hiltViewModel()
    val tacticalMissionViewModel: TacticalMissionViewModel = hiltViewModel()
    val tacticalObsidianVaultName by tacticalMissionViewModel.obsidianVaultName.collectAsStateWithLifecycle()
    val activeUserAwarenessState by userAwarenessViewModel.activeState.collectAsStateWithLifecycle()
    val sessionModeState by commandDeckViewModel.sessionModeState.collectAsStateWithLifecycle()

    LaunchedEffect(currentRoute, selectedTodayTab) {
        if (currentRoute == MAIN_SCREEN_TODAY_ROUTE && dayManagementUiState.selectedTab != selectedTodayTab) {
            dayManagementViewModel.selectTab(selectedTodayTab)
        }
    }
    val latestSessionReason by commandDeckViewModel.latestSessionReason.collectAsStateWithLifecycle()
    var isSessionModeCardExpanded by remember { mutableStateOf(commandDeckViewModel.isSessionModeCardExpanded()) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showStateSwitchDialog by remember { mutableStateOf(false) }
    var showContextMarkersSheet by remember { mutableStateOf(false) }
    var showGlobalCreateSheet by remember { mutableStateOf(false) }

    LaunchedEffect(commandDeckViewModel) {
        commandDeckViewModel.importExportUiEvents.collect { message ->
            when (message) {
                is CommandDeckUiEvent.ShowMessage -> snackbarHostState.showSnackbar(message.message)
                is CommandDeckUiEvent.ShowSessionModeChanged -> Unit
                is CommandDeckUiEvent.NavigateToSyncScreenWithData ->
                    onNavigateToSyncScreenWithData(message.json)
            }
        }
    }

    LaunchedEffect(commandDeckViewModel) {
        commandDeckViewModel.uiEvents.collect { event ->
            when (event) {
                is CommandDeckUiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is CommandDeckUiEvent.ShowSessionModeChanged -> {
                    val primaryResult =
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = "Report why mode changed",
                            duration = SnackbarDuration.Short,
                        )
                    if (primaryResult == SnackbarResult.ActionPerformed) {
                        commandDeckViewModel.prepareModeChangeReason(event.newMode)
                    }

                    if (event.previousMode != null) {
                        val secondaryResult =
                            snackbarHostState.showSnackbar(
                                message = "Попередній режим ${event.previousMode.title} завершено",
                                actionLabel = "Report prev mode results",
                                duration = SnackbarDuration.Short,
                            )
                        if (secondaryResult == SnackbarResult.ActionPerformed) {
                            commandDeckViewModel.preparePreviousSessionResult(event.previousMode)
                        }
                    }
                }
                is CommandDeckUiEvent.NavigateToSyncScreenWithData ->
                    onNavigateToSyncScreenWithData(event.json)
            }
        }
    }

    LaunchedEffect(dayManagementUiState.dayPlanId) {
        dayManagementUiState.dayPlanId?.let(dayPlanViewModel::loadDataForPlan)
    }

    val headerModifier =
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) { commandDeckViewModel.openContextInput() }

    val selectedTabIndex = pagerState.currentPage
    LaunchedEffect(selectedTabIndex, tabs) {
        tabs.getOrNull(selectedTabIndex)?.let(commandDeckViewModel::setSelectedTab)
    }
    val dashboardTabIndex = tabs.indexOf(CommandDeckTab.Dashboard)
    BackHandler(enabled = selectedTabIndex != dashboardTabIndex) {
        scope.launch {
            pagerState.scrollToPage(dashboardTabIndex)
        }
    }
    val titleStateBadge: @Composable (() -> Unit) = {
        UserAwarenessHeaderBadge(
            activeState = activeUserAwarenessState,
            onOpenQuickSwitch = { showStateSwitchDialog = true },
        )
    }
    val dashboardHeaderContent: @Composable () -> Unit = {
        val isDebugBuild = com.romankozak.forwardappmobile.BuildConfig.BUILD_TYPE == "debug"
        val showBadge =
            isDebugBuild ||
                com.romankozak.forwardappmobile.BuildConfig.IS_EXPERIMENTAL_BUILD
        val layout =
            CommandDeckHeaderPreset(
                onClick = {},
                onRightClick = { onNavigateToCharacter() },
                titleTrailingContent = titleStateBadge,
                rightContent = {
                    if (showBadge) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ) {
                            Text(
                                text =
                                    if (isDebugBuild) {
                                        "Debug"
                                    } else {
                                        "Experimental"
                                    },
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                },
            )
        Box(
            modifier =
                headerModifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                        .then(CommandDeckBackgroundModifier())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                layout.Content()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                when (currentRoute) {
                    MAIN_SCREEN_DASHBOARD_ROUTE -> Unit

                    MAIN_SCREEN_CORE_ROUTE -> Unit

                    MAIN_SCREEN_TODAY_ROUTE -> Unit

                    STRATEGIC_MANAGEMENT_ROUTE -> Unit

                    MAIN_SCREEN_STRATEGIC_ARC_ROUTE -> Unit

                    MAIN_SCREEN_TACTICS_ROUTE -> Unit

                    else ->
                        FAHeader(
                            layout = CommandDeckHeaderPreset(onClick = {}, titleTrailingContent = titleStateBadge),
                            backgroundStyle = FAHeaderBackground.CommandDeck,
                            modifier = headerModifier,
                        )
                }
            },
            floatingActionButton = {
                when {
                    currentRoute == MAIN_SCREEN_TODAY_ROUTE &&
                        dayManagementUiState.selectedTab in DayManagementTab.todaySubTabs() &&
                        dayManagementUiState.selectedTab != DayManagementTab.JOURNAL -> {
                        if (dayManagementUiState.selectedTab == DayManagementTab.DAY_START) {
                            TodayDayStartFab(
                                selectedDate = dayManagementUiState.selectedDate,
                                onDateSelected = dayManagementViewModel::navigateToDate,
                                onWakeUp = dayManagementRuntimeViewModel::wakeUp,
                                onSleep = dayManagementRuntimeViewModel::sleep,
                            )
                        } else {
                            TodayDatePickerFab(
                                selectedDate = dayManagementUiState.selectedDate,
                                onDateSelected = dayManagementViewModel::navigateToDate,
                            )
                        }
                    }
                }
            },
            bottomBar = {
                MainScreenBottomBar(
                    currentRoute = currentRoute,
                    contextHierarchyViewModel = contextHierarchyViewModel,
                    onNavigateToProjectHierarchy = onNavigateToProjectHierarchy,
                    onShowContextMarkersSheet = { showContextMarkersSheet = true },
                    onNavigateToPresets = onNavigateToPresets,
                    onNavigateToGlobalSearch = onNavigateToGlobalSearch,
                    onNavigateToQuickCatch = onNavigateToQuickCatch,
                    onShowCreateActions = { showGlobalCreateSheet = true },
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToInbox = onNavigateToInbox,
                    onNavigateToTracker = onNavigateToTracker,
                    onNavigateToReminders = onNavigateToReminders,
                    onNavigateToAiChat = onNavigateToAiChat,
                    onNavigateToAiInsights = onNavigateToAiInsights,
                    onNavigateToAiLifeManagement = onNavigateToAiLifeManagement,
                    onExportToFile = onExportToFile,
                    onImportFromFileRequest = onImportFromFileRequest,
                    onSelectiveImportFromFileRequest = onSelectiveImportFromFileRequest,
                    onExportAttachments = onExportAttachments,
                    onImportAttachmentsFromFileRequest = onImportAttachmentsFromFileRequest,
                    onWifiPush = onWifiPush,
                    onShowWifiServer = onShowWifiServer,
                    onShowWifiImport = onShowWifiImport,
                    onNavigateToAttachments = onNavigateToAttachments,
                    onNavigateToScripts = onNavigateToScripts,
                    onShowAbout = { showAboutDialog = true },
                    onNavigateToRecentItem = onNavigateToRecentItem,
                    recentViewModel = recentViewModel,
                    commandDeckViewModel = commandDeckViewModel,
                    dayManagementViewModel = dayManagementViewModel,
                    dayManagementRuntimeViewModel = dayManagementRuntimeViewModel,
                    dayManagementRuntimeUiState = dayManagementRuntimeUiState,
                    defaultTodayTab = defaultTodayTab,
                    dayPlanViewModel = dayPlanViewModel,
                    activityTrackerViewModel = activityTrackerViewModel,
                    dayFocusesViewModel = dayFocusesViewModel,
                    tacticalMissionViewModel = tacticalMissionViewModel,
                )
            },
        ) { paddingValues ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(Color.Transparent),
            ) {
                Spacer(Modifier.height(8.dp))

                CommandDeckTabRow(
                    tabs = tabs,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { index ->
                        scope.launch {
                            pagerState.scrollToPage(index)
                        }
                    },
                )

                if (currentRoute == MAIN_SCREEN_DASHBOARD_ROUTE) {
                    Spacer(Modifier.height(6.dp))
                    dashboardHeaderContent()
                    Spacer(Modifier.height(4.dp))
                } else {
                    Spacer(Modifier.height(4.dp))
                }

                MainScreenPagerContent(
                    tabs = tabs,
                    pagerState = pagerState,
                    navController = navController,
                    navigationManager = navigationManager,
                    context = context,
                    aiInsightsViewModel = aiInsightsViewModel,
                    focusContextsViewModel = focusContextsViewModel,
                    tacticalMissionViewModel = tacticalMissionViewModel,
                    tacticalObsidianVaultName = tacticalObsidianVaultName,
                    dayManagementViewModel = dayManagementViewModel,
                    currentDayManagementTab = dayManagementUiState.selectedTab,
                    defaultTodayTab = selectedTodayTab,
                    dayManagementRuntimeViewModel = dayManagementRuntimeViewModel,
                    dayPlanViewModel = dayPlanViewModel,
                    activityTrackerViewModel = activityTrackerViewModel,
                    dayFocusesViewModel = dayFocusesViewModel,
                    sessionModeState = sessionModeState,
                    latestSessionReason = latestSessionReason,
                    onSessionModeSelected = commandDeckViewModel::setSessionMode,
                    isSessionModeCardExpanded = isSessionModeCardExpanded,
                    onSessionModeCardExpandedChange = { expanded ->
                        isSessionModeCardExpanded = expanded
                        commandDeckViewModel.setSessionModeCardExpanded(expanded)
                    },
                )
            }
        }

        MainScreenCommandDeckTransientUi(commandDeckViewModel = commandDeckViewModel)

        GlobalCreateActionsHost(
            showCreateSheet = showGlobalCreateSheet,
            onDismissCreateSheet = { showGlobalCreateSheet = false },
            onCreateContext = commandDeckViewModel::createContext,
            onCreateDocument = commandDeckViewModel::createAttachmentFromCommandDeck,
            onCreateReminder = commandDeckViewModel::createReminder,
            snackbarHostState = snackbarHostState,
        )

        if (showStateSwitchDialog) {
            UserAwarenessQuickSwitchDialog(
                onDismiss = { showStateSwitchDialog = false },
                onNormal = userAwarenessViewModel::setNormal,
                onExhaustion = userAwarenessViewModel::setExhaustion,
                onUnproductive = userAwarenessViewModel::setUnproductive,
                onCrisis = userAwarenessViewModel::setCrisis,
            )
        }

        MainScreenContextMarkersSheet(
            showSheet = showContextMarkersSheet,
            onDismiss = { showContextMarkersSheet = false },
            contextHierarchyViewModel = contextHierarchyViewModel,
            onManageContextMarkers = onNavigateToManageContextMarkers,
            navigationManager = navigationManager,
            navController = navController,
            onContextSelected = { contextMarkerName, tag ->
                showContextMarkersSheet = false
                scope.launch {
                    val normalizedTag = tag?.trim()?.takeIf { it.isNotBlank() }?.let {
                        if (it.startsWith("#")) it else "#$it"
                    }
                    val targetContextId = normalizedTag?.let { normalized ->
                        contextHierarchyViewModel.findFirstContextIdForTag(normalized)
                    }

                    if (targetContextId != null) {
                        navigationManager.navigateOrFallback(
                            navController = navController,
                            target =
                                NavTarget.ContextDetail(
                                    contextId = targetContextId,
                                    initialViewMode = "BACKLOG",
                                    initialTagQuery = normalizedTag,
                                ),
                            recordInHistory = true,
                        )
                    } else {
                        contextHierarchyViewModel.onEvent(
                            com.romankozak.forwardappmobile
                                .features.contexts.ui.context_hierarchy_screen.models
                                .ContextHierarchyScreenEvent.ContextSelected(
                                    contextMarkerName,
                                ),
                        )
                    }
                }
            },
        )

        MainScreenCommandDeckDialogs(commandDeckViewModel = commandDeckViewModel)
        MainScreenAboutDialog(
            showDialog = showAboutDialog,
            onDismiss = { showAboutDialog = false },
            contextHierarchyViewModel = contextHierarchyViewModel,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TodayDayStartFab(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    onWakeUp: () -> Unit,
    onSleep: () -> Unit,
) {
    var isMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    Box {
        FloatingActionButton(onClick = { isMenuExpanded = true }) {
            Icon(
                imageVector = Icons.Outlined.Today,
                contentDescription = "Меню дій початку дня",
            )
        }
        DropdownMenu(
            expanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
            modifier =
                Modifier.background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                ),
        ) {
            DropdownMenuItem(
                text = { Text("Вибрати день") },
                leadingIcon = { Icon(Icons.Outlined.CalendarToday, contentDescription = null) },
                onClick = {
                    isMenuExpanded = false
                    showDatePicker = true
                },
            )
            DropdownMenuItem(
                text = { Text("Проснувся!") },
                leadingIcon = { Icon(Icons.Outlined.PlayCircle, contentDescription = null) },
                onClick = {
                    isMenuExpanded = false
                    onWakeUp()
                },
            )
            DropdownMenuItem(
                text = { Text("Пішов спати") },
                leadingIcon = { Icon(Icons.Outlined.TaskAlt, contentDescription = null) },
                onClick = {
                    isMenuExpanded = false
                    onSleep()
                },
            )
        }
    }

    TodayDatePickerDialogHost(
        visible = showDatePicker,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        onDismiss = { showDatePicker = false },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TodayDatePickerFab(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    FloatingActionButton(onClick = { showDatePicker = true }) {
        Icon(
            imageVector = Icons.Outlined.CalendarToday,
            contentDescription = "Вибрати день",
        )
    }

    TodayDatePickerDialogHost(
        visible = showDatePicker,
        selectedDate = selectedDate,
        onDateSelected = onDateSelected,
        onDismiss = { showDatePicker = false },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TodayDatePickerDialogHost(
    visible: Boolean,
    selectedDate: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) {
        return
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let(onDateSelected)
                    onDismiss()
                },
            ) {
                Text("Обрати")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

private fun buildExternalTarget(
    linkType: LinkType?,
    target: String,
    vault: String? = null,
    globalObsidianVaultName: String? = null,
): String {
    val trimmed = target.trim()
    if (linkType == LinkType.OBSIDIAN && !trimmed.startsWith("obsidian://", ignoreCase = true)) {
        val vaultName = vault?.takeIf { it.isNotBlank() } ?: globalObsidianVaultName?.takeIf { it.isNotBlank() }
        val encodedFile = URLEncoder.encode(trimmed, "UTF-8")
        return if (vaultName != null) {
            "obsidian://open?vault=${URLEncoder.encode(vaultName, "UTF-8")}&file=$encodedFile"
        } else {
            "obsidian://open?file=$encodedFile"
        }
    }
    return trimmed
}

@Composable
private fun MainScreenBottomBar(
    currentRoute: String,
    contextHierarchyViewModel: ContextHierarchyScreenViewModel,
    onNavigateToProjectHierarchy: () -> Unit,
    onShowContextMarkersSheet: () -> Unit,
    onNavigateToPresets: () -> Unit,
    onNavigateToGlobalSearch: () -> Unit,
    onNavigateToQuickCatch: () -> Unit,
    onShowCreateActions: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToTracker: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onNavigateToAiInsights: () -> Unit,
    onNavigateToAiLifeManagement: () -> Unit,
    onExportToFile: () -> Unit,
    onImportFromFileRequest: (Uri) -> Unit,
    onSelectiveImportFromFileRequest: (Uri) -> Unit,
    onExportAttachments: () -> Unit,
    onImportAttachmentsFromFileRequest: (Uri) -> Unit,
    onWifiPush: (String) -> Unit,
    onShowWifiServer: () -> Unit,
    onShowWifiImport: () -> Unit,
    onNavigateToAttachments: () -> Unit,
    onNavigateToScripts: () -> Unit,
    onShowAbout: () -> Unit,
    onNavigateToRecentItem: (RecentItem) -> Unit,
    recentViewModel: RecentViewModel,
    commandDeckViewModel: CommandDeckViewModel,
    dayManagementViewModel: DayManagementViewModel,
    dayManagementRuntimeViewModel: DayManagementRuntimeViewModel,
    dayManagementRuntimeUiState: DayManagementRuntimeUiState,
    defaultTodayTab: DayManagementTab,
    dayPlanViewModel: DayPlanViewModel,
    activityTrackerViewModel: ActivityTrackerViewModel,
    dayFocusesViewModel: DayFocusesViewModel,
    tacticalMissionViewModel: TacticalMissionViewModel,
) {
    val contextUiState by contextHierarchyViewModel.uiState.collectAsStateWithLifecycle()
    val dayManagementUiState by dayManagementViewModel.uiState.collectAsStateWithLifecycle()
    val globalActions =
        BottomPanelGlobalActions(
            onNavigateToProjectHierarchy = onNavigateToProjectHierarchy,
            onShowContextMarkersSheet = onShowContextMarkersSheet,
            onNavigateToPresets = onNavigateToPresets,
            onNavigateToGlobalSearch = onNavigateToGlobalSearch,
            onNavigateToQuickCatch = onNavigateToQuickCatch,
            onShowCreateActions = onShowCreateActions,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToInbox = onNavigateToInbox,
            onNavigateToTracker = onNavigateToTracker,
            onNavigateToReminders = onNavigateToReminders,
            onNavigateToAiChat = onNavigateToAiChat,
            onNavigateToAiInsights = onNavigateToAiInsights,
            onNavigateToAiLifeManagement = onNavigateToAiLifeManagement,
            onExportToFile = onExportToFile,
            onImportFromFileRequest = onImportFromFileRequest,
            onSelectiveImportFromFileRequest = onSelectiveImportFromFileRequest,
            onExportAttachments = onExportAttachments,
            onImportAttachmentsFromFileRequest = onImportAttachmentsFromFileRequest,
            onWifiPush = onWifiPush,
            onShowWifiServer = onShowWifiServer,
            onShowWifiImport = onShowWifiImport,
            onNavigateToAttachments = onNavigateToAttachments,
            onNavigateToScripts = onNavigateToScripts,
            onShowAbout = onShowAbout,
            featureToggles = contextUiState.featureToggles,
            onNavigateToRecentItem = onNavigateToRecentItem,
        )

    when (currentRoute) {
        MAIN_SCREEN_DASHBOARD_ROUTE ->
            DashboardBottomPanel(
                globalActions = globalActions,
                recentViewModel = recentViewModel,
            )
        MAIN_SCREEN_TODAY_ROUTE ->
            TodayBottomPanel(
                globalActions = globalActions,
                currentTab =
                    dayManagementUiState.selectedTab
                        .takeIf { it in DayManagementTab.todaySubTabs() }
                        ?: defaultTodayTab,
                onSelectTodayTab = { tab ->
                    dayManagementViewModel.selectTab(tab)
                    commandDeckViewModel.setSelectedTodayTab(tab)
                },
                runtimeUiState = dayManagementRuntimeUiState,
                onWakeUp = dayManagementRuntimeViewModel::wakeUp,
                onFinalizeFocus = dayManagementRuntimeViewModel::finalizeFocus,
                onFinalizePlan = dayManagementRuntimeViewModel::finalizePlan,
                onStartImplementation = dayManagementRuntimeViewModel::startImplementation,
                onStartFinalization = dayManagementRuntimeViewModel::startFinalization,
                onSleep = dayManagementRuntimeViewModel::sleep,
                dayPlanViewModel = dayPlanViewModel,
                activityTrackerViewModel = activityTrackerViewModel,
                dayFocusesViewModel = dayFocusesViewModel,
            )
        MAIN_SCREEN_TACTICS_ROUTE ->
            TacticsBottomPanel(
                globalActions = globalActions,
                viewModel = tacticalMissionViewModel,
            )
        MAIN_SCREEN_STRATEGIC_ARC_ROUTE ->
            StrategicArcBottomPanel(
                globalActions = globalActions,
            )
        STRATEGIC_MANAGEMENT_ROUTE ->
            StrategyBottomPanel(
                globalActions = globalActions,
                recentViewModel = recentViewModel,
            )
        MAIN_SCREEN_CORE_ROUTE ->
            CoreBottomPanel(
                globalActions = globalActions,
                recentViewModel = recentViewModel,
            )
        else ->
            DashboardBottomPanel(
                globalActions = globalActions,
                recentViewModel = recentViewModel,
            )
    }
}

@Composable
private fun MainScreenPagerContent(
    tabs: List<CommandDeckTab>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    navController: NavController,
    navigationManager: EnhancedNavigationManager?,
    context: android.content.Context,
    aiInsightsViewModel: AiInsightsViewModel,
    focusContextsViewModel: FocusContextsViewModel,
    tacticalMissionViewModel: TacticalMissionViewModel,
    tacticalObsidianVaultName: String,
    dayManagementViewModel: DayManagementViewModel,
    currentDayManagementTab: DayManagementTab,
    defaultTodayTab: DayManagementTab,
    dayManagementRuntimeViewModel: DayManagementRuntimeViewModel,
    dayPlanViewModel: DayPlanViewModel,
    activityTrackerViewModel: ActivityTrackerViewModel,
    dayFocusesViewModel: DayFocusesViewModel,
    sessionModeState: SessionModeState,
    latestSessionReason: String?,
    onSessionModeSelected: (com.romankozak.forwardappmobile.features.mainscreen.session.SessionMode) -> Unit,
    isSessionModeCardExpanded: Boolean,
    onSessionModeCardExpandedChange: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()

    HorizontalPager(
        state = pagerState,
        modifier =
            Modifier
                .fillMaxSize()
                .pointerInput(tabs.size, pagerState.currentPage) {
                    var horizontalDragAccum = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { horizontalDragAccum = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            horizontalDragAccum += dragAmount
                        },
                        onDragCancel = { horizontalDragAccum = 0f },
                        onDragEnd = {
                            val dragDelta = horizontalDragAccum
                            horizontalDragAccum = 0f
                            if (abs(dragDelta) < TAB_SWIPE_THRESHOLD_PX) return@detectHorizontalDragGestures

                            val targetPage =
                                when {
                                    dragDelta < 0f ->
                                        if (pagerState.currentPage < tabs.lastIndex) {
                                            pagerState.currentPage + 1
                                        } else {
                                            0
                                        }
                                    dragDelta > 0f ->
                                        if (pagerState.currentPage > 0) {
                                            pagerState.currentPage - 1
                                        } else {
                                            tabs.lastIndex
                                        }
                                    else -> null
                                }

                            targetPage?.let { page ->
                                scope.launch {
                                    pagerState.scrollToPage(page)
                                }
                            }
                        },
                    )
                },
        beyondViewportPageCount = 1,
        userScrollEnabled = false,
    ) { page ->
        when (tabs[page]) {
            CommandDeckTab.Dashboard ->
                DashboardContent(
                    modifier = Modifier.fillMaxSize(),
                    onOpenFocusedContext = { contextId ->
                        navigationManager.navigateOrFallback(
                            navController = navController,
                            target = NavTarget.ContextDetail(contextId = contextId),
                            recordInHistory = true,
                        )
                    },
                    sessionModeState = sessionModeState,
                    latestSessionReason = latestSessionReason,
                    onSessionModeSelected = onSessionModeSelected,
                    isSessionModeCardExpanded = isSessionModeCardExpanded,
                    onSessionModeCardExpandedChange = onSessionModeCardExpandedChange,
                    onOpenSessionContext = { contextId ->
                        navigationManager.navigateOrFallback(
                            navController = navController,
                            target = NavTarget.ContextDetail(contextId = contextId),
                            recordInHistory = true,
                        )
                    },
                    aiInsightsViewModel = aiInsightsViewModel,
                    focusContextsViewModel = focusContextsViewModel,
                )
            CommandDeckTab.Strategy ->
                StrategicManagementScreen(
                    navController = navController,
                    navigationManager = navigationManager,
                )
            CommandDeckTab.Core ->
                CoreLevelScreen(
                    navController = navController,
                    navigationManager = navigationManager,
                )
            CommandDeckTab.StrategicArc ->
                StrategicArcScreen(
                    navController = navController,
                    navigationManager = navigationManager,
                )
            CommandDeckTab.Tactics ->
                TacticalManagementScreen(
                    showFabMenu = false,
                    viewModel = tacticalMissionViewModel,
                    onLinkedProjectClick = { projectId ->
                        navigationManager.navigateOrFallback(
                            navController = navController,
                            target = NavTarget.ContextDetail(contextId = projectId),
                        ) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onLinkedAttachmentClick = { attachment ->
                        when {
                            attachment.attachmentType == "NOTE_DOCUMENT" &&
                                !attachment.entityId.isNullOrBlank() ->
                                navigationManager.navigateOrFallback(
                                    navController = navController,
                                    target = NavTarget.NoteDocument(id = attachment.entityId),
                                )
                            attachment.attachmentType == "JOURNAL_DOCUMENT" &&
                                !attachment.entityId.isNullOrBlank() ->
                                navigationManager.navigateOrFallback(
                                    navController = navController,
                                    target = NavTarget.JournalDocument(id = attachment.entityId),
                                )
                            attachment.attachmentType == "MUSIC_NOTE" &&
                                !attachment.entityId.isNullOrBlank() ->
                                navigationManager.navigateOrFallback(
                                    navController = navController,
                                    target = NavTarget.MusicNote(id = attachment.entityId),
                                )
                            attachment.attachmentType == "CHECKLIST" &&
                                !attachment.entityId.isNullOrBlank() ->
                                navigationManager.navigateOrFallback(
                                    navController = navController,
                                    target = NavTarget.Checklist(id = attachment.entityId),
                                )
                            attachment.linkType == LinkType.CONTEXT &&
                                !attachment.target.isNullOrBlank() ->
                                navigationManager.navigateOrFallback(
                                    navController = navController,
                                    target = NavTarget.ContextDetail(contextId = attachment.target),
                                    recordInHistory = true,
                                )
                            (
                                attachment.linkType == LinkType.URL ||
                                    attachment.linkType == LinkType.OBSIDIAN
                            ) && !attachment.target.isNullOrBlank() -> {
                                val resolvedTarget =
                                    buildExternalTarget(
                                        attachment.linkType,
                                        attachment.target,
                                        attachment.vault,
                                        tacticalObsidianVaultName,
                                    )
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(resolvedTarget)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        },
                                    )
                                }.onFailure {
                                    navigationManager.navigateOrFallback(
                                        navController = navController,
                                        target = NavTarget.AttachmentsLibrary,
                                    ) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    runCatching {
                                        navController.getBackStackEntry("attachments_library_screen")
                                            .savedStateHandle["attachment_library_query"] = attachment.id
                                    }
                                }
                            }
                            else -> {
                                navigationManager.navigateOrFallback(
                                    navController = navController,
                                    target = NavTarget.AttachmentsLibrary,
                                ) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                runCatching {
                                    navController.getBackStackEntry("attachments_library_screen")
                                        .savedStateHandle["attachment_library_query"] = attachment.id
                                }
                            }
                        }
                    },
                )
            CommandDeckTab.Today ->
                DayManagementScreen(
                    mainNavController = navController,
                    navigationManager = navigationManager,
                    viewModel = dayManagementViewModel,
                    runtimeViewModel = dayManagementRuntimeViewModel,
                    dayPlanViewModel = dayPlanViewModel,
                    activityTrackerViewModel = activityTrackerViewModel,
                    dayFocusesViewModel = dayFocusesViewModel,
                    currentDayManagementTab =
                        currentDayManagementTab
                            .takeIf { it in DayManagementTab.todaySubTabs() }
                            ?: defaultTodayTab,
                    showFabMenu = false,
                )
        }
    }
}

@Composable
private fun MainScreenCommandDeckTransientUi(commandDeckViewModel: CommandDeckViewModel) {
    val isContextInputVisible by commandDeckViewModel.isContextInputVisible.collectAsStateWithLifecycle()
    val contextInputText by commandDeckViewModel.contextInputText.collectAsStateWithLifecycle()

    ContextInputOverlay(
        visible = isContextInputVisible,
        text = contextInputText,
        onTextChange = commandDeckViewModel::onContextInputChange,
        onSend = commandDeckViewModel::submitContextInput,
        onTrack = commandDeckViewModel::startContextTracking,
        onClear = commandDeckViewModel::clearContextInput,
        onDismiss = commandDeckViewModel::closeContextInput,
    )
}

@Composable
private fun MainScreenCommandDeckDialogs(commandDeckViewModel: CommandDeckViewModel) {
    val importChoiceUri by commandDeckViewModel.importChoiceUri.collectAsStateWithLifecycle()
    val exportChoiceVisible by commandDeckViewModel.exportChoiceVisible.collectAsStateWithLifecycle()
    val syncUiState by commandDeckViewModel.syncUiState.collectAsStateWithLifecycle()
    val showWifiImportDialog by commandDeckViewModel.showWifiImportDialog.collectAsStateWithLifecycle()

    if (importChoiceUri != null) {
        AlertDialog(
            onDismissRequest = commandDeckViewModel::onImportChoiceDismiss,
            title = { Text("Choose Import Version") },
            text = { Text("Would you like to import a V1 (legacy) or V2 (snapshot) backup file?") },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = { commandDeckViewModel.confirmImportV1(importChoiceUri!!) }) {
                        Text("Import V1")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { commandDeckViewModel.confirmImportV2(importChoiceUri!!) }) {
                        Text("Import V2")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = commandDeckViewModel::onImportChoiceDismiss) {
                    Text("Cancel")
                }
            },
        )
    }

    if (exportChoiceVisible) {
        AlertDialog(
            onDismissRequest = commandDeckViewModel::onExportChoiceDismiss,
            title = { Text("Choose Export Version") },
            text = { Text("Would you like to export a V1 (legacy) or V2 (snapshot) backup file?") },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = commandDeckViewModel::confirmExportV1) {
                        Text("Export V1")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = commandDeckViewModel::confirmExportV2) {
                        Text("Export V2")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = commandDeckViewModel::onExportChoiceDismiss) {
                    Text("Cancel")
                }
            },
        )
    }

    if (syncUiState.showWifiServerDialog) {
        WifiServerDialog(
            address = syncUiState.wifiServerAddress,
            onDismiss = commandDeckViewModel::onDismissWifiServerDialog,
        )
    }

    if (showWifiImportDialog) {
        WifiImportDialog(
            desktopAddress = syncUiState.desktopAddress,
            onAddressChange = commandDeckViewModel::onWifiImportAddressChange,
            onDismiss = commandDeckViewModel::onDismissWifiImportDialog,
            onConfirm = commandDeckViewModel::onWifiImportConfirm,
        )
    }
}

@Composable
@Suppress("UnusedParameter")
private fun MainScreenContextMarkersSheet(
    showSheet: Boolean,
    onDismiss: () -> Unit,
    contextHierarchyViewModel: ContextHierarchyScreenViewModel,
    onManageContextMarkers: () -> Unit,
    navigationManager: EnhancedNavigationManager?,
    navController: NavController,
    onContextSelected: (String, String?) -> Unit,
) {
    val contextUiState by contextHierarchyViewModel.uiState.collectAsStateWithLifecycle()

    ContextMarkersSheet(
        showSheet = showSheet,
        onDismiss = onDismiss,
        contextMarkers = contextUiState.allContextMarkers,
        contextMarkerToEmojiMap = contextUiState.contextMarkerToEmojiMap,
        onManageContextMarkers = onManageContextMarkers,
        onContextSelected = onContextSelected,
    )
}

@Composable
private fun MainScreenAboutDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    contextHierarchyViewModel: ContextHierarchyScreenViewModel,
) {
    if (!showDialog) {
        return
    }

    val contextUiState by contextHierarchyViewModel.uiState.collectAsStateWithLifecycle()
    com.romankozak.forwardappmobile.ui.dialogs.AboutAppDialog(
        stats = contextUiState.appStatistics,
        onDismiss = onDismiss,
    )
}
