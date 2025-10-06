package com.romankozak.forwardappmobile.ui.screens.projectscreen.dialogs

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

enum class TransferStatus {
    IDLE, IN_PROGRESS, SUCCESS, ERROR
}

@Composable
fun ExportTransferDialog(
    onDismiss: () -> Unit,
    onCopyToClipboard: () -> Unit,
    onTransfer: () -> Unit,
    onGoToSettings: () -> Unit,
    serverAddress: String?,
    serverAddressMode: String,
    transferStatus: TransferStatus,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Export & Transfer",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Copy to Clipboard Section
                OutlinedButton(
                    onClick = onCopyToClipboard,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Copy to Clipboard")
                }

                Divider(modifier = Modifier.padding(vertical = 20.dp))

                // Server Transfer Section
                Text(
                    "Transfer to Server",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                ServerStatus(serverAddress, serverAddressMode, onGoToSettings)

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onTransfer,
                        enabled = transferStatus != TransferStatus.IN_PROGRESS && !serverAddress.isNullOrBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Send")
                    }

                    AnimatedContent(targetState = transferStatus, label = "status_indicator") { status ->
                        when (status) {
                            TransferStatus.IN_PROGRESS -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            TransferStatus.SUCCESS -> Text("✅", style = MaterialTheme.typography.headlineMedium)
                            TransferStatus.ERROR -> Text("❌", style = MaterialTheme.typography.headlineMedium)
                            TransferStatus.IDLE -> Spacer(modifier = Modifier.size(24.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun ServerStatus(
    serverAddress: String?,
    serverAddressMode: String,
    onGoToSettings: () -> Unit
) {
    val (icon, text, color) = when {
        serverAddress.isNullOrBlank() -> Triple(Icons.Default.Dns, "Server not found", MaterialTheme.colorScheme.error)
        else -> Triple(Icons.Default.Dns, serverAddress, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${serverAddressMode.uppercase()} MODE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onGoToSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Go to Settings")
            }
        }
    }
}