
package com.romankozak.forwardappmobile.ui.common.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity

@Composable
fun ShareDialog(
    onDismiss: () -> Unit,
    onCopyToClipboard: () -> Unit,
    onTransfer: (() -> Unit)? = null,
    content: String
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Поділитися") },
        text = {
            Column {
                ShareOption(
                    text = "Копіювати в буфер обміну",
                    icon = Icons.Default.ContentCopy,
                    onClick = {
                        onCopyToClipboard()
                        onDismiss()
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                ShareOption(
                    text = "Поділитися через Android",
                    icon = Icons.Default.Share,
                    onClick = {
                        shareContent(context, content)
                        onDismiss()
                    }
                )
                if (onTransfer != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ShareOption(
                        text = "Передати на сервер",
                        icon = Icons.Default.Upload,
                        onClick = {
                            onTransfer()
                            onDismiss()
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити")
            }
        }
    )
}

@Composable
private fun ShareOption(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = text)
        Spacer(modifier = Modifier.padding(start = 16.dp))
        Text(text)
    }
}

private fun shareContent(context: Context, content: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, content)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}
