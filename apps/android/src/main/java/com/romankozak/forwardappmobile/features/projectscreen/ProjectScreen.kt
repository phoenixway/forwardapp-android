package com.romankozak.forwardappmobile.features.projectscreen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.di.LocalAppComponent
import com.romankozak.forwardappmobile.features.projectscreen.components.topbar.AdaptiveTopBar
import com.romankozak.forwardappmobile.features.projectscreen.models.ProjectViewMode
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
import com.romankozak.forwardappmobile.features.projectscreen.components.ProjectViewModePanel

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProjectScreen(
    navController: NavController,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    projectId: String?,
) {
    val appComponent = LocalAppComponent.current
    val viewModel: ProjectScreenViewModel = viewModel(
        factory = appComponent.viewModelFactory
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            AdaptiveTopBar(
                isSelectionModeActive = state.isSelectionModeActive,
                project = state.projectName?.let {
                    Project(
                        id = projectId ?: "",
                        name = it,
                        description = null,
                        parentId = null,
                        createdAt = 0,
                        updatedAt = 0,
                        isCompleted = false,
                        isExpanded = false,
                        goalOrder = 0,
                        projectStatus = null,
                        projectStatusText = null,
                        isProjectManagementEnabled = false,
                        showCheckboxes = false,
                        tags = null
                    )
                },
                selectedCount = state.selectedItemIds.size,
                areAllSelected = false, // TODO: Implement
                onClearSelection = { /* TODO */ },
                onSelectAll = { /* TODO */ },
                onDelete = { /* TODO */ },
                onMarkAsComplete = { /* TODO */ },
                onMarkAsIncomplete = { /* TODO */ },
                onMoreActions = { /* TODO */ },
                onInboxClick = { Toast.makeText(context, "Inbox (не реалізовано)", Toast.LENGTH_SHORT).show() },
                currentViewMode = state.currentView
            )
        },
        bottomBar = {
            ProjectViewModePanel(
                currentMode = state.currentView,
                onModeChange = { newMode -> viewModel.onEvent(UiEvent.SwitchViewMode(newMode)) }
            )
        }
    ) { paddingValues ->
        when (state.currentView) {
            ProjectViewMode.Backlog -> Text(
                text = "Backlog Content for ID: $projectId",
                modifier = Modifier.padding(paddingValues).fillMaxWidth().padding(16.dp)
            )
            ProjectViewMode.Inbox -> Text(
                text = "Inbox Content for ID: $projectId",
                modifier = Modifier.padding(paddingValues).fillMaxWidth().padding(16.dp)
            )
            ProjectViewMode.Advanced -> Text(
                text = "Advanced Content for ID: $projectId",
                modifier = Modifier.padding(paddingValues).fillMaxWidth().padding(16.dp)
            )
            ProjectViewMode.Attachments -> Text(
                text = "Attachments Content for ID: $projectId",
                modifier = Modifier.padding(paddingValues).fillMaxWidth().padding(16.dp)
            )
        }
    }
}
