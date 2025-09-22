package com.romankozak.forwardappmobile.ui.components.notesEditors

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LimitedMarkdownEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    maxHeight: Dp,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCopy: () -> Unit,
) {
    var isOverflowing by remember(value.text) { mutableStateOf(false) }
    val textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface)
    val density = LocalDensity.current

    OutlinedCard(modifier = modifier) {
        Column {
            Box(
                modifier =
                    Modifier
                        .heightIn(max = maxHeight)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = textStyle,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    cursorBrush = SolidColor(LocalContentColor.current),
                    onTextLayout = {
                        val maxHeightPx = with(density) { maxHeight.toPx() }
                        isOverflowing = it.size.height > maxHeightPx
                    },
                    decorationBox = { innerTextField ->
                        if (value.text.isEmpty()) {
                            Text(
                                text = "Notes...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy description")
                }
                AnimatedVisibility(visible = isOverflowing) {
                    TextButton(onClick = onExpandClick) {
                        Text("More...")
                    }
                }
            }
        }
    }
}
