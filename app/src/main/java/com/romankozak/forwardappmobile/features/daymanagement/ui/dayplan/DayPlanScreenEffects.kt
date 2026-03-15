@file:Suppress("MatchingDeclarationName")

package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.SystemUiController
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.navigateOrFallback
import com.romankozak.forwardappmobile.features.missions.presentation.LinkPickerTab
import com.romankozak.forwardappmobile.features.missions.presentation.PickerCreateAction
import com.romankozak.forwardappmobile.ui.common.MatrixRainView
import kotlinx.coroutines.delay

data class DayPlanPresentationState(
    val uiState: DayPlanUiState,
    val dialogState: DayPlanDialogState,
    val overlayState: DayPlanOverlayState,
    val snackbarHostState: androidx.compose.material3.SnackbarHostState,
    val showMatrixSplash: MutableState<Boolean>,
    val matrixView: MutableState<MatrixRainView?>,
    val isContentReady: MutableState<Boolean>,
)

data class DayPlanEffectsConfig(
    val navigator: DayPlanScreenNavigator,
    val initialDayPlanId: String,
    val addTaskTrigger: Int,
    val isLight: Boolean,
    val systemUiController: SystemUiController,
)

@Composable
fun rememberDayPlanPresentationState(viewModel: DayPlanViewModel): DayPlanPresentationState {
    val uiState by viewModel.uiState.collectAsState()
    val isAddTaskDialogOpen by viewModel.isAddTaskDialogOpen.collectAsState()
    val selectedTask by viewModel.selectedTask.collectAsState()
    val isScopeLinksSheetVisible by viewModel.isScopeLinksSheetVisible.collectAsState()
    val connectionsOrder by viewModel.connectionsOrder.collectAsState()
    val taskToDelete by viewModel.showDeleteConfirmationDialog.collectAsState()
    val taskToEdit by viewModel.showEditConfirmationDialog.collectAsState()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }
    val overlayState =
        DayPlanOverlayState(
            showReminderDialog = remember { mutableStateOf(false) },
            activeLinkPickerTab = remember { mutableStateOf<LinkPickerTab?>(null) },
            pendingCreateAction = remember { mutableStateOf<PickerCreateAction?>(null) },
            showAddUrlDialog = remember { mutableStateOf(false) },
            showAddObsidianDialog = remember { mutableStateOf(false) },
        )
    val dialogState =
        DayPlanDialogState(
            isAddTaskDialogOpen = isAddTaskDialogOpen,
            isScopeLinksSheetVisible = isScopeLinksSheetVisible,
            selectedTask = selectedTask,
            taskToDelete = taskToDelete,
            taskToEdit = taskToEdit,
            connectionsOrder = connectionsOrder,
        )
    val showMatrixSplash = remember { mutableStateOf(true) }
    val matrixView = remember { mutableStateOf<MatrixRainView?>(null) }
    val isContentReady = remember { mutableStateOf(false) }

    return DayPlanPresentationState(
        uiState = uiState,
        dialogState = dialogState,
        overlayState = overlayState,
        snackbarHostState = snackbarHostState,
        showMatrixSplash = showMatrixSplash,
        matrixView = matrixView,
        isContentReady = isContentReady,
    )
}

@Composable
fun HandleDayPlanScreenEffects(
    presentationState: DayPlanPresentationState,
    viewModel: DayPlanViewModel,
    config: DayPlanEffectsConfig,
) {
    DisposableEffect(Unit) { onDispose { viewModel.clearSelectedTask() } }

    LaunchedEffect(config.isLight) {
        config.systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = config.isLight,
            isNavigationBarContrastEnforced = false,
        )
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect {
            when (it) {
                is DayPlanUiEvent.NavigateToEditTask -> {
                    config.navigator.navigationManager.navigateOrFallback(
                        navController = config.navigator.navController,
                        target = NavTarget.EditTask(taskId = it.taskId),
                    )
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(PRELOAD_CONTENT_DELAY_MILLIS)
        presentationState.isContentReady.value = true
        delay(MATRIX_SPLASH_VISIBLE_DELAY_MILLIS)
        presentationState.matrixView.value?.startFadeOut()
        delay(MATRIX_SPLASH_FADE_OUT_DELAY_MILLIS)
        presentationState.showMatrixSplash.value = false
    }

    LaunchedEffect(config.addTaskTrigger) {
        if (config.addTaskTrigger > 0) {
            viewModel.openAddTaskDialog()
        }
    }

    LaunchedEffect(presentationState.uiState.error) {
        presentationState.uiState.error?.let { error ->
            presentationState.snackbarHostState.showSnackbar(
                message = error,
                duration = androidx.compose.material3.SnackbarDuration.Short,
            )
            viewModel.dismissError()
        }
    }

    LaunchedEffect(Unit) { viewModel.loadDataForPlan(config.initialDayPlanId) }
}
