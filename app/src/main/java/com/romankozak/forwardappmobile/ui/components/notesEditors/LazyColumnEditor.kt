// File: app/src/main/java/com/romankozak/forwardappmobile/ui/components/notesEditors/LazyColumnEditor.kt
package com.romankozak.forwardappmobile.ui.components.notesEditors

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Alternative approach using LazyColumn which often handles scrolling better than verticalScroll.
 * This can be a drop-in replacement for MinimalCoreEditor if the enhanced version doesn't work.
 */
@Composable
fun LazyColumnEditor(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val density = LocalDensity.current

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var isFocused by remember { mutableStateOf(false) }

    // Cursor tracking with LazyColumn
    LaunchedEffect(value.selection.end, textLayoutResult, isFocused) {
        if (isFocused && textLayoutResult != null) {
            val layout = textLayoutResult ?: return@LaunchedEffect
            val cursorOffset = minOf(value.selection.end, value.text.length)

            if (cursorOffset >= 0) {
                try {
                    val cursorLine = layout.getLineForOffset(cursorOffset)
                    val lineHeight = layout.getLineBottom(0) - layout.getLineTop(0)

                    // Convert line to approximate item index (assuming ~3 lines per "item")
                    val approximateItemIndex = (cursorLine / 3).coerceAtMost(0)

                    delay(50) // Small delay for smoother scrolling
                    lazyListState.animateScrollToItem(approximateItemIndex)
                } catch (e: Exception) {
                    // Fallback - scroll based on text length
                    val approximateItem = (value.text.length / 300).coerceAtMost(0)
                    lazyListState.animateScrollToItem(approximateItem)
                }
            }
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier,
        contentPadding = PaddingValues(0.dp)
    ) {
        item {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 200.dp) // Ensure minimum height
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                    },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = LocalContentColor.current
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                onTextLayout = { layoutResult ->
                    textLayoutResult = layoutResult
                }
            )
        }
    }
}