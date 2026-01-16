package com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.features.navigation.routes.navigateToDayManagement
import com.romankozak.forwardappmobile.features.navigation.routes.navigateToStrategicManagement
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import com.romankozak.forwardappmobile.features.navigation.NavTargetRouter
import kotlinx.coroutines.flow.collectLatest

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.core.net.toUri
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.components.ProjectHierarchyScreenScaffold
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models.ProjectHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.context.ui.context_hierarchy_screen.models.ProjectUiEvent

private const val UI_TAG = "ProjectHierarchyScreenUI_DEBUG"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProjectHierarchyScreen(
    navController: NavController,
    syncDataViewModel: SyncDataViewModel,
    viewModel: ProjectHierarchyScreenViewModel = hiltViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lastOngoingActivity by viewModel.lastOngoingActivity.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusManager = LocalFocusManager.current

    
    LaunchedEffect(Unit) {
        viewModel.uiEventFlow.collectLatest { event ->
            when (event) {
                is ProjectUiEvent.NavigateToSyncScreenWithData -> {
                    syncDataViewModel.jsonString = event.json
                    navController.navigate("sync_screen")
                }
                is ProjectUiEvent.NavigateToDetails -> navController.navigate("goal_detail_screen/${event.projectId}")
                is ProjectUiEvent.ShowToast -> Toast.makeText(navController.context, event.message, Toast.LENGTH_LONG).show()
                is ProjectUiEvent.NavigateToGlobalSearch -> navController.navigate("global_search_screen/${event.query}")
                is ProjectUiEvent.NavigateToSettings -> navController.navigate("settings_screen")
                is ProjectUiEvent.NavigateToEditProjectScreen -> navController.navigate("project_settings_screen?projectId=${event.projectId}")
                is ProjectUiEvent.Navigate -> navController.navigate(NavTargetRouter.routeOf(event.target))
                is ProjectUiEvent.NavigateToDayPlan ->
                    navController.navigateToDayManagement(event.date, event.startTab)
                is ProjectUiEvent.NavigateToStrategicManagement ->
                    navController.navigateToStrategicManagement()
                is ProjectUiEvent.FocusSearchField -> {
                    
                }
                is ProjectUiEvent.HideKeyboard -> {
                    focusManager.clearFocus()
                }
                is ProjectUiEvent.OpenUri -> {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, event.uri.toUri())
                    navController.context.startActivity(intent)
                }
                is ProjectUiEvent.ScrollToIndex -> { }
            }
        }
    }

    
    DisposableEffect(navController, lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    android.util.Log.d("ProjectRevealDebug", "ProjectHierarchyScreen ON_RESUME")
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.remove<String?>("list_chooser_result")
                        ?.let { result ->
                            viewModel.onEvent(ProjectHierarchyScreenEvent.ListChooserResult(result))
                        }

                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.remove<Boolean>("open_search_dialog")
                        ?.let { shouldOpen ->
                            if (shouldOpen == true) {
                                viewModel.onEvent(ProjectHierarchyScreenEvent.ShowSearchDialog)
                            }
                        }

                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.remove<String>("projectIdToReveal")
                        ?.let { projectId ->
                            android.util.Log.d("ProjectRevealDebug", "Retrieved and removed projectIdToReveal: $projectId")
                            android.util.Log.d("ProjectRevealDebug", "Calling RevealProjectInHierarchy event")
                            viewModel.onEvent(ProjectHierarchyScreenEvent.RevealProjectInHierarchy(projectId))
                        }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    viewModel.enhancedNavigationManager?.let { navManager ->
        ProjectHierarchyScreenScaffold(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            enhancedNavigationManager = navManager,
            lastOngoingActivity = lastOngoingActivity,
            viewModel = viewModel,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
        )
    } ?: Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
