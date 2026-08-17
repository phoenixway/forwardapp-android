package com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandDeckImportExportSheet(
    onDismiss: () -> Unit,
    onExportToFile: () -> Unit,
    onImportFromFileRequest: (Uri) -> Unit,
    onSelectiveImportFromFileRequest: (Uri) -> Unit,
    onExportAttachments: () -> Unit,
    onImportAttachmentsFromFileRequest: (Uri) -> Unit,
    onWifiPush: (String) -> Unit,
    onShowWifiServer: () -> Unit,
    onShowWifiImport: () -> Unit,
) {
    val importLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
            Log.e("FullJsonImport", "filePicker result uri=$uri")
            uri?.let {
                onDismiss()
                onImportFromFileRequest(it)
            }
        }
    val selectiveImportLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                onDismiss()
                onSelectiveImportFromFileRequest(it)
            }
        }
    val importAttachmentsLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                onDismiss()
                onImportAttachmentsFromFileRequest(it)
            }
        }

    val importExportSheetColor = MaterialTheme.colorScheme.surfaceContainerLow
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = importExportSheetColor,
    ) {
        Column(
            modifier =
                Modifier
                    .background(importExportSheetColor)
                    .padding(bottom = 24.dp),
        ) {
            Text(
                text = "Імпорт / Експорт",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(160.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                item {
                    ImportExportTile(
                        icon = Icons.Default.CloudUpload,
                        title = "Експорт бекапу",
                        subtitle = "Зберегти JSON у файлі",
                        containerColor = importExportSheetColor,
                        onClick = {
                            onDismiss()
                            onExportToFile()
                        },
                    )
                }
                item {
                    ImportExportTile(
                        icon = Icons.Default.CloudDownload,
                        title = "Повний імпорт",
                        subtitle = "Замінити поточні дані",
                        containerColor = importExportSheetColor,
                        onClick = {
                            Log.e("FullJsonImport", "Full import tile clicked; launching picker")
                            importLauncher.launch("*/*")
                        },
                    )
                }
                item {
                    ImportExportTile(
                        icon = Icons.Default.FolderOpen,
                        title = "Вибірковий імпорт",
                        subtitle = "Обрати сутності",
                        containerColor = importExportSheetColor,
                        onClick = {
                            selectiveImportLauncher.launch("application/json")
                        },
                    )
                }
                item {
                    ImportExportTile(
                        icon = Icons.Default.Description,
                        title = "Експорт вкладень",
                        subtitle = "JSON вкладень",
                        containerColor = importExportSheetColor,
                        onClick = {
                            onDismiss()
                            onExportAttachments()
                        },
                    )
                }
                item {
                    ImportExportTile(
                        icon = Icons.Default.FolderOpen,
                        title = "Імпорт вкладень",
                        subtitle = "Додати вкладення",
                        containerColor = importExportSheetColor,
                        onClick = {
                            importAttachmentsLauncher.launch("application/json")
                        },
                    )
                }
                item {
                    ImportExportTile(
                        icon = Icons.Default.CloudUpload,
                        title = "Push змін по Wi-Fi",
                        subtitle = "Надіслати несинхронізоване",
                        containerColor = importExportSheetColor,
                        onClick = {
                            onDismiss()
                            onWifiPush("localhost:8080")
                        },
                    )
                }
                item {
                    ImportExportTile(
                        icon = Icons.Default.Wifi,
                        title = "Wi-Fi сервер",
                        subtitle = "Запустити локальний сервер",
                        containerColor = importExportSheetColor,
                        onClick = {
                            onDismiss()
                            onShowWifiServer()
                        },
                    )
                }
                item {
                    ImportExportTile(
                        icon = Icons.Default.Wifi,
                        title = "Wi-Fi імпорт",
                        subtitle = "Отримати дані з сервера",
                        containerColor = importExportSheetColor,
                        onClick = {
                            onDismiss()
                            onShowWifiImport()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportExportTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        onClick = onClick,
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
