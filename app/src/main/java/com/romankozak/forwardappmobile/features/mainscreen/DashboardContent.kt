package com.romankozak.forwardappmobile.features.mainscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayPlanViewModel
import com.romankozak.forwardappmobile.features.recent.RecentViewModel

private data class QuickAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit,
)

@Composable
fun AnimatedCommandDeck(
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
    dayPlanViewModel: DayPlanViewModel = hiltViewModel(),
    recentViewModel: RecentViewModel = hiltViewModel(),
) {
    val dayUiState by dayPlanViewModel.uiState.collectAsStateWithLifecycle()
    val recentItems by recentViewModel.recentItems.collectAsStateWithLifecycle()

    val tasksTotal = dayUiState.tasks.size
    val tasksCompleted =
        dayUiState.tasks.count {
            it.dayTask.completed || it.dayTask.status == TaskStatus.COMPLETED
        }

    val actions =
        listOf(
            QuickAction("Inbox", "Вхідні задачі", Icons.Outlined.Inbox, Color(0xFF6EC6FF), onNavigateToInbox),
            QuickAction("Tracker", "Активності", Icons.Outlined.Analytics, Color(0xFFFF8A80), onNavigateToTracker),
            QuickAction("Contexts", "Ієрархія", Icons.Outlined.AlternateEmail, Color(0xFF80CBC4), onNavigateToProjectHierarchy),
            QuickAction("Search", "Пошук", Icons.Outlined.Search, Color(0xFFF48FB1), onNavigateToGlobalSearch),
            QuickAction("Reminders", "Нагадування", Icons.Outlined.Notifications, Color(0xFFFFE082), onNavigateToReminders),
            QuickAction("Attachments", "Бібліотека", Icons.Outlined.AttachFile, Color(0xFF90CAF9), onNavigateToAttachments),
            QuickAction("Scripts", "Автоматизація", Icons.Outlined.Code, Color(0xFFA5D6A7), onNavigateToScripts),
            QuickAction("AI Chat", "Робочий чат", Icons.AutoMirrored.Outlined.Chat, Color(0xFFB39DDB), onNavigateToAiChat),
            QuickAction("AI Insights", "Аналітика", Icons.Outlined.AutoAwesome, Color(0xFFCE93D8), onNavigateToAiInsights),
            QuickAction("AI Life", "Life-management", Icons.Outlined.AutoAwesome, Color(0xFFE1BEE7), onNavigateToAiLifeManagement),
            QuickAction("Import/Export", "Бекапи і перенос", Icons.Outlined.ImportExport, Color(0xFFFFAB91), onNavigateToImportExport),
            QuickAction("Settings", "Налаштування", Icons.Outlined.Settings, Color(0xFFB0BEC5), onNavigateToSettings),
        )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 18.dp),
    ) {
        item {
            Text(
                text = "Огляд",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                DashboardMetricCard(
                    title = "Сьогодні",
                    value = "$tasksCompleted / $tasksTotal",
                    subtitle = "виконано задач",
                    modifier = Modifier.weight(1f),
                )
                DashboardMetricCard(
                    title = "Recent",
                    value = "${recentItems.size}",
                    subtitle = "останні переходи",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            DashboardMetricCard(
                title = "План дня",
                value = "${dayUiState.dayPlan?.linkedProjectIds.orEmpty().size} контекстів / ${dayUiState.dayPlan?.linkedAttachmentIds.orEmpty().size} вкладень",
                subtitle = "scope-посилання поточного дня",
            )
        }

        item {
            Text(
                text = "Швидкі дії",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        actions.forEach { action ->
            item {
                QuickActionCard(action = action)
            }
        }
    }
}

@Composable
private fun DashboardMetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
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
private fun QuickActionCard(action: QuickAction) {
    Card(
        onClick = action.onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(action.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                Text(action.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = "Відкрити",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = action.onClick),
            )
        }
    }
}
