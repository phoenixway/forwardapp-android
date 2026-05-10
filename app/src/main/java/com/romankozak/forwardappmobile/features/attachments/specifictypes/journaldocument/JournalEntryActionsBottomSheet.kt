package com.romankozak.forwardappmobile.features.attachments.specifictypes.journaldocument

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JournalEntryActionsBottomSheet(
    line: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onEnterReorderMode: () -> Unit,
    scope: CoroutineScope,
) {
    val sheetState = rememberModalBottomSheetState()
    val clipboardManager = LocalClipboardManager.current
    var draftLine by remember(line) { mutableStateOf(line) }
    val parsedEntry = remember(draftLine) { parseJournalDocumentEntry(draftLine) }

    fun dismiss() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header: title + marker chip + action icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Запис",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    parsedEntry.marker?.takeIf { it.isNotBlank() }?.let { marker ->
                        Text(
                            text = marker,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(50),
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                Row {
                    // Copy
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(draftLine)) },
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Копіювати",
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // Reorder
                    IconButton(
                        onClick = {
                            onEnterReorderMode()
                            dismiss()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Впорядкувати",
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    // Delete
                    IconButton(
                        onClick = {
                            onDelete()
                            dismiss()
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Видалити",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // Edit field
            OutlinedTextField(
                value = draftLine,
                onValueChange = { draftLine = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8,
                shape = MaterialTheme.shapes.medium,
            )

            // Save button (icon only, right-aligned)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(
                    onClick = {
                        onSave(draftLine)
                        dismiss()
                    },
                    enabled = draftLine.isNotBlank() && draftLine != line,
                    colors = IconButtonDefaults.filledIconButtonColors(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Зберегти",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

internal data class ParsedDocumentJournalEntry(
    val marker: String?,
    val text: String,
)

internal fun parseJournalDocumentEntry(line: String): ParsedDocumentJournalEntry {
    val parsed = com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.journallog.parseJournalLine(line)
    return ParsedDocumentJournalEntry(
        marker = parsed?.marker,
        text = parsed?.text ?: line.trim(),
    )
}