package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.theme.LocalInputPanelColors
import com.romankozak.forwardappmobile.features.mainscreen.StrategicArcViewModel
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelGlobalActions
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption

@Composable
fun StrategicArcBottomPanel(
    globalActions: BottomPanelGlobalActions,
    viewModel: StrategicArcViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val panelStyle = LocalInputPanelColors.current.addProjectLog
    var inputValue by remember { mutableStateOf(TextFieldValue("")) }
    var showContextPicker by remember { mutableStateOf(false) }

    fun submitQuest() {
        val title = inputValue.text.trim()
        if (title.isBlank()) return
        viewModel.addArcQuest(title)
        inputValue = TextFieldValue("")
    }

    StrategicArcBottomPanelContent(
        selectedTab = selectedTab,
        inputValue = inputValue,
        onInputChange = { inputValue = it },
        onSubmitQuest = ::submitQuest,
        onSelectTab = viewModel::selectTab,
        onShowContextPicker = { showContextPicker = true },
        onClearInput = { inputValue = TextFieldValue("") },
        globalActions = globalActions,
        panelStyle = panelStyle,
    )

    StrategicArcContextPickerHost(
        visible = showContextPicker,
        contextOptions =
            uiState.allProjects.map {
                ProjectOption(id = it.id, name = it.name, parentId = it.parentId)
            },
        preselectedContextIds = uiState.arcQuests.mapNotNull { it.linkedContextId }.toSet(),
        onDismiss = { showContextPicker = false },
        onContextSelected = { contextId ->
            viewModel.addArcQuestFromContext(contextId)
            showContextPicker = false
        },
        onCreateRootContext = viewModel::createRootContextForPicker,
    )
}
