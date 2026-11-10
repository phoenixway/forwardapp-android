package com.romankozak.forwardappmobile.features.attachments.specifictypes.checklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.features.attachments.data.ChecklistUsageRow

internal fun ChecklistUsageRow.destination(): NavTarget = when (destinationKind) {
    "WORKSPACE" -> NavTarget.Workspace(destinationId)
    "PARTICIPATION" -> NavTarget.PlanningParticipation(destinationId)
    "SEMANTIC" -> NavTarget.SemanticEntityEditor(destinationId)
    "TACTICS" -> NavTarget.TacticalManagement
    else -> error("Unsupported checklist usage destination: $destinationKind")
}

@Composable
internal fun ChecklistUsagesDialog(
    checklistId: String,
    onDismiss: () -> Unit,
    onOpen: (NavTarget) -> Unit,
    viewModel: ChecklistUsagesViewModel = hiltViewModel(),
) {
    val state by remember(checklistId, viewModel) { viewModel.observe(checklistId) }
        .collectAsStateWithLifecycle(initialValue = ChecklistUsagesState())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.checklist_usages)) },
        text = {
            when {
                state.loading -> CircularProgressIndicator(Modifier.size(24.dp))
                state.failed -> Text(stringResource(R.string.checklist_usages_error))
                state.usages.isEmpty() -> Text(stringResource(R.string.checklist_usages_empty))
                else -> LazyColumn(Modifier.fillMaxWidth().heightIn(max = 440.dp)) {
                    items(state.usages, key = { "${it.kind}:${it.ownerId}" }) { usage ->
                        ListItem(
                            headlineContent = { Text(usage.title) },
                            supportingContent = { Text(stringResource(when (usage.kind) {
                                "WORKSPACE" -> R.string.checklist_usage_workspace
                                "PROBLEM" -> R.string.checklist_usage_problem
                                "BEACON" -> R.string.checklist_usage_beacon
                                else -> if (usage.destinationKind == "TACTICS") R.string.checklist_usage_legacy_mission else R.string.checklist_usage_mission
                            })) },
                            trailingContent = { Icon(Icons.Outlined.ChevronRight, null) },
                            modifier = Modifier.clickable { onDismiss(); onOpen(usage.destination()) },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.deck_close)) } },
    )
}
