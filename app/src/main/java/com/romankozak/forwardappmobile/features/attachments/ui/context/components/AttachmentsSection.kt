package com.romankozak.forwardappmobile.features.attachments.ui.context.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.features.attachments.ui.context.AttachmentType

private const val ATTACHMENTS_ANIMATION_DURATION_MS = 300

data class AttachmentSectionActions(
    val onAddAttachment: (AttachmentType) -> Unit,
    val onDeleteItem: (BacklogItemContent) -> Unit,
    val onItemClick: (BacklogItemContent) -> Unit,
    val onCopyContentRequest: (BacklogItemContent) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AttachmentsSection(
    attachments: List<BacklogItemContent>,
    isExpanded: Boolean,
    actions: AttachmentSectionActions,
) {
    val haptic = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = isExpanded,
        enter =
            expandVertically(animationSpec = tween(ATTACHMENTS_ANIMATION_DURATION_MS)) +
                fadeIn(tween(ATTACHMENTS_ANIMATION_DURATION_MS)),
        exit =
            shrinkVertically(animationSpec = tween(ATTACHMENTS_ANIMATION_DURATION_MS)) +
                fadeOut(tween(ATTACHMENTS_ANIMATION_DURATION_MS)),
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                AttachmentsHeader(attachmentsCount = attachments.size)

                Spacer(modifier = Modifier.height(12.dp))

                if (attachments.isEmpty()) {
                    EmptyAttachmentsState(onAddAttachment = actions.onAddAttachment)
                } else {
                    AttachmentsList(
                        attachments = attachments,
                        onItemClick = actions.onItemClick,
                        onDeleteItem = { content ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            actions.onDeleteItem(content)
                        },
                        onCopyContentRequest = actions.onCopyContentRequest,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AddAttachmentButton(actions.onAddAttachment)
                }
            }
        }
    }
}

@Composable
private fun AttachmentsHeader(attachmentsCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.AttachFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text =
                stringResource(R.string.attachments_header) +
                    if (attachmentsCount > 0) " ($attachmentsCount)" else "",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun EmptyAttachmentsState(onAddAttachment: (AttachmentType) -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.AttachFile,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.no_attachments_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        AddAttachmentButton(onAddAttachment)
    }
}

@Composable
private fun AttachmentsList(
    attachments: List<BacklogItemContent>,
    onItemClick: (BacklogItemContent) -> Unit,
    onDeleteItem: (BacklogItemContent) -> Unit,
    onCopyContentRequest: (BacklogItemContent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.heightIn(max = 300.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(attachments, key = { it.hashCode() }) { item ->
            AnimatedVisibility(
                visible = true,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                AttachmentItemCard(
                    item = item,
                    onItemClick = onItemClick,
                    onDeleteItem = onDeleteItem,
                    onCopyContentRequest = onCopyContentRequest,
                )
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
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        val endAction = @Composable { AttachmentDeleteAction { onDeleteItem(item) } }
        AttachmentItemContent(
            item = item,
            onItemClick = onItemClick,
            onDeleteItem = onDeleteItem,
            onCopyContentRequest = onCopyContentRequest,
            endAction = endAction,
        )
    }
}

@Composable
private fun AttachmentDeleteAction(onDelete: () -> Unit) {
    IconButton(onClick = onDelete) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.delete_attachment_description),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun AttachmentItemContent(
    item: BacklogItemContent,
    onItemClick: (BacklogItemContent) -> Unit,
    onDeleteItem: (BacklogItemContent) -> Unit,
    onCopyContentRequest: (BacklogItemContent) -> Unit,
    endAction: @Composable () -> Unit,
) {
    when (item) {
        is BacklogItemContent.LinkItem -> {
            LinkItemRow(
                linkItem = item,
                isSelected = false,
                isHighlighted = false,
                onClick = { onItemClick(item) },
                onLongClick = { },
                endAction = endAction,
                onDelete = { onDeleteItem(item) },
                onCopyContentRequest = onCopyContentRequest,
            )
        }
        is BacklogItemContent.NoteDocumentItem -> {
            NoteDocumentItemRow(
                noteDocumentItem = item,
                onDelete = { onDeleteItem(item) },
            )
        }
        is BacklogItemContent.ChecklistItem -> {
            ChecklistItemRow(
                checklistItem = item,
                onDelete = { onDeleteItem(item) },
            )
        }
        is BacklogItemContent.MusicNoteItem -> {
            NoteDocumentItemRow(
                noteDocumentItem =
                    BacklogItemContent.NoteDocumentItem(
                        item.musicNote.toNoteDocumentEntity(),
                        item.backlogItem,
                    ),
                onDelete = { onDeleteItem(item) },
            )
        }
        else -> Unit
    }
}

@Composable
private fun AddAttachmentButton(onAddAttachment: (AttachmentType) -> Unit) {
    var showAddMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        FilledTonalButton(
            onClick = { showAddMenu = true },
            modifier = Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.add_attachment_description),
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.add_attachment_button),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        DropdownMenu(
            expanded = showAddMenu,
            onDismissRequest = { showAddMenu = false },
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surface),
        ) {
            val onSelect: (AttachmentType) -> Unit = { type ->
                onAddAttachment(type)
                showAddMenu = false
            }

            AttachmentTypeMenuItem(R.string.attachment_type_notes, AttachmentType.NOTES, onSelect)
            AttachmentTypeMenuItem(R.string.attachment_type_music_notes, AttachmentType.MUSIC_NOTES, onSelect)
            AttachmentTypeMenuItem(R.string.attachment_type_checklist, AttachmentType.CHECKLIST, onSelect)
            AttachmentTypeMenuItem(R.string.attachment_type_web_link, AttachmentType.WEB_LINK, onSelect)
            AttachmentTypeMenuItem(R.string.attachment_type_obsidian, AttachmentType.OBSIDIAN_LINK, onSelect)
            AttachmentTypeMenuItem(R.string.attachment_type_project_link, AttachmentType.CONTEXT_LINK, onSelect)
            HorizontalDivider()
            AttachmentTypeMenuItem(R.string.menu_add_project_shortcut, AttachmentType.CONTEXT_SHORTCUT, onSelect)
        }
    }
}

private fun com.romankozak.forwardappmobile.core.data.models.entities.MusicNoteEntity.toNoteDocumentEntity() =
    com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity(
        id = id,
        contextId = contextId,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        content = content,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
    )

@Composable
private fun AttachmentTypeMenuItem(
    textRes: Int,
    type: AttachmentType,
    onSelect: (AttachmentType) -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = stringResource(textRes),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        onClick = { onSelect(type) },
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}
