package com.romankozak.forwardappmobile.ui.common.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatIndentDecrease
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun ListEditingPanel(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Column {
            Row {
                IconButton(onClick = { onValueChange(ListEditingLogic.toggleList(value)) }) {
                    Icon(Icons.Default.FormatListBulleted, contentDescription = "Toggle List")
                }
                IconButton(onClick = { onValueChange(ListEditingLogic.moveLineUp(value)) }) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Line Up")
                }
                IconButton(onClick = { onValueChange(ListEditingLogic.moveLineDown(value)) }) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Line Down")
                }
                IconButton(onClick = { onValueChange(ListEditingLogic.indent(value)) }) {
                    Icon(Icons.Default.FormatIndentIncrease, contentDescription = "Indent")
                }
                IconButton(onClick = { onValueChange(ListEditingLogic.outdent(value)) }) {
                    Icon(Icons.Default.FormatIndentDecrease, contentDescription = "Outdent")
                }
            }
            Row {
                IconButton(onClick = { onValueChange(ListEditingLogic.moveBlockUp(value)) }) {
                    Icon(Icons.Default.VerticalAlignTop, contentDescription = "Move Block Up")
                }
                IconButton(onClick = { onValueChange(ListEditingLogic.moveBlockDown(value)) }) {
                    Icon(Icons.Default.VerticalAlignBottom, contentDescription = "Move Block Down")
                }
                IconButton(onClick = { onValueChange(ListEditingLogic.indentBlock(value)) }) {
                    Icon(Icons.Default.FormatIndentIncrease, contentDescription = "Indent Block")
                }
                IconButton(onClick = { onValueChange(ListEditingLogic.outdentBlock(value)) }) {
                    Icon(Icons.Default.FormatIndentDecrease, contentDescription = "Outdent Block")
                }
            }
            Row {
                IconButton(onClick = { onValueChange(ListEditingLogic.deleteLine(value)) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Line")
                }
                IconButton(onClick = {
                    val line = ListEditingLogic.getLineForClipboard(value)
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("list item", line))
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Line")
                }
                IconButton(onClick = {
                    val line = ListEditingLogic.getLineForClipboard(value)
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("list item", line))
                    onValueChange(ListEditingLogic.deleteLine(value))
                }) {
                    Icon(Icons.Default.ContentCut, contentDescription = "Cut Line")
                }
                IconButton(onClick = {
                    val clipboardText = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                    onValueChange(ListEditingLogic.pasteLine(value, clipboardText))
                }) {
                    Icon(Icons.Default.ContentPaste, contentDescription = "Paste Line")
                }
            }
        }
    }
}
