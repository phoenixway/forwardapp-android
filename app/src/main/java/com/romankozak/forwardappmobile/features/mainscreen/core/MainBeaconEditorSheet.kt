package com.romankozak.forwardappmobile.features.mainscreen.core

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconSyncStatus
import com.romankozak.forwardappmobile.ui.components.AddConnectionType
import com.romankozak.forwardappmobile.ui.components.ConnectionItemUi
import com.romankozak.forwardappmobile.ui.components.ConnectionPanelMode
import com.romankozak.forwardappmobile.ui.components.ConnectionsPanel
import com.romankozak.forwardappmobile.ui.components.CreateConnectionType

private enum class MainBeaconEditorTab(val title: String) {
    IDENTITY("Identity"),
    CONTROL("Control"),
    LINKS("Links"),
    LEVELS("Levels"),
    META("Meta"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainBeaconEditorSheet(
    state: MainBeaconEditorState,
    selectedContextSummary: String,
    selectedDocumentSummary: String,
    selectedContextLabels: List<String>,
    selectedDocumentLabels: List<String>,
    connectionItems: List<ConnectionItemUi>,
    onDismiss: () -> Unit,
    onStateChange: (MainBeaconEditorState) -> Unit,
    onPickContexts: () -> Unit,
    onPickDocuments: () -> Unit,
    onConnectionClick: (ConnectionItemUi) -> Unit,
    onConnectionRemove: (ConnectionItemUi) -> Unit,
    onAddConnection: (AddConnectionType) -> Unit,
    onCreateConnection: (CreateConnectionType) -> Unit,
    onEditLevel: (Int) -> Unit,
    onSave: () -> Unit,
    onDuplicate: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    var moreExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(MainBeaconEditorTab.IDENTITY) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    EditorHeader(
                        title = state.title.ifBlank { "Untitled beacon" },
                        onSave = onSave,
                        isSaveEnabled = state.title.trim().isNotEmpty(),
                        onDuplicate = onDuplicate,
                        onDelete = onDelete,
                        moreExpanded = moreExpanded,
                        onMoreExpandedChange = { moreExpanded = it },
                    )
                }

                item {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        edgePadding = 0.dp,
                    ) {
                        MainBeaconEditorTab.entries.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = { Text(tab.title) },
                            )
                        }
                    }
                }

                when (selectedTab) {
                    MainBeaconEditorTab.IDENTITY -> {
                        item {
                            IdentitySectionCard(status = state.readinessStatus) {
                                MainBeaconTextField(
                                    label = "Title",
                                    value = state.title,
                                    onValueChange = { onStateChange(state.copy(title = it)) },
                                    singleLine = true,
                                    minLines = 1,
                                    supportingText = if (state.title.isBlank()) "Required" else null,
                                )
                                MainBeaconTextField(
                                    label = "Description",
                                    value = state.description,
                                    onValueChange = { onStateChange(state.copy(description = it)) },
                                    supportingText = "What this beacon means for you",
                                )
                                MainBeaconTextField(
                                    label = "Why it matters",
                                    value = state.whyItMatters,
                                    onValueChange = { onStateChange(state.copy(whyItMatters = it)) },
                                    supportingText = "Why this belongs among the main beacons",
                                )
                            }
                        }
                    }

                    MainBeaconEditorTab.CONTROL -> {
                        item {
                            SectionCard(title = "Meaning tests") {
                                MainBeaconTextField(
                                    label = "Success shape",
                                    value = state.successShape,
                                    onValueChange = { onStateChange(state.copy(successShape = it)) },
                                )
                                MainBeaconTextField(
                                    label = "Failure shape",
                                    value = state.failureShape,
                                    onValueChange = { onStateChange(state.copy(failureShape = it)) },
                                )
                                MainBeaconTextField(
                                    label = "Anti-goal",
                                    value = state.antiGoal,
                                    onValueChange = { onStateChange(state.copy(antiGoal = it)) },
                                )
                                MainBeaconTextField(
                                    label = "Decision impact",
                                    value = state.decisionImpact,
                                    onValueChange = { onStateChange(state.copy(decisionImpact = it)) },
                                )
                            }
                        }
                        item {
                            SectionCard(title = "Current control") {
                                ReadinessSelector(
                                    selected = state.readinessStatus,
                                    onSelected = { onStateChange(state.copy(readinessStatus = it)) },
                                    label = "General readiness status",
                                )
                                MainBeaconTextField(
                                    label = "Blocker",
                                    value = state.blockerText,
                                    onValueChange = { onStateChange(state.copy(blockerText = it)) },
                                )
                                MainBeaconTextField(
                                    label = "Next action",
                                    value = state.nextActionText,
                                    onValueChange = { onStateChange(state.copy(nextActionText = it)) },
                                )
                            }
                        }
                    }

                    MainBeaconEditorTab.LINKS -> {
                        item {
                            ConnectionsPanel(
                                items = connectionItems,
                                onConnectionClick = onConnectionClick,
                                onConnectionRemove = onConnectionRemove,
                                onAddConnection = onAddConnection,
                                onCreateConnection = onCreateConnection,
                                mode = ConnectionPanelMode.COMPACT,
                                preferActionsBesideTitleWhenWide = true,
                                wrapContentHeight = true,
                                showTitle = false,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    MainBeaconEditorTab.LEVELS -> {
                        item {
                            SectionCard(
                                title = "Progress through levels",
                                subtitle = "Status of this beacon on each management level",
                            ) {
                                state.levelStatuses.forEachIndexed { index, level ->
                                    LevelSummaryRow(
                                        state = level,
                                        onClick = { onEditLevel(index) },
                                    )
                                }
                            }
                        }
                    }

                    MainBeaconEditorTab.META -> {
                        item {
                            SectionCard(title = "Meta") {
                                Text(
                                    text = "Created: ${formatTimestamp(state.createdAt)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = "Updated: ${formatTimestamp(state.updatedAt)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorHeader(
    title: String,
    onSave: () -> Unit,
    isSaveEnabled: Boolean,
    onDuplicate: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    moreExpanded: Boolean,
    onMoreExpandedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(
            onClick = onSave,
            enabled = isSaveEnabled,
            colors =
                IconButtonDefaults.iconButtonColors(
                    containerColor =
                        if (isSaveEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                    contentColor =
                        if (isSaveEnabled) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                ),
            modifier =
                Modifier
                    .padding(end = 4.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(999.dp)),
        ) {
            Icon(
                imageVector = Icons.Default.Save,
                contentDescription = "Save",
                modifier = Modifier.size(18.dp),
            )
        }
        Box {
            IconButton(onClick = { onMoreExpandedChange(true) }) {
                Icon(Icons.Default.MoreVert, contentDescription = "More")
            }
            DropdownMenu(
                expanded = moreExpanded,
                onDismissRequest = { onMoreExpandedChange(false) },
            ) {
                if (onDuplicate != null) {
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        onClick = {
                            onMoreExpandedChange(false)
                            onDuplicate()
                        },
                    )
                }
                if (onDelete != null) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onMoreExpandedChange(false)
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainBeaconLevelStatusSheet(
    state: MainBeaconLevelEditorState,
    onDismiss: () -> Unit,
    onStateChange: (MainBeaconLevelEditorState) -> Unit,
    onSave: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
        ) {
            item {
                Text(
                    text = state.levelType.displayLabel(),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            item {
                ReadinessSelector(
                    selected = state.generalStatus,
                    onSelected = { onStateChange(state.copy(generalStatus = it)) },
                    label = "General status",
                )
            }
            item {
                SyncStatusSelector(
                    selected = state.syncStatus,
                    onSelected = { onStateChange(state.copy(syncStatus = it)) },
                )
            }
            item {
                MainBeaconTextField(
                    label = "Blocker",
                    value = state.blockerText,
                    onValueChange = { onStateChange(state.copy(blockerText = it)) },
                )
            }
            item {
                MainBeaconTextField(
                    label = "Next action",
                    value = state.nextActionText,
                    onValueChange = { onStateChange(state.copy(nextActionText = it)) },
                )
            }
            item {
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun IdentitySectionCard(
    status: MainBeaconReadinessStatus,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Main identity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text(status.name) },
                )
            }
            content()
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    ElevatedCard(shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                content()
            },
        )
    }
}

@Composable
private fun LevelSummaryRow(
    state: MainBeaconLevelEditorState,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = state.levelType.displayLabel(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.generalStatus.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = state.generalStatus.statusColor(),
                )
                if (state.syncStatus != MainBeaconSyncStatus.IN_SYNC) {
                    Text(
                        text = state.syncStatus.compactLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun ReadinessSelector(
    selected: MainBeaconReadinessStatus,
    onSelected: (MainBeaconReadinessStatus) -> Unit,
    label: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MainBeaconReadinessStatus.entries.forEach { status ->
                FilterChip(
                    selected = selected == status,
                    onClick = { onSelected(status) },
                    label = { Text(status.name) },
                )
            }
        }
    }
}

@Composable
private fun SyncStatusSelector(
    selected: MainBeaconSyncStatus,
    onSelected: (MainBeaconSyncStatus) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Sync status", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MainBeaconSyncStatus.entries.forEach { status ->
                FilterChip(
                    selected = selected == status,
                    onClick = { onSelected(status) },
                    label = { Text(status.name) },
                )
            }
        }
    }
}

@Composable
private fun MainBeaconTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    supportingText: String? = null,
    singleLine: Boolean = false,
    minLines: Int = 2,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            singleLine = singleLine,
            minLines = minLines,
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun MainBeaconReadinessStatus.statusColor(): Color =
    when (this) {
        MainBeaconReadinessStatus.READY -> Color(0xFF2E7D32)
        MainBeaconReadinessStatus.CONDITIONAL -> Color(0xFFB26A00)
        MainBeaconReadinessStatus.BLOCKED -> Color(0xFFC62828)
        MainBeaconReadinessStatus.DEFECTED -> Color(0xFF5F6368)
    }

private fun MainBeaconSyncStatus.compactLabel(): String =
    when (this) {
        MainBeaconSyncStatus.UNSET -> "unset"
        MainBeaconSyncStatus.IN_SYNC -> "in sync"
        MainBeaconSyncStatus.IN_PROCESS -> "in process"
        MainBeaconSyncStatus.NEEDS_REVIEW -> "needs review"
        MainBeaconSyncStatus.OUTDATED_BY_PARENT -> "outdated by parent"
    }

private fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null || timestamp <= 0L) return "N/A"
    return java.text.SimpleDateFormat("d MMM yyyy, HH:mm", java.util.Locale.getDefault())
        .format(java.util.Date(timestamp))
}
