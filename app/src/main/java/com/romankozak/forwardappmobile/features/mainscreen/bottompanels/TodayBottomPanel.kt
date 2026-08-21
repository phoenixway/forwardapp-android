package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.features.activitytracker.ActivityTrackerViewModel
import com.romankozak.forwardappmobile.features.activitytracker.copyToClipboard
import com.romankozak.forwardappmobile.features.activitytracker.exportLogToMarkdown
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.rememberHoldMenu2
import com.romankozak.forwardappmobile.features.daymanagement.runtime.presentation.DayManagementRuntimeUiState
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus.DayFocusesViewModel
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayPlanViewModel
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelGlobalActions

@Composable
@Suppress("LongParameterList", "LongMethod")
fun TodayBottomPanel(
    globalActions: BottomPanelGlobalActions,
    currentTab: DayManagementTab,
    onSelectTodayTab: (DayManagementTab) -> Unit,
    runtimeUiState: DayManagementRuntimeUiState,
    onWakeUp: () -> Unit,
    onFinalizeThemes: () -> Unit,
    onFinalizeFocus: () -> Unit,
    onFinalizePlan: () -> Unit,
    onStartImplementation: () -> Unit,
    onStartFinalization: () -> Unit,
    onSleep: () -> Unit,
    dayPlanViewModel: DayPlanViewModel = hiltViewModel(),
    activityTrackerViewModel: ActivityTrackerViewModel = hiltViewModel(),
    dayFocusesViewModel: DayFocusesViewModel = hiltViewModel(),
) {
    val dayPlanUiState by dayPlanViewModel.uiState.collectAsStateWithLifecycle()
    val allTags by dayPlanViewModel.allTags.collectAsStateWithLifecycle()
    val contextMarkerNames by dayPlanViewModel.contextMarkerNames.collectAsStateWithLifecycle()
    val groupedActivityLog by activityTrackerViewModel.groupedActivityLog.collectAsStateWithLifecycle()
    val activityInputText by activityTrackerViewModel.inputText.collectAsStateWithLifecycle()
    val lastOngoingActivity by activityTrackerViewModel.lastOngoingActivity.collectAsStateWithLifecycle()
    val dayFocusesUiState by dayFocusesViewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    var inputValue by remember { mutableStateOf(TextFieldValue("")) }
    var showContextPicker by remember { mutableStateOf(false) }
    var showClearJournalConfirmDialog by remember { mutableStateOf(false) }
    var showPredictedDurationDialog by remember { mutableStateOf(false) }
    var journalQuickDoneDialogState by remember { mutableStateOf<String?>(null) }
    val journalHoldMenuController = rememberHoldMenu2()

    LaunchedEffect(dayPlanUiState.dayPlan?.id) {
        dayPlanUiState.dayPlan?.id?.let(dayFocusesViewModel::loadDataForPlan)
    }
    val contextOptions =
        remember(dayPlanUiState.availableProjects) {
            buildTodayContextOptions(dayPlanUiState.availableProjects)
        }
    val additionalMoreActions =
        buildTodayAdditionalMoreActions(
            currentTab = currentTab,
            runtimeUiState = runtimeUiState,
            callbacks =
                TodayMoreActionCallbacks(
                    onWakeUp = onWakeUp,
                    onSleep = onSleep,
                    onSetPredictedDayDuration = { showPredictedDurationDialog = true },
                    onFinalizePlan = onFinalizePlan,
                    onFinalizeThemes = onFinalizeThemes,
                    onFinalizeFocus = onFinalizeFocus,
                    onStartImplementation = onStartImplementation,
                    onAddTaskFromContext = { showContextPicker = true },
                    onAddFocus = { dayFocusesViewModel.openCreateDialog(DayFocusType.FOCUS) },
                    onAddResponsibility = {
                        dayFocusesViewModel.openCreateDialog(DayFocusType.RESPONSIBILITY)
                    },
                    onOpenQuickDoneDialog = {
                        if (activityInputText.isNotBlank()) {
                            journalQuickDoneDialogState = activityInputText
                        }
                    },
                    onTimelessRecordClick = activityTrackerViewModel::onTimelessRecordClick,
                    onExportJournalToMarkdown = {
                        val markdown = exportLogToMarkdown(groupedActivityLog.values.flatten())
                        copyToClipboard(context, markdown)
                    },
                    onClearJournal = { showClearJournalConfirmDialog = true },
                ),
        )

    fun submitTask() {
        val submitted =
            submitTodayQuickTask(
                dayPlanViewModel = dayPlanViewModel,
                dayPlanId = dayPlanUiState.dayPlan?.id,
                rawInput = inputValue.text,
            )
        if (submitted) {
            inputValue = TextFieldValue("")
        }
    }

    TodayBottomPanelContent(
        currentTab = currentTab,
        inputValue = inputValue,
        allTags = allTags,
        contextMarkerNames = contextMarkerNames,
        activityInputText = activityInputText,
        isActivityOngoing = lastOngoingActivity != null,
        journalHoldMenuController = journalHoldMenuController,
        globalActions = globalActions,
        additionalMoreActions = additionalMoreActions,
        runtimeUiState = runtimeUiState,
        onInputValueChange = { inputValue = it },
        onActivityTextChange = activityTrackerViewModel::onInputTextChanged,
        onToggleActivityStartStop = activityTrackerViewModel::onToggleStartStop,
        onTimelessRecordClick = activityTrackerViewModel::onTimelessRecordClick,
        onQuickDoneClick = { textValue -> journalQuickDoneDialogState = textValue },
        onDaySummaryClick = activityTrackerViewModel::onAddTodaySummary,
        onSubmitInput = {
            when (currentTab) {
                DayManagementTab.DAY_FOCUSES -> {
                    dayFocusesViewModel.addQuickFocus(inputValue.text)
                    inputValue = TextFieldValue("")
                }
                else -> submitTask()
            }
        },
        onSelectTodayTab = onSelectTodayTab,
        onStartFinalization = onStartFinalization,
        onSleep = onSleep,
    )

    TodayContextPickerHost(
        visible = showContextPicker,
        contextOptions = contextOptions,
        onDismiss = { showContextPicker = false },
        onContextSelected = { contextId ->
            dayPlanViewModel.addTaskFromContext(contextId)
            showContextPicker = false
        },
    )

    TodayJournalDialogsHost(
        showClearJournalConfirmDialog = showClearJournalConfirmDialog,
        quickDonePresetText = journalQuickDoneDialogState,
        showHoldMenuOverlay = currentTab == DayManagementTab.JOURNAL,
        holdMenuController = journalHoldMenuController,
        onDismissClearJournal = { showClearJournalConfirmDialog = false },
        onConfirmClearJournal = {
            activityTrackerViewModel.onClearLogConfirm()
            showClearJournalConfirmDialog = false
        },
        onDismissQuickDone = { journalQuickDoneDialogState = null },
        onConfirmQuickDone = { desc, xp, antyXp ->
            activityTrackerViewModel.onAddCompletedAction(desc, xp, antyXp)
            activityTrackerViewModel.onInputTextChanged("")
            journalQuickDoneDialogState = null
        },
    )

    TodayFocusDialogsHost(
        dayFocusesUiState = dayFocusesUiState,
        dayFocusesViewModel = dayFocusesViewModel,
        predictedDayDurationMinutes = dayPlanUiState.dayPlan?.predictedDurationMinutes,
    )

    if (showPredictedDurationDialog) {
        PredictedDayDurationDialog(
            currentDurationMinutes = dayPlanUiState.dayPlan?.predictedDurationMinutes,
            onDismiss = { showPredictedDurationDialog = false },
            onSave = { minutes ->
                dayPlanViewModel.setPredictedDurationMinutes(minutes)
                showPredictedDurationDialog = false
            },
            onClear = {
                dayPlanViewModel.setPredictedDurationMinutes(null)
                showPredictedDurationDialog = false
            },
        )
    }
}

@Composable
private fun PredictedDayDurationDialog(
    currentDurationMinutes: Long?,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
    onClear: () -> Unit,
) {
    var hoursText by remember(currentDurationMinutes) {
        mutableStateOf(currentDurationMinutes?.let(::formatHoursInput).orEmpty())
    }
    val parsedMinutes = hoursText.toPredictedDurationMinutesOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Прогнозована тривалість дня") },
        text = {
            OutlinedTextField(
                value = hoursText,
                onValueChange = { hoursText = it },
                label = { Text("Години") },
                placeholder = { Text("8 або 8.5") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                supportingText = { Text("Це дає перерахунок бюджетів фокусів у години.") },
            )
        },
        confirmButton = {
            TextButton(
                enabled = parsedMinutes != null,
                onClick = { parsedMinutes?.let(onSave) },
            ) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
            TextButton(onClick = onClear) {
                Text("Очистити")
            }
        },
    )
}

private fun String.toPredictedDurationMinutesOrNull(): Long? {
    val hours = trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (hours <= 0.0) return null
    return (hours * 60.0).toLong().coerceAtLeast(1L)
}

private fun formatHoursInput(minutes: Long): String {
    val hours = minutes / 60.0
    return if (minutes % 60L == 0L) {
        hours.toInt().toString()
    } else {
        String.format(java.util.Locale.getDefault(), "%.1f", hours)
    }
}
