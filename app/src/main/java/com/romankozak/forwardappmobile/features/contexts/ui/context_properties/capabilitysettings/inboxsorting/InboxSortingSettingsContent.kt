package com.romankozak.forwardappmobile.features.contexts.ui.context_properties.capabilitysettings.inboxsorting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.data.repository.InboxSortingService

@Composable
fun InboxSortingSettingsContent(
    contextId: String,
    viewModel: InboxSortingSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(contextId) {
        viewModel.bind(contextId)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InboxSortingHeader()
        RulesEditor(
            rulesText = uiState.rulesText,
            onRulesTextChanged = viewModel::onRulesTextChanged,
        )
        SaveRulesAction(
            enabled = !uiState.isSaving && !uiState.isApplying,
            onSave = viewModel::saveRules,
        )
        SortActions(
            enabled = !uiState.isSaving && !uiState.isApplying,
            onApplySort = viewModel::applySort,
        )

        uiState.lastMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun InboxSortingHeader() {
    Text(
        text = "Правила сортування",
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        text = "Формат: backlog:newest|oldest, inbox:newest|oldest|alpha, connections:newest|oldest|type|alpha",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RulesEditor(
    rulesText: String,
    onRulesTextChanged: (String) -> Unit,
) {
    OutlinedTextField(
        value = rulesText,
        onValueChange = onRulesTextChanged,
        modifier = Modifier.fillMaxWidth(),
        minLines = 6,
        label = { Text("Rules") },
    )
}

@Composable
private fun SaveRulesAction(
    enabled: Boolean,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = onSave, enabled = enabled) {
            Text("Зберегти")
        }
    }
}

@Composable
private fun SortActions(
    enabled: Boolean,
    onApplySort: (InboxSortingService.SortTarget) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SortActionButton(
            label = "Sort Backlog",
            target = InboxSortingService.SortTarget.BACKLOG,
            enabled = enabled,
            onApplySort = onApplySort,
        )
        SortActionButton(
            label = "Sort Inbox",
            target = InboxSortingService.SortTarget.INBOX_RECORDS,
            enabled = enabled,
            onApplySort = onApplySort,
        )
        SortActionButton(
            label = "Sort Connections",
            target = InboxSortingService.SortTarget.ATTACHMENTS,
            enabled = enabled,
            onApplySort = onApplySort,
        )
    }
}

@Composable
private fun SortActionButton(
    label: String,
    target: InboxSortingService.SortTarget,
    enabled: Boolean,
    onApplySort: (InboxSortingService.SortTarget) -> Unit,
) {
    Button(
        onClick = { onApplySort(target) },
        enabled = enabled,
    ) {
        Text(label)
    }
}
