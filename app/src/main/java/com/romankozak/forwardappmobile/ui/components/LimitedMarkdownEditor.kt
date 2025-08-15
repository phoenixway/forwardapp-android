package com.romankozak.forwardappmobile.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Constraints
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
        // Використовуємо SubcomposeLayout для вимірювання висоти тексту
        SubcomposeLayout { constraints ->
            // Вимірюємо повну висоту текстового поля
            val fullContentPlaceable = subcompose("fullContent") {
                // Використовуємо BasicTextField для точного вимірювання
                BasicTextField(
                    value = value,
                    onValueChange = {}, // onValueChange не потрібен для вимірювання
                    modifier = Modifier.padding(16.dp), // Важливо, щоб відступи збігалися
                    textStyle = LocalTextStyle.current,
                    readOnly = true
                )
            }[0].measure(constraints)

            val isOverflowing = fullContentPlaceable.height > maxHeight.toPx()

            // Основний видимий компонент
            val editorPlaceable = subcompose("editor") {
                Column {
                    Box(Modifier.heightIn(max = maxHeight)) {
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            textStyle = LocalTextStyle.current.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(LocalContentColor.current),
                            decorationBox = { innerTextField ->
                                // Placeholder, якщо поле пусте
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
                    // Показуємо кнопку "More...", тільки якщо є переповнення
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