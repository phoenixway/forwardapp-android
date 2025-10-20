@file:OptIn(ExperimentalMaterial3Api::class)
package com.romankozak.forwardappmobile.ui.screens.goalsettings

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
import com.romankozak.forwardappmobile.ui.screens.common.tabs.EvaluationTabContent
import com.romankozak.forwardappmobile.ui.screens.common.tabs.EvaluationTabUiState
import com.romankozak.forwardappmobile.ui.screens.common.tabs.GeneralTabContent
import com.romankozak.forwardappmobile.ui.screens.common.tabs.RemindersTabContent

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import com.romankozak.forwardappmobile.ui.components.SegmentedTab

@Composable
fun GoalSettingsScreen(
    navController: NavController,
    viewModel: GoalSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        android.util.Log.d("GoalSettingsDebug", "LaunchedEffect started")
        viewModel.events.collect {
            android.util.Log.d("GoalSettingsDebug", "Event received: $it")
            when (it) {
                is com.romankozak.forwardappmobile.ui.screens.projectsettings.ProjectSettingsEvent.NavigateBack -> navController.popBackStack()
                is com.romankozak.forwardappmobile.ui.screens.projectsettings.ProjectSettingsEvent.Navigate -> navController.navigate(it.route)
            }
        }
    }

    val tabs = listOf("General", "Evaluation", "Reminders")
    val tabIcons = listOf(Icons.Default.Settings, Icons.Default.BarChart, Icons.Default.Notifications)
    val titleText = if (uiState.isNewGoal) "New Goal" else "Edit Goal"

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
                titleLabel = "Назва цілі",
                description = uiState.description,
                onDescriptionChange = viewModel::onDescriptionChange,
                onExpandDescriptionClick = viewModel::openDescriptionEditor
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