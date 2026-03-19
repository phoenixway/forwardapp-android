package com.romankozak.forwardappmobile.features.mainscreen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.navigateOrFallback
import com.romankozak.forwardappmobile.core.navigation.routes.GOAL_LISTS_ROUTE
import com.romankozak.forwardappmobile.core.navigation.routes.STRATEGIC_MANAGEMENT_ROUTE
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.ContextHierarchyScreenViewModel
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components.ContextMarkersSheet
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementScreen
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.CoreBottomPanel
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.DashboardBottomPanel
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.StrategicArcBottomPanel
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.StrategyBottomPanel
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.TacticsBottomPanel
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.TodayBottomPanel
import com.romankozak.forwardappmobile.features.missions.presentation.TacticalManagementScreen
import com.romankozak.forwardappmobile.features.recent.RecentViewModel
import com.romankozak.forwardappmobile.features.strategicmanagement.StrategicManagementScreen
import com.romankozak.forwardappmobile.features.userawareness.UserAwarenessHeaderBadge
import com.romankozak.forwardappmobile.features.userawareness.UserAwarenessQuickSwitchDialog
import com.romankozak.forwardappmobile.features.userawareness.UserAwarenessViewModel
import com.romankozak.forwardappmobile.ui.components.CommonBottomPanelLayout
import com.romankozak.forwardappmobile.ui.components.header.CommandDeckHeaderPreset
import com.romankozak.forwardappmobile.ui.components.header.CommandDeckBackgroundModifier
import com.romankozak.forwardappmobile.ui.components.header.FAHeader
import com.romankozak.forwardappmobile.ui.components.header.FAHeaderBackground
import com.romankozak.forwardappmobile.ui.dialogs.WifiImportDialog
import com.romankozak.forwardappmobile.ui.dialogs.WifiServerDialog
import kotlinx.coroutines.launch
import java.net.URLEncoder

const val MAIN_SCREEN_DASHBOARD_ROUTE = "command_deck_dashboard"
const val MAIN_SCREEN_CORE_ROUTE = "command_deck_core"
const val MAIN_SCREEN_STRATEGIC_ARC_ROUTE = "command_deck_strategic_arc"
const val MAIN_SCREEN_TACTICS_ROUTE = "command_deck_tactics"
const val MAIN_SCREEN_TODAY_ROUTE = "command_deck_today"

@Composable
fun MainScreenLayout(
    navController: NavController,
    navigationManager: EnhancedNavigationManager? = null,
    onNavigateToProjectHierarchy: () -> Unit,
    onNavigateToManageContextMarkers: () -> Unit = {},
    onNavigateToPresets: () -> Unit,
    onNavigateToCharacter: () -> Unit,
    onNavigateToGlobalSearch: () -> Unit,
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
    val pagerState =
        rememberPagerState(
            initialPage = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2 % tabs.size),
        ) {
            Int.MAX_VALUE
        }
    val scope = rememberCoroutineScope()

    val currentRoute =
        when (tabs[(pagerState.currentPage % tabs.size + tabs.size) % tabs.size]) {
            CommandDeckTab.Dashboard -> MAIN_SCREEN_DASHBOARD_ROUTE
            CommandDeckTab.Core -> MAIN_SCREEN_CORE_ROUTE
            CommandDeckTab.Strategy -> STRATEGIC_MANAGEMENT_ROUTE
            CommandDeckTab.StrategicArc -> MAIN_SCREEN_STRATEGIC_ARC_ROUTE
            CommandDeckTab.Tactics -> MAIN_SCREEN_TACTICS_ROUTE
            CommandDeckTab.Today -> MAIN_SCREEN_TODAY_ROUTE
        }

    val isContextInputVisible by commandDeckViewModel.isContextInputVisible.collectAsStateWithLifecycle()
    val contextInputText by commandDeckViewModel.contextInputText.collectAsStateWithLifecycle()
    val importChoiceUri by commandDeckViewModel.importChoiceUri.collectAsStateWithLifecycle()
    val exportChoiceVisible by commandDeckViewModel.exportChoiceVisible.collectAsStateWithLifecycle()
    val syncUiState by commandDeckViewModel.syncUiState.collectAsStateWithLifecycle()
    val showWifiImportDialog by commandDeckViewModel.showWifiImportDialog.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val contextUiState by contextHierarchyViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val userAwarenessViewModel: UserAwarenessViewModel = hiltViewModel()
    val activeUserAwarenessState by userAwarenessViewModel.activeState.collectAsStateWithLifecycle()
    var showAboutDialog by remember { mutableStateOf(false) }
    var showStateSwitchDialog by remember { mutableStateOf(false) }
    var showContextMarkersSheet by remember { mutableStateOf(false) }

    LaunchedEffect(commandDeckViewModel) {
        commandDeckViewModel.uiEvents.collect { message ->
            when (message) {
                is CommandDeckUiEvent.ShowMessage -> snackbarHostState.showSnackbar(message.message)
                is CommandDeckUiEvent.NavigateToSyncScreenWithData ->
                    onNavigateToSyncScreenWithData(message.json)
            }
        }
    }

    val headerModifier =
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) { commandDeckViewModel.openContextInput() }

    val selectedTabIndex = (pagerState.currentPage % tabs.size + tabs.size) % tabs.size
    val titleStateBadge: @Composable (() -> Unit) = {
        UserAwarenessHeaderBadge(
            activeState = activeUserAwarenessState,
            onOpenQuickSwitch = { showStateSwitchDialog = true },
        )
    }
    val dashboardHeaderContent: @Composable () -> Unit = {
        val showBadge =
            com.romankozak.forwardappmobile.BuildConfig.DEBUG ||
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
                                    if (com.romankozak.forwardappmobile.BuildConfig.DEBUG) {
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
            bottomBar = {
                CommonBottomPanelLayout {
                    when (currentRoute) {
                        MAIN_SCREEN_DASHBOARD_ROUTE ->
                            DashboardBottomPanel(
                                onNavigateToProjectHierarchy = onNavigateToProjectHierarchy,
                                onShowContextMarkersSheet = { showContextMarkersSheet = true },
                                onNavigateToPresets = onNavigateToPresets,
                                onNavigateToGlobalSearch = onNavigateToGlobalSearch,
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
                                featureToggles = contextUiState.featureToggles,
                                onNavigateToRecentItem = onNavigateToRecentItem,
                                recentViewModel = recentViewModel,
                            )
                        MAIN_SCREEN_TODAY_ROUTE ->
                            TodayBottomPanel(
                                onNavigateToProjectHierarchy = onNavigateToProjectHierarchy,
                                onShowContextMarkersSheet = { showContextMarkersSheet = true },
                                onNavigateToPresets = onNavigateToPresets,
                                onNavigateToGlobalSearch = onNavigateToGlobalSearch,
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
                                featureToggles = contextUiState.featureToggles,
                                onNavigateToRecentItem = onNavigateToRecentItem,
                                recentViewModel = recentViewModel,
                            )
                        MAIN_SCREEN_TACTICS_ROUTE ->
                            TacticsBottomPanel(
                                onNavigateToProjectHierarchy = onNavigateToProjectHierarchy,
                                onShowContextMarkersSheet = { showContextMarkersSheet = true },
                                onNavigateToPresets = onNavigateToPresets,
                                onNavigateToGlobalSearch = onNavigateToGlobalSearch,
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
                                featureToggles = contextUiState.featureToggles,
                                onNavigateToRecentItem = onNavigateToRecentItem,
                                recentViewModel = recentViewModel,
                            )
                        MAIN_SCREEN_STRATEGIC_ARC_ROUTE ->
                            StrategicArcBottomPanel(
                                onNavigateToProjectHierarchy = onNavigateToProjectHierarchy,
                                onShowContextMarkersSheet = { showContextMarkersSheet = true },
                                onNavigateToPresets = onNavigateToPresets,
                                onNavigateToGlobalSearch = onNavigateToGlobalSearch,
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
                                featureToggles = contextUiState.featureToggles,
                                onNavigateToRecentItem = onNavigateToRecentItem,
                                recentViewModel = recentViewModel,
                            )
                        STRATEGIC_MANAGEMENT_ROUTE -> // Strategy
                            StrategyBottomPanel(
                                onNavigateToProjectHierarchy = onNavigateToProjectHierarchy,
                                onShowContextMarkersSheet = { showContextMarkersSheet = true },
                                onNavigateToPresets = onNavigateToPresets,
                                onNavigateToGlobalSearch = onNavigateToGlobalSearch,
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
                                featureToggles = contextUiState.featureToggles,
                                onNavigateToRecentItem = onNavigateToRecentItem,
                                recentViewModel = recentViewModel,
                            )
                        MAIN_SCREEN_CORE_ROUTE ->
                            CoreBottomPanel(
                                onNavigateToProjectHierarchy = onNavigateToProjectHierarchy,
                                onShowContextMarkersSheet = { showContextMarkersSheet = true },
                                onNavigateToPresets = onNavigateToPresets,
                                onNavigateToGlobalSearch = onNavigateToGlobalSearch,
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
                                featureToggles = contextUiState.featureToggles,
                                onNavigateToRecentItem = onNavigateToRecentItem,
                                recentViewModel = recentViewModel,
                            )
                        else ->
                            DashboardBottomPanel( // Fallback to Dashboard for unknown routes
                                onNavigateToProjectHierarchy = onNavigateToProjectHierarchy,
                                onShowContextMarkersSheet = { showContextMarkersSheet = true },
                                onNavigateToPresets = onNavigateToPresets,
                                onNavigateToGlobalSearch = onNavigateToGlobalSearch,
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
                                featureToggles = contextUiState.featureToggles,
                                onNavigateToRecentItem = onNavigateToRecentItem,
                                recentViewModel = recentViewModel,
                            )
                    }
                }
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
                            val currentRealIndex = pagerState.currentPage % tabs.size
                            val diff = index - currentRealIndex
                            val targetInfinitePage = pagerState.currentPage + diff
                            pagerState.animateScrollToPage(targetInfinitePage)
                        }
                    },
                )

                if (currentRoute == MAIN_SCREEN_DASHBOARD_ROUTE) {
                    dashboardHeaderContent()
                    Spacer(Modifier.height(1.dp))
                } else {
                    Spacer(Modifier.height(16.dp))
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = true, // Enable swiping
                ) { page ->
                    val actualTabIndex = (page % tabs.size + tabs.size) % tabs.size
                    when (tabs[actualTabIndex]) {
                        CommandDeckTab.Dashboard -> {
                            DashboardContent(
                                modifier = Modifier.fillMaxSize(),
                                onOpenFocusedContext = { contextId ->
                                    navigationManager.navigateOrFallback(
                                        navController = navController,
                                        target = NavTarget.ContextDetail(contextId = contextId),
                                        recordInHistory = true,
                                    )
                                },
                            )
                        }
                        CommandDeckTab.Strategy -> {
                            StrategicManagementScreen(
                                navController = navController,
                                navigationManager = navigationManager,
                            )
                        }
                        CommandDeckTab.Core -> {
                            CoreLevelScreen(
                                navController = navController,
                                navigationManager = navigationManager,
                            )
                        }
                        CommandDeckTab.StrategicArc -> {
                            StrategicArcScreen(
                                navController = navController,
                                navigationManager = navigationManager,
                            )
                        }
                        CommandDeckTab.Tactics -> {
                            TacticalManagementScreen(
                                showFabMenu = false,
                                onLinkedProjectClick = { projectId ->
                                    navigationManager.navigateOrFallback(
                                        navController = navController,
                                        target = NavTarget.ContextHierarchy,
                                    ) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    runCatching {
                                        navController.getBackStackEntry(GOAL_LISTS_ROUTE)
                                            .savedStateHandle["projectIdToReveal"] = projectId
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
                        }
                        CommandDeckTab.Today -> {
                            DayManagementScreen(
                                mainNavController = navController,
                                navigationManager = navigationManager,
                                startTab = "PLAN",
                                showFabMenu = false,
                            )
                        }
                    }
                }
            }
        }

        ContextInputOverlay(
            visible = isContextInputVisible,
            text = contextInputText,
            onTextChange = commandDeckViewModel::onContextInputChange,
            onSend = commandDeckViewModel::submitContextInput,
            onTrack = commandDeckViewModel::startContextTracking,
            onClear = commandDeckViewModel::clearContextInput,
            onDismiss = commandDeckViewModel::closeContextInput,
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

        ContextMarkersSheet(
            showSheet = showContextMarkersSheet,
            onDismiss = { showContextMarkersSheet = false },
            contextMarkers = contextUiState.allContextMarkers,
            contextMarkerToEmojiMap = contextUiState.contextMarkerToEmojiMap,
            onManageContextMarkers = onNavigateToManageContextMarkers,
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
                            com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent.ContextSelected(
                                contextMarkerName,
                            ),
                        )
                    }
                }
            },
        )

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

        if (showAboutDialog) {
            com.romankozak.forwardappmobile.ui.dialogs.AboutAppDialog(
                stats = contextUiState.appStatistics,
                onDismiss = { showAboutDialog = false },
            )
        }
    }
}

private fun buildExternalTarget(
    linkType: LinkType?,
    target: String,
): String {
    val trimmed = target.trim()
    if (linkType == LinkType.OBSIDIAN && !trimmed.startsWith("obsidian://", ignoreCase = true)) {
        return "obsidian://open?file=${URLEncoder.encode(trimmed, "UTF-8")}"
    }
    return trimmed
}
