package com.romankozak.forwardappmobile.features.mainscreen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.navigation.routes.GOAL_LISTS_ROUTE
import com.romankozak.forwardappmobile.core.navigation.routes.STRATEGIC_MANAGEMENT_ROUTE
import com.romankozak.forwardappmobile.features.activitytracker.ActivityTrackerViewModel
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementScreen
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayPlanViewModel
import com.romankozak.forwardappmobile.features.missions.presentation.TacticalManagementScreen
import com.romankozak.forwardappmobile.features.recent.RecentViewModel
import com.romankozak.forwardappmobile.features.strategicmanagement.StrategicManagementScreen
import com.romankozak.forwardappmobile.ui.components.header.CommandDeckBackgroundModifier
import com.romankozak.forwardappmobile.ui.components.header.CommandDeckHeaderPreset
import com.romankozak.forwardappmobile.ui.components.header.CoreHeader
import com.romankozak.forwardappmobile.ui.components.header.FAHeader
import com.romankozak.forwardappmobile.ui.components.header.FAHeaderBackground
import com.romankozak.forwardappmobile.ui.components.header.StrategicArcHeader
import com.romankozak.forwardappmobile.ui.components.header.StrategyHeader
import com.romankozak.forwardappmobile.ui.components.header.TacticsHeader
import com.romankozak.forwardappmobile.ui.components.header.TodayHeader
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
                when (currentRoute) {
                    MAIN_SCREEN_DASHBOARD_ROUTE -> {
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
                    }

                    MAIN_SCREEN_CORE_ROUTE ->
                        FAHeader(
                            layout = CoreHeader(),
                            backgroundStyle = FAHeaderBackground.CommandDeck,
                            modifier = headerModifier,
                        )

                    MAIN_SCREEN_TODAY_ROUTE -> {
                        val dayPlanViewModel: DayPlanViewModel = hiltViewModel()
                        val dayPlanUiState by dayPlanViewModel.uiState.collectAsState()
                        val activityTrackerViewModel: ActivityTrackerViewModel = hiltViewModel()
                        val activityLog by activityTrackerViewModel.activityLog.collectAsStateWithLifecycle()

                        val (xpToday, antyXpToday) =
                            remember(activityLog, dayPlanUiState.dayPlan?.date) {
                                val targetDate = dayPlanUiState.dayPlan?.date ?: System.currentTimeMillis()
                                val recordsForDay =
                                    activityLog.filter { record ->
                                        isSameDay(record.createdAt, targetDate)
                                    }
                                val xp = recordsForDay.sumOf { it.xpGained ?: 0 }
                                val antyXp = recordsForDay.sumOf { it.antyXp ?: 0 }
                                xp to antyXp
                            }

                        FAHeader(
                            layout =
                                TodayHeader(
                                    onNavigateToPreviousDay = {
                                        Log.d("TodayTab", "onNavigateToPreviousDay callback invoked.")
                                        dayPlanViewModel.navigateToPreviousDay()
                                    },
                                    onNavigateToNextDay = {
                                        Log.d(
                                            "TodayTab",
                                            "onNavigateToNextDay callback invoked. Enabled: ${!dayPlanUiState.isToday}",
                                        )
                                        dayPlanViewModel.navigateToNextDay()
                                    },
                                    isNextDayNavigationEnabled = !dayPlanUiState.isToday,
                                    date = dayPlanUiState.dayPlan?.date,
                                ),
                            backgroundStyle = FAHeaderBackground.CommandDeck,
                            modifier = headerModifier,
                        )
                    }

                    STRATEGIC_MANAGEMENT_ROUTE ->
                        FAHeader(
                            layout = StrategyHeader(onModeClick = {}),
                            backgroundStyle = FAHeaderBackground.CommandDeck,
                            modifier = headerModifier,
                        )

                    MAIN_SCREEN_STRATEGIC_ARC_ROUTE ->
                        FAHeader(
                            layout = StrategicArcHeader(onModeClick = {}),
                            backgroundStyle = FAHeaderBackground.CommandDeck,
                            modifier = headerModifier,
                        )

                    MAIN_SCREEN_TACTICS_ROUTE ->
                        FAHeader(
                            layout = TacticsHeader(),
                            backgroundStyle = FAHeaderBackground.CommandDeck,
                            modifier = headerModifier,
                        )

                    else ->
                        FAHeader(
                            layout = CommandDeckHeaderPreset(onClick = {}),
                            backgroundStyle = FAHeaderBackground.CommandDeck,
                            modifier = headerModifier,
                        )
                }
            },
            bottomBar = {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .then(CommandDeckBackgroundModifier())
                            .padding(horizontal = 22.dp, vertical = 12.dp),
                ) {
                    when (currentRoute) {
                        MAIN_SCREEN_DASHBOARD_ROUTE ->
                            DashboardBottomBar(
                                onNavigateToProjectHierarchy = onNavigateToProjectHierarchy,
                                onNavigateToProjectSearch = {
                                    navController.navigate(GOAL_LISTS_ROUTE) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    runCatching {
                                        navController.getBackStackEntry(GOAL_LISTS_ROUTE)
                                            .savedStateHandle["open_search_dialog"] = true
                                    }
                                },
                                onNavigateToTracker = onNavigateToTracker,
                                onNavigateToInbox = onNavigateToInbox,
                                onNavigateToReminders = onNavigateToReminders,
                                onNavigateToPresets = onNavigateToPresets,
                                onNavigateToAiInsights = onNavigateToAiInsights,
                                onNavigateToSettings = onNavigateToSettings,
                                onNavigateToRecentItem = onNavigateToRecentItem,
                                onExportToFile = onExportToFile,
                                onImportFromFileRequest = onImportFromFileRequest,
                                onSelectiveImportFromFileRequest = onSelectiveImportFromFileRequest,
                                onExportAttachments = onExportAttachments,
                                onImportAttachmentsFromFileRequest = onImportAttachmentsFromFileRequest,
                                onWifiPush = onWifiPush,
                                recentViewModel = recentViewModel,
                            )
                        MAIN_SCREEN_STRATEGIC_ARC_ROUTE -> {
                            val viewModel: StrategicArcViewModel = hiltViewModel()
                            StrategicArcBottomBar(viewModel)
                        }
                        MAIN_SCREEN_TODAY_ROUTE -> {
                            val viewModel: DayPlanViewModel = hiltViewModel()
                            TodayBottomBar(viewModel, onNavigateToSettings)
                        }
                        MAIN_SCREEN_CORE_ROUTE -> CoreBottomBar()
                        MAIN_SCREEN_TACTICS_ROUTE -> TacticsBottomBar()
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
