package com.romankozak.forwardappmobile.ui.screens.commanddeck

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.romankozak.forwardappmobile.features.missions.presentation.TacticalManagementScreen
import com.romankozak.forwardappmobile.routes.STRATEGIC_MANAGEMENT_ROUTE
import com.romankozak.forwardappmobile.ui.components.header.CoreHeader
import com.romankozak.forwardappmobile.ui.components.header.CommandDeckHeaderPreset
import com.romankozak.forwardappmobile.ui.components.header.FAHeader
import com.romankozak.forwardappmobile.ui.components.header.FAHeaderBackground
import com.romankozak.forwardappmobile.ui.components.header.StrategicArcHeader
import com.romankozak.forwardappmobile.ui.components.header.StrategyHeader
import com.romankozak.forwardappmobile.ui.components.header.TacticsHeader
import com.romankozak.forwardappmobile.ui.components.header.TodayHeader
import com.romankozak.forwardappmobile.routes.GOAL_LISTS_ROUTE
import com.romankozak.forwardappmobile.features.daymanagement.presentation.DayManagementScreen
import com.romankozak.forwardappmobile.features.daymanagement.presentation.dayplan.DayPlanViewModel
import com.romankozak.forwardappmobile.ui.screens.strategicmanagement.StrategicManagementScreen
import com.romankozak.forwardappmobile.ui.screens.commanddeck.components.DashboardBottomBar
import com.romankozak.forwardappmobile.ui.components.header.CommandDeckBackgroundModifier
import com.romankozak.forwardappmobile.data.database.models.RecentItem
import com.romankozak.forwardappmobile.ui.screens.activitytracker.ActivityTrackerViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Calendar
import com.romankozak.forwardappmobile.ui.recent.RecentViewModel

const val COMMAND_DECK_DASHBOARD_ROUTE = "command_deck_dashboard"
const val COMMAND_DECK_CORE_ROUTE = "command_deck_core"
const val COMMAND_DECK_STRATEGIC_ARC_ROUTE = "command_deck_strategic_arc"
const val COMMAND_DECK_TACTICS_ROUTE = "command_deck_tactics"
const val COMMAND_DECK_TODAY_ROUTE = "command_deck_today"




@Composable
fun SharedCommandDeckLayout(
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
    onNavigateToImportExport: () -> Unit,
    onNavigateToAttachments: () -> Unit,
    onNavigateToScripts: () -> Unit,
    onNavigateToRecentItem: (RecentItem) -> Unit,
    recentViewModel: RecentViewModel = hiltViewModel(),
    commandDeckViewModel: CommandDeckViewModel = hiltViewModel()
) {
    val tabs = CommandDeckTab.entries.toList()
    val innerNavController = rememberNavController()

    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isContextInputVisible by commandDeckViewModel.isContextInputVisible.collectAsStateWithLifecycle()
    val contextInputText by commandDeckViewModel.contextInputText.collectAsStateWithLifecycle()

    val headerModifier =
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) { commandDeckViewModel.openContextInput() }

    val selectedTabIndex = remember(currentRoute) {
        tabs.indexOfFirst { tab ->
            when (tab) {
                CommandDeckTab.Dashboard -> currentRoute == COMMAND_DECK_DASHBOARD_ROUTE
                CommandDeckTab.Core -> currentRoute == COMMAND_DECK_CORE_ROUTE
                CommandDeckTab.Strategy -> currentRoute == STRATEGIC_MANAGEMENT_ROUTE
                CommandDeckTab.StrategicArc -> currentRoute == COMMAND_DECK_STRATEGIC_ARC_ROUTE
                CommandDeckTab.Tactics -> currentRoute == COMMAND_DECK_TACTICS_ROUTE
                CommandDeckTab.Today -> currentRoute == COMMAND_DECK_TODAY_ROUTE
            }
        }.coerceAtLeast(0)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                when (currentRoute) {
                    COMMAND_DECK_DASHBOARD_ROUTE -> {
                        val showBadge = com.romankozak.forwardappmobile.BuildConfig.DEBUG || com.romankozak.forwardappmobile.BuildConfig.IS_EXPERIMENTAL_BUILD
                        FAHeader(
                            layout = CommandDeckHeaderPreset(
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
                                }
                            ),
                            backgroundStyle = FAHeaderBackground.CommandDeck,
                            modifier = headerModifier
                        )
                    }

                    COMMAND_DECK_CORE_ROUTE -> FAHeader(
                        layout = CoreHeader(),
                        backgroundStyle = FAHeaderBackground.CommandDeck,
                        modifier = headerModifier
                    )

                    COMMAND_DECK_TODAY_ROUTE -> {
                        val dayPlanBackStackEntry = remember(innerNavController.currentBackStackEntry) {
                            innerNavController.getBackStackEntry(COMMAND_DECK_TODAY_ROUTE)
                        }
                        val dayPlanViewModel: DayPlanViewModel = hiltViewModel(dayPlanBackStackEntry)
                        val dayPlanUiState by dayPlanViewModel.uiState.collectAsState()
                        val activityTrackerViewModel: ActivityTrackerViewModel = hiltViewModel(dayPlanBackStackEntry)
                        val activityLog by activityTrackerViewModel.activityLog.collectAsStateWithLifecycle()

                        val (xpToday, antyXpToday) = remember(activityLog, dayPlanUiState.dayPlan?.date) {
                            val targetDate = dayPlanUiState.dayPlan?.date ?: System.currentTimeMillis()
                            val recordsForDay = activityLog.filter { record ->
                                isSameDay(record.createdAt, targetDate)
                            }
                            val xp = recordsForDay.sumOf { it.xpGained ?: 0 }
                            val antyXp = recordsForDay.sumOf { it.antyXp ?: 0 }
                            xp to antyXp
                        }

                        FAHeader(
                            layout = TodayHeader(
                                onNavigateToPreviousDay = {
                                    Log.d("TodayTab", "onNavigateToPreviousDay callback invoked.")
                                    dayPlanViewModel.navigateToPreviousDay()
                                },
                                onNavigateToNextDay = {
                                    Log.d(
                                        "TodayTab",
                                        "onNavigateToNextDay callback invoked. Enabled: ${!dayPlanUiState.isToday}"
                                    )
                                    dayPlanViewModel.navigateToNextDay()
                                },
                                isNextDayNavigationEnabled = !dayPlanUiState.isToday,
                                date = dayPlanUiState.dayPlan?.date,
                            ),
                            backgroundStyle = FAHeaderBackground.CommandDeck,
                            modifier = headerModifier
                        )
                    }

                    STRATEGIC_MANAGEMENT_ROUTE -> FAHeader(
                        layout = StrategyHeader(onModeClick = {}),
                        backgroundStyle = FAHeaderBackground.CommandDeck,
                        modifier = headerModifier
                    )

                    COMMAND_DECK_STRATEGIC_ARC_ROUTE -> FAHeader(
                        layout = StrategicArcHeader(onModeClick = {}),
                        backgroundStyle = FAHeaderBackground.CommandDeck,
                        modifier = headerModifier
                    )

                    COMMAND_DECK_TACTICS_ROUTE -> FAHeader(
                        layout = TacticsHeader(),
                        backgroundStyle = FAHeaderBackground.CommandDeck,
                        modifier = headerModifier
                    )

                    else -> FAHeader(
                        layout = CommandDeckHeaderPreset(onClick = {}),
                        backgroundStyle = FAHeaderBackground.CommandDeck,
                        modifier = headerModifier
                    )
                }
            },
            bottomBar = {
                if (currentRoute == COMMAND_DECK_DASHBOARD_ROUTE) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .then(CommandDeckBackgroundModifier())
                            .padding(horizontal = 22.dp, vertical = 12.dp)
                    ) {
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
                            recentViewModel = recentViewModel
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(Color.Transparent)
            ) {
                Spacer(Modifier.height(8.dp))

                CommandDeckTabRow(
                    tabs = tabs,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { index ->
                        val newRoute = when (tabs[index]) {
                            CommandDeckTab.Dashboard -> COMMAND_DECK_DASHBOARD_ROUTE
                            CommandDeckTab.Core -> COMMAND_DECK_CORE_ROUTE
                            CommandDeckTab.Strategy -> STRATEGIC_MANAGEMENT_ROUTE
                            CommandDeckTab.StrategicArc -> COMMAND_DECK_STRATEGIC_ARC_ROUTE
                            CommandDeckTab.Tactics -> COMMAND_DECK_TACTICS_ROUTE
                            CommandDeckTab.Today -> COMMAND_DECK_TODAY_ROUTE
                        }
                        if (newRoute != currentRoute) {
                            innerNavController.navigate(newRoute) {
                                popUpTo(innerNavController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )

                Spacer(Modifier.height(16.dp))

                NavHost(
                    navController = innerNavController,
                    startDestination = COMMAND_DECK_DASHBOARD_ROUTE
                ) {
                    composable(COMMAND_DECK_DASHBOARD_ROUTE) {
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
                    onNavigateToImportExport = onNavigateToImportExport,
                    onNavigateToAttachments = onNavigateToAttachments,
                            onNavigateToScripts = onNavigateToScripts
                        )
                    }
                    composable(STRATEGIC_MANAGEMENT_ROUTE) {
                        StrategicManagementScreen(navController = navController)
                    }
                    composable(COMMAND_DECK_CORE_ROUTE) {
                        CoreLevelScreen(navController = navController)
                    }
                    composable(COMMAND_DECK_STRATEGIC_ARC_ROUTE) {
                        StrategicArcScreen(navController = navController)
                    }
                    composable(COMMAND_DECK_TACTICS_ROUTE) {
                        TacticalManagementScreen()
                    }
                    composable(COMMAND_DECK_TODAY_ROUTE) {
                        DayManagementScreen(mainNavController = navController, startTab = "PLAN")
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
            onDismiss = commandDeckViewModel::closeContextInput
        )
    }
}

private fun isSameDay(timestamp: Long, other: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp }
    val cal2 = Calendar.getInstance().apply { timeInMillis = other }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
