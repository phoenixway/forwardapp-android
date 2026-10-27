package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.ui.components.notesEditors.MarkdownEditorViewer

/** Occurrence-local journal, deliberately distinct from the recurring template description. */
@Composable
internal fun TaskNotesTab(state: EditTaskUiState, onNotesChange: (String) -> Unit, onDescriptionChange: (String) -> Unit) {
    var preview by rememberSaveable(state.task?.id) { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        state.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NotesSectionLabel("Нотатки дня", Modifier.weight(1f))
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Дії нотаток")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text(if (preview) "Редагувати текст" else "Переглянути Markdown") },
                        onClick = { preview = !preview; menuOpen = false })
                    DropdownMenuItem(text = { Text("Про ці поля") },
                        onClick = { showHelp = true; menuOpen = false })
                }
            }
        }
        TaskTextEditor("${state.task?.id}:notes", "Нотатки дня", "Додати нотатку…",
            state.notes, onNotesChange, preview, state.isSaving)
        Spacer(Modifier.height(24.dp))
        NotesSectionLabel("Інструкція")
        Spacer(Modifier.height(8.dp))
        TaskTextEditor("${state.task?.id}:description", "Інструкція", "Додати інструкцію…",
            state.description, onDescriptionChange, preview, state.isSaving)
    }
    if (showHelp) AlertDialog(onDismissRequest = { showHelp = false }, title = { Text("Інструкція та нотатки") },
        text = { Text("Це два незалежні тексти. Інструкція може бути спільною для повторень. Нотатки дня — план, думки та записи виконання конкретного завдання; у наступні повторення вони не копіюються.\n\nОбидва поля підтримують Markdown. Збереження — кнопкою вгорі екрана.") },
        confirmButton = { TextButton(onClick = { showHelp = false }) { Text("Зрозуміло") } })
}

@Composable
private fun NotesSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun TaskTextEditor(key: String, label: String, placeholder: String, text: String,
    onChange: (String) -> Unit, preview: Boolean, saving: Boolean) {
    var value by rememberSaveable(key, stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(text)) }
    LaunchedEffect(text) { if (value.text != text) value = TextFieldValue(text) }
    if (preview) {
        if (text.isBlank()) Text("Немає тексту", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        else MarkdownEditorViewer(value, {}, isEditMode = false,
            modifier = Modifier.fillMaxWidth().height(240.dp).semantics { contentDescription = label })
    } else BasicTextField(
        value = value,
        onValueChange = { next -> if (!saving) { value = next; onChange(next.text) } },
        readOnly = saving,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = 8.dp)
            .semantics { contentDescription = label },
        decorationBox = { innerTextField ->
            Box {
                if (value.text.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                innerTextField()
            }
        },
    )
}

/** Preserve existing explicit Save; accidental Back/dismiss cannot silently discard journal text. */
@Composable
internal fun rememberTaskNotesCloseRequest(
    state: EditTaskUiState,
    viewModel: EditTaskViewModel,
    onClose: () -> Unit,
    onKeepEditing: () -> Unit = {},
): () -> Unit {
    var confirm by rememberSaveable(state.task?.id) { mutableStateOf(false) }
    val close: () -> Unit = {
        if (!state.isSaving) {
            if (viewModel.hasUnsavedNotes) confirm = true else { viewModel.reset(); onClose() }
        }
    }
    BackHandler(enabled = viewModel.hasUnsavedNotes || state.isSaving) { close() }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false; onKeepEditing() },
        title = { Text("Зберегти нотатки?") },
        text = { Text(state.saveError ?: "У завданні є незбережений текст.") },
        confirmButton = { TextButton(enabled = !state.isSaving && !state.isLoadingDetails && state.loadError == null, onClick = viewModel::saveTask) { Text("Зберегти") } },
        dismissButton = { Row {
            TextButton(enabled = !state.isSaving, onClick = { confirm = false; onKeepEditing() }) { Text("Продовжити") }
            TextButton(enabled = !state.isSaving, onClick = { confirm = false; viewModel.reset(); onClose() }) { Text("Відкинути") }
        } },
    )
    return close
}
