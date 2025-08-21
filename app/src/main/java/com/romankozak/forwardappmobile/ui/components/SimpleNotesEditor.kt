import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/**
 * Простий, надійний і прогнозований редактор для багаторядкового тексту.
 *
 * @param value Поточний стан редактора (текст, позиція курсора, виділення).
 * @param onValueChange Функція, що викликається при будь-якій зміні стану.
 * @param label Текст-підказка для поля вводу.
 * @param modifier Модифікатор для налаштування зовнішнього вигляду та поведінки.
 */
@Composable
fun SimpleNotesEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp), // Задаємо мінімальну висоту
        label = { Text(label) },
        placeholder = { Text("Введіть будь-які нотатки...") }
    )
}