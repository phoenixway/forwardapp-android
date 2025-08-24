package com.romankozak.forwardappmobile.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.romankozak.forwardappmobile.ui.components.notesEditors.MarkdownEditorViewer

@OptIn(ExperimentalMaterial3Api::class)
// Файл: FullScreenMarkdownEditor.kt (виправлена версія)

@Composable
fun FullScreenMarkdownEditor(
    initialValue: TextFieldValue,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    // ✨ ЗМІНА 1: Внутрішній стан тепер має тип TextFieldValue
    var textFieldValue by remember { mutableStateOf(initialValue) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Редагувати опис") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Закрити")
                        }
                    },
                    actions = {
                        // ✨ ЗМІНА 2: При збереженні беремо текст з textFieldValue
                        IconButton(onClick = { onSave(textFieldValue.text) }) {
                            Icon(Icons.Default.Done, contentDescription = "Зберегти")
                        }
                    }
                )
            }
        ) { paddingValues ->
            // ✨ ЗМІНА 3: Передаємо повний стан і функцію оновлення до дочірнього компонента
            MarkdownEditorViewer(
                value = textFieldValue,
                onValueChange = { newTextFieldValue ->
                    textFieldValue = newTextFieldValue
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}