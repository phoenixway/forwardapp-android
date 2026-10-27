package com.romankozak.forwardappmobile.features.workspace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun WorkspaceSendToDialog(workspaceId: String, onDismiss: () -> Unit) {
    val model: WorkspaceSendToViewModel = hiltViewModel(key = "workspace-send:$workspaceId")
    LaunchedEffect(workspaceId) { model.open(workspaceId) }
    val state by model.state.collectAsStateWithLifecycle()
    WorkspaceSendToDialogContent(state, model::send, onDismiss)
}

@Composable
internal fun WorkspaceSendToDialogContent(
    state: WorkspaceSendToState,
    onSelect: (WorkspaceSendTarget) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!state.busy) onDismiss() },
        title = { Text("Send to…") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.title.isNotBlank()) Text(state.title, style = MaterialTheme.typography.bodyMedium)
                WorkspaceSendTarget.entries.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { target ->
                            OutlinedCard(onClick = { if (state.canSend) onSelect(target) }, enabled = state.canSend,
                                modifier = Modifier.weight(1f).heightIn(min = 132.dp)) {
                                Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
                                    Icon(when (target) {
                                        WorkspaceSendTarget.DAY_TASK -> Icons.Outlined.TaskAlt
                                        WorkspaceSendTarget.TACTICAL_MISSION -> Icons.Outlined.RocketLaunch
                                        WorkspaceSendTarget.ARC_QUEST -> Icons.Outlined.Flag
                                    }, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text(target.label, style = MaterialTheme.typography.labelLarge)
                                    Text(target.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                if (state.loading || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.completed?.let { Text("Створено: ${it.label}", color = MaterialTheme.colorScheme.primary) }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = !state.busy, onClick = onDismiss) { Text(if (state.completed != null) "Готово" else "Закрити") } },
    )
}
