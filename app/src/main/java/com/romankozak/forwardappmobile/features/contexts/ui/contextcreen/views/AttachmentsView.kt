package com.romankozak.forwardappmobile.features.contexts.ui.contextcreen.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.features.contexts.ui.contextcreen.BacklogViewModel
import com.romankozak.forwardappmobile.features.attachments.ui.project.components.ChecklistItemRow
import com.romankozak.forwardappmobile.features.attachments.ui.project.components.LinkItemRow
import com.romankozak.forwardappmobile.features.attachments.ui.project.components.NoteDocumentItemRow
import androidx.compose.ui.graphics.Color

@Composable
fun AttachmentsView(
    modifier: Modifier = Modifier,
    viewModel: BacklogViewModel,
    listContent: List<ListItemContent>
) {
    val attachments = listContent.filter {
        it is ListItemContent.LinkItem || it is ListItemContent.NoteDocumentItem || it is ListItemContent.ChecklistItem
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (attachments.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.no_attachments_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(attachments, key = { it.hashCode() }) {
                    AttachmentItemCard(
                        item = it,
                        onItemClick = { viewModel.itemActionHandler.onItemClick(it) },
                        onDeleteItem = { viewModel.deleteAttachmentEverywhere(it) },
                        onCopyContentRequest = { viewModel.itemActionHandler.copyContentRequest(it) },
                        onShareAttachment = { attachment ->
                            viewModel.itemActionHandler.shareAttachmentToProject(attachment)
                        },
                        onDeleteCompletely = { viewModel.deleteAttachmentEverywhere(it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentItemCard(
    item: ListItemContent,
    onItemClick: (ListItemContent) -> Unit,
    onDeleteItem: (ListItemContent) -> Unit,
    onCopyContentRequest: (ListItemContent) -> Unit,
    onShareAttachment: (ListItemContent) -> Unit,
    onDeleteCompletely: (ListItemContent) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = { onItemClick(item) }),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        color = Color.Transparent,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            when (item) {
                is ListItemContent.LinkItem -> {
                    LinkItemRow(
                        linkItem = item,
                        isSelected = false,
                        isHighlighted = false,
                        onClick = { onItemClick(item) },
                        onLongClick = { },
                        endAction = {},
                        onDelete = { onDeleteItem(item) },
                        onCopyContentRequest = onCopyContentRequest,
                    )
                }
                is ListItemContent.NoteDocumentItem -> {
                    NoteDocumentItemRow(
                        noteDocumentItem = item,
                        onClick = { onItemClick(item) },
                        onDelete = { onDeleteItem(item) },
                        trailingContent = {},
                    )
                }
                is ListItemContent.ChecklistItem -> {
                    ChecklistItemRow(
                        checklistItem = item,
                        onClick = { onItemClick(item) },
                        onDelete = { onDeleteItem(item) },
                        trailingContent = {},
                    )
                }
                else -> {}
            }

            AttachmentActionsRow(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 2.dp),
                onShareAttachment = { onShareAttachment(item) },
                onDeleteItem = { onDeleteItem(item) },
                onDeleteCompletely = { onDeleteCompletely(item) },
            )
        }
    }
}

@Composable
private fun AttachmentActionsRow(
    modifier: Modifier = Modifier,
    onShareAttachment: () -> Unit,
    onDeleteItem: () -> Unit,
    onDeleteCompletely: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onShareAttachment()
            },
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = stringResource(R.string.copy_attachment_to_project),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDeleteCompletely()
            },
        ) {
            Icon(
                imageVector = Icons.Filled.DeleteForever,
                contentDescription = stringResource(R.string.delete_attachment_completely),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDeleteItem()
            },
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.delete_attachment_description),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
