package com.romankozak.forwardappmobile.ui.common.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun ListEditingPanel(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            ListEditingClipboardRow(value = value, onValueChange = onValueChange, clipboardManager = clipboardManager)
            ListEditingMoveRow(value = value, onValueChange = onValueChange)
        }
    }
}

@Composable
private fun ListEditingClipboardRow(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    clipboardManager: ClipboardManager,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        StyledIconButton(onClick = { onValueChange(ListEditingLogic.toggleList(value)) }) {
            Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "Toggle List")
        }
        StyledIconButton(onClick = { onValueChange(ListEditingLogic.indent(value)) }) {
            Icon(Icons.AutoMirrored.Filled.FormatIndentIncrease, contentDescription = "Indent")
        }
        StyledIconButton(onClick = { onValueChange(ListEditingLogic.outdent(value)) }) {
            Icon(Icons.AutoMirrored.Filled.FormatIndentDecrease, contentDescription = "Outdent")
        }
        StyledIconButton(onClick = {
            val line = extractCurrentLine(value)
            clipboardManager.setPrimaryClip(ClipData.newPlainText("list item", line))
            onValueChange(ListEditingLogic.deleteLine(value))
        }) {
            Icon(Icons.Default.ContentCut, contentDescription = "Cut Line")
        }
        StyledIconButton(onClick = {
            val line = extractCurrentLine(value)
            clipboardManager.setPrimaryClip(ClipData.newPlainText("list item", line))
        }) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Line")
        }
        StyledIconButton(onClick = {
            val clipboardText = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            onValueChange(ListEditingLogic.pasteLine(value, clipboardText))
        }) {
            Icon(Icons.Default.ContentPaste, contentDescription = "Paste Line")
        }
    }
}

private fun extractCurrentLine(value: TextFieldValue): String {
    val text = value.text
    val cursor = value.selection.start.coerceIn(0, text.length)
    val lineStart =
        text.lastIndexOf('\n', startIndex = (cursor - 1).coerceAtLeast(0))
            .let { if (it == -1) 0 else it + 1 }
    val lineEnd = text.indexOf('\n', startIndex = cursor).let { if (it == -1) text.length else it }
    return text.substring(lineStart, lineEnd)
}

@Composable
private fun ListEditingMoveRow(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        StyledIconButton(onClick = { onValueChange(ListEditingLogic.moveLineUp(value)) }) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Line Up")
        }
        StyledIconButton(onClick = { onValueChange(ListEditingLogic.moveLineDown(value)) }) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Line Down")
        }
        StyledIconButton(onClick = { onValueChange(ListEditingLogic.moveBlockUp(value)) }) {
            Icon(Icons.Default.VerticalAlignTop, contentDescription = "Move Block Up")
        }
        StyledIconButton(onClick = { onValueChange(ListEditingLogic.moveBlockDown(value)) }) {
            Icon(Icons.Default.VerticalAlignBottom, contentDescription = "Move Block Down")
        }
        StyledIconButton(onClick = { onValueChange(ListEditingLogic.deleteLine(value)) }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Line")
        }
    }
}

@Composable
private fun StyledIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.size(40.dp).clip(CircleShape),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        IconButton(onClick = onClick) {
            content()
        }
    }
}
