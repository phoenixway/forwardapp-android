package com.romankozak.forwardappmobile.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenMarkdownEditor(
    initialText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    // Внутрішній стан для збереження тексту, поки діалог відкритий
    var text by remember { mutableStateOf(initialText) }

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
                        // Кнопка "Зберегти" використовує внутрішній стан 'text'
                        IconButton(onClick = { onSave(text) }) {
                            Icon(Icons.Default.Done, contentDescription = "Зберегти")
                        }
                    }
                )
            }
        ) { paddingValues ->
            // ✨ ОСНОВНА ЗМІНА:
            // Замість старого WebView, тепер тут викликається наш новий компонент.
            MarkdownEditorViewer(
                initialText = text,
                // Коли текст в редакторі змінюється, ми оновлюємо внутрішній стан.
                onTextChange = { newText ->
                    text = newText
                },
                // Передаємо відступи від Scaffold, щоб компонент правильно вписався в екран.
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}