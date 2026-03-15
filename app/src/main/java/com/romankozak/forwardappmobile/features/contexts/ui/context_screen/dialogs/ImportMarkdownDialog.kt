package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.romankozak.forwardappmobile.R

@Composable
fun ImportMarkdownDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var markdownText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        ImportMarkdownDialogContent(
            markdownText = markdownText,
            onMarkdownTextChange = { markdownText = it },
            onDismiss = onDismiss,
            onConfirm = onConfirm,
        )
    }
}

@Composable
private fun ImportMarkdownDialogContent(
    markdownText: String,
    onMarkdownTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val isImportEnabled = markdownText.isNotBlank()

    Card(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.import_from_markdown),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = stringResource(R.string.import_markdown_description),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            OutlinedTextField(
                value = markdownText,
                onValueChange = onMarkdownTextChange,
                label = { Text(stringResource(R.string.markdown_text_label)) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                placeholder = {
                    Text(
                        "- Звичайний запис\n" +
                            "* Інший запис\n" +
                            "- [ ] Невиконане завдання\n" +
                            "- [x] Виконане завдання",
                    )
                },
            )

            ImportMarkdownDialogActions(
                isImportEnabled = isImportEnabled,
                markdownText = markdownText,
                onDismiss = onDismiss,
                onConfirm = onConfirm,
            )
        }
    }
}

@Composable
private fun ImportMarkdownDialogActions(
    isImportEnabled: Boolean,
    markdownText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.cancel))
        }
        Button(
            onClick = { onConfirm(markdownText) },
            enabled = isImportEnabled,
        ) {
            Text(stringResource(R.string.import_action))
        }
    }
}
