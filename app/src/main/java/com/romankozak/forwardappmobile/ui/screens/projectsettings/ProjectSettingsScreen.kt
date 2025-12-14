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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
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
            },
            onEditPresets = {
                showPresetPicker = false
                navController.navigate(
                    com.romankozak.forwardappmobile.ui.navigation.NavTargetRouter.routeOf(
                        com.romankozak.forwardappmobile.ui.navigation.NavTarget.StructurePresets
                    )
                )
            },
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
    onApplyPreset: () -> Unit,
    features: Map<String, Boolean>,
    onToggleFeature: (String, Boolean) -> Unit,
) {
    Column(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PresetCard(currentPreset = currentPreset, onApplyPreset = onApplyPreset)
        FeatureFlagCard(
            modifier = Modifier.fillMaxHeight(),
            features = features,
            onToggleFeature = onToggleFeature
        )
    }
}

@Composable
private fun PresetCard(
    currentPreset: String?,
    onApplyPreset: () -> Unit,
) {
    Surface(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Structural preset", style = MaterialTheme.typography.titleMedium)
            Text(
                currentPreset ?: "Не вибрано",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onApplyPreset,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Apply preset", modifier = androidx.compose.ui.Modifier.size(18.dp))
                    Spacer(modifier = androidx.compose.ui.Modifier.width(6.dp))
                    Text("Choose", maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun FeatureFlagCard(
    modifier: Modifier = Modifier,
    features: Map<String, Boolean>,
    onToggleFeature: (String, Boolean) -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
    ) {
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Feature toggles", style = MaterialTheme.typography.titleMedium)
            Column(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                features.toList().forEach { (key, value) ->
                    Surface(
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                    ) {
        Row(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(key)
                            Switch(checked = value, onCheckedChange = { onToggleFeature(key, it) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChooserDialog(
    presets: List<StructurePreset>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onEditPresets: () -> Unit,
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
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Скасувати", maxLines = 1) }
        },
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
                HorizontalDivider(modifier = androidx.compose.ui.Modifier.padding(top = 6.dp, bottom = 4.dp))
                Row(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onEditPresets,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = androidx.compose.ui.Modifier.size(18.dp))
                        Spacer(modifier = androidx.compose.ui.Modifier.width(6.dp))
                        Text("Редагувати пресети", maxLines = 1)
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
