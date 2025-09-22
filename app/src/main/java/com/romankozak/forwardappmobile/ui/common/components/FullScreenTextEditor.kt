package com.romankozak.forwardappmobile.ui.common.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenTextEditor(
    initialText: String,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
    title: String,
) {
    var text by remember(initialText) {
        mutableStateOf(
            TextFieldValue(
                if (initialText.isEmpty()) "- " else initialText
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(onClick = { onSave(text.text) }) {
                        Text("Зберегти")
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { newText ->
                    val oldText = text
                    text = newText

                    // Handle Enter key
                    if (newText.text.length > oldText.text.length && newText.text[oldText.selection.start] == '\n') {
                        val lineStart = newText.text.lastIndexOf('\n', startIndex = oldText.selection.start - 1) + 1
                        val previousLine = newText.text.substring(lineStart, oldText.selection.start)
                        val leadingWhitespace = previousLine.takeWhile { it.isWhitespace() }
                        val listMarker = previousLine.trim().substringBefore(' ') + " "

                        if (previousLine.trim().startsWith("- ")) {
                            val newCursorPos = newText.selection.start + leadingWhitespace.length + listMarker.length
                            val finalText = newText.text.substring(0, newText.selection.start) + leadingWhitespace + listMarker + newText.text.substring(newText.selection.start)
                            text = TextFieldValue(finalText, selection = TextRange(newCursorPos))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (WindowInsets.ime.getBottom(LocalDensity.current) > 0) 56.dp else 0.dp), // Add padding to avoid the editing panel
                label = { Text("Текст запису") },
            )

            if (WindowInsets.ime.getBottom(LocalDensity.current) > 0) {
                ListEditingPanel(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .imePadding(),
                    value = text,
                    onValueChange = { text = it }
                )
            }
        }
    }
}
