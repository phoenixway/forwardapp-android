package com.romankozak.forwardappmobile.features.mainscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.navigation.routes.STRATEGIC_MANAGEMENT_ROUTE
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
import com.romankozak.forwardappmobile.ui.components.CommonBottomPanelLayout
import com.romankozak.forwardappmobile.ui.components.header.CommandDeckHeaderPreset
import com.romankozak.forwardappmobile.ui.components.header.FAHeader
import com.romankozak.forwardappmobile.ui.components.header.FAHeaderBackground
import kotlinx.coroutines.launch
import java.util.Calendar

const val MAIN_SCREEN_DASHBOARD_ROUTE = "command_deck_dashboard"
const val MAIN_SCREEN_CORE_ROUTE = "command_deck_core"
const val MAIN_SCREEN_STRATEGIC_ARC_ROUTE = "command_deck_strategic_arc"
const val MAIN_SCREEN_TACTICS_ROUTE = "command_deck_tactics"
const val MAIN_SCREEN_TODAY_ROUTE = "command_deck_today"

@Composable
fun MainScreenLayout(
    navController: NavController,
    onNavigateToProjectHierarchy: () -> Unit,
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
    onImportFromFileRequest: () -> Unit,
    onSelectiveImportFromFileRequest: () -> Unit,
    onExportAttachments: () -> Unit,
    onImportAttachmentsFromFileRequest: () -> Unit,
    onWifiPush: (String) -> Unit,
    onNavigateToAttachments: () -> Unit,
    onNavigateToScripts: () -> Unit,
    onNavigateToRecentItem: (RecentItem) -> Unit,
    recentViewModel: RecentViewModel = hiltViewModel(),
    commandDeckViewModel: CommandDeckViewModel = hiltViewModel(),
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
    val pagerState = rememberPagerState(initialPage = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2 % tabs.size)) { Int.MAX_VALUE }
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

    val headerModifier =
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) { commandDeckViewModel.openContextInput() }

    val selectedTabIndex = (pagerState.currentPage % tabs.size + tabs.size) % tabs.size

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                val showBadge = com.romankozak.forwardappmobile.BuildConfig.DEBUG || com.romankozak.forwardappmobile.BuildConfig.IS_EXPERIMENTAL_BUILD
                FAHeader(
                    layout =
                        CommandDeckHeaderPreset(
                            onClick = {},
                            onRightClick = { onNavigateToCharacter() },
                            rightContent = {
                                if (showBadge) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    ) {
                                        Text(
                                            text = if (com.romankozak.forwardappmobile.BuildConfig.DEBUG) "Debug" else "Experimental",
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            },
                        ),
                    backgroundStyle = FAHeaderBackground.CommandDeck,
                    modifier = headerModifier,
                )
            },
            bottomBar = {
                CommonBottomPanelLayout {
                    when (currentRoute) {
                        MAIN_SCREEN_DASHBOARD_ROUTE ->
                            DashboardBottomPanel(
                                navController = navController,
                                onNavigateToProjectHierarchy = onNavigateToProjectHierarchy,
                                onNavigateToPresets = onNavigateToPresets,
                                onNavigateToCharacter = onNavigateToCharacter,
                                onNavigateToGlobalSearch = onNavigateToGlobalSearch,
                                onNavigateToSettings = onNavigateToSettings,
                                onNavigateToInbox = onNavigateToInbox,
                                onNavigateToTracker = onNavigateToTracker,
                                onNavigateToReminders = onNavigateToReminders,
                                onNavigateToAiInsights = onNavigateToAiInsights,
                                onExportToFile = onExportToFile,
                                onImportFromFileRequest = onImportFromFileRequest,
                                onSelectiveImportFromFileRequest = onSelectiveImportFromFileRequest,
                                onExportAttachments = onExportAttachments,
                                onImportAttachmentsFromFileRequest = onImportAttachmentsFromFileRequest,
                                onWifiPush = onWifiPush,
                                onNavigateToRecentItem = onNavigateToRecentItem,
                                recentViewModel = recentViewModel,
                            )
                        MAIN_SCREEN_TODAY_ROUTE ->
                            TodayBottomPanel(
                                navController = navController,
                                onNavigateToProjectHierarchy = onNavigateToProjectHierarchy,
                                onNavigateToPresets = onNavigateToPresets,
                                onNavigateToCharacter = onNavigateToCharacter,
                                onNavigateToGlobalSearch = onNavigateToGlobalSearch,
                                onNavigateToSettings = onNavigateToSettings,
                                onNavigateToInbox = onNavigateToInbox,
                                onNavigateToTracker = onNavigateToTracker,
                                onNavigateToReminders = onNavigateToReminders,
                                onNavigateToAiInsights = onNavigateToAiInsights,
                                onExportToFile = onExportToFile,
                                onImportFromFileRequest = onImportFromFileRequest,
                                onSelectiveImportFromFileRequest = onSelectiveImportFromFileRequest,
                                onExportAttachments = onExportAttachments,
                                onImportAttachmentsFromFileRequest = onImportAttachmentsFromFileRequest,
                                onWifiPush = onWifiPush,
                                onNavigateToRecentItem = onNavigateToRecentItem,
                                recentViewModel = recentViewModel,
                            )
                        MAIN_SCREEN_TACTICS_ROUTE ->
                            TacticsBottomPanel(
                                navController = navController,
                                onNavigateToProjectHierarchy = onNavigateToProjectHierarchy,
                                onNavigateToPresets = onNavigateToPresets,
                                onNavigateToCharacter = onNavigateToCharacter,
                                onNavigateToGlobalSearch = onNavigateToGlobalSearch,
                                onNavigateToSettings = onNavigateToSettings,
                                onNavigateToInbox = onNavigateToInbox,
                                onNavigateToTracker = onNavigateToTracker,
                                onNavigateToReminders = onNavigateToReminders,
                                onNavigateToAiInsights = onNavigateToAiInsights,
                                onExportToFile = onExportToFile,
                                onImportFromFileRequest = onImportFromFileRequest,
                                onSelectiveImportFromFileRequest = onSelectiveImportFromFileRequest,
                                onExportAttachments = onExportAttachments,
                                onImportAttachmentsFromFileRequest = onImportAttachmentsFromFileRequest,
                                onWifiPush = onWifiPush,
                                onNavigateToRecentItem = onNavigateToRecentItem,
                                recentViewModel = recentViewModel,
                            )
                        MAIN_SCREEN_STRATEGIC_ARC_ROUTE ->
                            StrategicArcBottomPanel(
                                navController = navController,
                                onNavigateToProjectHierarchy = onNavigateToProjectHierarchy,
                                onNavigateToPresets = onNavigateToPresets,
                                onNavigateToCharacter = onNavigateToCharacter,
                                onNavigateToGlobalSearch = onNavigateToGlobalSearch,
                                onNavigateToSettings = onNavigateToSettings,
                                onNavigateToInbox = onNavigateToInbox,
                                onNavigateToTracker = onNavigateToTracker,
                                onNavigateToReminders = onNavigateToReminders,
                                onNavigateToAiInsights = onNavigateToAiInsights,
                                onExportToFile = onExportToFile,
                                onImportFromFileRequest = onImportFromFileRequest,
                                onSelectiveImportFromFileRequest = onSelectiveImportFromFileRequest,
                                onExportAttachments = onExportAttachments,
                                onImportAttachmentsFromFileRequest = onImportAttachmentsFromFileRequest,
                                onWifiPush = onWifiPush,
                                onNavigateToRecentItem = onNavigateToRecentItem,
                                recentViewModel = recentViewModel,
                            )
                        STRATEGIC_MANAGEMENT_ROUTE -> // Strategy
                            StrategyBottomPanel(
                                navController = navController,
                            )
                        MAIN_SCREEN_CORE_ROUTE ->
                            CoreBottomPanel(
                                navController = navController,
                            )
                        else ->
                            DashboardBottomPanel( // Fallback to Dashboard for unknown routes
                                navController = navController,
                                onNavigateToProjectHierarchy = onNavigateToProjectHierarchy,
                                onNavigateToPresets = onNavigateToPresets,
                                onNavigateToCharacter = onNavigateToCharacter,
                                onNavigateToGlobalSearch = onNavigateToGlobalSearch,
                                onNavigateToSettings = onNavigateToSettings,
                                onNavigateToInbox = onNavigateToInbox,
                                onNavigateToTracker = onNavigateToTracker,
                                onNavigateToReminders = onNavigateToReminders,
                                onNavigateToAiInsights = onNavigateToAiInsights,
                                onExportToFile = onExportToFile,
                                onImportFromFileRequest = onImportFromFileRequest,
                                onSelectiveImportFromFileRequest = onSelectiveImportFromFileRequest,
                                onExportAttachments = onExportAttachments,
                                onImportAttachmentsFromFileRequest = onImportAttachmentsFromFileRequest,
                                onWifiPush = onWifiPush,
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

                Spacer(Modifier.height(16.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = true, // Enable swiping
                ) { page ->
                    val actualTabIndex = (page % tabs.size + tabs.size) % tabs.size
                    when (tabs[actualTabIndex]) {
                        CommandDeckTab.Dashboard -> {
                            AnimatedCommandDeck(
                                onNavigateToProjectHierarchy = onNavigateToProjectHierarchy,
                                onNavigateToGlobalSearch = onNavigateToGlobalSearch,
                                onNavigateToSettings = onNavigateToSettings,
                                onNavigateToInbox = onNavigateToInbox,
                                onNavigateToTracker = onNavigateToTracker,
                                onNavigateToReminders = onNavigateToReminders,
                                onNavigateToAiChat = onNavigateToAiChat,
                                onNavigateToAiInsights = onNavigateToAiInsights,
                                onNavigateToAiLifeManagement = onNavigateToAiLifeManagement,
                                onNavigateToImportExport = { /*TODO: replace with real code*/ },
                                onNavigateToAttachments = onNavigateToAttachments,
                                onNavigateToScripts = onNavigateToScripts,
                            )
                        }
                        CommandDeckTab.Strategy -> {
                            StrategicManagementScreen(navController = navController)
                        }
                        CommandDeckTab.Core -> {
                            CoreLevelScreen(navController = navController)
                        }
                        CommandDeckTab.StrategicArc -> {
                            StrategicArcScreen(navController = navController)
                        }
                        CommandDeckTab.Tactics -> {
                            TacticalManagementScreen()
                        }
                        CommandDeckTab.Today -> {
                            DayManagementScreen(mainNavController = navController, startTab = "PLAN")
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
    }
}

private fun isSameDay(
    timestamp: Long,
    other: Long,
): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp }
    val cal2 = Calendar.getInstance().apply { timeInMillis = other }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
