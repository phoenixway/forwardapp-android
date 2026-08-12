package com.romankozak.forwardappmobile.desktop.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.desktop.features.contexts.DesktopWorkspaceDependencies
import com.romankozak.forwardappmobile.desktop.features.sync.DesktopAndroidConnectionState
import com.romankozak.forwardappmobile.desktop.features.sync.DesktopAndroidSyncController
import com.romankozak.forwardappmobile.shared.application.contexts.WorkspaceBackupEntry
import com.romankozak.forwardappmobile.shared.application.contexts.WorkspaceImportInspection
import com.romankozak.forwardappmobile.shared.application.contexts.WorkspaceRecoveryAction
import com.romankozak.forwardappmobile.shared.application.contexts.WorkspaceRecoveryStore
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DesktopSettingsScreen(
    dependencies: DesktopWorkspaceDependencies,
    syncController: DesktopAndroidSyncController,
) {
    val scope = rememberCoroutineScope()
    val store =
        remember(dependencies.fileStore, scope) {
            WorkspaceRecoveryStore(
                gateway = DesktopWorkspaceRecoveryGateway(dependencies.fileStore),
                scope = scope,
            )
        }
    val state by store.state.collectAsState()
    val syncState by syncController.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        state.pendingAction?.let { action ->
            RecoveryConfirmationDialog(
                action = action,
                onDismiss = store::dismissPendingAction,
                onConfirm = store::confirmPendingAction,
            )
        }
        Text(
            text = "Settings",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Desktop-specific configuration: data directory, sync transport, keyboard shortcuts, experimental modules, telemetry policy.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xCCFFFFFF)),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Android Sync",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = syncState.settings.androidAddress,
                    onValueChange = syncController::onAddressChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Android server address") },
                    supportingText = { Text("Example: 192.168.1.42:8080. Android remains the only server.") },
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Auto sync",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = syncStatusText(syncState.connection, syncState.isSyncing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = syncStatusColor(syncState.connection, syncState.isSyncing),
                        )
                    }
                    Switch(
                        checked = syncState.settings.autoSyncEnabled,
                        onCheckedChange = syncController::onAutoSyncChanged,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = syncController::syncNow,
                        enabled = syncState.settings.androidAddress.isNotBlank() && !syncState.isSyncing,
                    ) {
                        Text(if (syncState.isSyncing) "Syncing..." else "Sync Now")
                    }
                    SettingsFact(
                        label = "Last sync",
                        value = syncState.settings.lastSyncAt?.let(::formatSyncTime) ?: "Never",
                    )
                }
                syncState.lastError?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFA64132),
                        modifier =
                            Modifier
                                .background(Color(0xFFFBE8E5), RoundedCornerShape(18.dp))
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                    )
                } ?: Text(
                    text = syncState.lastSyncMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1D7C70),
                    modifier =
                        Modifier
                            .background(Color(0xFFE7F5EF), RoundedCornerShape(18.dp))
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
        }
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xCCFFFFFF)),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Workspace Recovery",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                SettingsFact(label = "Workspace file", value = state.workspacePath)
                SettingsFact(label = "Available backups", value = state.backupCount.toString())
                SettingsFact(label = "Default export path", value = state.defaultExportPath)
                OutlinedTextField(
                    value = state.importPath,
                    onValueChange = store::onImportPathChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Import snapshot path") },
                    supportingText = {
                        Text("Absolute path to a desktop snapshot or Android backup JSON.")
                    },
                    singleLine = true,
                )
                state.importInspection?.let { inspection ->
                    ImportInspectionCard(importInspection = inspection)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = store::requestRestoreLatest) {
                        Text("Restore Latest Backup")
                    }
                    OutlinedButton(onClick = store::exportSnapshot) {
                        Text("Export Snapshot")
                    }
                    OutlinedButton(
                        onClick = store::previewImport,
                        enabled = state.importPath.isNotBlank(),
                    ) {
                        Text("Preview Import")
                    }
                    OutlinedButton(
                        onClick = store::requestImportSnapshot,
                        enabled = state.canImport,
                    ) {
                        Text("Import Snapshot")
                    }
                    OutlinedButton(
                        onClick = store::requestRestoreSelected,
                        enabled = state.selectedBackup != null,
                    ) {
                        Text("Restore Selected")
                    }
                }
                if (state.backups.isEmpty()) {
                    Text(
                        text = "No backups available yet. They will appear after snapshot writes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Recent Backups",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        state.backups.take(8).forEach { backup ->
                            BackupRow(
                                backup = backup,
                                isSelected = state.selectedBackupId == backup.id,
                                onSelect = { store.selectBackup(backup.id) },
                            )
                        }
                    }
                }
                Text(
                    text = state.lastActionMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = messageColor(state.lastActionMessage),
                    modifier =
                        Modifier
                            .background(messageBackground(state.lastActionMessage), RoundedCornerShape(18.dp))
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun RecoveryConfirmationDialog(
    action: WorkspaceRecoveryAction,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = action.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                text = action.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(action.confirmLabel)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ImportInspectionCard(
    importInspection: WorkspaceImportInspection,
) {
    val badgeLabel =
        when (importInspection) {
            is WorkspaceImportInspection.Valid -> "Ready"
            is WorkspaceImportInspection.FileNotFound -> "Missing"
            is WorkspaceImportInspection.Invalid -> "Invalid"
        }
    val badgeBackground =
        when (importInspection) {
            is WorkspaceImportInspection.Valid -> Color(0x1F2E7D32)
            is WorkspaceImportInspection.FileNotFound -> Color(0x1FCC7A00)
            is WorkspaceImportInspection.Invalid -> Color(0x1FA64132)
        }
    val badgeColor =
        when (importInspection) {
            is WorkspaceImportInspection.Valid -> Color(0xFF2E7D32)
            is WorkspaceImportInspection.FileNotFound -> Color(0xFFCC7A00)
            is WorkspaceImportInspection.Invalid -> Color(0xFFA64132)
        }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8F3)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Import Preview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = badgeLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = badgeColor,
                    modifier =
                        Modifier
                            .background(badgeBackground, RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            when (importInspection) {
                is WorkspaceImportInspection.Valid -> {
                    SettingsFact(label = "Source", value = importInspection.sourcePath)
                    SettingsFact(label = "Format", value = importInspection.format.title)
                    SettingsFact(label = "Contexts", value = importInspection.contextsCount.toString())
                    SettingsFact(label = "Backlog Items", value = importInspection.backlogItemsCount.toString())
                }

                is WorkspaceImportInspection.FileNotFound ->
                    SettingsFact(label = "Source", value = importInspection.sourcePath)

                is WorkspaceImportInspection.Invalid ->
                    SettingsFact(label = "Source", value = importInspection.sourcePath)
            }
        }
    }
}

@Composable
private fun BackupRow(
    backup: WorkspaceBackupEntry,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) Color(0xFFE6F2EE) else Color(0xFFF6F7F2),
                    RoundedCornerShape(16.dp),
                )
                .clickable(onClick = onSelect)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = backup.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Column(
            horizontalAlignment = androidx.compose.ui.Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = backup.lastModifiedLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${backup.sizeLabel} • ${if (isSelected) "Selected" else "Restore target"}",
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) Color(0xFF1D7C70) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsFact(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun messageColor(message: String): Color =
    when {
        message.contains("invalid", ignoreCase = true) -> Color(0xFFA64132)
        message.contains("not found", ignoreCase = true) -> Color(0xFFCC7A00)
        message.contains("ready", ignoreCase = true) -> Color(0xFF1D7C70)
        message.contains("restored", ignoreCase = true) -> Color(0xFF1D7C70)
        message.contains("exported", ignoreCase = true) -> Color(0xFF1D7C70)
        message.contains("imported", ignoreCase = true) -> Color(0xFF1D7C70)
        else -> Color(0xFF52606D)
    }

private fun messageBackground(message: String): Color =
    when {
        message.contains("invalid", ignoreCase = true) -> Color(0xFFFBE8E5)
        message.contains("not found", ignoreCase = true) -> Color(0xFFFFF3DC)
        message.contains("ready", ignoreCase = true) -> Color(0xFFE7F5EF)
        message.contains("restored", ignoreCase = true) -> Color(0xFFE7F5EF)
        message.contains("exported", ignoreCase = true) -> Color(0xFFE7F5EF)
        message.contains("imported", ignoreCase = true) -> Color(0xFFE7F5EF)
        else -> Color(0xFFF2F4EF)
    }

private fun syncStatusText(
    connection: DesktopAndroidConnectionState,
    isSyncing: Boolean,
): String =
    if (isSyncing) {
        "Syncing desktop delta and Android delta."
    } else {
        when (connection) {
            DesktopAndroidConnectionState.Idle -> "Idle."
            DesktopAndroidConnectionState.Checking -> "Checking Android server."
            DesktopAndroidConnectionState.Connected -> "Connected."
            DesktopAndroidConnectionState.Disconnected -> "Disconnected, retrying."
        }
    }

private fun syncStatusColor(
    connection: DesktopAndroidConnectionState,
    isSyncing: Boolean,
): Color =
    when {
        isSyncing -> Color(0xFF1D6E64)
        connection == DesktopAndroidConnectionState.Connected -> Color(0xFF1D7C70)
        connection == DesktopAndroidConnectionState.Disconnected -> Color(0xFFA64132)
        else -> Color(0xFF52606D)
    }

private fun formatSyncTime(timestamp: Long): String =
    SYNC_TIME_FORMATTER.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))

private val SYNC_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
