package com.romankozak.forwardappmobile.ui.screens.commanddeck

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import com.romankozak.forwardappmobile.ui.screens.daymanagement.DayManagementScreen
import com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.DayPlanViewModel
import com.romankozak.forwardappmobile.ui.screens.strategicmanagement.StrategicManagementScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Scaffold
import com.romankozak.forwardappmobile.ui.screens.commanddeck.components.DashboardBottomBar

const val COMMAND_DECK_DASHBOARD_ROUTE = "command_deck_dashboard"
const val COMMAND_DECK_CORE_ROUTE = "command_deck_core"
const val COMMAND_DECK_STRATEGIC_ARC_ROUTE = "command_deck_strategic_arc"
const val COMMAND_DECK_TACTICS_ROUTE = "command_deck_tactics"
const val COMMAND_DECK_TODAY_ROUTE = "command_deck_today"




@Composable
fun SharedCommandDeckLayout(
    navController: NavController,
    onNavigateToProjectHierarchy: () -> Unit,
    onNavigateToGlobalSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToTracker: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onNavigateToAiLifeManagement: () -> Unit,
    onNavigateToImportExport: () -> Unit,
    onNavigateToAttachments: () -> Unit,
    onNavigateToScripts: () -> Unit,
) {
    val tabs = CommandDeckTab.entries.toList()
    val innerNavController = rememberNavController()

    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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

    Scaffold(
        topBar = {
            when (currentRoute) {
                COMMAND_DECK_DASHBOARD_ROUTE -> FAHeader(
                    layout = CommandDeckHeaderPreset(onClick = onNavigateToProjectHierarchy),
                    backgroundStyle = FAHeaderBackground.CommandDeck
                )

                COMMAND_DECK_CORE_ROUTE -> FAHeader(
                    layout = CoreHeader(),
                    backgroundStyle = FAHeaderBackground.CommandDeck
                )

                COMMAND_DECK_TODAY_ROUTE -> {
                    val dayPlanBackStackEntry = remember(innerNavController.currentBackStackEntry) {
                        innerNavController.getBackStackEntry(COMMAND_DECK_TODAY_ROUTE)
                    }
                    val dayPlanViewModel: DayPlanViewModel = hiltViewModel(dayPlanBackStackEntry)
                    val dayPlanUiState by dayPlanViewModel.uiState.collectAsState()

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
                            date = dayPlanUiState.dayPlan?.date
                        ),
                        backgroundStyle = FAHeaderBackground.CommandDeck
                    )
                }

                STRATEGIC_MANAGEMENT_ROUTE -> FAHeader(
                    layout = StrategyHeader(onModeClick = {}),
                    backgroundStyle = FAHeaderBackground.CommandDeck
                )

                COMMAND_DECK_STRATEGIC_ARC_ROUTE -> FAHeader(
                    layout = StrategicArcHeader(onModeClick = {}),
                    backgroundStyle = FAHeaderBackground.CommandDeck
                )

                COMMAND_DECK_TACTICS_ROUTE -> FAHeader(
                    layout = TacticsHeader(),
                    backgroundStyle = FAHeaderBackground.CommandDeck
                )

                else -> FAHeader(
                    layout = CommandDeckHeaderPreset(onClick = {}),
                    backgroundStyle = FAHeaderBackground.CommandDeck
                )
            }
        },
        bottomBar = {
            if (currentRoute == COMMAND_DECK_DASHBOARD_ROUTE) {
                DashboardBottomBar(
                    onNavigateToProjectHierarchy = onNavigateToProjectHierarchy,
                    onNavigateToTracker = onNavigateToTracker,
                    onNavigateToInbox = onNavigateToInbox,
                    onNavigateToReminders = onNavigateToReminders,
                    onNavigateToMore = { /* TODO */ }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        )
                    )
                )
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
}

