@file:OptIn(ExperimentalMaterial3Api::class)
package com.romankozak.forwardappmobile.ui.screens.projectsettings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.components.SegmentedTab
import com.romankozak.forwardappmobile.ui.navigation.NavTargetRouter
import com.romankozak.forwardappmobile.data.database.models.StructurePreset

@Composable
fun ProjectSettingsScreen(
    navController: NavController,
    viewModel: ProjectSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPresetDialog by remember { mutableStateOf(false) }
    var showPresetPicker by remember { mutableStateOf(false) }

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
                    navController.navigate(NavTargetRouter.routeOf(event.target))
                }
            }
        }
    }

    val tabs = listOf("General", "Display", "Features", "Evaluation", "Reminders")
    val tabIcons = listOf(Icons.Default.Settings, Icons.Default.Style, Icons.Default.Build, Icons.Default.BarChart, Icons.Default.Notifications)
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
                onShowCheckboxesChange = viewModel::onShowCheckboxesChange,
            )
            "Features" -> FeaturesTabContent(
                currentPreset = uiState.currentPresetLabel,
                onNewPreset = {
                    navController.navigate(
                        com.romankozak.forwardappmobile.ui.navigation.NavTargetRouter.routeOf(
                            com.romankozak.forwardappmobile.ui.navigation.NavTarget.StructurePresetEditor()
                        )
                    )
                },
                onApplyPreset = { showPresetPicker = true },
                features = uiState.features,
                onToggleFeature = viewModel::onToggleFeature,
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

    if (showPresetDialog) {
        AddPresetDialog(
            onDismiss = { showPresetDialog = false },
            onConfirm = { code, label, description ->
                // Editor екран обробить створення, діалог не використовується
                showPresetDialog = false
            }
        )
    }

    if (showPresetPicker) {
        PresetChooserDialog(
            presets = uiState.availablePresets,
            onDismiss = { showPresetPicker = false },
            onSelect = { code ->
                viewModel.onApplyPreset(code)
                showPresetPicker = false
            }
        )
    }

    if (uiState.isDescriptionEditorOpen) {
        FullScreenMarkdownEditor(
            initialValue = uiState.description,
            onDismiss = { viewModel.closeDescriptionEditor() },
            onSave = { newText -> viewModel.onDescriptionChangeAndCloseEditor(newText) },
        )
    }
}

@Composable
private fun FeaturesTabContent(
    currentPreset: String?,
    onNewPreset: () -> Unit,
    onApplyPreset: () -> Unit,
    features: Map<String, Boolean>,
    onToggleFeature: (String, Boolean) -> Unit,
) {
    Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Поточний пресет: ${currentPreset ?: "не вибрано"}")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onNewPreset) { Text("New preset") }
            Button(onClick = onApplyPreset) { Text("Apply preset") }
        }
        Text("Фічі", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
        features.toList().forEach { (key, value) ->
            Row(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(key)
                Switch(checked = value, onCheckedChange = { onToggleFeature(key, it) })
            }
        }
    }
}

@Composable
private fun PresetChooserDialog(
    presets: List<StructurePreset>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    if (presets.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
            title = { Text("Немає пресетів") },
            text = { Text("Спершу створіть пресет.") }
        )
        return
    }
    var selected by remember { mutableStateOf(presets.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSelect(selected.code) }) { Text("Застосувати") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
        title = { Text("Оберіть пресет") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { preset ->
                    Row(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { selected = preset },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(preset.label)
                        if (preset.id == selected.id) {
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun AddPresetDialog(
    onDismiss: () -> Unit,
    onConfirm: (code: String, label: String, description: String?) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                if (code.isNotBlank() && label.isNotBlank()) {
                    onConfirm(code, label, description.ifBlank { null })
                }
            }) { Text("Створити") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
        title = { Text("Новий пресет") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code") })
                androidx.compose.material3.OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label") })
                androidx.compose.material3.OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
            }
        }
    )
}
