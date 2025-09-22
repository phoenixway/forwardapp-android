package com.romankozak.forwardappmobile.ui.components.notesEditors

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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun MinimalCoreEditor(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
) {
    val scrollState = rememberScrollState()
    val density = LocalDensity.current

    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var containerHeight by remember { mutableStateOf(0) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(value.selection.end, textLayoutResult, containerHeight, isFocused) {
        if (isFocused && textLayoutResult != null && containerHeight > 0) {
            val layout = textLayoutResult ?: return@LaunchedEffect

            val cursorOffset = minOf(value.selection.end, value.text.length)
            if (cursorOffset >= 0) {
                try {
                    val cursorLine = layout.getLineForOffset(cursorOffset)
                    val lineTop = layout.getLineTop(cursorLine)
                    val lineBottom = layout.getLineBottom(cursorLine)

                    val lineTopPx = with(density) { lineTop }
                    val lineBottomPx = with(density) { lineBottom }

                    val currentScrollValue = scrollState.value
                    val visibleTop = currentScrollValue
                    val visibleBottom = currentScrollValue + containerHeight

                    val scrollTo =
                        when {
                            lineBottomPx > visibleBottom -> {
                                (lineBottomPx - containerHeight + 50).coerceAtLeast(0f).toInt()
                            }
                            lineTopPx < visibleTop -> {
                                (lineTopPx - 50).coerceAtLeast(0f).toInt()
                            }
                            else -> null
                        }

                    scrollTo?.let { targetScroll ->
                        delay(16)
                        scrollState.animateScrollTo(targetScroll)
                    }
                } catch (e: Exception) {
                    val approximateLineHeight = 20.dp
                    val lineHeightPx = with(density) { approximateLineHeight.toPx() }
                    val approximateLine = cursorOffset.toFloat() / 50
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
        modifier =
            modifier
                .clipToBounds()
                .onGloballyPositioned { coordinates ->
                    containerHeight = coordinates.size.height
                }.onFocusChanged { focusState ->
                    isFocused = focusState.isFocused
                }.verticalScroll(scrollState),
        textStyle =
            MaterialTheme.typography.bodyLarge.copy(
                color = LocalContentColor.current,
            ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        onTextLayout = { layoutResult ->
            textLayoutResult = layoutResult
        },
    )
}
