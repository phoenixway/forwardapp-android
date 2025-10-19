@file:OptIn(ExperimentalMaterial3Api::class)
package com.romankozak.forwardappmobile.ui.screens.projectsettings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.ui.components.notesEditors.FullScreenMarkdownEditor
import com.romankozak.forwardappmobile.ui.screens.common.SettingsScreen
import com.romankozak.forwardappmobile.ui.screens.common.tabs.DisplayTabContent
import com.romankozak.forwardappmobile.ui.screens.common.tabs.EvaluationTabContent
import com.romankozak.forwardappmobile.ui.screens.common.tabs.EvaluationTabUiState
import com.romankozak.forwardappmobile.ui.screens.common.tabs.GeneralTabContent
import com.romankozak.forwardappmobile.ui.screens.common.tabs.RemindersTabContent

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import com.romankozak.forwardappmobile.ui.components.SegmentedTab

@Composable
fun ProjectSettingsScreen(
    navController: NavController,
    viewModel: ProjectSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        viewModel.events.collect { event ->
            when (event) {
                is ProjectSettingsEvent.NavigateBack -> {
                    event.message?.let {
                        android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
                    }
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("refresh_needed", true)
                    navController.popBackStack()
                }
                is ProjectSettingsEvent.Navigate -> {
                    navController.navigate(event.route)
                }
            }
        }
    }

    val tabs = listOf("General", "Display", "Evaluation", "Reminders")
    val tabIcons = listOf(Icons.Default.Settings, Icons.Default.Style, Icons.Default.BarChart, Icons.Default.Notifications)
    val titleText = if (uiState.isNewProject) "New Project" else "Edit Project"

    SettingsScreen(
        title = titleText,
        navController = navController,
        tabs = tabs,
        tabIcons = tabIcons,
        selectedTabIndex = uiState.selectedTabIndex,
        onTabSelected = viewModel::onTabSelected,
        onSave = viewModel::onSave,
        isSaveEnabled = uiState.title.text.isNotBlank()
    ) {
        when (tabs[it]) {
            "General" -> GeneralTabContent(
                title = uiState.title,
                onTitleChange = viewModel::onTextChange,
                titleLabel = "Назва проекту",
                description = uiState.description,
                onDescriptionChange = viewModel::onDescriptionChange,
                onExpandDescriptionClick = viewModel::openDescriptionEditor,
                tags = uiState.tags,
                onAddTag = viewModel::onAddTag,
                onRemoveTag = viewModel::onRemoveTag
            )
            "Display" -> DisplayTabContent(
                showCheckboxes = uiState.showCheckboxes,
                onShowCheckboxesChange = viewModel::onShowCheckboxesChange
            )
            "Evaluation" -> EvaluationTabContent(
                uiState = EvaluationTabUiState(
                    valueImportance = uiState.valueImportance,
                    valueImpact = uiState.valueImpact,
                    effort = uiState.effort,
                    cost = uiState.cost,
                    risk = uiState.risk,
                    weightEffort = uiState.weightEffort,
                    weightCost = uiState.weightCost,
                    weightRisk = uiState.weightRisk,
                    rawScore = uiState.rawScore,
                    scoringStatus = uiState.scoringStatus,
                    isScoringEnabled = uiState.isScoringEnabled,
                ),
                onViewModelAction = viewModel
            )
            "Reminders" -> RemindersTabContent(
                reminderTime = uiState.reminderTime,
                onViewModelAction = viewModel
            )
        }
    }

    if (uiState.isDescriptionEditorOpen) {
        FullScreenMarkdownEditor(
            initialValue = uiState.description,
            onDismiss = { viewModel.closeDescriptionEditor() },
            onSave = { newText -> viewModel.onDescriptionChangeAndCloseEditor(newText) },
        )
    }
}
