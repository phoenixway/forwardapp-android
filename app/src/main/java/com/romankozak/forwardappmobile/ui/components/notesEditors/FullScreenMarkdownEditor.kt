package com.romankozak.forwardappmobile.ui.components.notesEditors

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.romankozak.forwardappmobile.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenMarkdownEditor(
    initialValue: TextFieldValue,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    // The state for edit mode and the current text now live here
    var isEditMode by remember { mutableStateOf(true) }
    var currentText by remember { mutableStateOf(initialValue) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.edit_description)) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                        }
                    },
                    actions = {
                        // The mode toggle icon is now here
                        IconButton(onClick = { isEditMode = !isEditMode }) {
                            Icon(
                                imageVector = if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                                contentDescription = stringResource(
                                    if (isEditMode) R.string.toggle_to_preview_mode else R.string.toggle_to_edit_mode
                                )
                            )
                        }
                        // The save button ("checkmark")
                        IconButton(onClick = { onSave(currentText.text) }) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.create))
                        }
                    }
                )
            }
        ) { paddingValues ->
            // Pass the state down to the simplified MarkdownEditorViewer
            MarkdownEditorViewer(
                modifier = Modifier.padding(paddingValues),
                value = currentText,
                onValueChange = { currentText = it },
                isEditMode = isEditMode // Pass the state
            )
        }
    }
}
