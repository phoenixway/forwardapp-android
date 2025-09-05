package com.romankozak.forwardappmobile.ui.screens.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.ui.ModelsState
import com.romankozak.forwardappmobile.ui.screens.backlogs.PlanningSettingsState

fun getFileName(uri: Uri, context: Context): String {
    var fileName: String? = null
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
    } catch (e: Exception) {
        // Fallback in case of error
        fileName = uri.lastPathSegment
    }
    return fileName ?: "Unknown file"
}

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
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                        onSave(
                            tempShowModes,
                            tempDailyTag,
                            tempMediumTag,
                            tempLongTag,
                            tempVaultName
                        )
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

            OllamaSettingsCard(
                state = uiState,
                onUrlChange = viewModel::onUrlChanged,
                onFetchClick = viewModel::fetchAvailableModels,
                onFastModelSelect = viewModel::onFastModelSelected,
                onSmartModelSelect = viewModel::onSmartModelSelected
            )

            NerSettingsCard(
                state = uiState,
                onModelFileSelected = { uri -> viewModel.onNerModelFileSelected(uri) },
                onTokenizerFileSelected = { uri -> viewModel.onNerTokenizerFileSelected(uri) },
                onLabelsFileSelected = { uri -> viewModel.onNerLabelsFileSelected(uri) }
            )

            SettingsCard(title = "Planning Modes") {
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

            SettingsCard(title = "Integrations") {
                Text("Specify the exact name of your Obsidian Vault for link integration.")
                AnimatedTextField(
                    value = tempVaultName,
                    onValueChange = { tempVaultName = it },
                    label = "Obsidian Vault Name",
                    helper = "Exact vault name for link integration",
                    singleLine = true
                )
            }

            SettingsCard(title = "Contexts") {
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

@Composable
private fun NerSettingsCard(
    state: SettingsUiState,
    onModelFileSelected: (String) -> Unit,
    onTokenizerFileSelected: (String) -> Unit,
    onLabelsFileSelected: (String) -> Unit
) {
    val context = LocalContext.current

    val modelLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { onModelFileSelected(it.toString()) }
    }
    val tokenizerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { onTokenizerFileSelected(it.toString()) }
    }
    val labelsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { onLabelsFileSelected(it.toString()) }
    }

    SettingsCard(title = "Date/Time NER Model (ONNX)") {
        FileSelector(
            label = "Model File (.onnx)",
            selectedFileUri = state.nerModelUri,
            onSelectClick = { modelLauncher.launch("*/*") },
            context = context
        )
        FileSelector(
            label = "Tokenizer File (.json)",
            selectedFileUri = state.nerTokenizerUri,
            onSelectClick = { tokenizerLauncher.launch("application/json") },
            context = context
        )
        FileSelector(
            label = "Labels File (.json)",
            selectedFileUri = state.nerLabelsUri,
            onSelectClick = { labelsLauncher.launch("application/json") },
            context = context
        )
    }
}

@Composable
private fun FileSelector(
    label: String,
    selectedFileUri: String,
    onSelectClick: () -> Unit,
    context: Context
) {
    val fileName = remember(selectedFileUri) {
        if (selectedFileUri.isNotBlank()) getFileName(Uri.parse(selectedFileUri), context) else "Not selected"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                fileName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(onClick = onSelectClick) {
            Text("Select")
        }
    }
}


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
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
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