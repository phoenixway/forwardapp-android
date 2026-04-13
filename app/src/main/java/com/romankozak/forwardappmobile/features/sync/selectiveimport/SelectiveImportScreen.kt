package com.romankozak.forwardappmobile.features.sync.selectiveimport

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.romankozak.forwardappmobile.core.data.models.sync.DiffStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportPreviewItemStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportPreviewModel
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportPreviewSection
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportPreviewSummary
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceImportSourceMode
import com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSnapshotFormat

private fun hasItemsSelected(uiState: SelectiveImportState): Boolean = uiState.previewSummary.totalSelectedCount > 0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectiveImportScreen(
    viewModel: SelectiveImportViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    val importLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                viewModel.loadBackupFile(uri.toString())
            } else {
                onNavigateBack()
            }
        }

    LaunchedEffect(uiState.isLoading) {
        if (uiState.backupContent == null && !uiState.isLoading && uiState.error == null) {
            importLauncher.launch("application/json")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is SelectiveImportEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Вибірковий імпорт") })
        },
        bottomBar = {
            val selectedCount = uiState.previewSummary.totalSelectedCount
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(onClick = onNavigateBack) {
                    Text("Cancel")
                }
                Button(
                    onClick = { viewModel.onImportClicked() },
                    enabled = !uiState.isLoading && uiState.error == null && hasItemsSelected(uiState),
                ) {
                    Text("Імпортувати ($selectedCount)")
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
                uiState.error != null -> {
                    Text(
                        text = "Error: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
                uiState.backupContent != null -> {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                    ) {
                        DiffSummaryBar(
                            previewSummary = uiState.previewSummary,
                            sourceMode = uiState.sourceMode,
                            sourceFormat = uiState.sourceFormat,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))
                        BackupContentList(
                            previewModel = uiState.previewModel,
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
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
private fun StatusBadge(status: DiffStatus) {
    val (label, bg, fg) =
        when (status) {
            DiffStatus.NEW -> Triple("Новий", MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), MaterialTheme.colorScheme.secondary)
            DiffStatus.UPDATED ->
                Triple(
                    "Оновлення",
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                    MaterialTheme.colorScheme.tertiary,
                )
            DiffStatus.DELETED -> Triple("Видалено", MaterialTheme.colorScheme.error.copy(alpha = 0.15f), MaterialTheme.colorScheme.error)
        }
    Surface(
        color = bg,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun SummaryChip(
    label: String,
    value: Int,
    status: DiffStatus? = null,
) {
    val (bg, fg) =
        when (status) {
            DiffStatus.NEW -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f) to MaterialTheme.colorScheme.onSecondaryContainer
            DiffStatus.UPDATED -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f) to MaterialTheme.colorScheme.onTertiaryContainer
            DiffStatus.DELETED -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f) to MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        }
    Surface(
        color = bg,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(text = label, color = fg, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = value.toString(), color = fg, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun DiffSummaryBar(
    previewSummary: WorkspaceImportPreviewSummary,
    sourceMode: WorkspaceImportSourceMode?,
    sourceFormat: WorkspaceSnapshotFormat?,
) {
    val newCount = previewSummary.totalNewCount
    val updatedCount = previewSummary.totalUpdatedCount
    val deletedCount = previewSummary.totalDeletedCount
    val totalSelected = previewSummary.totalSelectedCount
    val totalAvailable = previewSummary.totalCount

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Зміни у файлі",
                style = MaterialTheme.typography.titleMedium,
            )
            if (sourceMode != null || sourceFormat != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    sourceMode?.let { mode ->
                        SourceChip(
                            label = "Режим",
                            value = mode.title,
                        )
                    }
                    sourceFormat?.let { format ->
                        SourceChip(
                            label = "Формат",
                            value = format.title,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryChip(label = "Нові", value = newCount, status = DiffStatus.NEW)
                SummaryChip(label = "Оновлення", value = updatedCount, status = DiffStatus.UPDATED)
                SummaryChip(label = "Видалення", value = deletedCount, status = DiffStatus.DELETED)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Вибрано $totalSelected з $totalAvailable",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SourceChip(
    label: String,
    value: String,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

@Composable
private fun SelectableRow(
    label: String,
    isSelected: Boolean,
    isSelectable: Boolean,
    status: DiffStatus,
    subtitle: String? = null,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            StatusBadge(status = status)
            Checkbox(
                checked = isSelected,
                enabled = isSelectable,
                onCheckedChange = { onToggle(it) },
            )
        }
    }
}

@Composable
private fun BackupContentList(
    previewModel: WorkspaceImportPreviewModel,
    viewModel: SelectiveImportViewModel,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        items(previewModel.sections, key = { it.kind.name }) { section ->
            PreviewSection(
                scope = this,
                section = section,
                viewModel = viewModel,
            )
        }
    }
}

private fun PreviewSection(
    scope: LazyListScope,
    section: WorkspaceImportPreviewSection,
    viewModel: SelectiveImportViewModel,
) {
    if (section.items.isEmpty()) return

    with(scope) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(
                title = "${section.kind.title} (${section.items.size})",
                onSelectAll = { viewModel.onPreviewSectionToggle(section.kind, true) },
                onDeselectAll = { viewModel.onPreviewSectionToggle(section.kind, false) },
            )
        }
        items(section.items, key = { it.id }) { previewItem ->
            SelectableRow(
                label = previewItem.title,
                subtitle = previewItem.subtitle,
                isSelected = previewItem.isSelected,
                isSelectable = previewItem.isSelectable,
                status = previewItem.status.toDiffStatus(),
                onToggle = { isSelected ->
                    viewModel.onPreviewItemToggle(section.kind, previewItem.id, isSelected)
                },
            )
        }
    }
}

private fun WorkspaceImportPreviewItemStatus.toDiffStatus(): DiffStatus =
    when (this) {
        WorkspaceImportPreviewItemStatus.New -> DiffStatus.NEW
        WorkspaceImportPreviewItemStatus.Updated -> DiffStatus.UPDATED
        WorkspaceImportPreviewItemStatus.Deleted -> DiffStatus.DELETED
    }
