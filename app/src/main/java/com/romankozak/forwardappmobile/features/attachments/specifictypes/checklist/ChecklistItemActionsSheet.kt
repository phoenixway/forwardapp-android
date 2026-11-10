package com.romankozak.forwardappmobile.features.attachments.specifictypes.checklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.romankozak.forwardappmobile.R

/** Uses the same application clipboard as checklist multi-selection and cross-checklist paste. */
internal fun ChecklistViewModel.putItemInEntityClipboard(itemId: String, cut: Boolean): Int {
    onClearSelection()
    onItemLongPressed(itemId)
    return try {
        if (cut) cutSelectedToEntityClipboard() else copySelectedToEntityClipboard()
    } finally {
        onClearSelection()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChecklistItemActionsSheet(
    itemId: String?,
    viewModel: ChecklistViewModel,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onAddAttachmentLink: () -> Unit,
    onAddContextLink: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val copiedMessage = stringResource(R.string.checklist_item_copied)
    val cutMessage = stringResource(R.string.checklist_item_cut)
    val missingMessage = stringResource(R.string.checklist_item_missing)
    val canPaste = viewModel.canPasteChecklistItemsFromEntityClipboard()
    fun putInClipboard(cut: Boolean) {
        onDismiss()
        val count = itemId?.let { viewModel.putItemInEntityClipboard(it, cut) } ?: 0
        onMessage(if (count == 0) missingMessage else if (cut) cutMessage else copiedMessage)
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column {
            ListItem(
                headlineContent = { Text(stringResource(R.string.checklist_item_cut_action)) },
                leadingContent = { Icon(Icons.Outlined.ContentCut, null) },
                modifier = Modifier.clickable(enabled = itemId != null) { putInClipboard(cut = true) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.checklist_item_copy_action)) },
                leadingContent = { Icon(Icons.Outlined.ContentCopy, null) },
                modifier = Modifier.clickable(enabled = itemId != null) { putInClipboard(cut = false) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.checklist_item_paste_action)) },
                leadingContent = { Icon(Icons.Outlined.ContentPaste, null) },
                colors = ListItemDefaults.colors(
                    headlineColor = if (canPaste) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    leadingIconColor = if (canPaste) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                ),
                modifier = Modifier.clickable(enabled = canPaste) {
                    onDismiss()
                    viewModel.pasteChecklistItemsFromEntityClipboard(onResult = onMessage)
                },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.checklist_item_delete_action)) },
                leadingContent = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable(enabled = itemId != null) {
                    onDismiss()
                    itemId?.let(viewModel::onDeleteItem)
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.checklist_item_attachment_link)) },
                leadingContent = { Icon(Icons.Default.AttachFile, null) },
                modifier = Modifier.clickable { onDismiss(); onAddAttachmentLink() },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.checklist_item_context_link)) },
                leadingContent = { Icon(Icons.Default.AccountTree, null) },
                modifier = Modifier.clickable { onDismiss(); onAddContextLink() },
            )
        }
    }
}
