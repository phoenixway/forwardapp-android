package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.features.attachments.ObsidianNoteAddress
import com.romankozak.forwardappmobile.features.attachments.ui.ObsidianAddressFields
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@Composable
internal fun ContextCreationOverlay(
    isVisible: Boolean,
    newContextName: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreateRootContext: (suspend (String) -> String?)?,
    onContextSelected: (String) -> Unit,
    onPickerDismiss: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    if (!isVisible || onCreateRootContext == null) return

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_action_project)) },
        text = {
            OutlinedTextField(
                value = newContextName,
                onValueChange = onNameChange,
                singleLine = true,
                label = { Text(stringResource(R.string.picker_context_name_label)) },
                placeholder = { Text(stringResource(R.string.picker_context_name_placeholder)) },
            )
        },
        confirmButton = {
            Button(
                enabled = newContextName.isNotBlank(),
                onClick = {
                    val name = newContextName.trim()
                    scope.launch {
                        val id = onCreateRootContext(name)
                        if (!id.isNullOrBlank()) {
                            onContextSelected(id)
                            onPickerDismiss()
                        }
                        onDismiss()
                    }
                },
            ) {
                Text(stringResource(R.string.create))
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
internal fun DocumentCreationOverlay(
    type: DocumentCreationType?,
    name: String,
    target: String,
    vault: String,
    onNameChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    onVaultChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onCreateDocument: (suspend (NewDocumentDraft) -> String?)?,
    onAttachmentSelected: (String) -> Unit,
    onPickerDismiss: () -> Unit,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    type ?: return
    var busy by remember(type) { mutableStateOf(false) }
    var error by remember(type) { mutableStateOf<String?>(null) }

    DocumentCreationDialog(
        type = type,
        name = name,
        target = target,
        vault = vault,
        onNameChange = onNameChange,
        onTargetChange = onTargetChange,
        onVaultChange = onVaultChange,
        onDismiss = { if (!busy) onDismiss() },
        busy = busy,
        error = error,
        onConfirm = {
            if (!busy) {
                busy = true
                error = null
                scope.launch {
                    try {
                        val request = buildNewDocumentDraft(type = type, name = name, target = target, vault = vault)
                        val id = onCreateDocument?.invoke(request)
                        check(!id.isNullOrBlank()) { "Не вдалося створити елемент" }
                        onAttachmentSelected(id)
                        onPickerDismiss()
                        onDismiss()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (failure: Exception) {
                        error = failure.message ?: "Не вдалося створити елемент"
                    } finally { busy = false }
                }
            }
        },
    )
}

internal fun buildNewDocumentDraft(
    type: DocumentCreationType,
    name: String,
    target: String,
    vault: String,
): NewDocumentDraft =
    when (type) {
        DocumentCreationType.NOTE -> NewDocumentDraft.Note(name = name.trim().ifBlank { "New note" })
        DocumentCreationType.MUSIC_NOTE -> NewDocumentDraft.MusicNote(name = name.trim().ifBlank { "New music note" })
        DocumentCreationType.CHECKLIST -> NewDocumentDraft.Checklist(name = name.trim().ifBlank { "New checklist" })
        DocumentCreationType.WEB_LINK -> NewDocumentDraft.WebLink(url = target.trim(), name = name.trim())
        DocumentCreationType.OBSIDIAN -> ObsidianNoteAddress.parse(target, vault).let { address ->
            NewDocumentDraft.Obsidian(
                noteName = address.target,
                displayName = name.trim().ifBlank { address.displayName },
                vault = address.vault,
            )
        }
    }

internal enum class DocumentCreationType {
    NOTE,
    MUSIC_NOTE,
    CHECKLIST,
    WEB_LINK,
    OBSIDIAN,
}

@Composable
private fun DocumentCreationDialog(
    type: DocumentCreationType,
    name: String,
    target: String,
    vault: String,
    onNameChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    onVaultChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    busy: Boolean,
    error: String?,
) {
    val title =
        when (type) {
            DocumentCreationType.NOTE -> stringResource(R.string.attachment_type_notes)
            DocumentCreationType.MUSIC_NOTE -> stringResource(R.string.attachment_type_music_notes)
            DocumentCreationType.CHECKLIST -> stringResource(R.string.attachment_type_checklist)
            DocumentCreationType.WEB_LINK -> stringResource(R.string.attachment_type_web_link)
            DocumentCreationType.OBSIDIAN -> stringResource(R.string.attachment_type_obsidian)
        }
    val targetLabel =
        when (type) {
            DocumentCreationType.NOTE,
            DocumentCreationType.MUSIC_NOTE,
            DocumentCreationType.CHECKLIST,
            -> null
            DocumentCreationType.WEB_LINK -> "URL"
            DocumentCreationType.OBSIDIAN -> stringResource(R.string.note_name)
        }

    AlertDialog(
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (type == DocumentCreationType.OBSIDIAN) {
                    ObsidianAddressFields(target, vault, onTargetChange, onVaultChange)
                } else if (targetLabel != null) {
                    OutlinedTextField(
                        value = target,
                        onValueChange = onTargetChange,
                        label = { Text(targetLabel) },
                        singleLine = true,
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.display_name_optional)) },
                    singleLine = true,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !busy &&
                    when (type) {
                        DocumentCreationType.NOTE,
                        DocumentCreationType.MUSIC_NOTE,
                        DocumentCreationType.CHECKLIST,
                        -> true
                        DocumentCreationType.WEB_LINK -> target.isNotBlank()
                        DocumentCreationType.OBSIDIAN -> runCatching { ObsidianNoteAddress.parse(target, vault) }.isSuccess
                    },
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp)) else Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
