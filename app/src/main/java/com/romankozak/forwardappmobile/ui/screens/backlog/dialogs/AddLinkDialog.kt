package com.romankozak.forwardappmobile.ui.screens.backlog.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLinkDialog(
    title: String,
    onDismiss: () -> Unit,
    // MODIFIED: onConfirm now provides both name and target
    onConfirm: (name: String, target: String) -> Unit,
    // MODIFIED: Added specific placeholders
    namePlaceholder: String,
    targetPlaceholder: String = "",
    // MODIFIED: Added visibility toggle
    isTargetVisible: Boolean = true,
) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }

    // MODIFIED: Confirmation logic now depends on which fields are visible
    val isConfirmEnabled = if (isTargetVisible) {
        name.isNotBlank() && target.isNotBlank()
    } else {
        name.isNotBlank()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                // First input field (always visible)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(namePlaceholder) },
                    singleLine = true,
                )
                // Second input field (conditionally visible)
                if (isTargetVisible) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text(targetPlaceholder) },
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, target) },
                enabled = isConfirmEnabled,
            ) {
                Text(stringResource(R.string.add_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}