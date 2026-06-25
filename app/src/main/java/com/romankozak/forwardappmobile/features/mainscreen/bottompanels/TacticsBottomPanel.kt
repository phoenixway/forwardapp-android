package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
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
    val selectedSlotId by viewModel.selectedActivitySlotContextId.collectAsStateWithLifecycle()
    val activitySlotContexts by viewModel.activitySlotContexts.collectAsStateWithLifecycle()
    val inputSuggestionActions = remember { InputSuggestionActions() }
    var inputValue by remember { mutableStateOf(TextFieldValue("")) }
    var showContextPicker by remember { mutableStateOf(false) }
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
        viewModel.addQuickMissionForCurrentSlot(title)
        inputValue = TextFieldValue("")
    }

    val selectedSlotName = activitySlotContexts.firstOrNull { it.id == selectedSlotId }?.name
    val placeholder =
        if (selectedMode == TacticsWorkspaceMode.SLOTS && selectedSlotName != null) {
            "Нова місія для: $selectedSlotName..."
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
}
