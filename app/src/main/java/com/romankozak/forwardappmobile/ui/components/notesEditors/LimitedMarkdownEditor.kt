// File: app/src/main/java/com/romankozak/forwardappmobile/ui/components/notesEditors/LimitedMarkdownEditor.kt
package com.romankozak.forwardappmobile.ui.components.notesEditors

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.TextStyle
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
    OutlinedCard(modifier = modifier) {
        // --- START OF FIX ---
        // Read the text style here, in the @Composable context.
        val textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface)
        // --- END OF FIX ---

        // Use SubcomposeLayout to measure the height of the text
        SubcomposeLayout(modifier = Modifier.fillMaxWidth()) { constraints ->
            // Measure the full height using Text, as it's more reliable for this.
            // CRITICAL: We pass the same width and padding modifiers
            // to ensure the text wraps identically to the visible field.
            val fullContentPlaceable = subcompose("fullContent") {
                Text(
                    // Use a space to measure height even for an empty field
                    text = value.text.ifEmpty { " " },
                    modifier = Modifier
                        .fillMaxWidth() // This forces the text to wrap
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    style = textStyle
                )
            }[0].measure(constraints)

            val isOverflowing = fullContentPlaceable.height > maxHeight.toPx()

            // The main visible component
            val editorPlaceable = subcompose("editor") {
                Column {
                    val scrollState = rememberScrollState()
                    Box(
                        Modifier
                            .heightIn(max = maxHeight)
                            .verticalScroll(scrollState) // Add scrolling for better UX
                    ) {
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            textStyle = textStyle, // Use the same style
                            cursorBrush = SolidColor(LocalContentColor.current),
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
                    // Show the "More..." button only if there is an overflow
                    if (isOverflowing) {
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
            }[0].measure(constraints)

            layout(editorPlaceable.width, editorPlaceable.height) {
                editorPlaceable.placeRelative(0, 0)
            }
        }
    }
}
