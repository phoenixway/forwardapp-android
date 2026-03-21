package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.MutableState
import com.romankozak.forwardappmobile.features.missions.presentation.LinkPickerTab
import com.romankozak.forwardappmobile.features.missions.presentation.PickerCreateAction

data class DayPlanContentState(
    val initialDayPlanId: String,
    val uiState: DayPlanUiState,
    val navigator: DayPlanScreenNavigator,
    val snackbarHostState: SnackbarHostState,
)

data class DayPlanOverlayState(
    val showReminderDialog: MutableState<Boolean>,
    val activeLinkPickerTab: MutableState<LinkPickerTab?>,
    val pendingCreateAction: MutableState<PickerCreateAction?>,
    val showAddUrlDialog: MutableState<Boolean>,
    val showAddObsidianDialog: MutableState<Boolean>,
)

data class DayPlanDialogState(
    val isAddTaskDialogOpen: Boolean,
    val isEditTaskDialogOpen: Boolean,
    val isScopeLinksSheetVisible: Boolean,
    val selectedTask: DayTaskWithReminder?,
    val taskToDelete: DayTaskWithReminder?,
    val taskToEdit: DayTaskWithReminder?,
    val connectionsOrder: List<String>,
)

data class DayPlanVisualState(
    val isContentReady: Boolean,
)
