package com.romankozak.forwardappmobile.ui.screens.mainscreen

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.routes.navigateToDayManagement
import com.romankozak.forwardappmobile.routes.navigateToStrategicManagement
import com.romankozak.forwardappmobile.ui.components.NewRecentListsSheet
import com.romankozak.forwardappmobile.ui.dialogs.UiContext
import com.romankozak.forwardappmobile.ui.navigation.NavigationHistoryMenu
import com.romankozak.forwardappmobile.ui.screens.mainscreen.OptimizedExpandingProjectHierarchyBottomNav
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectHierarchyScreenEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectHierarchyScreenUiState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectHierarchyScreenSubState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningMode
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectUiEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.HierarchyDisplaySettings
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.FlatHierarchyItem
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.BreadcrumbItem
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.DropPosition
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.SearchResult
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningSettingsState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.AppStatistics
import com.romankozak.forwardappmobile.ui.screens.mainscreen.components.HandleProjectHierarchyDialogs
import com.romankozak.forwardappmobile.ui.shared.SyncDataViewModel
import com.romankozak.forwardappmobile.ui.reminders.dialogs.ReminderPropertiesDialog
import kotlinx.coroutines.flow.collectLatest

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.core.net.toUri
import com.romankozak.forwardappmobile.ui.screens.mainscreen.components.ProjectHierarchyScreenScaffold

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
                is ProjectUiEvent.Navigate -> navController.navigate(event.route)
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
