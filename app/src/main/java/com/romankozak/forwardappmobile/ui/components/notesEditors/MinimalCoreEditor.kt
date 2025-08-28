// File: app/src/main/java/com/romankozak/forwardappmobile/ui/components/notesEditors/MinimalCoreEditor.kt
package com.romankozak.forwardappmobile.ui.components.notesEditors

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Enhanced minimal editor with proper cursor tracking and scrolling behavior.
 * This version addresses the cursor visibility issue by implementing manual scroll tracking.
 */
@Composable
fun MinimalCoreEditor(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    // Track layout information
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var containerHeight by remember { mutableStateOf(0) }
    var isFocused by remember { mutableStateOf(false) }

    // Function to scroll to cursor position
    LaunchedEffect(value.selection.end, textLayoutResult, containerHeight, isFocused) {
        if (isFocused && textLayoutResult != null && containerHeight > 0) {
            val layout = textLayoutResult ?: return@LaunchedEffect

            // Get cursor position
            val cursorOffset = minOf(value.selection.end, value.text.length)
            if (cursorOffset >= 0) {
                try {
                    // Get the line containing the cursor
                    val cursorLine = layout.getLineForOffset(cursorOffset)
                    val lineTop = layout.getLineTop(cursorLine)
                    val lineBottom = layout.getLineBottom(cursorLine)

                    // Convert to pixels
                    val lineTopPx = with(density) { lineTop }
                    val lineBottomPx = with(density) { lineBottom }

                    val currentScrollValue = scrollState.value
                    val visibleTop = currentScrollValue
                    val visibleBottom = currentScrollValue + containerHeight

                    // Calculate needed scroll adjustment
                    val scrollTo = when {
                        lineBottomPx > visibleBottom -> {
                            // Cursor is below visible area - scroll down
                            (lineBottomPx - containerHeight + 50).coerceAtLeast(0f).toInt()
                        }
                        lineTopPx < visibleTop -> {
                            // Cursor is above visible area - scroll up
                            (lineTopPx - 50).coerceAtLeast(0f).toInt()
                        }
                        else -> null // Cursor is visible, no scroll needed
                    }

                    scrollTo?.let { targetScroll ->
                        // Small delay to ensure smooth scrolling after text changes
                        delay(16)
                        scrollState.animateScrollTo(targetScroll)
                    }
                } catch (e: Exception) {
                    // Fallback: scroll to approximate position based on cursor position
                    val approximateLineHeight = 20.dp
                    val lineHeightPx = with(density) { approximateLineHeight.toPx() }
                    val approximateLine = cursorOffset.toFloat() / 50 // Rough estimate
                    val approximateY = approximateLine * lineHeightPx

                    if (approximateY > scrollState.value + containerHeight - 100) {
                        scrollState.animateScrollTo((approximateY - containerHeight / 2).coerceAtLeast(0f).toInt())
                    }
                }
            }
        }
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .clipToBounds() // Ensure content doesn't overflow
            .onGloballyPositioned { coordinates ->
                containerHeight = coordinates.size.height
            }
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .verticalScroll(scrollState),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = LocalContentColor.current
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        onTextLayout = { layoutResult ->
            textLayoutResult = layoutResult
        }
    )
}