package com.romankozak.forwardappmobile.ui.screens.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.ModelsState
import com.romankozak.forwardappmobile.ui.screens.settings.SettingsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSettingsCard(
    state: SettingsUiState,
    onIpConfigModeChange: (Boolean) -> Unit,
    onIpChange: (String) -> Unit,
    onOllamaPortChange: (String) -> Unit,
    onWifiSyncPortChange: (String) -> Unit,
    onFastApiPortChange: (String) -> Unit,
    onFetchClick: () -> Unit,
    onFastModelSelect: (String) -> Unit,
    onSmartModelSelect: (String) -> Unit,
    onRefreshDiscovery: () -> Unit
) {
    SettingsCard(
        title = "Ollama AI Integration",
        icon = Icons.Default.Dns
    ) {
        AnimatedTextField(
            value = state.manualServerIp,
            onValueChange = onIpChange,
            label = "Ollama Server URL",
            helper = "e.g., http://192.168.1.5:11434",
            singleLine = true
        )
        OutlinedButton(
            onClick = onFetchClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.manualServerIp.isNotBlank() && state.modelsState !is ModelsState.Loading
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