package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.features.mainscreen.StrategicArcViewModel
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestSourceType
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption

@Composable
fun StrategicArcBottomPanel(
    onOpenArtifact: (String) -> Unit,
    viewModel: StrategicArcViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    var inputValue by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var showContextPicker by remember { mutableStateOf(false) }
    var showBeaconPicker by remember { mutableStateOf(false) }
    var showGroupPicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun submitQuest() {
        val title = inputValue.text.trim()
        if (title.isBlank()) return
        viewModel.addArcQuest(title)
        inputValue = TextFieldValue("")
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    StrategicArcBottomPanelContent(
        selectedTab = selectedTab,
        inputValue = inputValue,
        onInputChange = { inputValue = it },
        onSubmitQuest = ::submitQuest,
        onSelectTab = viewModel::selectTab,
        onShowContextPicker = { showContextPicker = true },
        onClearInput = { inputValue = TextFieldValue("") },
        additionalActions = strategicArcMenuActions(
            ready = !uiState.isLoading && uiState.error == null,
            hasPrevious = uiState.previousArcKey != null,
            onPrevious = viewModel::navigateToPreviousArcMonth,
            onNext = viewModel::navigateToNextArcMonth,
            onBeacon = { showBeaconPicker = true },
            onGroup = { showGroupPicker = true },
            onArtifact = { viewModel.openOrCreateArcArtifact(onOpenArtifact) },
            onLinks = viewModel::toggleScopeLinksSheet,
        ),
    )

    StrategicArcContextPickerHost(
        visible = showContextPicker,
        contextOptions =
            uiState.allProjects.map {
                ProjectOption(id = it.id, name = it.name)
            },
        preselectedContextIds = uiState.arcQuests.mapNotNull { it.linkedContextId }.toSet(),
        onDismiss = { showContextPicker = false },
        onEntitySelected = viewModel::addArcQuestFromEntity,
        onCreateRootContext = viewModel::createRootContextForPicker,
    )
    if (showBeaconPicker) ArcQuestBeaconPickerDialog(
        beacons = uiState.beacons,
        usedSourceIds = uiState.arcQuests.filter { it.sourceType == ArcQuestSourceType.BEACON.name }.mapNotNull { it.sourceId }.toSet(),
        onDismiss = { showBeaconPicker = false },
        onSelected = { viewModel.addArcQuestFromBeacon(it); showBeaconPicker = false },
    )
    if (showGroupPicker) ArcQuestBeaconGroupPickerDialog(
        groups = uiState.beaconGroups,
        usedSourceIds = uiState.arcQuests.filter { it.sourceType == ArcQuestSourceType.BEACON_GROUP.name }.mapNotNull { it.sourceId }.toSet(),
        onDismiss = { showGroupPicker = false },
        onSelected = { viewModel.addArcQuestFromBeaconGroup(it); showGroupPicker = false },
    )
}
