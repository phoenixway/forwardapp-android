package com.romankozak.forwardappmobile.features.workspace

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.ContextScreenViewModel
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.ProjectsScreen

/** Preserve legacy route arguments while admitting semantic commands through an exact canonical owner. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LegacyWorkspaceDestinationScreen(
    navController: NavController,
    viewModel: ContextScreenViewModel,
    projectId: String?,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val bootstrap: WorkspaceSemanticBootstrapViewModel = hiltViewModel()
    val state by bootstrap.state.collectAsStateWithLifecycle()
    val workspace by bootstrap.legacyWorkspace.collectAsStateWithLifecycle()
    LaunchedEffect(state.createdId) {
        bootstrap.consumeCreatedDestination()?.let { viewModel.enhancedNavigationManager.navigate(it) }
    }
    if (state.open) WorkspaceSemanticBootstrapDialog(state, bootstrap)
    ProjectsScreen(
        navController = navController,
        viewModel = viewModel,
        projectId = projectId,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope,
        onSemanticBootstrap = workspace?.let { owner ->
            { bootstrap.open(owner.nameOverride?.trim()?.takeIf(String::isNotEmpty) ?: owner.id, owner.descriptionOverride) }
        },
    )
}
