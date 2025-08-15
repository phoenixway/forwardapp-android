package com.romankozak.forwardappmobile.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MarkdownEditorViewer(
    initialText: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    var isEditMode by remember { mutableStateOf(true) }

    Column(modifier = modifier) {
        // Перемикач режимів "Редактор" / "Перегляд"
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            SegmentedButton(
                selected = isEditMode,
                onClick = { isEditMode = true },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Редактор")
            }
            SegmentedButton(
                selected = !isEditMode,
                onClick = { isEditMode = false },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Перегляд")
            }
        }

        // Анімована зміна між редактором і переглядом
        AnimatedContent(
            targetState = isEditMode,
            label = "EditorViewerAnimation",
            modifier = Modifier
                .padding(top = 16.dp)
                .padding(horizontal = 16.dp)

                .weight(1f)
        ) { isEditing ->
            if (isEditing) {
                // НАТИВНИЙ РЕДАКТОР ANDROID
                OutlinedTextField(
                    value = text,
                    onValueChange = { newText ->
                        text = newText
                        onTextChange(newText)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // WEBVIEW ДЛЯ РЕНДЕРИНГУ
                AndroidView(
                    factory = { context ->
                        WebViewMarkdownViewer(context)
                    },
                    update = { viewer ->
                        viewer.renderMarkdown(text)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}