package com.romankozak.forwardappmobile.ui.screens.commanddeck

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset // Corrected import
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.romankozak.forwardappmobile.routes.STRATEGIC_MANAGEMENT_ROUTE
import com.romankozak.forwardappmobile.ui.components.header.CommandDeckHeaderPreset
import com.romankozak.forwardappmobile.ui.components.header.FAHeader
import com.romankozak.forwardappmobile.ui.components.header.FAHeaderBackground
import com.romankozak.forwardappmobile.ui.components.header.StrategicArcHeader
import com.romankozak.forwardappmobile.ui.components.header.StrategyHeader
import com.romankozak.forwardappmobile.ui.components.header.TodayHeader
import com.romankozak.forwardappmobile.ui.screens.daymanagement.DayManagementScreen
import com.romankozak.forwardappmobile.features.missions.presentation.TacticalManagementScreen
import com.romankozak.forwardappmobile.ui.screens.strategicmanagement.StrategicManagementScreen
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.PaddingValues

import androidx.hilt.navigation.compose.hiltViewModel

import kotlinx.coroutines.launch

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



    Column(
        modifier = Modifier
            .fillMaxSize()
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
        when (currentRoute) {
            COMMAND_DECK_DASHBOARD_ROUTE -> FAHeader(
                layout = CommandDeckHeaderPreset(),
                backgroundStyle = FAHeaderBackground.CommandDeck
            )

            COMMAND_DECK_TODAY_ROUTE -> FAHeader(layout = TodayHeader(), backgroundStyle = FAHeaderBackground.CommandDeck)

            STRATEGIC_MANAGEMENT_ROUTE -> FAHeader(
                layout = StrategyHeader(onModeClick = {}),
                backgroundStyle = FAHeaderBackground.CommandDeck
            )

            COMMAND_DECK_STRATEGIC_ARC_ROUTE -> FAHeader(
                layout = StrategicArcHeader(onModeClick = {}),
                backgroundStyle = FAHeaderBackground.CommandDeck
            )

            else -> FAHeader(
                layout = CommandDeckHeaderPreset(),
                backgroundStyle = FAHeaderBackground.CommandDeck
            )
        }

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

        NavHost(
            navController = innerNavController,
            startDestination = COMMAND_DECK_DASHBOARD_ROUTE
        ) {
            composable(COMMAND_DECK_DASHBOARD_ROUTE) {
                DashboardContent(
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DeckModuleCard(
                        title = "Core Beacons",
                        subtitle = "Foundational principles active: 3",
                        progress = 72,
                        accentColor = Color(0xFFBB86FC)
                    )
                }
            }
            composable(COMMAND_DECK_STRATEGIC_ARC_ROUTE) {
                DeckModuleCard(
                    title = "Strategic Arc",
                    subtitle = "Plan for this month: Expansion Arc",
                    progress = 62,
                    accentColor = Color(0xFF9575CD),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
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


@Composable
fun DashboardContent(
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
    onNavigateToScripts: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CoreQuickActions(
            onNavigateToProjectHierarchy,
            onNavigateToGlobalSearch,
            onNavigateToSettings,
            onNavigateToInbox,
            onNavigateToTracker,
            onNavigateToReminders,
            onNavigateToAiChat,
            onNavigateToAiLifeManagement,
            onNavigateToImportExport,
            onNavigateToAttachments,
            onNavigateToScripts
        )
    }
}

// -----------------------------------------------
// ALL THE COMPOSABLES FROM CommandDeckScreen.kt
// -----------------------------------------------

// ENUM TABS
enum class CommandDeckTab(
    val title: String,
    val symbol: String,
) {
    Dashboard("Command Deck", "⌗"),
    Core("Core", "⌘"),
    Strategy("Strategy", "⌖"),
    StrategicArc("Strategic Arc", "⟲"),
    Tactics("Tactics", "◎"),
    Today("Today", "⌁"),
}



// TAB ROW
@Composable
private fun CommandDeckTabRow(
    tabs: List<CommandDeckTab>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        itemsIndexed(tabs) { index, tab ->
            CommandDeckTabItem(
                tab = tab,
                isSelected = index == selectedTabIndex,
                onClick = { onTabSelected(index) }
            )
        }
    }
}


// TAB COLORS
fun tabAccentColor(tab: CommandDeckTab): Color {
    return when (tab) {
        CommandDeckTab.Core -> Color(0xFFBB86FC)
        CommandDeckTab.Strategy -> Color(0xFF4FC3F7)
        CommandDeckTab.StrategicArc -> Color(0xFF9575CD)
        CommandDeckTab.Tactics -> Color(0xFF26A69A)
        CommandDeckTab.Today -> Color(0xFFFFB74D)
        CommandDeckTab.Dashboard -> Color(0xFF6200EE)
    }
}

// TAB ITEM
@Composable
fun CommandDeckTabItem(
    tab: CommandDeckTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = tabAccentColor(tab)

    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.18f else 0f,
        animationSpec = tween(450),
        label = "glow"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 320f),
        label = "scale"
    )

    val isSpecialTab = tab == CommandDeckTab.StrategicArc || tab == CommandDeckTab.Tactics || tab == CommandDeckTab.Today
    val symbolFontSize = if (isSpecialTab) 22.sp else 18.sp // Even larger font size
    val circleSize = if (isSpecialTab) 32.dp else 28.dp // Even larger circle for special tabs

    Row(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.10f + glowAlpha),
                        accent.copy(alpha = 0.03f)
                    )
                )
            )
            .border(
                width = if (isSelected) 1.4.dp else 0.8.dp,
                color = accent.copy(alpha = if (isSelected) 0.9f else 0.45f),
                shape = RoundedCornerShape(26.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ------------------------
        // UNIFIED CIRCLE ICON AREA
        // ------------------------
        Box(
            modifier = Modifier
                .size(circleSize) // Use dynamic size
                .clip(CircleShape)
                .background(accent.copy(alpha = if (isSelected) 0.22f else 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tab.symbol,
                fontSize = symbolFontSize, // Use dynamic font size
                fontWeight = FontWeight.Bold,
                color = if (isSelected) accent else Color(0xFFBBBBBB),
                modifier = if (tab == CommandDeckTab.Tactics || tab == CommandDeckTab.StrategicArc) {
                    Modifier.offset(y = (-2).dp) // Apply a small upward offset
                } else {
                    Modifier
                }
            )
        }

        if (isSelected) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = tab.title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = accent
            )
        }
    }
}



// MODULE CARD
@Composable
fun DeckModuleCard(
    title: String,
    subtitle: String,
    progress: Int?,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E).copy(alpha = 0.22f)
        ),
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = Color(0xFFCCCCCC)
            )

            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    color = accentColor,
                    trackColor = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                )
            }
        }
    }
}

// CORE QUICK ACTIONS
@Composable
private fun CoreQuickActions(
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
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        QuickActionCard("Inbox", "View inbox", Icons.Outlined.Inbox, onNavigateToInbox)
        QuickActionCard("Tracker", "Track activity", Icons.Outlined.Analytics, onNavigateToTracker)
        QuickActionCard("Reminders", "See reminders", Icons.Outlined.Notifications, onNavigateToReminders)
        QuickActionCard("AI Chat", "Chat with AI", Icons.Outlined.Chat, onNavigateToAiChat)
        QuickActionCard("AI Life Management", "AI guidance", Icons.Outlined.AutoAwesome, onNavigateToAiLifeManagement)
        QuickActionCard("Import/Export", "Transfer data", Icons.Outlined.ImportExport, onNavigateToImportExport)
        QuickActionCard("Attachments", "All files & notes", Icons.Outlined.AttachFile, onNavigateToAttachments)
        QuickActionCard("Scripts", "Automation scripts", Icons.Outlined.Code, onNavigateToScripts)
        QuickActionCard("Проєкти", "Project hierarchy", Icons.Outlined.AccountTree, onNavigateToProjectHierarchy)
        QuickActionCard("Глобальний пошук", "Search anything", Icons.Outlined.Search, onNavigateToGlobalSearch)
        QuickActionCard("Налаштування", "App settings", Icons.Outlined.Settings, onNavigateToSettings)
    }
}

// QUICK ACTION CARD
@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E).copy(alpha = 0.18f)
        ),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF6200EE).copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color(0xFF6200EE)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )

                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color(0xFFCCCCCC)
                )
            }
        }
    }
}
