package com.romankozak.forwardappmobile.features.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.repository.WifiAutoSyncPeer

@Composable
internal fun PeerEditorDialog(peer: WifiAutoSyncPeer, onDismiss: () -> Unit, onSave: (WifiAutoSyncPeer) -> Unit) {
    var name by remember(peer.id, peer.address) { mutableStateOf(peer.name) }
    var address by remember(peer.id, peer.address) { mutableStateOf(peer.address) }
    var key by remember(peer.id, peer.address) { mutableStateOf(peer.key) }
    var revealKey by remember(peer.id, peer.address) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (peer.id.isBlank()) "Додати пристрій" else "Редагувати пристрій") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Назва") }, singleLine = true)
                OutlinedTextField(address, { address = it }, label = { Text("Адреса IP:порт") }, singleLine = true)
                OutlinedTextField(key, { key = it }, label = { Text("Ключ іншого пристрою") }, singleLine = true,
                    visualTransformation = if (revealKey) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = { IconButton(onClick = { revealKey = !revealKey }) {
                        Icon(if (revealKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            if (revealKey) "Приховати ключ" else "Показати ключ")
                    } })
                Text("Адресу й ключ знайдете на іншому пристрої на цьому екрані.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(peer.copy(name = name, address = address.trim(), key = key.trim())) },
            enabled = address.isNotBlank() && key.isNotBlank()) { Text("Зберегти") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
    )
}
