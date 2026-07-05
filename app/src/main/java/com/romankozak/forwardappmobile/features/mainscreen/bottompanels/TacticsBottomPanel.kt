package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.InputSuggestionActions
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelGlobalActions
import com.romankozak.forwardappmobile.features.missions.presentation.TacticsWorkspaceMode
import com.romankozak.forwardappmobile.features.missions.presentation.TacticalMissionViewModel

@Composable
fun TacticsBottomPanel(
    globalActions: BottomPanelGlobalActions,
    viewModel: TacticalMissionViewModel = hiltViewModel(),
) {
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val contextMarkerNames by viewModel.contextMarkerNames.collectAsStateWithLifecycle()
    val projectOptions by viewModel.projectOptions.collectAsStateWithLifecycle()
    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val missionStreams by viewModel.missionStreams.collectAsStateWithLifecycle()
    val recentMissionStreams by viewModel.recentMissionStreams.collectAsStateWithLifecycle()
    val selectedMissionStreamId by viewModel.selectedMissionStreamId.collectAsStateWithLifecycle()
    val missionStreamCounts by viewModel.missionStreamCounts.collectAsStateWithLifecycle()
    val iterationDurationDays by viewModel.iterationDurationDays.collectAsStateWithLifecycle()
    val iterationDurationHours by viewModel.iterationDurationHours.collectAsStateWithLifecycle()
    val activitySlotContexts by viewModel.activitySlotContexts.collectAsStateWithLifecycle()
    val selectedPlanningContextId by viewModel.selectedPlanningContextId.collectAsStateWithLifecycle()
    val canPasteAsMissions by viewModel.canPasteAsMissions.collectAsStateWithLifecycle()
    val inputSuggestionActions = remember { InputSuggestionActions() }
    var inputValue by remember { mutableStateOf(TextFieldValue("")) }
    var showContextPicker by remember { mutableStateOf(false) }
    var showIterationDurationDialog by remember { mutableStateOf(false) }
    val autocompleteSuggestions =
        remember(inputValue, allTags, contextMarkerNames) {
            inputSuggestionActions.buildSuggestions(
                currentText = inputValue.text,
                cursorPosition = inputValue.selection.start.coerceAtLeast(0),
                contextMarkerNames = contextMarkerNames,
                tags = allTags,
            )
        }

    fun submitMission() {
        val title = inputValue.text.trim()
        if (title.isBlank()) return
        viewModel.addQuickMissionForCurrentStream(title)
        inputValue = TextFieldValue("")
    }

    val selectedStreamName = missionStreams.firstOrNull { it.id == selectedMissionStreamId }?.title
    val placeholder =
        if (selectedMode == TacticsWorkspaceMode.STREAMS && selectedStreamName != null) {
            "Нова місія в потік: $selectedStreamName..."
        } else {
            "Нова місія тижня..."
    }

    TacticsBottomPanelContent(
        inputValue = inputValue,
        onValueChange = { inputValue = it },
        onSubmit = ::submitMission,
        placeholder = placeholder,
        autocompleteSuggestions = autocompleteSuggestions,
        onSuggestionClick = { suggestion ->
            inputSuggestionActions
                .applySuggestion(
                    currentText = inputValue.text,
                    cursorPosition = inputValue.selection.start.coerceAtLeast(0),
                    suggestion = suggestion,
                )?.let { result ->
                    inputValue =
                        TextFieldValue(
                            text = result.text,
                            selection = TextRange(result.cursorPosition),
                        )
                }
        },
        onAddMissionFromContext = { showContextPicker = true },
        onToggleScopeLinksSheet = viewModel::toggleScopeLinksSheet,
        selectedMode = selectedMode,
        missionStreams = recentMissionStreams,
        allMissionStreams = missionStreams,
        selectedMissionStreamId = selectedMissionStreamId,
        missionStreamCounts = missionStreamCounts,
        iterationDurationDays = iterationDurationDays,
        iterationDurationHours = iterationDurationHours,
        activitySlotContexts = activitySlotContexts,
        selectedPlanningContextId = selectedPlanningContextId,
        projectOptions = projectOptions,
        canPasteAsMissions = canPasteAsMissions,
        onModeSelected = viewModel::selectMode,
        onMissionStreamSelected = viewModel::selectMissionStream,
        onPlanningContextSelected = viewModel::selectPlanningContext,
        onOpenMissionStreamsSheet = viewModel::openMissionStreamsSheet,
        onPasteMissions = viewModel::pasteClipboardAsMissions,
        onSetIterationDuration = { showIterationDurationDialog = true },
        globalActions = globalActions,
    )

    TacticsContextPickerHost(
        visible = showContextPicker,
        projectOptions = projectOptions,
        onDismiss = { showContextPicker = false },
        onContextSelected = { contextId ->
            viewModel.addWeeklyMissionFromContext(contextId)
            showContextPicker = false
        },
    )

    if (showIterationDurationDialog) {
        TacticalIterationDurationDialog(
            currentDays = iterationDurationDays,
            currentHours = iterationDurationHours,
            onDismiss = { showIterationDurationDialog = false },
            onSave = { days, hours ->
                viewModel.setIterationDuration(days, hours)
                showIterationDurationDialog = false
            },
            onClear = {
                viewModel.setIterationDuration(null, null)
                showIterationDurationDialog = false
            },
        )
    }
}

@Composable
private fun TacticalIterationDurationDialog(
    currentDays: Int?,
    currentHours: Int?,
    onDismiss: () -> Unit,
    onSave: (Int, Int?) -> Unit,
    onClear: () -> Unit,
) {
    var daysText by remember(currentDays) { mutableStateOf(currentDays?.toString().orEmpty()) }
    var hoursText by remember(currentHours) { mutableStateOf(currentHours?.toString().orEmpty()) }
    val parsedDays = daysText.trim().toIntOrNull()?.takeIf { it > 0 }
    val parsedHours = hoursText.trim().toIntOrNull()?.takeIf { it > 0 }
    val maxHours = parsedDays?.times(24)
    val isHoursValid = hoursText.isBlank() || (parsedHours != null && maxHours != null && parsedHours <= maxHours)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Тривалість тактичної ітерації") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it.filter(Char::isDigit).take(3) },
                    label = { Text("Днів від сьогодні") },
                    placeholder = { Text("7") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("Дні задають дедлайн тактичної ітерації.") },
                )
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it.filter(Char::isDigit).take(4) },
                    label = { Text("Робочих годин в ітерації") },
                    placeholder = { Text("40") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !isHoursValid,
                    supportingText = {
                        Text(
                            text =
                                if (maxHours == null) {
                                    "Спершу вкажи дні."
                                } else {
                                    "Не більше $maxHours год для $parsedDays дн."
                                },
                        )
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = parsedDays != null && isHoursValid,
                onClick = { parsedDays?.let { onSave(it, parsedHours) } },
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
