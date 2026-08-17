package com.romankozak.forwardappmobile.desktop.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.desktop.app.navigation.DesktopDestination
import com.romankozak.forwardappmobile.desktop.design.ForwardDesktopTheme
import com.romankozak.forwardappmobile.desktop.features.dashboard.DesktopDashboardScreen
import com.romankozak.forwardappmobile.desktop.features.contexts.rememberDesktopWorkspaceDependencies
import com.romankozak.forwardappmobile.desktop.features.settings.DesktopSettingsScreen
import com.romankozak.forwardappmobile.desktop.features.sync.rememberDesktopAndroidSyncController
import com.romankozak.forwardappmobile.desktop.features.workbench.DesktopWorkbenchScreen
import androidx.compose.runtime.collectAsState

@Composable
fun ForwardDesktopApp() {
    var currentDestination by remember { mutableStateOf(DesktopDestination.Workbench) }
    var pendingWorkbenchContextId by remember { mutableStateOf<String?>(null) }
    val workspaceDependencies = rememberDesktopWorkspaceDependencies()
    val syncController = rememberDesktopAndroidSyncController(workspaceDependencies.fileStore)
    val syncState by syncController.state.collectAsState()

    ForwardDesktopTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFF2EEE6), Color(0xFFE7EEF1)),
                            ),
                        ),
            ) {
                DesktopNavigationRail(
                    currentDestination = currentDestination,
                    onDestinationSelected = { destination ->
                        if (destination == DesktopDestination.Workbench) {
                            pendingWorkbenchContextId = null
                        }
                        currentDestination = destination
                    },
                )
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .padding(24.dp),
                ) {
                    when (currentDestination) {
                        DesktopDestination.Dashboard ->
                            DesktopDashboardScreen(
                                repository = workspaceDependencies.repository,
                                refreshKey = syncState.workspaceRevision,
                                onOpenContextWorkspace = {
                                    pendingWorkbenchContextId = null
                                    currentDestination = DesktopDestination.Workbench
                                },
                                onContextClick = { contextId ->
                                    pendingWorkbenchContextId = contextId
                                    currentDestination = DesktopDestination.Workbench
                                },
                            )
                        DesktopDestination.Workbench ->
                            DesktopWorkbenchScreen(
                                dependencies = workspaceDependencies,
                                initialContextId = pendingWorkbenchContextId,
                                refreshKey = syncState.workspaceRevision,
                            )
                        DesktopDestination.Settings ->
                            DesktopSettingsScreen(
                                dependencies = workspaceDependencies,
                                syncController = syncController,
                            )
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopNavigationRail(
    currentDestination: DesktopDestination,
    onDestinationSelected: (DesktopDestination) -> Unit,
) {
    NavigationRail(
        modifier =
            Modifier
                .width(148.dp)
                .fillMaxSize()
                .padding(vertical = 20.dp),
        containerColor = Color.Transparent,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "FWD",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            DesktopDestination.entries.forEach { destination ->
                NavigationRailItem(
                    selected = currentDestination == destination,
                    onClick = { onDestinationSelected(destination) },
                    icon = {
                        when (destination) {
                            DesktopDestination.Dashboard ->
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Outlined.Dashboard,
                                    contentDescription = destination.title,
                                )
                            DesktopDestination.Workbench ->
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Outlined.FolderOpen,
                                    contentDescription = destination.title,
                                )
                            DesktopDestination.Settings ->
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = destination.title,
                                )
                        }
                    },
                    label = {
                        Text(
                            text = destination.title,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                )
            }
        }
    }
}
