package com.romankozak.forwardappmobile.features.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast

@Composable
internal fun WorkspaceNavigationClipboardActions(
    onDismiss: () -> Unit,
    viewModel: WorkspaceOccurrencesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    WorkspaceNavigationClipboardSection(viewModel.workspaceId, state, true, viewModel::copyWorkspace, viewModel::cutOccurrence,
        onPrepared = { message ->
            onDismiss()
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        })
}

/** No Workspace mutation: these buttons prepare the same H1 clipboard as Navigation. */
@Composable
internal fun WorkspaceNavigationClipboardSection(
    workspaceId: String,
    state: WorkspaceOccurrencesState,
    enabled: Boolean,
    onCopy: () -> Boolean,
    onCut: (String) -> Boolean,
    onPrepared: (String) -> Unit = {},
) {
    var choosing by rememberSaveable(workspaceId) { mutableStateOf(false) }
    var message by rememberSaveable(workspaceId) { mutableStateOf<String?>(null) }
    val ready = enabled && !state.loading && state.error == null && state.workspaceAvailable
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Navigation", style = MaterialTheme.typography.titleSmall)
        ClipboardActionRow("Копіювати", "Додати ще одну появу цього Workspace", Icons.Default.ContentCopy, ready) {
            if (onCopy()) {
                val feedback = "Скопійовано. Вставте у Navigation; сам Workspace не дублюється."
                message = feedback
                onPrepared(feedback)
            } else message = "Workspace недоступний"
        }
        ClipboardActionRow("Вирізати", "Вибрати й перемістити появу з нащадками", Icons.Default.ContentCut,
            ready && state.occurrences.isNotEmpty()) { choosing = true }
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        else state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (ready && state.occurrences.isEmpty()) Text("Ще немає появ. Скопіюйте Workspace, щоб додати його в Navigation.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        message?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
    }
    if (choosing) WorkspaceOccurrencePickerDialog(state, "Яку появу перемістити?", { choosing = false }) { occurrence ->
        val success = ready && onCut(occurrence.placementId)
        val feedback = if (success) "Вирізано. Вставте у потрібне місце Navigation." else "Поява вже недоступна"
        message = feedback
        choosing = false
        if (success) onPrepared(feedback)
    }
}

@Composable
private fun ClipboardActionRow(title: String, subtitle: String, icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else .38f))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else .38f))
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else .38f))
            }
        }
    }
}
