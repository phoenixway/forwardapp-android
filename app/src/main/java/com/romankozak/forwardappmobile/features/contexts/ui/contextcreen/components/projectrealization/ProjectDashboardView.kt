package com.romankozak.forwardappmobile.features.contexts.ui.contextcreen.components.projectrealization

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.romankozak.forwardappmobile.features.contexts.data.models.Project
import com.romankozak.forwardappmobile.features.contexts.data.models.ProjectExecutionLog
import com.romankozak.forwardappmobile.data.database.models.ProjectTimeMetrics
import com.romankozak.forwardappmobile.data.database.models.ProjectArtifact

import androidx.compose.ui.graphics.Color

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
    selectedTab: ProjectManagementTab,
    onTabSelected: (ProjectManagementTab) -> Unit,
    enableDashboard: Boolean,
    enableLog: Boolean,
    enableArtifact: Boolean,
) {
    if (project == null) return

    val availableTabs = remember(enableDashboard, enableLog, enableArtifact) {
        ProjectManagementTab.values().filter { tab ->
            when (tab) {
                ProjectManagementTab.Dashboard -> enableDashboard
                ProjectManagementTab.Log -> enableLog
                ProjectManagementTab.Artifact -> enableArtifact
                else -> true
            }
        }
    }
    val safeSelectedTab = remember(selectedTab, availableTabs) {
        if (selectedTab in availableTabs) selectedTab else availableTabs.firstOrNull() ?: ProjectManagementTab.Insights
    }
    LaunchedEffect(safeSelectedTab, availableTabs) {
        if (selectedTab !in availableTabs && availableTabs.isNotEmpty()) {
            onTabSelected(availableTabs.first())
        }
    }

    if (availableTabs.isEmpty()) return

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = availableTabs.indexOf(safeSelectedTab).coerceAtLeast(0),
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            availableTabs.forEach { tab ->
                Tab(
                    selected = safeSelectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    icon = { Icon(tab.icon, contentDescription = tab.displayName) },
                )
            }
        }

        when (safeSelectedTab) {
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
                    onEditArtifact = onEditArtifact,
                    onSaveArtifact = { onSaveArtifact("") }
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
