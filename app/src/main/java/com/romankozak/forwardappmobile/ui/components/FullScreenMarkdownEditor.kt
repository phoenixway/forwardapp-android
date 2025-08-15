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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenMarkdownEditor(
    // ✨ ЗМІНА 1: Змінюємо тип вхідного параметра з String на TextFieldValue
    initialValue: TextFieldValue,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    // ✨ ЗМІНА 2: Ініціалізуємо внутрішній стан текстом з initialValue.text
    var text by remember { mutableStateOf(initialValue.text) }

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
                        // Кнопка "Зберегти" передає звичайний String, як і очікує ViewModel
                        IconButton(onClick = { onSave(text) }) {
                            Icon(Icons.Default.Done, contentDescription = "Зберегти")
                        }
                    }
                )
            }
        ) { paddingValues ->
            // Використовуємо наш універсальний компонент MarkdownEditorViewer
            MarkdownEditorViewer(
                initialText = text,
                onTextChange = { newText ->
                    text = newText
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}