package com.romankozak.forwardappmobile.features.mainscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ImportExport
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.data.models.entities.TaskStatus
import com.romankozak.forwardappmobile.features.ai.insights.AiInsightsViewModel
import com.romankozak.forwardappmobile.features.ai.insights.AiMessage
import com.romankozak.forwardappmobile.features.ai.insights.MessageType
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayPlanViewModel
import com.romankozak.forwardappmobile.features.recent.RecentViewModel

private data class ActionCard(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit,
)

@Composable
fun DashboardContent(
    modifier: Modifier = Modifier,
    onNavigateToProjectHierarchy: () -> Unit = {},
    onNavigateToGlobalSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToInbox: () -> Unit = {},
    onNavigateToTracker: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    onNavigateToAiChat: () -> Unit = {},
    onNavigateToAiInsights: () -> Unit = {},
    onNavigateToAiLifeManagement: () -> Unit = {},
    onNavigateToImportExport: () -> Unit = {},
    onNavigateToAttachments: () -> Unit = {},
    onNavigateToScripts: () -> Unit = {},
    dayPlanViewModel: DayPlanViewModel = hiltViewModel(),
    recentViewModel: RecentViewModel = hiltViewModel(),
) {
    AnimatedCommandDeck(
        modifier = modifier,
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
        onNavigateToScripts = onNavigateToScripts,
        dayPlanViewModel = dayPlanViewModel,
        recentViewModel = recentViewModel,
    )
}

@Composable
private fun AnimatedCommandDeck(
    modifier: Modifier = Modifier,
    onNavigateToProjectHierarchy: () -> Unit,
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
    dayPlanViewModel: DayPlanViewModel,
    recentViewModel: RecentViewModel,
) {
    var overviewExpanded by remember { mutableStateOf(false) }
    var quickActionsStage by remember { mutableIntStateOf(0) }
    var aiInsightsExpanded by remember { mutableStateOf(false) }

    val dayUiState by dayPlanViewModel.uiState.collectAsStateWithLifecycle()
    val recentItems by recentViewModel.recentItems.collectAsStateWithLifecycle()
    val aiInsightsViewModel: AiInsightsViewModel = hiltViewModel()
    val aiInsights by aiInsightsViewModel.messages.collectAsStateWithLifecycle()

    val tasksTotal = dayUiState.tasks.size
    val tasksCompleted =
        dayUiState.tasks.count {
            it.dayTask.completed || it.dayTask.status == TaskStatus.COMPLETED
        }

    val actions =
        listOf(
            ActionCard("Inbox", "Вхідні задачі", Icons.Outlined.Inbox, Color(0xFF6EC6FF), onNavigateToInbox),
            ActionCard("Tracker", "Активності", Icons.Outlined.Analytics, Color(0xFFFF8A80), onNavigateToTracker),
            ActionCard("Contexts", "Ієрархія", Icons.Outlined.AlternateEmail, Color(0xFF80CBC4), onNavigateToProjectHierarchy),
            ActionCard("Search", "Пошук", Icons.Outlined.Search, Color(0xFFF48FB1), onNavigateToGlobalSearch),
            ActionCard("Reminders", "Нагадування", Icons.Outlined.Notifications, Color(0xFFFFE082), onNavigateToReminders),
            ActionCard("Attachments", "Бібліотека", Icons.Outlined.AttachFile, Color(0xFF90CAF9), onNavigateToAttachments),
            ActionCard("Scripts", "Автоматизація", Icons.Outlined.Code, Color(0xFFA5D6A7), onNavigateToScripts),
            ActionCard("AI Chat", "Робочий чат", Icons.AutoMirrored.Outlined.Chat, Color(0xFFB39DDB), onNavigateToAiChat),
            ActionCard("AI Insights", "Аналітика", Icons.Outlined.AutoAwesome, Color(0xFFCE93D8), onNavigateToAiInsights),
            ActionCard("AI Life", "Life-management", Icons.Outlined.AutoAwesome, Color(0xFFE1BEE7), onNavigateToAiLifeManagement),
            ActionCard("Import/Export", "Бекапи і перенос", Icons.Outlined.ImportExport, Color(0xFFFFAB91), onNavigateToImportExport),
            ActionCard("Settings", "Налаштування", Icons.Outlined.Settings, Color(0xFFB0BEC5), onNavigateToSettings),
        )

    val visibleQuickActions =
        when (quickActionsStage) {
            0 -> emptyList()
            1 -> actions.takeLast(4)
            else -> actions
        }

    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(0.dp),
                )
                .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 18.dp),
    ) {
        item {
            SectionHeader(
                title = "Огляд",
                subtitle = if (overviewExpanded) "Розгорнуто" else "Згорнуто",
                isExpanded = overviewExpanded,
                onClick = { overviewExpanded = !overviewExpanded },
            )
        }

        if (overviewExpanded) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MetricCard(
                        title = "Сьогодні",
                        value = "$tasksCompleted / $tasksTotal",
                        subtitle = "виконано задач",
                    )
                    MetricCard(
                        title = "Recent",
                        value = "${recentItems.size}",
                        subtitle = "останні переходи",
                    )
                }
            }

            item {
                MetricCard(
                    title = "План дня",
                    value = "${dayUiState.dayPlan?.linkedProjectIds.orEmpty().size} контекстів / ${dayUiState.dayPlan?.linkedAttachmentIds.orEmpty().size} вкладень",
                    subtitle = "scope-посилання поточного дня",
                )
            }
        }

        item {
            SectionHeader(
                title = "AI Insights",
                subtitle = if (aiInsightsExpanded) "Останні інсайти" else "Згорнуто",
                isExpanded = aiInsightsExpanded,
                onClick = { aiInsightsExpanded = !aiInsightsExpanded },
            )
        }

        if (aiInsightsExpanded) {
            val latestInsights = aiInsights.filterNot { it.isRead }.take(3)
            if (latestInsights.isNotEmpty()) {
                latestInsights.forEach { insight ->
                    item {
                        AiInsightCard(
                            insight = insight,
                            onMarkRead = { aiInsightsViewModel.markRead(insight.id) },
                        )
                    }
                }
            } else {
                item {
                    MetricCard(
                        title = "Немає інсайтів",
                        value = "Поки що немає аналітики",
                        subtitle = "AI ще не згенерував інсайти",
                    )
                }
            }
        }

        item {
            val stageLabel =
                when (quickActionsStage) {
                    0 -> "Згорнуто"
                    1 -> "Кілька останніх"
                    else -> "Усі"
                }
            SectionHeader(
                title = "Швидкі дії",
                subtitle = stageLabel,
                isExpanded = quickActionsStage != 0,
                onClick = { quickActionsStage = (quickActionsStage + 1) % 3 },
            )
        }

        visibleQuickActions.forEach { action ->
            item {
                ActionCardItem(action = action)
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.padding(end = 12.dp)) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActionCardItem(action: ActionCard) {
    Card(
        onClick = action.onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(38.dp).background(action.tint.copy(alpha = 0.24f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = action.tint,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(action.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                Text(action.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AiInsightCard(
    insight: AiMessage,
    onMarkRead: () -> Unit,
) {
    val backgroundColor = when (insight.type) {
        MessageType.MOTIVATION -> MaterialTheme.colorScheme.primaryContainer
        MessageType.JOKE -> MaterialTheme.colorScheme.secondaryContainer
        MessageType.INFO -> MaterialTheme.colorScheme.tertiaryContainer
        MessageType.WARNING -> MaterialTheme.colorScheme.errorContainer
        MessageType.ERROR -> MaterialTheme.colorScheme.errorContainer
    }
    val textColor = when (insight.type) {
        MessageType.MOTIVATION -> MaterialTheme.colorScheme.onPrimaryContainer
        MessageType.JOKE -> MaterialTheme.colorScheme.onSecondaryContainer
        MessageType.INFO -> MaterialTheme.colorScheme.onTertiaryContainer
        MessageType.WARNING -> MaterialTheme.colorScheme.onErrorContainer
        MessageType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
    }
    val timeFormatter = remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                    onMarkRead()
                    true
                } else {
                    false
                }
            },
        )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = "Позначити прочитаним",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = insight.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (insight.isRead) FontWeight.Normal else FontWeight.Bold,
                )
                Text(
                    text = "${insight.type.name.lowercase().replaceFirstChar { it.uppercase() }} • ${timeFormatter.format(java.util.Date(insight.timestamp))}",
                    color = textColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
