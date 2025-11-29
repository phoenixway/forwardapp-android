package com.romankozak.forwardappmobile.ui.screens.selectiveimport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectiveImportScreen(
    viewModel: SelectiveImportViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is SelectiveImportEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Selective Import") })
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onNavigateBack) {
                    Text("Cancel")
                }
                Button(
                    onClick = { viewModel.onImportClicked() },
                    enabled = !uiState.isLoading && uiState.error == null
                ) {
                    Text("Import")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
                uiState.error != null -> {
                    Text(
                        text = "Error: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                uiState.backupContent != null -> {
                    BackupContentList(
                        content = uiState.backupContent!!,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun BackupContentList(
    content: SelectableDatabaseContent,
    viewModel: SelectiveImportViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        if (content.projects.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Projects (${content.projects.size})",
                    onSelectAll = { viewModel.toggleAllSelection(EntityType.PROJECT, true) },
                    onDeselectAll = { viewModel.toggleAllSelection(EntityType.PROJECT, false) }
                )
            }
            items(content.projects, key = { it.item.id }) { selectableProject ->
                SelectableRow(
                    label = selectableProject.item.name,
                    isSelected = selectableProject.isSelected,
                    onToggle = { isSelected ->
                        viewModel.toggleProjectSelection(selectableProject.item.id, isSelected)
                    }
                )
            }
        }

        if (content.goals.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = "Goals (${content.goals.size})",
                    onSelectAll = { viewModel.toggleAllSelection(EntityType.GOAL, true) },
                    onDeselectAll = { viewModel.toggleAllSelection(EntityType.GOAL, false) }
                )
            }
            items(content.goals, key = { it.item.id }) { selectableGoal ->
                SelectableRow(
                    label = selectableGoal.item.text,
                    isSelected = selectableGoal.isSelected,
                    onToggle = { isSelected ->
                        viewModel.toggleGoalSelection(selectableGoal.item.id, isSelected)
                    }
                )
            }
        }
        
        // No sections for Thoughts and Stats as they don't have user-friendly names
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        Row {
            Button(onClick = onSelectAll, modifier = Modifier.padding(end = 8.dp)) {
                Text("All")
            }
            Button(onClick = onDeselectAll) {
                Text("None")
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun SelectableRow(
    label: String,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, modifier = Modifier.weight(1f))
            Checkbox(checked = isSelected, onCheckedChange = onToggle)
        }
    }
}
