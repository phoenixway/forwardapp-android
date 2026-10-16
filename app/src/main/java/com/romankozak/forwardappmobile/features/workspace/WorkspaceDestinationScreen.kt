package com.romankozak.forwardappmobile.features.workspace

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.ContextScreenViewModel
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.ProjectsScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun WorkspaceDestinationScreen(
    navController: NavController,
    navigationManager: EnhancedNavigationManager,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: WorkspaceDestinationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (val current = state) {
        WorkspaceDestinationUiState.Loading ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

        is WorkspaceDestinationUiState.Unavailable ->
            WorkspaceDestinationMessage(
                title = "Робочий простір",
                message = current.message,
                onBack = navController::popBackStack,
            )

        is WorkspaceDestinationUiState.Ready -> {
            if (current.compatibilityContextId != null) {
                val compatibilityViewModel: ContextScreenViewModel = hiltViewModel()
                compatibilityViewModel.enhancedNavigationManager = navigationManager
                ProjectsScreen(
                    navController = navController,
                    viewModel = compatibilityViewModel,
                    projectId = current.workspaceId,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    workspacePresentationOverride = current.toContextPresentation(),
                    canonicalWorkspaceId = current.workspaceId,
                    linkedSemanticContent = {
                        WorkspaceLinkedSemanticsSection(
                            onOpen = { navigationManager.navigate(it) },
                        )
                    },
                )
            } else {
                WorkspaceOnlyDestination(
                    state = current,
                    onBack = navController::popBackStack,
                    onProperties = {
                        navigationManager.navigate(NavTarget.WorkspaceProperties(current.workspaceId))
                    },
                    linkedSemanticContent = {
                        WorkspaceLinkedSemanticsSection(
                            onOpen = { navigationManager.navigate(it) },
                        )
                    },
                )
            }
        }
    }
}

private fun WorkspaceDestinationUiState.Ready.toContextPresentation() =
    ContextPresentation(
        id = workspaceId,
        name = title,
        description = description,
        parentId = null,
        roleCode = roleCode,
        order = 0L,
        tags = null,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkspaceOnlyDestination(
    state: WorkspaceDestinationUiState.Ready,
    onBack: () -> Unit,
    onProperties: () -> Unit,
    linkedSemanticContent: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = onProperties) {
                        Icon(Icons.Default.Settings, contentDescription = "Властивості Workspace")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Робочий простір", style = MaterialTheme.typography.labelLarge)
            state.description?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
            state.roleCode?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            linkedSemanticContent()
            Text(
                "Для цього робочого простору операційні розділи ще недоступні.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkspaceDestinationMessage(
    title: String,
    message: String,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text(message)
        }
    }
}
