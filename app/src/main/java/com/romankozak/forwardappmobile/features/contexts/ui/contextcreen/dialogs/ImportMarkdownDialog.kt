package com.romankozak.forwardappmobile.features.contexts.ui.contextcreen.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
                    onValueChange = { markdownText = it },
                    label = { Text(stringResource(R.string.markdown_text_label)) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                    
                    placeholder = { Text("- Звичайний запис\n* Інший запис\n- [ ] Невиконане завдання\n- [x] Виконане завдання") },
                )

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
                        onClick = {
                            if (markdownText.isNotBlank()) {
                                onConfirm(markdownText)
                            }
                        },
                        enabled = markdownText.isNotBlank(),
                    ) {
                        Text(stringResource(R.string.import_action))
                    }
                }
            }
        }
    }
}
