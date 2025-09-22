package com.romankozak.forwardappmobile.ui.screens.customlist

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.romankozak.forwardappmobile.ui.common.components.FullScreenTextEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomListScreen(viewModel: CustomListViewModel = hiltViewModel(), onNavigateBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var textValue by remember { mutableStateOf(TextFieldValue()) }

    LaunchedEffect(uiState.list) {
        val list = uiState.list
        if (list != null && list.content != textValue.text) {
            val content = list.content ?: ""
            textValue = TextFieldValue(
                if (content.isEmpty()) "- " else content
            )
        }
    }

    FullScreenTextEditor(
        title = uiState.list?.name ?: "Custom List",
        value = textValue,
        onValueChange = { newText ->
            val oldText = textValue
            textValue = newText

            // Handle Enter key
            if (newText.text.length > oldText.text.length && oldText.selection.end < newText.text.length && newText.text[oldText.selection.start] == '\n') {
                val lineStart = newText.text.lastIndexOf('\n', startIndex = oldText.selection.start - 1) + 1
                val previousLine = newText.text.substring(lineStart, oldText.selection.start)
                val leadingWhitespace = previousLine.takeWhile { it.isWhitespace() }
                
                if (previousLine.trim().startsWith("- ")) {
                    val listMarker = previousLine.trim().substringBefore(' ') + " "
                    val newCursorPos = newText.selection.start + leadingWhitespace.length + listMarker.length
                    val finalText = newText.text.substring(0, newText.selection.start) + leadingWhitespace + listMarker + newText.text.substring(newText.selection.start)
                    textValue = TextFieldValue(finalText, selection = TextRange(newCursorPos))
                }
            }
        },
        onSave = {
            viewModel.onSaveContent(textValue.text)
            onNavigateBack()
        },
        onCancel = onNavigateBack
    )
}