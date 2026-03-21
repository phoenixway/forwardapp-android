@file:Suppress("MatchingDeclarationName")

package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.google.accompanist.systemuicontroller.SystemUiController
import com.romankozak.forwardappmobile.features.missions.presentation.LinkPickerTab
import com.romankozak.forwardappmobile.features.missions.presentation.PickerCreateAction

data class DayPlanPresentationState(
    val uiState: DayPlanUiState,
    val dialogState: DayPlanDialogState,
    val overlayState: DayPlanOverlayState,
    val snackbarHostState: androidx.compose.material3.SnackbarHostState,
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
    val isEditTaskDialogOpen by viewModel.isEditTaskDialogOpen.collectAsState()
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
            isEditTaskDialogOpen = isEditTaskDialogOpen,
            isScopeLinksSheetVisible = isScopeLinksSheetVisible,
            selectedTask = selectedTask,
            taskToDelete = taskToDelete,
            taskToEdit = taskToEdit,
            connectionsOrder = connectionsOrder,
        )
    val isContentReady = remember { mutableStateOf(true) }

    return DayPlanPresentationState(
        uiState = uiState,
        dialogState = dialogState,
        overlayState = overlayState,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun HandleDayPlanScreenEffects(
    presentationState: DayPlanPresentationState,
    viewModel: DayPlanViewModel,
    config: DayPlanEffectsConfig,
) {
    DisposableEffect(Unit) {
        onDispose {
            viewModel.dismissEditTaskDialog()
            viewModel.clearSelectedTask()
        }
    }

    LaunchedEffect(config.isLight) {
        config.systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = config.isLight,
            isNavigationBarContrastEnforced = false,
        )
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
