// File: app/src/main/java/com/romankozak/forwardappmobile/ui/components/AttachmentsSection.kt

package com.romankozak.forwardappmobile.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.ui.screens.backlog.components.LinkItemRow
import com.romankozak.forwardappmobile.ui.screens.backlog.components.NoteItemRow

enum class AttachmentType {
    NOTE, WEB_LINK, OBSIDIAN_LINK, LIST_LINK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentsSection(
    attachments: List<ListItemContent>,
    isExpanded: Boolean,
    onAddAttachment: (AttachmentType) -> Unit,
    onDeleteItem: (ListItemContent) -> Unit,
    onItemClick: (ListItemContent) -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically(animationSpec = tween(300)) + fadeIn(tween(300)),
        exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(tween(300))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // Заголовок з кількістю
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.attachments_header) +
                                if (attachments.isNotEmpty()) " (${attachments.size})" else "",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Порожній стан
                if (attachments.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AttachFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.no_attachments_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AddAttachmentButton(onAddAttachment)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(attachments, key = { it.hashCode() }) { item ->
                            AnimatedVisibility(
                                visible = true,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                AttachmentItemCard(
                                    item = item,
                                    onItemClick = onItemClick,
                                    onDeleteItem = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        // MODIFIED: Видалено виклик локального Snackbar, передача дії батькові
                                        onDeleteItem(item)
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    AddAttachmentButton(onAddAttachment)
                }
            }
        }
    }
}


@Composable
private fun AttachmentItemCard(
    item: ListItemContent,
    onItemClick: (ListItemContent) -> Unit,
    onDeleteItem: (ListItemContent) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        val endAction = @Composable {
            IconButton(
                onClick = { onDeleteItem(item) },
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.delete_attachment_description),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        when (item) {
            is ListItemContent.NoteItem -> {
                NoteItemRow(
                    noteContent = item,
                    isSelected = false,
                    isHighlighted = false,
                    onClick = { onItemClick(item) },
                    onLongClick = { },
                    endAction = endAction,
                )
            }
            is ListItemContent.LinkItem -> {
                LinkItemRow(
                    link = item.link.linkData,
                    isSelected = false,
                    isHighlighted = false,
                    onClick = { onItemClick(item) },
                    onLongClick = { },
                    endAction = endAction,
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun AddAttachmentButton(
    onAddAttachment: (AttachmentType) -> Unit
) {
    var showAddMenu by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        FilledTonalButton(
            onClick = { showAddMenu = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(R.string.add_attachment_description),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.add_attachment_button),
                style = MaterialTheme.typography.labelLarge
            )
        }

        DropdownMenu(
            expanded = showAddMenu,
            onDismissRequest = { showAddMenu = false },
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            AttachmentTypeMenuItem(R.string.attachment_type_note, AttachmentType.NOTE) {
                onAddAttachment(it); showAddMenu = false
            }
            AttachmentTypeMenuItem(R.string.attachment_type_web_link, AttachmentType.WEB_LINK) {
                onAddAttachment(it); showAddMenu = false
            }
            AttachmentTypeMenuItem(R.string.attachment_type_obsidian, AttachmentType.OBSIDIAN_LINK) {
                onAddAttachment(it); showAddMenu = false
            }
            AttachmentTypeMenuItem(R.string.attachment_type_list_link, AttachmentType.LIST_LINK) {
                onAddAttachment(it); showAddMenu = false
            }
        }
    }
}

@Composable
private fun AttachmentTypeMenuItem(
    textRes: Int,
    type: AttachmentType,
    onSelect: (AttachmentType) -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = stringResource(textRes),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        onClick = { onSelect(type) },
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}



