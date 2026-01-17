package com.romankozak.forwardappmobile.features.attachments.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*

@Composable
fun AddWebLinkDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String, name: String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add web link") },
        text = {
            Column {
                TextField(value = url, onValueChange = { url = it }, placeholder = { Text("URL") })
                TextField(value = name, onValueChange = { name = it }, placeholder = { Text("Name") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(url, name) }) {
                Text("Add")
            }
        }
    )
}

@Composable
fun AddObsidianLinkDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String, name: String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Obsidian link") },
        text = {
            Column {
                TextField(value = url, onValueChange = { url = it }, placeholder = { Text("URL") })
                TextField(value = name, onValueChange = { name = it }, placeholder = { Text("Name") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(url, name) }) {
                Text("Add")
            }
        }
    )
}