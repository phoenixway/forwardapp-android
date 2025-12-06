package com.romankozak.forwardappmobile.ui.screens.commanddeck

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset

// -----------------------------------------------
// ENUM
// -----------------------------------------------
enum class CommandDeckTab(
    val title: String,
    val symbol: String,
) {
    Dashboard("Dashboard", "⌗"),
    Core("Core", "⌘"),
    Strategy("Strategy", "⌖"),
    StrategicArc("Strategic Arc", "⟲"),
    Tactics("Tactics", "◎"),
    Today("Today", "⌁"),
}

// -----------------------------------------------
// HEADER
// -----------------------------------------------
@Composable
fun CommandDeckHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 26.dp, start = 20.dp, end = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Command Deck",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "⌬",
            fontSize = 40.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 16.dp)
        )
    }
}
// -----------------------------------------------
// MAIN SCREEN
// -----------------------------------------------
@Composable
fun CommandDeckScreen(
    navController: NavController,
    onNavigateToProjectHierarchy: () -> Unit,
    onNavigateToDayManagement: () -> Unit,
    onNavigateToStrategicManagement: () -> Unit,
    onNavigateToTacticalManagement: () -> Unit,
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
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = CommandDeckTab.entries.toList()

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
        CommandDeckHeader()

        Spacer(Modifier.height(8.dp))

        CommandDeckTabRow(
            tabs = tabs,
            selectedTabIndex = selectedTabIndex,
            onTabSelected = { index ->
                selectedTabIndex = index
                when (tabs[index]) {
                    CommandDeckTab.Dashboard -> {}
                    CommandDeckTab.Core -> {}
                    CommandDeckTab.Strategy -> onNavigateToStrategicManagement()
                    CommandDeckTab.StrategicArc -> {}
                    CommandDeckTab.Tactics -> onNavigateToTacticalManagement()
                    CommandDeckTab.Today -> onNavigateToDayManagement()
                }
            }
        )

        Spacer(Modifier.height(12.dp))

        AnimatedContent(
            targetState = selectedTabIndex,
            transitionSpec = {
                (fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 8 })
                    .togetherWith(
                        fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 8 }
                    )
            },
            label = "command_deck_content"
        ) { tabIndex ->

            when (tabs[tabIndex]) {

                CommandDeckTab.Dashboard -> {
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

                CommandDeckTab.Core -> {
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

                CommandDeckTab.Strategy -> {
                    DeckModuleCard(
                        title = "Strategy",
                        subtitle = "Current Strategic Epoch: Growth Q1",
                        progress = 48,
                        accentColor = Color(0xFF4FC3F7),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                CommandDeckTab.StrategicArc -> {
                    DeckModuleCard(
                        title = "Strategic Arc",
                        subtitle = "Plan for April: Expansion Arc",
                        progress = 62,
                        accentColor = Color(0xFF9575CD),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                CommandDeckTab.Tactics -> {
                    DeckModuleCard(
                        title = "Tactical Missions",
                        subtitle = "Active missions: 5 (1 critical)",
                        progress = 35,
                        accentColor = Color(0xFF26A69A),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                CommandDeckTab.Today -> {
                    DeckModuleCard(
                        title = "Today",
                        subtitle = "3 actions planned",
                        progress = null,
                        accentColor = Color(0xFFFFB74D),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}


// ------------------------------------------------------
// TAB ROW WITH AUTO–CENTERING
// ------------------------------------------------------

@Composable
private fun CommandDeckTabRow(
    tabs: List<CommandDeckTab>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
) {
    val scrollState = rememberScrollState()
    val coroutine = rememberCoroutineScope()

    LaunchedEffect(selectedTabIndex) {
        val tabWidth = 130f
        val target = (selectedTabIndex * tabWidth) - (scrollState.maxValue / 2f) + tabWidth / 2

        coroutine.launch {
            scrollState.animateScrollTo(target.toInt().coerceAtLeast(0))
        }
    }

    Row(
        modifier = Modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
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
// ------------------------------------------------------
// TAB COLORS
// ------------------------------------------------------

fun tabAccentColor(tab: CommandDeckTab): Color {
    return when (tab) {
        CommandDeckTab.Core -> Color(0xFFBB86FC)
        CommandDeckTab.Strategy -> Color(0xFF4FC3F7)
        CommandDeckTab.StrategicArc -> Color(0xFF9575CD)
        CommandDeckTab.Tactics -> Color(0xFF26A69A)
        CommandDeckTab.Today -> Color(0xFFFFB74D)
        CommandDeckTab.Dashboard -> MaterialTheme.colorScheme.primary
    }
}


// ------------------------------------------------------
// TAB ITEM (GLOW + SHIMMER + SCALE)
// ------------------------------------------------------

@Composable
fun CommandDeckTabItem(
    tab: CommandDeckTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = tabAccentColor(tab)

    // Glow animation (macOS smooth aura)
    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.32f else 0f,
        animationSpec = tween(600)
    )

    // Shimmer wave animation
    val shimmer = remember { Animatable(0f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            shimmer.animateTo(
                1f,
                animationSpec = tween(durationMillis = 1100)
            )
            shimmer.snapTo(0f)
        }
    }

    // Scale when selected
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.10f else 1f,
        animationSpec = spring(dampingRatio = 0.60f, stiffness = 300f)
    )

    // Shimmer wave brush
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
                if (isSelected) drawRect(waveBrush, alpha = 0.85f)
            }
            .border(
                width = if (isSelected) 1.8.dp else 1.dp,
                color = if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(38.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Symbol (Core, Strategy, etc.)
        Text(
            text = tab.symbol,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isSelected) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = tab.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
        }
    }
}
// ------------------------------------------------------
// MODULE CARD
// ------------------------------------------------------

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
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
        ),
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // Title
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Subtitle
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Optional progress
            if (progress != null) {
                LinearProgressIndicator(
                    progress = progress / 100f,
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
// ------------------------------------------------------
// CORE QUICK ACTIONS SECTION
// ------------------------------------------------------

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


// ------------------------------------------------------
// QUICK ACTION CARD
// ------------------------------------------------------

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
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
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
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

