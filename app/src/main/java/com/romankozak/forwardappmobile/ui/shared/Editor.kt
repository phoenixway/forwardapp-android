package com.romankozak.forwardappmobile.ui.shared

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.romankozak.forwardappmobile.ui.shared.styleLine

@Composable
fun Editor(
    content: TextFieldValue,
    onContentChange: (TextFieldValue) -> Unit,
    onTitleChange: (String) -> Unit,
    onEnter: (TextFieldValue) -> Unit,
    onToggleFold: (Int) -> Unit,
    collapsedLines: Set<Int>,
    currentLine: Int?,
    modifier: Modifier = Modifier,
    isToolbarVisible: Boolean,
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val contentFocusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(content.selection, textLayoutResult, isToolbarVisible) {
        if (!isToolbarVisible) return@LaunchedEffect
        val layoutResult = textLayoutResult ?: return@LaunchedEffect
        val cursorRect = layoutResult.getCursorRect(content.selection.start)

        val paddedRect = cursorRect.copy(
            top = (cursorRect.top - 150).coerceAtLeast(0f),
            bottom = (cursorRect.bottom + 150)
        )

        coroutineScope.launch {
            delay(350) // Wait for animations
            bringIntoViewRequester.bringIntoView(paddedRect)
        }
    }

    val highlightColor = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val accentColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
    ) {
        Gutter(
            lines = content.text.lines(),
            collapsedLines = collapsedLines,
            onToggleFold = onToggleFold,
            lineHeight = 24.sp
        )

        BasicTextField(
            value = content,
            onValueChange = {
                newValue ->
                val firstLine = newValue.text.lines().firstOrNull() ?: ""

                val markerRegex = Regex(
                    """^(\s*)(\*|•|\d+\.|\[[\s*x?\])\\s*""",
                    RegexOption.IGNORE_CASE
                )
                val title = firstLine.replaceFirst(markerRegex, "").trim()

                onTitleChange(title)

                val oldValue = content
                if (
                    newValue.text.length > oldValue.text.length &&
                    newValue.text.count { it == '\n' } > oldValue.text.count { it == '\n' }
                ) {
                    onEnter(newValue)
                } else {
                    onContentChange(newValue)
                }
            },
            onTextLayout = { result ->
                textLayoutResult = result
            },
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
                .fillMaxHeight()
                .focusRequester(contentFocusRequester)
                .bringIntoViewRequester(bringIntoViewRequester)
                .drawBehind {
                    currentLine?.let { line ->
                        textLayoutResult?.let { layoutResult ->
                            if (line < layoutResult.lineCount) {
                                val top = layoutResult.getLineTop(line)
                                val bottom = layoutResult.getLineBottom(line)
                                drawRect(
                                    color = highlightColor,
                                    topLeft = Offset(0f, top),
                                    size = Size(size.width, bottom - top)
                                )
                            }
                        }
                    }
                },
            textStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, color = textColor),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = ListVisualTransformation(collapsedLines, textColor, accentColor),
        )
    }
}

@Composable
private fun Gutter(lines: List<String>, collapsedLines: Set<Int>, onToggleFold: (Int) -> Unit, lineHeight: TextUnit) {
    val focusManager = LocalFocusManager.current
    val lineHeightDp = with(LocalDensity.current) { lineHeight.toDp() }
    Column(modifier = Modifier.width(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        lines.forEachIndexed { index, line ->
            val indent = line.takeWhile { it.isWhitespace() }.length
            val nextIndent =
                if (index + 1 < lines.size) lines[index + 1].takeWhile { it.isWhitespace() }.length else -1
            val isParent = nextIndent > indent && line.isNotBlank()

            Box(modifier = Modifier.height(lineHeightDp), contentAlignment = Alignment.Center) {
                if (isParent) {
                    val isCollapsed = collapsedLines.contains(index)
                    val icon =
                        if (isCollapsed) Icons.Default.ChevronRight else Icons.Default.KeyboardArrowDown
                    Icon(
                        imageVector = icon,
                        contentDescription = if (isCollapsed) "Expand" else "Collapse",
                        modifier =
                            Modifier
                                .size(16.dp)
                                .clickable {
                                    focusManager.clearFocus()
                                    onToggleFold(index)
                                },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}



private class ListVisualTransformation(
    private val collapsedLines: Set<Int>,
    private val textColor: Color,
    private val accentColor: Color,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        val lines = originalText.lines()
        val visibleLines = mutableListOf<IndexedValue<String>>()

        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            visibleLines.add(IndexedValue(i, line))
            if (collapsedLines.contains(i)) {
                val indent = line.takeWhile { it.isWhitespace() }.length
                i++
                while (
                    i < lines.size &&
                    (lines[i].isBlank() || lines[i].takeWhile { it.isWhitespace() }.length > indent)
                ) {
                    i++
                }
            } else {
                i++
            }
        }

        val transformedText = buildAnnotatedString {
            visibleLines.forEachIndexed { visibleIndex, indexedValue ->
                val (_, line) = indexedValue
                styleLine(line, textColor, accentColor)
                if (visibleIndex < visibleLines.size - 1) {
                    append("\n")
                }
            }
        }

        val offsetMapping =
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    if (offset <= 0) return 0
                    val prefix = originalText.substring(0, offset)
                    val parts = prefix.lines()
                    val originalLineIndex = parts.size - 1
                    val charInLine = parts.lastOrNull()?.length ?: 0

                    var transformedLineStart = 0
                    var found = false
                    for (v in visibleLines) {
                        if (v.index == originalLineIndex) {
                            found = true
                            break
                        }
                        transformedLineStart += v.value.length + 1
                    }
                    if (!found) return transformedText.length
                    return (transformedLineStart + charInLine).coerceIn(0, transformedText.length)
                }

                override fun transformedToOriginal(offset: Int): Int {
                    if (offset <= 0) return 0
                    val prefix = transformedText.substring(0, offset)
                    val parts = prefix.lines()
                    val transformedLineIndex = parts.size - 1
                    val charInLine = parts.lastOrNull()?.length ?: 0
                    if (transformedLineIndex >= visibleLines.size) return originalText.length
                    val originalLineIndex = visibleLines[transformedLineIndex].index
                    val originalLineStart = lines.take(originalLineIndex).sumOf { it.length + 1 }
                    return (originalLineStart + charInLine).coerceIn(0, originalText.length)
                }
            }

        return TransformedText(transformedText, offsetMapping)
    }
}
