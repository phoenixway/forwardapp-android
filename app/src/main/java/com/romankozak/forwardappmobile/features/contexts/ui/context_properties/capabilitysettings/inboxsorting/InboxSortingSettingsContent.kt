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
        Text(
            text = "Правила сортування",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Формат: backlog:newest|oldest, inbox:newest|oldest|alpha, attachments:newest|oldest|type|alpha",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = uiState.rulesText,
            onValueChange = viewModel::onRulesTextChanged,
            modifier = Modifier.fillMaxWidth(),
            minLines = 6,
            label = { Text("Rules") },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = viewModel::saveRules,
                enabled = !uiState.isSaving && !uiState.isApplying,
            ) {
                Text("Зберегти")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { viewModel.applySort(InboxSortingService.SortTarget.BACKLOG) },
                enabled = !uiState.isSaving && !uiState.isApplying,
            ) {
                Text("Sort Backlog")
            }
            Button(
                onClick = { viewModel.applySort(InboxSortingService.SortTarget.INBOX_RECORDS) },
                enabled = !uiState.isSaving && !uiState.isApplying,
            ) {
                Text("Sort Inbox")
            }
            Button(
                onClick = { viewModel.applySort(InboxSortingService.SortTarget.ATTACHMENTS) },
                enabled = !uiState.isSaving && !uiState.isApplying,
            ) {
                Text("Sort Attachments")
            }
        }

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

