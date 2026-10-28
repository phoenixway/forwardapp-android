package com.romankozak.forwardappmobile.features.attachments.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.attachments.ObsidianNoteAddress

@Composable
fun ObsidianAddressFields(target: String, vault: String, onTargetChange: (String) -> Unit, onVaultChange: (String) -> Unit) {
    val parsed = runCatching { ObsidianNoteAddress.parse(target, vault) }
    val error = if (target.isBlank()) null else parsed.exceptionOrNull()?.message
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = target, onValueChange = onTargetChange, singleLine = true, modifier = Modifier.fillMaxWidth(),
            label = { Text("Obsidian URL, ім’я або шлях") },
            placeholder = { Text("Note або Folder/Note.md") },
            isError = error != null,
            supportingText = { Text(error ?: "Унікальне ім’я у vault або шлях від його кореня. .md необов’язкове.") },
        )
        if (parsed.getOrNull()?.openUrl == null) {
            OutlinedTextField(
                value = vault, onValueChange = onVaultChange, singleLine = true, modifier = Modifier.fillMaxWidth(),
                label = { Text("Vault (optional)") },
                placeholder = { Text("Falls back to Settings vault") },
            )
        } else {
            Text("Vault і адреса беруться з Obsidian URL", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
