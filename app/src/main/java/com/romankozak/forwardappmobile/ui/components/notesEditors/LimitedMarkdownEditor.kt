// File: app/src/main/java/com/romankozak/forwardappmobile/ui/components/notesEditors/LimitedMarkdownEditor.kt
package com.romankozak.forwardappmobile.ui.components.notesEditors

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
    modifier: Modifier = Modifier
) {
    // State to track if the text is overflowing. We check it again when the text changes.
    var isOverflowing by remember(value.text) { mutableStateOf(false) }
    val textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface)
    val density = LocalDensity.current

    OutlinedCard(modifier = modifier) {
        Column {
            // This Box will handle the clipping and scrolling for the text field.
            Box(
                modifier = Modifier
                    .heightIn(max = maxHeight)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = textStyle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    cursorBrush = SolidColor(LocalContentColor.current),
                    onTextLayout = {
                        // This callback is invoked when the text layout is calculated.
                        // We convert maxHeight from Dp to Px to compare it with the text's height.
                        val maxHeightPx = with(density) { maxHeight.toPx() }
                        // Update the overflow state based on the comparison.
                        isOverflowing = it.size.height > maxHeightPx
                    },
                    decorationBox = { innerTextField ->
                        // Placeholder if the field is empty
                        if (value.text.isEmpty()) {
                            Text(
                                text = "Notes...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                )
            }

            // Show the "More..." button only if there is an overflow.
            // Animate its appearance and disappearance.
            AnimatedVisibility(visible = isOverflowing) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    TextButton(onClick = onExpandClick) {
                        Text("More...")
                    }
                }
            }
        }
    }
}
