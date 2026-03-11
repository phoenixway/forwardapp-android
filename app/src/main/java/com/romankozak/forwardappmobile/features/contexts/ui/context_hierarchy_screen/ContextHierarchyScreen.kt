package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.navigation.EnhancedNavigationManager
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.navigateOrFallback
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.components.ProjectHierarchyScreenScaffold
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ContextHierarchyScreenEvent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import kotlinx.coroutines.flow.collectLatest
import java.net.URLEncoder

private const val UI_TAG = "ProjectHierarchyScreenUI_DEBUG"

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProjectHierarchyScreen(
    navController: NavController,
    syncDataViewModel: SyncDataViewModel,
    viewModel: ContextHierarchyScreenViewModel = hiltViewModel(),
    navigationManager: EnhancedNavigationManager? = null,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lastOngoingActivity by viewModel.lastOngoingActivity.collectAsStateWithLifecycle()
    val focusedContextIds by viewModel.focusedContextIds.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        viewModel.uiEventFlow.collectLatest { event ->
            when (event) {
                is ProjectUiEvent.NavigateToSyncScreenWithData -> {
                    syncDataViewModel.jsonString = event.json
                    navigationManager.navigateOrFallback(
                        navController = navController,
                        target = NavTarget.Sync,
                    )
                }
                is ProjectUiEvent.NavigateToDetails ->
                    navigationManager.navigateOrFallback(
                        navController = navController,
                        target = NavTarget.ContextDetail(contextId = event.projectId),
                        recordInHistory = true,
                    )
                is ProjectUiEvent.ShowToast -> Toast.makeText(navController.context, event.message, Toast.LENGTH_LONG).show()
                is ProjectUiEvent.NavigateToGlobalSearch -> {
                    val encoded = URLEncoder.encode(event.query, "UTF-8")
                    navigationManager.navigateOrFallback(
                        navController = navController,
                        target = NavTarget.GlobalSearch(query = encoded),
                        recordInHistory = true,
                    )
                }
                is ProjectUiEvent.NavigateToSettings ->
                    navigationManager.navigateOrFallback(
                        navController = navController,
                        target = NavTarget.Settings,
                    )
                is ProjectUiEvent.NavigateToEditProjectScreen ->
                    navigationManager.navigateOrFallback(
                        navController = navController,
                        target = NavTarget.ProjectSettings(projectId = event.projectId),
                    )
                is ProjectUiEvent.Navigate ->
                    navigationManager.navigateOrFallback(
                        navController = navController,
                        target = event.target,
                    )
                is ProjectUiEvent.NavigateToDayPlan ->
                    navigationManager.navigateOrFallback(
                        navController = navController,
                        target = NavTarget.DayManagement(date = event.date, startTab = event.startTab),
                    )
                is ProjectUiEvent.NavigateToStrategicManagement ->
                    navigationManager.navigateOrFallback(
                        navController = navController,
                        target = NavTarget.StrategicManagement,
                    )
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
                            viewModel.onEvent(ContextHierarchyScreenEvent.ListChooserResult(result))
                        }

                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.remove<Boolean>("open_search_dialog")
                        ?.let { shouldOpen ->
                            if (shouldOpen == true) {
                                viewModel.onEvent(ContextHierarchyScreenEvent.ShowSearchDialog)
                            }
                        }

                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.remove<String>("projectIdToReveal")
                        ?.let { projectId ->
                            android.util.Log.d("ProjectRevealDebug", "Retrieved and removed projectIdToReveal: $projectId")
                            android.util.Log.d("ProjectRevealDebug", "Calling RevealProjectInHierarchy event")
                            viewModel.onEvent(
                                ContextHierarchyScreenEvent.RevealContextInHierarchy(
                                    projectId = projectId,
                                ),
                            )
                        }
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    viewModel.enhancedNavigationManager?.let { navManager ->
        ProjectHierarchyScreenScaffold(
            uiState = uiState,
            focusedContextIds = focusedContextIds,
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
