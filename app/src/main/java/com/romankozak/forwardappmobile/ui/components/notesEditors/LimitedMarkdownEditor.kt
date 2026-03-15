package com.romankozak.forwardappmobile.ui.components.notesEditors

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

data class LimitedMarkdownEditorState(
    val value: TextFieldValue,
    val maxHeight: Dp,
)

data class LimitedMarkdownEditorActions(
    val onValueChange: (TextFieldValue) -> Unit,
    val onExpandClick: () -> Unit,
    val onCopy: () -> Unit,
)

@Composable
fun LimitedMarkdownEditor(
    state: LimitedMarkdownEditorState,
    actions: LimitedMarkdownEditorActions,
    modifier: Modifier = Modifier,
) {
    var isOverflowing by remember(state.value.text) { mutableStateOf(false) }
    val textStyle =
        LocalTextStyle.current.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
        )
    val density = LocalDensity.current

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DescriptionHeader()
            EditorInputArea(
                state = state,
                textStyle = textStyle,
                density = density,
                onValueChange = actions.onValueChange,
                onOverflowChanged = { isOverflowing = it },
            )
            EditorFooter(
                isOverflowing = isOverflowing,
                onCopy = actions.onCopy,
                onExpandClick = actions.onExpandClick,
            )
        }
    }
}

@Composable
private fun DescriptionHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Опис",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EditorInputArea(
    state: LimitedMarkdownEditorState,
    textStyle: androidx.compose.ui.text.TextStyle,
    density: androidx.compose.ui.unit.Density,
    onValueChange: (TextFieldValue) -> Unit,
    onOverflowChanged: (Boolean) -> Unit,
) {
    Box(
        modifier =
            Modifier
                .heightIn(max = state.maxHeight)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
    ) {
        BasicTextField(
            value = state.value,
            onValueChange = onValueChange,
            textStyle = textStyle,
            modifier = Modifier.fillMaxWidth(),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box {
                    if (state.value.text.isEmpty()) {
                        Text(
                            text = "Додайте опис і нотатки...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    innerTextField()
                }
            },
            onTextLayout = {
                val maxHeightPx = with(density) { state.maxHeight.toPx() }
                onOverflowChanged(it.size.height > maxHeightPx)
            },
        )
    }
}

@Composable
private fun EditorFooter(
    isOverflowing: Boolean,
    onCopy: () -> Unit,
    onExpandClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onCopy,
            modifier = Modifier.size(40.dp),
            colors =
                IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Копіювати опис",
                modifier = Modifier.size(20.dp),
            )
        }

        AnimatedVisibility(
            visible = isOverflowing,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            TextButton(
                onClick = onExpandClick,
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Більше")
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .padding(start = 4.dp)
                            .size(18.dp),
                )
            }
        }
    }
}
