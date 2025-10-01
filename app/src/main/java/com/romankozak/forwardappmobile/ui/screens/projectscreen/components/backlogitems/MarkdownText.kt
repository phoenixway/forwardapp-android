package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.backlogitems

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    isCompleted: Boolean = false,
    obsidianVaultName: String = "",
    onTagClick: (String) -> Unit = {},
    onTextClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        maxLines = maxLines,
    )
}