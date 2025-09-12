package com.romankozak.forwardappmobile.ui.screens.backlog.components.project_dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ProjectExecutionLog
import com.romankozak.forwardappmobile.data.database.models.ProjectStatus
import com.romankozak.forwardappmobile.data.database.models.ProjectTimeMetrics
import com.romankozak.forwardappmobile.ui.screens.backlog.components.projectrealization.DashboardContent
import com.romankozak.forwardappmobile.ui.screens.backlog.components.projectrealization.InsightsContent
import com.romankozak.forwardappmobile.ui.screens.backlog.components.projectrealization.LogContent

private enum class ProjectManagementTab(
    val displayName: String,
) {
    Dashboard("Дашборд"),
    Log("Історія"),
    Insights("Інсайти"),
}

@Composable
fun ProjectDashboardView(
    modifier: Modifier = Modifier,
    goalList: GoalList?,
    projectLogs: List<ProjectExecutionLog>,
    onStatusUpdate: (ProjectStatus, String?) -> Unit,
    onToggleProjectManagement: (Boolean) -> Unit,
    onRecalculateTime: () -> Unit,
    projectTimeMetrics: ProjectTimeMetrics?,
) {
    if (goalList == null) return

    var selectedTab by remember { mutableStateOf(ProjectManagementTab.Dashboard) }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            ProjectManagementTab.values().forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(tab.displayName) },
                )
            }
        }

        when (selectedTab) {
            ProjectManagementTab.Dashboard ->
                DashboardContent(
                    goalList = goalList,
                    onStatusUpdate = onStatusUpdate,
                    onToggleProjectManagement = onToggleProjectManagement,
                    onRecalculateTime = onRecalculateTime,
                    projectTimeMetrics = projectTimeMetrics,
                )
            ProjectManagementTab.Log ->
                LogContent(logs = projectLogs, isManagementEnabled = goalList.isProjectManagementEnabled == true)
            ProjectManagementTab.Insights ->
                InsightsContent(isManagementEnabled = goalList.isProjectManagementEnabled == true)
        }
    }
}
