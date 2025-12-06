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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.romankozak.forwardappmobile.routes.STRATEGIC_MANAGEMENT_ROUTE
import com.romankozak.forwardappmobile.ui.components.header.CommandDeckHeaderPreset
import com.romankozak.forwardappmobile.ui.components.header.FAHeader
import com.romankozak.forwardappmobile.ui.components.header.StrategicArcHeader
import com.romankozak.forwardappmobile.ui.components.header.StrategyHeader
import com.romankozak.forwardappmobile.ui.components.header.TodayHeader
import com.romankozak.forwardappmobile.ui.screens.daymanagement.DayManagementScreen
import com.romankozak.forwardappmobile.features.missions.presentation.TacticalManagementScreen
import com.romankozak.forwardappmobile.ui.screens.strategicmanagement.StrategicManagementScreen
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
            COMMAND_DECK_DASHBOARD_ROUTE -> FAHeader(config = CommandDeckHeaderPreset())
            COMMAND_DECK_TODAY_ROUTE -> FAHeader(config = TodayHeader())
            STRATEGIC_MANAGEMENT_ROUTE -> FAHeader(config = StrategyHeader(onModeClick = {}))
            COMMAND_DECK_STRATEGIC_ARC_ROUTE -> FAHeader(config = StrategicArcHeader(onModeClick = {}))
            else -> FAHeader(config = CommandDeckHeaderPreset())
        }

        Spacer(Modifier.height(12.dp))

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


        Spacer(Modifier.height(12.dp))

        NavHost(navController = innerNavController, startDestination = COMMAND_DECK_DASHBOARD_ROUTE) {
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
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(selectedTabIndex) {
        val tabWidth = 130f
        val target = (selectedTabIndex * tabWidth) - (scrollState.maxValue / 2f) + tabWidth / 2
        coroutineScope.launch {
            scrollState.animateScrollTo(target.toInt().coerceAtLeast(0))
        }
    }

    Row(
        modifier = Modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
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
    val coroutineScope = rememberCoroutineScope()

    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.32f else 0f,
        animationSpec = tween(600),
        label = "glow"
    )

    val shimmer = remember { Animatable(0f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            coroutineScope.launch {
                shimmer.animateTo(
                    1f,
                    animationSpec = tween(durationMillis = 1100)
                )
                shimmer.snapTo(0f)
            }
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.10f else 1f,
        animationSpec = spring(dampingRatio = 0.60f, stiffness = 300f),
        label = "scale"
    )

    val waveBrush = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            accent.copy(alpha = 0.40f),
            Color.Transparent
        ),
        start = Offset(x = -220f + shimmer.value * 440f, y = 0f),
        end = Offset(x = shimmer.value * 440f, y = 0f)
    )

    Row(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(38.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.12f + glowAlpha),
                        accent.copy(alpha = 0.04f * glowAlpha),
                        Color.Transparent
                    )
                )
            )
            .drawBehind {
                if (isSelected) {
                    drawRect(
                        brush = waveBrush,
                        size = size,
                        alpha = 0.85f
                    )
                }
            }
            .border(
                width = if (isSelected) 1.8.dp else 1.dp,
                color = if (isSelected) accent else Color(0xFF444444),
                shape = RoundedCornerShape(38.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tab.symbol,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) accent else Color(0xFFAAAAAA)
        )

        if (isSelected) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = tab.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
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
