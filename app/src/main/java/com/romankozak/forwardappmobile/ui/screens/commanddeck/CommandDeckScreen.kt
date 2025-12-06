package com.romankozak.forwardappmobile.ui.screens.commanddeck

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

enum class CommandDeckTab(
    val title: String,
    val symbol: String,
) {
    Core("Core", "⌘"),
    Strategy("Strategy", "⌖"),
    Tactics("Tactics", "◎"),
    Actions("Actions", "⌁"),
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val tabs = CommandDeckTab.entries

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Command Deck",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            CommandDeckTabRow(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { index ->
                    selectedTabIndex = index
                    when (tabs[index]) {
                        CommandDeckTab.Core -> { }
                        CommandDeckTab.Strategy -> onNavigateToStrategicManagement()
                        CommandDeckTab.Tactics -> onNavigateToTacticalManagement()
                        CommandDeckTab.Actions -> onNavigateToDayManagement()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()

            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(400)) +
                        slideInVertically(
                            animationSpec = tween(400),
                            initialOffsetY = { it / 8 }
                        )).togetherWith(
                        fadeOut(animationSpec = tween(200)) +
                            slideOutVertically(
                                animationSpec = tween(200),
                                targetOffsetY = { -it / 8 }
                            )
                    )
                },
                label = "command_deck_content"
            ) { tabIndex ->
                when (tabs[tabIndex]) {
                    CommandDeckTab.Core -> CoreTabContent(
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
                        onNavigateToScripts = onNavigateToScripts,
                    )
                    CommandDeckTab.Strategy -> PlaceholderContent("Strategy")
                    CommandDeckTab.Tactics -> PlaceholderContent("Tactics")
                    CommandDeckTab.Actions -> PlaceholderContent("Actions")
                }
            }
        }
    }
}

@Composable
private fun CoreTabContent(
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        CommandDeckActionCard(
            title = "Inbox",
            subtitle = "View your inbox",
            icon = Icons.Outlined.Inbox,
            onClick = onNavigateToInbox
        )

        CommandDeckActionCard(
            title = "Tracker",
            subtitle = "Track your activities",
            icon = Icons.Outlined.Analytics,
            onClick = onNavigateToTracker
        )

        CommandDeckActionCard(
            title = "Reminders",
            subtitle = "View your reminders",
            icon = Icons.Outlined.Notifications,
            onClick = onNavigateToReminders
        )

        CommandDeckActionCard(
            title = "AI Chat",
            subtitle = "Chat with AI",
            icon = Icons.Outlined.Chat,
            onClick = onNavigateToAiChat
        )

        CommandDeckActionCard(
            title = "AI Life Management",
            subtitle = "Manage your life with AI",
            icon = Icons.Outlined.AutoAwesome,
            onClick = onNavigateToAiLifeManagement
        )

        CommandDeckActionCard(
            title = "Import/Export",
            subtitle = "Import or export data",
            icon = Icons.Outlined.ImportExport,
            onClick = onNavigateToImportExport
        )

        CommandDeckActionCard(
            title = "Attachments",
            subtitle = "View your attachments",
            icon = Icons.Outlined.AttachFile,
            onClick = onNavigateToAttachments
        )

        CommandDeckActionCard(
            title = "Scripts",
            subtitle = "Run your scripts",
            icon = Icons.Outlined.Code,
            onClick = onNavigateToScripts
        )

        CommandDeckActionCard(
            title = "Проєкти",
            subtitle = "Ієрархія цілей та проєктів",
            icon = Icons.Outlined.AccountTree,
            onClick = onNavigateToProjectHierarchy
        )

        CommandDeckActionCard(
            title = "Глобальний пошук",
            subtitle = "Знайти будь-що",
            icon = Icons.Outlined.Search,
            onClick = onNavigateToGlobalSearch
        )

        CommandDeckActionCard(
            title = "Налаштування",
            subtitle = "Параметри додатку",
            icon = Icons.Outlined.Settings,
            onClick = onNavigateToSettings
        )
    }
}

@Composable
private fun CommandDeckActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PlaceholderContent(tabName: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Перехід до $tabName...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CommandDeckTabRow(
    tabs: List<CommandDeckTab>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            val isSelected = selectedTabIndex == index
            CommandDeckTabItem(
                tab = tab,
                isSelected = isSelected,
                onClick = { onTabSelected(index) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CommandDeckTabItem(
    tab: CommandDeckTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.surface
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tab.symbol,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tab.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
