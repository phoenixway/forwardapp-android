package com.romankozak.forwardappmobile.features.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.Role
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
@OptIn(ExperimentalLayoutApi::class)
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
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = ((maxWidth + 16.dp) / 116.dp).toInt().coerceAtLeast(1)
            val tileWidth = (maxWidth - 16.dp * (columns - 1)) / columns
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)) {
                ClipboardActionTile("Копіювати", Icons.Default.ContentCopy, ready, Modifier.width(tileWidth)) {
                    if (onCopy()) {
                        val feedback = "Скопійовано. Вставте у Navigation; сам Workspace не дублюється."
                        message = feedback
                        onPrepared(feedback)
                    } else message = "Workspace недоступний"
                }
                ClipboardActionTile("Вирізати", Icons.Default.ContentCut,
                    ready && state.occurrences.isNotEmpty(), Modifier.width(tileWidth)) { choosing = true }
            }
        }
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
private fun ClipboardActionTile(
    title: String,
    icon: ImageVector,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else .38f)
    Column(
        modifier = modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, Modifier.size(24.dp), tint = tint)
        Spacer(Modifier.height(8.dp))
        Text(title, textAlign = TextAlign.Center, color = tint, fontSize = 12.sp, lineHeight = 14.sp)
    }
}
