package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.projectrealization

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.romankozak.forwardappmobile.data.database.models.Project
import com.romankozak.forwardappmobile.data.database.models.ProjectExecutionLog
import com.romankozak.forwardappmobile.data.database.models.ProjectStatusValues
import com.romankozak.forwardappmobile.data.database.models.ProjectTimeMetrics
import com.romankozak.forwardappmobile.data.database.models.ProjectArtifact
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.projectrealization.DashboardContent
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.projectrealization.InsightsContent
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.projectrealization.LogContent

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb

private enum class ProjectManagementTab(
    val displayName: String,
    val icon: ImageVector,
) {
    Dashboard("Дашборд", Icons.Default.Dashboard),
    Artifact("Артефакт", Icons.Default.Description),
    Log("Історія", Icons.Default.History),
    Insights("Інсайти", Icons.Default.Lightbulb),
}

@Composable
fun ProjectDashboardView(
    modifier: Modifier = Modifier,
    project: Project?,
    projectLogs: List<ProjectExecutionLog>,
    projectArtifact: ProjectArtifact?,
    onStatusUpdate: (String, String?) -> Unit,
    onToggleProjectManagement: (Boolean) -> Unit,
    onRecalculateTime: () -> Unit,
    projectTimeMetrics: ProjectTimeMetrics?,
    onEditLog: (ProjectExecutionLog) -> Unit,
    onDeleteLog: (ProjectExecutionLog) -> Unit,
    onSaveArtifact: (String) -> Unit,
    onEditArtifact: (ProjectArtifact) -> Unit,
) {
    if (project == null) return

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
                    icon = { Icon(tab.icon, contentDescription = tab.displayName) },
                )
            }
        }

        when (selectedTab) {
            ProjectManagementTab.Dashboard ->
                DashboardContent(
                    project = project,
                    onStatusUpdate = onStatusUpdate,
                    onToggleProjectManagement = onToggleProjectManagement,
                    onRecalculateTime = onRecalculateTime,
                    projectTimeMetrics = projectTimeMetrics,
                )
            ProjectManagementTab.Artifact -> {
                ArtifactContent(
                    artifact = projectArtifact,
                    isManagementEnabled = project.isProjectManagementEnabled == true,
                    onEditArtifact = onEditArtifact
                )
            }
            ProjectManagementTab.Log ->
                LogContent(
                    logs = projectLogs,
                    isManagementEnabled = project.isProjectManagementEnabled == true,
                    onEditLog = onEditLog,
                    onDeleteLog = onDeleteLog
                )
            ProjectManagementTab.Insights ->
                InsightsContent(isManagementEnabled = project.isProjectManagementEnabled == true)
        }
    }
}
