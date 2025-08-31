// File: app/src/main/java/com/romankozak/forwardappmobile/ui/components/notesEditors/StyledTextFieldWrapper.kt
package com.romankozak.forwardappmobile.ui.components.notesEditors

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Оновлена версія, яка обробляє автостворення списків всередині onValueChange
 * для надійної роботи з будь-якою клавіатурою.
 */
@Composable
fun StyledTextFieldWrapper(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
) {
    TextField(
        value = value,
        onValueChange = { newTextFieldValue ->
            // Замість onKeyEvent, вся логіка тепер тут
            onValueChange(handleListContinuation(value, newTextFieldValue))
        },
        modifier = modifier.fillMaxSize(),
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = MaterialTheme.typography.bodyLarge.fontSize
        ),
        placeholder = {
            Text(
                text = "Почніть друкувати...\nПідказка: введіть '- ' або '* ' для створення списку",
                style = TextStyle(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize
                )
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        singleLine = false,
    )
}

/**
 * Функція, що обробляє логіку продовження/завершення маркованого списку.
 * @param oldTextValue Попередній стан текстового поля.
 * @param newTextValue Новий стан після дії користувача.
 * @return Модифікований TextFieldValue з новим маркером списку, або без нього.
 */
private fun handleListContinuation(oldTextValue: TextFieldValue, newTextValue: TextFieldValue): TextFieldValue {
    val oldText = oldTextValue.text
    val newText = newTextValue.text
    val oldCursorPos = oldTextValue.selection.end

    // Перевіряємо, чи була дія саме "додавання одного символу нового рядка"
    if (newText.length == oldText.length + 1 && newText.endsWith('\n', ignoreCase = false) && newText.substring(0, oldCursorPos) == oldText) {

        val lineStart = oldText.lastIndexOf('\n', oldCursorPos - 1) + 1
        val previousLine = oldText.substring(lineStart, oldCursorPos)

        val bulletPatterns = listOf("- ", "* ", "• ")
        val matchedPattern = bulletPatterns.find { previousLine.startsWith(it) }

        if (matchedPattern != null) {
            return if (previousLine.trim() == matchedPattern.trim()) {
                // Випадок, коли рядок містить тільки маркер - завершуємо список
                // Видаляємо і маркер, і доданий новий рядок
                val textBefore = oldText.substring(0, lineStart)
                val textAfter = oldText.substring(oldCursorPos)
                val resultingText = textBefore + textAfter
                TextFieldValue(
                    text = resultingText,
                    selection = TextRange(lineStart)
                )
            } else {
                // Випадок, коли у рядку є текст - продовжуємо список
                // Додаємо новий маркер у новому рядку
                val textWithContinuation = newText + matchedPattern
                TextFieldValue(
                    text = textWithContinuation,
                    selection = TextRange(textWithContinuation.length)
                )
            }
        }
    }
    // Якщо це не додавання нового рядка, або ми не в списку, повертаємо новий стан як є
    return newTextValue
}