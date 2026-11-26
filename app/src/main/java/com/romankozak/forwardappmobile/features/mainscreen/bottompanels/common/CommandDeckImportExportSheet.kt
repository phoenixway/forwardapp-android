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
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Файли",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
                    )
                }
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
                        title = "Імпорт бекапу",
                        subtitle = "Відновити або об’єднати дані",
                        containerColor = importExportSheetColor,
                        onClick = {
                            Log.e("FullJsonImport", "Backup import tile clicked; launching picker")
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
                item(span = { GridItemSpan(maxLineSpan) }) {
                    WifiTransferInstructions()
                }
                item {
                    ImportExportTile(
                        icon = Icons.Default.Wifi,
                        title = "Приймати через Wi‑Fi",
                        subtitle = "Показати адресу цього пристрою для підключення",
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        onClick = {
                            onDismiss()
                            onShowWifiServer()
                        },
                    )
                }
                item {
                    ImportExportTile(
                        icon = Icons.Default.CloudUpload,
                        title = "Надіслати через Wi‑Fi",
                        subtitle = "Передати зміни на пристрій, що приймає",
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        onClick = {
                            onDismiss()
                            onWifiPush("")
                        },
                    )
                }
                item {
                    ImportExportTile(
                        icon = Icons.Default.Wifi,
                        title = "Отримати через Wi‑Fi",
                        subtitle = "Забрати дані з пристрою, що приймає",
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
private fun WifiTransferInstructions() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Між пристроями · вручну", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "1. На пристрої, до якого підключаєтесь, відкрийте «Приймати через Wi‑Fi».\n" +
                    "2. Тут збережіть його адресу й ключ у Налаштуваннях → Інтеграції. Ключ показано в налаштуваннях того пристрою.\n" +
                    "3. Оберіть «Надіслати» або «Отримати» відповідно до напрямку передачі.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Увімкнений сервер лише очікує підключення; передачу запускаєте кнопкою.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
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
