package com.romankozak.forwardappmobile.ui.screens.projectscreen.dialogs

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog


enum class TransferStatus {
    IDLE,
    IN_PROGRESS,
    SUCCESS,
    ERROR,
}

private const val TAG = "SendDebug"

@Composable
fun ExportTransferDialog(
    onDismiss: () -> Unit,
    onCopyToClipboard: () -> Unit,
    onTransfer: (url: String) -> Unit,
    desktopUrl: String,
    transferStatus: TransferStatus,
) {
    var url by remember { mutableStateOf(desktopUrl) }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                modifier =
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
            ) {
                Text(
                    "Експорт та передача",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                
                OutlinedButton(
                    onClick = onCopyToClipboard,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Копіювати беклог в буфер обміну")
                }

                Divider(modifier = Modifier.padding(vertical = 16.dp))

                
                Text(
                    "Передача на сервер",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL сервера") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    
                    Button(
                        onClick = {
                            
                            Log.d(TAG, "КРОК 1: Кнопка 'Відправити' в діалозі натиснута. URL: $url")
                            onTransfer(url)
                        },
                        enabled = transferStatus != TransferStatus.IN_PROGRESS && url.isNotBlank(),
                    ) {
                        Text("Відправити")
                    }

                    
                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        when (transferStatus) {
                            TransferStatus.IN_PROGRESS -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            TransferStatus.SUCCESS -> Text("✅", style = MaterialTheme.typography.headlineMedium)
                            TransferStatus.ERROR -> Text("❌", style = MaterialTheme.typography.headlineMedium)
                            TransferStatus.IDLE -> Unit
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Закрити")
                }
            }
        }
    }
}
