package com.romankozak.forwardappmobile.features.contexts.ui.context_properties.capabilitysettings.backlog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun BacklogSettingsContent(
    contextId: String,
    viewModel: BacklogSettingsViewModel = hiltViewModel(),
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
            text = "Backlog settings",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Якщо backlog-запис з #tag автопереноситься в backlog інших контекстів, у вихідному контексті він більше не показуватиметься.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Remove after autocopy entries with tags",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = uiState.removeAfterAutocopyEntriesWithTags,
                onCheckedChange = viewModel::onRemoveAfterAutocopyEntriesWithTagsChanged,
                enabled = !uiState.isSaving,
            )
        }
    }
}
