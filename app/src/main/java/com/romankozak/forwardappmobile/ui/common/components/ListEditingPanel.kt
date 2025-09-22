package com.romankozak.forwardappmobile.ui.common.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatIndentDecrease
import androidx.compose.material.icons.filled.FormatIndentIncrease
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun ListEditingPanel(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
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

            Divider(modifier = Modifier.height(32.dp).width(1.dp))

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
    }
}
