package com.romankozak.forwardappmobile.desktop.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextSummary
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedDayPlan
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedDayTask
import com.romankozak.forwardappmobile.shared.domain.contexts.DesktopWorkspaceRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun DesktopDashboardScreen(
    repository: DesktopWorkspaceRepository,
    refreshKey: Long = 0L,
    onContextClick: (String) -> Unit,
) {
    var dashboardState by remember { mutableStateOf(DayDashboardState()) }
    LaunchedEffect(repository, refreshKey) {
        val contexts = repository.getContexts()
        val dayPlans = repository.getDayPlans()
        val dayTasks = repository.getDayTasks()
        dashboardState =
            DayDashboardState(
                contextsById = contexts.associateBy { context -> context.id },
                todayTasks = resolveTodayTasks(dayPlans = dayPlans, dayTasks = dayTasks),
            )
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        DashboardHeroCard(
            modifier = Modifier.weight(1.5f).fillMaxHeight(),
        )
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            DashboardSummaryCard(
                title = "Today",
                body = "Day tasks imported from the mobile workspace.",
            )
            TodayTasksCard(
                tasks = dashboardState.todayTasks,
                contextsById = dashboardState.contextsById,
                onContextClick = onContextClick,
            )
        }
    }
}

@Composable
private fun DashboardHeroCard(
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF143F40), Color(0xFF315C55), Color(0xFFBA8E44)),
                        ),
                    )
                    .padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "ForwardApp Desktop",
                    style = MaterialTheme.typography.displaySmall,
                    color = Color(0xFFF7F3EC),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Не порт Android-екранів 1:1, а окремий desktop shell з чистими модулями, явними boundaries і міграцією shared domain.",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFE3E0D6),
                )
            }
            Text(
                text = "Stage 0: shell + architecture boundary",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFFDF2D8),
            )
        }
    }
}

@Composable
private fun DashboardSummaryCard(
    title: String,
    body: String,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCCFFFFFF)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TodayTasksCard(
    tasks: List<SharedDayTask>,
    contextsById: Map<String, SharedContextSummary>,
    onContextClick: (String) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCCFFFFFF)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Day Tasks",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (tasks.isEmpty()) {
                Text(
                    text = "No tasks for today in the imported workspace.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                tasks.forEach { task ->
                    DayTaskRow(
                        task = task,
                        contextsById = contextsById,
                        onContextClick = onContextClick,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayTaskRow(
    task: SharedDayTask,
    contextsById: Map<String, SharedContextSummary>,
    onContextClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = task.title.ifBlank { "Untitled task" },
            style = MaterialTheme.typography.titleMedium,
            color =
                if (task.isDone) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            fontWeight = FontWeight.Medium,
        )
        task.description?.takeIf { description -> description.isNotBlank() }?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val contextIds = (listOfNotNull(task.projectId) + task.linkedProjectIds).distinct()
        if (contextIds.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                contextIds.forEach { contextId ->
                    ContextLinkChip(
                        title = contextsById[contextId]?.name ?: contextId,
                        onClick = { onContextClick(contextId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextLinkChip(
    title: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color(0xFFE7F5EF),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF1D6E64),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private data class DayDashboardState(
    val contextsById: Map<String, SharedContextSummary> = emptyMap(),
    val todayTasks: List<SharedDayTask> = emptyList(),
)

private fun resolveTodayTasks(
    dayPlans: List<SharedDayPlan>,
    dayTasks: List<SharedDayTask>,
    today: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<SharedDayTask> {
    val todayPlanIds =
        dayPlans
            .filter { plan -> Instant.ofEpochMilli(plan.date).atZone(zoneId).toLocalDate() == today }
            .mapTo(hashSetOf()) { plan -> plan.id }
    return dayTasks
        .filter { task -> task.dayPlanId in todayPlanIds }
        .sortedWith(compareBy({ task -> task.isDone }, { task -> task.order }, { task -> task.title.lowercase() }))
}
