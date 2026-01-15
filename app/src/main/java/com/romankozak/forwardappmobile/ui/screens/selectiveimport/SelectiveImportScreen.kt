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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import com.romankozak.forwardappmobile.data.sync.DiffStatus

private fun hasItemsSelected(uiState: SelectiveImportState): Boolean {
    val backupContent = uiState.backupContent ?: return false
    return backupContent.totalSelectedCount() > 0
}

private fun SelectableDatabaseContent.totalSelectedCount(): Int =
    allSections().sumOf { section -> section.count { it.isSelectable && it.isSelected } }

private fun SelectableDatabaseContent.countByStatus(status: DiffStatus): Int =
    allSections().sumOf { section -> section.count { it.status == status } }

private fun SelectableDatabaseContent.allSections(): List<List<SelectableDiffItem<*>>> =
    listOf(
        projects,
        goals,
        legacyNotes,
        activityRecords,
        listItems,
        documents,
        checklists,
        linkItems,
        inboxRecords,
        projectExecutionLogs,
        scripts,
        attachments,
    )

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
            TopAppBar(title = { Text("Вибірковий імпорт") })
        },
        bottomBar = {
            val selectedCount = uiState.backupContent?.totalSelectedCount() ?: 0
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
                    enabled = !uiState.isLoading && uiState.error == null && hasItemsSelected(uiState)
                ) {
                    Text("Імпортувати ($selectedCount)")
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        DiffSummaryBar(content = uiState.backupContent!!)
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))
                        BackupContentList(
                            content = uiState.backupContent!!,
                            viewModel = viewModel
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
private fun StatusBadge(status: DiffStatus) {
    val (label, bg, fg) = when (status) {
        DiffStatus.NEW -> Triple("Новий", MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), MaterialTheme.colorScheme.secondary)
        DiffStatus.UPDATED -> Triple("Оновлення", MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), MaterialTheme.colorScheme.tertiary)
        DiffStatus.DELETED -> Triple("Видалено", MaterialTheme.colorScheme.error.copy(alpha = 0.15f), MaterialTheme.colorScheme.error)
    }
    Surface(
        color = bg,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SummaryChip(label: String, value: Int, status: DiffStatus? = null) {
    val (bg, fg) = when (status) {
        DiffStatus.NEW -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f) to MaterialTheme.colorScheme.onSecondaryContainer
        DiffStatus.UPDATED -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f) to MaterialTheme.colorScheme.onTertiaryContainer
        DiffStatus.DELETED -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f) to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = bg,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = label, color = fg, style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = value.toString(), color = fg, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun DiffSummaryBar(content: SelectableDatabaseContent) {
    val newCount = content.countByStatus(DiffStatus.NEW)
    val updatedCount = content.countByStatus(DiffStatus.UPDATED)
    val deletedCount = content.countByStatus(DiffStatus.DELETED)
    val totalSelected = content.totalSelectedCount()
    val totalAvailable = content.allSections().sumOf { it.size }

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Зміни у файлі",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryChip(label = "Нові", value = newCount, status = DiffStatus.NEW)
                SummaryChip(label = "Оновлення", value = updatedCount, status = DiffStatus.UPDATED)
                SummaryChip(label = "Видалення", value = deletedCount, status = DiffStatus.DELETED)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Вибрано $totalSelected з $totalAvailable",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            StatusBadge(status = status)
            Checkbox(
                checked = isSelected,
                enabled = isSelectable,
                onCheckedChange = { onToggle(it) }
            )
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
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        if (content.projects.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Проекти (${content.projects.size})",
                    onSelectAll = { viewModel.toggleAllSelection(EntityType.PROJECT, true) },
                    onDeselectAll = { viewModel.toggleAllSelection(EntityType.PROJECT, false) }
                )
            }
            items(content.projects, key = { it.item.id }) { selectableProject ->
                SelectableRow(
                    label = selectableProject.item.name,
                    isSelected = selectableProject.isSelected,
                    isSelectable = selectableProject.isSelectable,
                    status = selectableProject.status,
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
                    title = "Цілі (${content.goals.size})",
                    onSelectAll = { viewModel.toggleAllSelection(EntityType.GOAL, true) },
                    onDeselectAll = { viewModel.toggleAllSelection(EntityType.GOAL, false) }
                )
            }
            items(content.goals, key = { it.item.id }) { selectableGoal ->
                SelectableRow(
                    label = selectableGoal.item.text,
                    isSelected = selectableGoal.isSelected,
                    isSelectable = selectableGoal.isSelectable,
                    status = selectableGoal.status,
                    onToggle = { isSelected ->
                        viewModel.toggleGoalSelection(selectableGoal.item.id, isSelected)
                    }
                )
            }
        }

        if (content.legacyNotes.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = "Нотатки (${content.legacyNotes.size})",
                    onSelectAll = { viewModel.toggleAllSelection(EntityType.LEGACY_NOTE, true) },
                    onDeselectAll = { viewModel.toggleAllSelection(EntityType.LEGACY_NOTE, false) }
                )
            }
            items(content.legacyNotes, key = { it.item.id }) { selectableItem ->
                SelectableRow(
                    label = selectableItem.item.title.ifBlank { "Без назви" },
                    isSelected = selectableItem.isSelected,
                    isSelectable = selectableItem.isSelectable,
                    status = selectableItem.status,
                    onToggle = { isSelected ->
                        viewModel.toggleLegacyNoteSelection(selectableItem.item.id, isSelected)
                    }
                )
            }
        }

        if (content.activityRecords.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = "Активності (${content.activityRecords.size})",
                    onSelectAll = { viewModel.toggleAllSelection(EntityType.ACTIVITY_RECORD, true) },
                    onDeselectAll = { viewModel.toggleAllSelection(EntityType.ACTIVITY_RECORD, false) }
                )
            }
            items(content.activityRecords, key = { it.item.id }) { selectableItem ->
                SelectableRow(
                    label = selectableItem.item.text.ifBlank { "Без опису" },
                    isSelected = selectableItem.isSelected,
                    isSelectable = selectableItem.isSelectable,
                    status = selectableItem.status,
                    onToggle = { isSelected ->
                        viewModel.toggleActivityRecordSelection(selectableItem.item.id, isSelected)
                    }
                )
            }
        }

        if (content.listItems.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = "Елементи списку (${content.listItems.size})",
                    onSelectAll = { viewModel.toggleAllSelection(EntityType.LIST_ITEM, true) },
                    onDeselectAll = { viewModel.toggleAllSelection(EntityType.LIST_ITEM, false) }
                )
            }
            items(content.listItems, key = { it.item.id }) { selectableItem ->
                SelectableRow(
                    label = "ListItem #${selectableItem.item.order} → ${selectableItem.item.entityId}",
                    subtitle = selectableItem.changeInfo,
                    isSelected = selectableItem.isSelected,
                    isSelectable = selectableItem.isSelectable,
                    status = selectableItem.status,
                    onToggle = { isSelected ->
                        viewModel.toggleListItemSelection(selectableItem.item.id, isSelected)
                    }
                )
            }
        }

        if (content.documents.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = "Документи (${content.documents.size})",
                    onSelectAll = { viewModel.toggleAllSelection(EntityType.DOCUMENT, true) },
                    onDeselectAll = { viewModel.toggleAllSelection(EntityType.DOCUMENT, false) }
                )
            }
            items(content.documents, key = { it.item.id }) { selectableItem ->
                SelectableRow(
                    label = selectableItem.item.name.ifBlank { "Без назви" },
                    isSelected = selectableItem.isSelected,
                    isSelectable = selectableItem.isSelectable,
                    status = selectableItem.status,
                    onToggle = { isSelected ->
                        viewModel.toggleDocumentSelection(selectableItem.item.id, isSelected)
                    }
                )
            }
        }

        if (content.checklists.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = "Чеклісти (${content.checklists.size})",
                    onSelectAll = { viewModel.toggleAllSelection(EntityType.CHECKLIST, true) },
                    onDeselectAll = { viewModel.toggleAllSelection(EntityType.CHECKLIST, false) }
                )
            }
            items(content.checklists, key = { it.item.id }) { selectableItem ->
                SelectableRow(
                    label = selectableItem.item.name.ifBlank { "Без назви" },
                    isSelected = selectableItem.isSelected,
                    isSelectable = selectableItem.isSelectable,
                    status = selectableItem.status,
                    onToggle = { isSelected ->
                        viewModel.toggleChecklistSelection(selectableItem.item.id, isSelected)
                    }
                )
            }
        }

        if (content.linkItems.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = "Посилання (${content.linkItems.size})",
                    onSelectAll = { viewModel.toggleAllSelection(EntityType.LINK_ITEM, true) },
                    onDeselectAll = { viewModel.toggleAllSelection(EntityType.LINK_ITEM, false) }
                )
            }
            items(content.linkItems, key = { it.item.id }) { selectableItem ->
                SelectableRow(
                    label = (selectableItem.item.linkData.displayName ?: selectableItem.item.linkData.target).ifBlank { "Без назви" },
                    isSelected = selectableItem.isSelected,
                    isSelectable = selectableItem.isSelectable,
                    status = selectableItem.status,
                    onToggle = { isSelected ->
                        viewModel.toggleLinkItemSelection(selectableItem.item.id, isSelected)
                    }
                )
            }
        }

        if (content.inboxRecords.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = "Записи Inbox (${content.inboxRecords.size})",
                    onSelectAll = { viewModel.toggleAllSelection(EntityType.INBOX_RECORD, true) },
                    onDeselectAll = { viewModel.toggleAllSelection(EntityType.INBOX_RECORD, false) }
                )
            }
            items(content.inboxRecords, key = { it.item.id }) { selectableItem ->
                SelectableRow(
                    label = selectableItem.item.text.ifBlank { "Без вмісту" },
                    isSelected = selectableItem.isSelected,
                    isSelectable = selectableItem.isSelectable,
                    status = selectableItem.status,
                    onToggle = { isSelected ->
                        viewModel.toggleInboxRecordSelection(selectableItem.item.id, isSelected)
                    }
                )
            }
        }

        if (content.projectExecutionLogs.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = "Логи виконання проектів (${content.projectExecutionLogs.size})",
                    onSelectAll = { viewModel.toggleAllSelection(EntityType.PROJECT_EXECUTION_LOG, true) },
                    onDeselectAll = { viewModel.toggleAllSelection(EntityType.PROJECT_EXECUTION_LOG, false) }
                )
            }
            items(content.projectExecutionLogs, key = { it.item.id }) { selectableItem ->
                SelectableRow(
                    label = selectableItem.item.description.ifBlank { "Без опису" },
                    isSelected = selectableItem.isSelected,
                    isSelectable = selectableItem.isSelectable,
                    status = selectableItem.status,
                    onToggle = { isSelected ->
                        viewModel.toggleProjectExecutionLogSelection(selectableItem.item.id, isSelected)
                    }
                )
            }
        }

        if (content.scripts.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = "Скрипти (${content.scripts.size})",
                    onSelectAll = { viewModel.toggleAllSelection(EntityType.SCRIPT, true) },
                    onDeselectAll = { viewModel.toggleAllSelection(EntityType.SCRIPT, false) }
                )
            }
            items(content.scripts, key = { it.item.id }) { selectableItem ->
                SelectableRow(
                    label = selectableItem.item.name.ifBlank { "Без назви" },
                    isSelected = selectableItem.isSelected,
                    isSelectable = selectableItem.isSelectable,
                    status = selectableItem.status,
                    onToggle = { isSelected ->
                        viewModel.toggleScriptSelection(selectableItem.item.id, isSelected)
                    }
                )
            }
        }

        if (content.attachments.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = "Вкладення (${content.attachments.size})",
                    onSelectAll = { viewModel.toggleAllSelection(EntityType.ATTACHMENT, true) },
                    onDeselectAll = { viewModel.toggleAllSelection(EntityType.ATTACHMENT, false) }
                )
            }
            items(content.attachments, key = { it.item.id }) { selectableItem ->
                SelectableRow(
                    label = "Attachment: ${selectableItem.item.entityId}", // Temporary
                    isSelected = selectableItem.isSelected,
                    isSelectable = selectableItem.isSelectable,
                    status = selectableItem.status,
                    onToggle = { isSelected ->
                        viewModel.toggleAttachmentSelection(selectableItem.item.id, isSelected)
                    }
                )
            }
        }
    }
}
