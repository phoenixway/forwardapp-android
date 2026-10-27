package com.romankozak.forwardappmobile.features.workspace

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
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
                title = WorkspaceSurfaceCopy.TITLE,
                message = current.message,
                onBack = navController::popBackStack,
            )

        is WorkspaceDestinationUiState.Ready -> {
            val bootstrap: WorkspaceSemanticBootstrapViewModel = hiltViewModel()
            val bootstrapState by bootstrap.state.collectAsStateWithLifecycle()
            LaunchedEffect(bootstrapState.createdId) {
                bootstrap.consumeCreatedDestination()?.let { navigationManager.navigate(it) }
            }
            if (bootstrapState.open) WorkspaceSemanticBootstrapDialog(bootstrapState, bootstrap)
            // ContextScreen's operational read already admits standalone,
            // retired-shell and System Workspaces. A legacy shell is not a
            // prerequisite for this UI; canonical workspaceId owns its data.
            val operationalViewModel: ContextScreenViewModel = hiltViewModel()
            operationalViewModel.enhancedNavigationManager = navigationManager
            ProjectsScreen(
                navController = navController,
                viewModel = operationalViewModel,
                projectId = current.workspaceId,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                workspacePresentationOverride = current.toContextPresentation(),
                canonicalWorkspaceId = current.workspaceId,
                onSemanticBootstrap = { bootstrap.open(current.title, current.description) },
                dashboardSemanticContent = {
                    WorkspaceEntityNavigationHub(
                        onOpen = { navigationManager.navigate(it) },
                        onShowRelations = { operationalViewModel.onProjectViewChange(ContextViewMode.RELATIONS) },
                    )
                },
            )
        }
    }
}

internal fun WorkspaceDestinationUiState.Ready.toContextPresentation() =
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
