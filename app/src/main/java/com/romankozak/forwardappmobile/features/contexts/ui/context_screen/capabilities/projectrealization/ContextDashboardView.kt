package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.projectrealization

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.ContextArtifact
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.ContextTimeMetrics

@Composable
fun ProjectDashboardView(
    modifier: Modifier = Modifier,
    project: Context?,
    projectLogs: List<ContextLog>,
    contextArtifact: ContextArtifact?,
    onStatusUpdate: (String, String?) -> Unit,
    onToggleProjectManagement: (Boolean) -> Unit,
    onRecalculateTime: () -> Unit,
    contextTimeMetrics: ContextTimeMetrics?,
    onEditLog: (ContextLog) -> Unit,
    onDeleteLog: (ContextLog) -> Unit,
    onSaveArtifact: (String) -> Unit,
    onEditArtifact: (ContextArtifact) -> Unit,
    selectedTab: ContextManagementTab,
    onTabSelected: (ContextManagementTab) -> Unit,
    enableDashboard: Boolean,
    enableLog: Boolean,
    enableArtifact: Boolean,
) {
    if (project == null) return

    val availableTabs =
        remember(enableDashboard, enableLog, enableArtifact) {
            ContextManagementTab.values().filter { tab ->
                when (tab) {
                    ContextManagementTab.Dashboard -> enableDashboard
                    ContextManagementTab.Log -> false
                    ContextManagementTab.Artifact -> false
                    else -> true
                }
            }
        }
    val safeSelectedTab =
        remember(selectedTab, availableTabs) {
            if (selectedTab in availableTabs) selectedTab else availableTabs.firstOrNull() ?: ContextManagementTab.Insights
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
            ContextManagementTab.Dashboard ->
                DashboardContent(
                    project = project,
                    onStatusUpdate = onStatusUpdate,
                    onToggleProjectManagement = onToggleProjectManagement,
                    onRecalculateTime = onRecalculateTime,
                    contextTimeMetrics = contextTimeMetrics,
                )
            ContextManagementTab.Artifact -> Unit
            ContextManagementTab.Log -> Unit
            ContextManagementTab.Insights ->
                InsightsContent(isManagementEnabled = project.isContextManagementEnabled == true)
        }
    }
}
