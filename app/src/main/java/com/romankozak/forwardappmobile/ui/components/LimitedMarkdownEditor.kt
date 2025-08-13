package com.romankozak.forwardappmobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LimitedMarkdownEditor(
    value: String,
    onValueChange: (String) -> Unit,
    maxHeight: Dp,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(modifier = modifier) {
        SubcomposeLayout { constraints ->
            // --- Крок 1: Вимірюємо повну висоту контенту БЕЗ обмежень по висоті ---
            val fullContentPlaceable = subcompose("fullContent") {
                Box(Modifier.padding(16.dp)) {
                    MarkdownText(text = value)
                }
            }[0].measure(constraints.copy(maxHeight = Constraints.Infinity))

            val isOverflowing = fullContentPlaceable.height > maxHeight.toPx()
            val editorMaxHeight = if (isOverflowing) maxHeight else Dp.Infinity

            // --- Крок 2: Вимірюємо видиму частину редактора з ОБМЕЖЕННЯМИ ---
            val editorPlaceable = subcompose("editor") {
                Box(modifier = Modifier.heightIn(max = editorMaxHeight)) {
                    MarkdownText(
                        text = value.ifEmpty { "Notes (Markdown supported)" },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        style = if (value.isEmpty()) {
                            LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            LocalTextStyle.current
                        }
                    )
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        textStyle = TextStyle(color = Color.Transparent),
                        cursorBrush = SolidColor(LocalContentColor.current)
                    )
                }
            }[0].measure(constraints)

            // --- Крок 3: Вимірюємо індикатор "More...", якщо він потрібен ---
            val moreIndicatorPlaceable = if (isOverflowing) {
                subcompose("moreIndicator") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, MaterialTheme.colorScheme.surface)
                                )
                            )
                            .clickable(onClick = onExpandClick),
                        // ✨ ЗМІНА: Вирівнювання по правому краю
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        TextButton(
                            onClick = onExpandClick,
                            // ✨ ДОДАНО: Відступ справа для краси
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("More...")
                        }
                    }
                }[0].measure(constraints)
            } else {
                null
            }

            // --- Крок 4: Розміщуємо все на екрані ---
            layout(editorPlaceable.width, editorPlaceable.height) {
                editorPlaceable.placeRelative(0, 0)
                moreIndicatorPlaceable?.placeRelative(0, editorPlaceable.height - moreIndicatorPlaceable.height)
            }
        }
    }
}