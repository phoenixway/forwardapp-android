package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.capabilities.attachments

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.features.attachments.ui.project.components.ChecklistItemRow
import com.romankozak.forwardappmobile.features.attachments.ui.project.components.LinkItemRow
import com.romankozak.forwardappmobile.features.attachments.ui.project.components.NoteDocumentItemRow
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.ContextScreenViewModel

@Composable
fun AttachmentsView(
    modifier: Modifier = Modifier,
    viewModel: ContextScreenViewModel,
    attachmentItems: List<BacklogItemContent>,
) {
    val attachments =
        attachmentItems.filter {
            it is BacklogItemContent.LinkItem || it is BacklogItemContent.NoteDocumentItem || it is BacklogItemContent.ChecklistItem
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
                        onDeleteItem = { viewModel.onDeleteEverywhere(it) },
                        onCopyContentRequest = { viewModel.itemActionHandler.copyContentRequest(it) },
                        onShareAttachment = { attachment ->
                            viewModel.itemActionHandler.shareAttachmentToProject(attachment)
                        },
                        onDeleteCompletely = { viewModel.onDeleteEverywhere(it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentItemCard(
    item: BacklogItemContent,
    onItemClick: (BacklogItemContent) -> Unit,
    onDeleteItem: (BacklogItemContent) -> Unit,
    onCopyContentRequest: (BacklogItemContent) -> Unit,
    onShareAttachment: (BacklogItemContent) -> Unit,
    onDeleteCompletely: (BacklogItemContent) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = { onItemClick(item) }),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 2.dp,
        shadowElevation = 3.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(vertical = 10.dp)) {
            AttachmentTypeBadge(
                item = item,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 6.dp),
            )
            when (item) {
                is BacklogItemContent.LinkItem -> {
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
                is BacklogItemContent.NoteDocumentItem -> {
                    NoteDocumentItemRow(
                        noteDocumentItem = item,
                        onClick = { onItemClick(item) },
                        onDelete = { onDeleteItem(item) },
                        trailingContent = {},
                    )
                }
                is BacklogItemContent.ChecklistItem -> {
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
                modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 4.dp),
                onShareAttachment = { onShareAttachment(item) },
                onDeleteItem = { onDeleteItem(item) },
                onDeleteCompletely = { onDeleteCompletely(item) },
            )
        }
    }
}

@Composable
private fun AttachmentTypeBadge(
    item: BacklogItemContent,
    modifier: Modifier = Modifier,
) {
    val text =
        when (item) {
            is BacklogItemContent.LinkItem ->
                when (item.link.linkData.type) {
                    com.romankozak.forwardappmobile.core.data.models.entities.LinkType.OBSIDIAN -> "Obsidian"
                    com.romankozak.forwardappmobile.core.data.models.entities.LinkType.URL -> "URL"
                    com.romankozak.forwardappmobile.core.data.models.entities.LinkType.CONTEXT -> "Контекст"
                    else -> "Посилання"
                }
            is BacklogItemContent.NoteDocumentItem -> "Документ"
            is BacklogItemContent.ChecklistItem -> "Чекліст"
            else -> "Вкладення"
        }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
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
        FilledTonalIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onShareAttachment()
            },
            colors =
                IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                ),
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = stringResource(R.string.copy_attachment_to_project),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        FilledTonalIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDeleteCompletely()
            },
            colors =
                IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                ),
        ) {
            Icon(
                imageVector = Icons.Filled.DeleteForever,
                contentDescription = stringResource(R.string.delete_attachment_completely),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
        }
        FilledTonalIconButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onDeleteItem()
            },
            colors =
                IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
                ),
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
