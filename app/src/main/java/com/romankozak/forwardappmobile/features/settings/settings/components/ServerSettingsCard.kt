package com.romankozak.forwardappmobile.features.settings.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.settings.settings.SettingsUiState
import com.romankozak.forwardappmobile.ui.ModelsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSettingsCard(
    state: SettingsUiState,
    onIpChange: (String) -> Unit,
    onFetchClick: () -> Unit,
    onFastModelSelect: (String) -> Unit,
    onSmartModelSelect: (String) -> Unit,
    onNumCtxChange: (String) -> Unit,
    onNumPredictChange: (String) -> Unit,
    onNumBatchChange: (String) -> Unit,
    onNumGpuChange: (String) -> Unit,
    onNumThreadChange: (String) -> Unit,
    onAutoCompressChange: (Boolean) -> Unit,
    onCompressThresholdChange: (String) -> Unit,
) {
    SettingsCard(
        title = "Ollama AI Integration",
        icon = Icons.Default.Dns,
    ) {
        AnimatedTextField(
            value = state.manualServerIp,
            onValueChange = onIpChange,
            label = "Ollama Server URL",
            helper = "e.g., http://192.168.1.5:11434",
            singleLine = true,
        )
        OutlinedButton(
            onClick = onFetchClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.manualServerIp.isNotBlank() && state.modelsState !is ModelsState.Loading,
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
                    onModelSelected = onFastModelSelect,
                )
                ModelSelector(
                    label = "Smart Model",
                    selectedValue = state.smartModel,
                    models = modelsState.models,
                    onModelSelected = onSmartModelSelect,
                )
            }
            ModelsState.Loading -> { /* Handled by button state */ }
        }

        Text(
            text = "Advanced Ollama",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        AnimatedTextField(
            value = state.ollamaNumCtx.toString(),
            onValueChange = onNumCtxChange,
            label = "Context window (num_ctx)",
            helper = "Higher values need more VRAM/RAM. Current default: 8192",
            singleLine = true,
        )
        AnimatedTextField(
            value = state.ollamaNumPredict.toString(),
            onValueChange = onNumPredictChange,
            label = "Response limit (num_predict)",
            helper = "-1 means no app-side generation limit",
            singleLine = true,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnimatedTextField(
                value = state.ollamaNumBatch.toString(),
                onValueChange = onNumBatchChange,
                label = "Batch",
                helper = "0 = omit",
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            AnimatedTextField(
                value = state.ollamaNumGpu.toString(),
                onValueChange = onNumGpuChange,
                label = "GPU",
                helper = "-1 = auto",
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        AnimatedTextField(
            value = state.ollamaNumThread.toString(),
            onValueChange = onNumThreadChange,
            label = "CPU threads",
            helper = "0 = omit / Ollama default",
            singleLine = true,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto-compress chat context", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Summarize older messages before context window is exhausted",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = state.ollamaAutoCompressContext,
                onCheckedChange = onAutoCompressChange,
            )
        }
        AnimatedTextField(
            value = state.ollamaCompressThresholdPercent.toString(),
            onValueChange = onCompressThresholdChange,
            label = "Compression threshold (%)",
            helper = "When estimated prompt size exceeds this part of num_ctx",
            singleLine = true,
            enabled = state.ollamaAutoCompressContext,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSelector(
    label: String,
    selectedValue: String,
    models: List<String>,
    onModelSelected: (String) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier =
                Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            models.forEach { model ->
                DropdownMenuItem(
                    text = { Text(model) },
                    colors =
                        MenuDefaults.itemColors(
                            textColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    onClick = {
                        onModelSelected(model)
                        isExpanded = false
                    },
                )
            }
        }
    }
}
