package com.romankozak.forwardappmobile.ui.screens.settings.components

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.domain.ner.NerState
import com.romankozak.forwardappmobile.ui.screens.settings.SettingsUiState
import com.romankozak.forwardappmobile.ui.screens.settings.components.SettingsCard

import com.romankozak.forwardappmobile.ui.screens.settings.components.FileSelector

@Composable
fun NerSettingsCard(
    state: SettingsUiState,
    onModelFileSelected: (String) -> Unit,
    onTokenizerFileSelected: (String) -> Unit,
    onLabelsFileSelected: (String) -> Unit,
    onReloadClick: () -> Unit,
) {
    val context = LocalContext.current
    val areAllFilesSelected = state.nerModelUri.isNotBlank() && state.nerTokenizerUri.isNotBlank() && state.nerLabelsUri.isNotBlank()

    val modelLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                onModelFileSelected(it.toString())
            } catch (e: SecurityException) {
                Log.e("NerSettings", "Failed to take persistable permission for model file. The user might see errors later.", e)
            }
        }
    }

    val tokenizerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                onTokenizerFileSelected(it.toString())
            } catch (e: SecurityException) {
                Log.e("NerSettings", "Failed to take persistable permission for tokenizer file.", e)
            }
        }
    }

    val labelsLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                onLabelsFileSelected(it.toString())
            } catch (e: SecurityException) {
                Log.e("NerSettings", "Failed to take persistable permission for labels file.", e)
            }
        }
    }

    SettingsCard(
        title = "Date/Time NER Model (ONNX)",
        icon = Icons.Default.Memory,
    ) {
        FileSelector(
            label = "Model File (.onnx)",
            selectedFileUri = state.nerModelUri,
            onSelectClick = { modelLauncher.launch(arrayOf("*/*", "application/octet-stream")) },
            context = context,
        )
        FileSelector(
            label = "Tokenizer File (.json)",
            selectedFileUri = state.nerTokenizerUri,
            onSelectClick = { tokenizerLauncher.launch(arrayOf("application/json", "text/plain")) },
            context = context,
        )
        FileSelector(
            label = "Labels File (.json)",
            selectedFileUri = state.nerLabelsUri,
            onSelectClick = { labelsLauncher.launch(arrayOf("application/json", "text/plain")) },
            context = context,
        )
        Spacer(modifier = Modifier.height(12.dp))
        NerStatusIndicator(
            nerState = state.nerState,
            areAllFilesSelected = areAllFilesSelected,
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onReloadClick,
            enabled = areAllFilesSelected && state.nerState !is NerState.Downloading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(imageVector = Icons.Default.Sync, contentDescription = "Reload", modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(if (state.nerState is NerState.Error) "Try Again" else "Reload Model")
        }
    }
}

@Composable
fun NerStatusIndicator(nerState: NerState, areAllFilesSelected: Boolean) {
    val (icon, color, text) = when (nerState) {
        is NerState.Downloading -> Triple(Icons.Default.Sync, MaterialTheme.colorScheme.primary, "Loading: ${nerState.progress}%")
        is NerState.Error -> Triple(Icons.Default.Error, MaterialTheme.colorScheme.error, "Error: ${nerState.message}")
        NerState.NotInitialized -> {
            val message = if (areAllFilesSelected) "Model not loaded. Press 'Reload Model'." else "Select all three model files"
            Triple(Icons.Default.Info, MaterialTheme.colorScheme.onSurfaceVariant, message)
        }
        NerState.Ready -> Triple(Icons.Default.CheckCircle, Color(0xFF388E3C), "Model loaded successfully")
    }

    val animatedColor by animateColorAsState(targetValue = color, label = "ner_status_color")

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            Icon(imageVector = icon, contentDescription = "Status", tint = animatedColor)
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = animatedColor, modifier = Modifier.weight(1f))
        }
        if (nerState is NerState.Downloading) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(progress = { nerState.progress / 100f }, modifier = Modifier.fillMaxWidth())
        }
    }
}
