
package com.romankozak.forwardappmobile.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun EditorToolbar(
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onChecklist: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        IconButton(onClick = onMoveUp) {
            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
        }
        IconButton(onClick = onMoveDown) {
            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
        }
        IconButton(onClick = onCopy) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
        }
        IconButton(onClick = onPaste) {
            Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
        }
        IconButton(onClick = onBold) {
            Icon(Icons.Default.FormatBold, contentDescription = "Bold")
        }
        IconButton(onClick = onItalic) {
            Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
        }
        IconButton(onClick = onUnderline) {
            Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline")
        }
        IconButton(onClick = onChecklist) {
            Icon(Icons.Default.CheckBox, contentDescription = "Checklist")
        }
    }
}
