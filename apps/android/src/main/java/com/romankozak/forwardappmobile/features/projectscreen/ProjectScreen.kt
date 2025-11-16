package com.romankozak.forwardappmobile.features.projectscreen

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.romankozak.forwardappmobile.di.LocalAppComponent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import com.romankozak.forwardappmobile.features.projectscreen.components.ProjectViewModePanel
import com.romankozak.forwardappmobile.features.projectscreen.models.ProjectViewMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.MoreVert
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight

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
            TopAppBar(
                title = {
                    // TODO: Restore original AdaptiveTopBar title styling
                    Text(
                        text = state.projectName ?: "Project",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // TODO: Restore search functionality
                    IconButton(onClick = { Toast.makeText(context, "Пошук (не реалізовано)", Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Filled.Search, contentDescription = "Пошук")
                    }
                    // TODO: Restore more options menu
                    IconButton(onClick = { Toast.makeText(context, "Більше опцій (не реалізовано)", Toast.LENGTH_SHORT).show() }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Більше опцій")
                    }
                }
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
