package com.romankozak.forwardappmobile.ui.screens.backlog.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R

@Composable
fun AddWebLinkDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String, name: String?) -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val isUrlValid = url.isNotBlank() 

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.menu_add_web_link)) },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    isError = !isUrlValid && url.isNotEmpty(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.display_name_optional)) },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(url, name.takeIf { it.isNotBlank() }) },
                enabled = isUrlValid,
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
fun AddObsidianLinkDialog(
    onDismiss: () -> Unit,
    onConfirm: (noteName: String) -> Unit,
) {
    var noteName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.menu_add_obsidian_link)) },
        text = {
            OutlinedTextField(
                value = noteName,
                onValueChange = { noteName = it },
                label = { Text(stringResource(R.string.note_name)) },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(noteName) },
                enabled = noteName.isNotBlank(),
            ) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
