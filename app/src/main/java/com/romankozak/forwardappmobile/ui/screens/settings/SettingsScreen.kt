package com.romankozak.forwardappmobile.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.ui.ModelsState
import com.romankozak.forwardappmobile.ui.screens.backlogs.PlanningSettingsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    planningSettings: PlanningSettingsState,
    initialVaultName: String,
    reservedContextCount: Int,
    onManageContextsClick: () -> Unit,
    onBack: () -> Unit,
    onSave: (
        showModes: Boolean, dailyTag: String, mediumTag: String, longTag: String,
        vaultName: String
    ) -> Unit,
    // --- ДОДАНО: ViewModel для налаштувань Ollama ---
    viewModel: SettingsViewModel = hiltViewModel()
) {
    // --- Стан для Ollama збирається з ViewModel ---
    val ollamaState by viewModel.uiState.collectAsStateWithLifecycle()

    // --- Існуючий локальний стан для старих налаштувань ---
    var tempShowModes by remember(planningSettings.showModes) { mutableStateOf(planningSettings.showModes) }
    var tempDailyTag by remember(planningSettings.dailyTag) { mutableStateOf(planningSettings.dailyTag) }
    var tempMediumTag by remember(planningSettings.mediumTag) { mutableStateOf(planningSettings.mediumTag) }
    var tempLongTag by remember(planningSettings.longTag) { mutableStateOf(planningSettings.longTag) }
    var tempVaultName by remember(initialVaultName) { mutableStateOf(initialVaultName) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onBack,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) { Text("Cancel") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        // Зберігаємо старі налаштування
                        onSave(
                            tempShowModes,
                            tempDailyTag,
                            tempMediumTag,
                            tempLongTag,
                            tempVaultName
                        )
                        // --- ДОДАНО: Зберігаємо налаштування Ollama ---
                        viewModel.saveSettings()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Save") }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // --- ДОДАНО: Картка налаштувань Ollama ---
            OllamaSettingsCard(
                state = ollamaState,
                onUrlChange = viewModel::onUrlChanged,
                onFetchClick = viewModel::fetchAvailableModels,
                onFastModelSelect = viewModel::onFastModelSelected,
                onSmartModelSelect = viewModel::onSmartModelSelected
            )

            // --- Існуюча картка Planning Modes ---
            SettingsCard(title = "Planning Modes") {
                // ... (без змін)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Show planning scale modes", modifier = Modifier.weight(1f))
                    Switch(checked = tempShowModes, onCheckedChange = { tempShowModes = it })
                }
                AnimatedTextField(
                    value = tempDailyTag,
                    onValueChange = { tempDailyTag = it },
                    label = "Daily Mode Tag",
                    helper = "Tag used for daily planning mode"
                )
                AnimatedTextField(
                    value = tempMediumTag,
                    onValueChange = { tempMediumTag = it },
                    label = "Medium Mode Tag",
                    helper = "Tag used for medium planning mode"
                )
                AnimatedTextField(
                    value = tempLongTag,
                    onValueChange = { tempLongTag = it },
                    label = "Long Mode Tag",
                    helper = "Tag used for long planning mode"
                )
            }

            // --- Існуюча картка Integrations ---
            SettingsCard(title = "Integrations") {
                // ... (без змін)
                Text("Specify the exact name of your Obsidian Vault for link integration.")
                AnimatedTextField(
                    value = tempVaultName,
                    onValueChange = { tempVaultName = it },
                    label = "Obsidian Vault Name",
                    helper = "Exact vault name for link integration",
                    singleLine = true
                )
            }

            // --- Існуюча картка Contexts ---
            SettingsCard(title = "Contexts") {
                // ... (без змін)
                OutlinedButton(
                    onClick = onManageContextsClick,
                    modifier = Modifier.fillMaxWidth(),
                    content = { Text("Manage Reserved Contexts ($reservedContextCount)") },
                    colors = ButtonDefaults.outlinedButtonColors()
                )
            }
        }
    }
}


// --- ДОДАНО: Новий Composable для картки налаштувань Ollama ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OllamaSettingsCard(
    state: SettingsUiState,
    onUrlChange: (String) -> Unit,
    onFetchClick: () -> Unit,
    onFastModelSelect: (String) -> Unit,
    onSmartModelSelect: (String) -> Unit
) {
    SettingsCard(title = "Ollama AI Integration") {
        AnimatedTextField(
            value = state.ollamaUrl,
            onValueChange = onUrlChange,
            label = "Ollama Server URL",
            helper = "e.g., http://192.168.1.5:11434",
            singleLine = true
        )
        OutlinedButton(
            onClick = onFetchClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.ollamaUrl.isNotBlank() && state.modelsState !is ModelsState.Loading
        ) {
            if (state.modelsState is ModelsState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("Fetch Available Models")
            }
        }

        when (val modelsState = state.modelsState) {
            is ModelsState.Error -> {
                Text(modelsState.message, color = MaterialTheme.colorScheme.error)
            }
            is ModelsState.Success -> {
                ModelSelector(
                    label = "Fast Model",
                    selectedValue = state.fastModel,
                    models = modelsState.models,
                    onModelSelected = onFastModelSelect
                )
                ModelSelector(
                    label = "Smart Model",
                    selectedValue = state.smartModel,
                    models = modelsState.models,
                    onModelSelected = onSmartModelSelect
                )
            }
            ModelsState.Loading -> { /* Handled by button state */ }
        }
    }
}

// --- ДОДАНО: Допоміжний Composable для випадаючого списку моделей ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSelector(
    label: String,
    selectedValue: String,
    models: List<String>,
    onModelSelected: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = it },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            models.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model) },
                    onClick = {
                        onModelSelected(model)
                        isExpanded = false
                    }
                )
            }
        }
    }
}


@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun AnimatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    helper: String,
    singleLine: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val backgroundColor by animateColorAsState(
        if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        else Color.Transparent, label = ""
    )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = { Text(helper) },
        singleLine = singleLine,
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .onFocusChanged { isFocused = it.isFocused }
    )
}