package com.romankozak.forwardappmobile.ui.screens.projectscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import androidx.compose.ui.platform.LocalContext
import com.romankozak.forwardappmobile.data.database.models.RelatedLink
import com.romankozak.forwardappmobile.ui.screens.projectscreen.components.attachments.*
import com.romankozak.forwardappmobile.ui.screens.projectscreen.dialogs.AddObsidianLinkDialog
import com.romankozak.forwardappmobile.ui.screens.projectscreen.dialogs.AddWebLinkDialog
import com.romankozak.forwardappmobile.ui.screens.projectscreen.UiEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectAttachmentsScreen(
    navController: NavController,
    projectId: String?,
    viewModel: BacklogViewModel = hiltViewModel(),
) {
    val listContent by viewModel.listContent.collectAsStateWithLifecycle()
    val attachments = listContent.filter { it is ListItemContent.LinkItem || it is ListItemContent.NoteItem || it is ListItemContent.CustomListItem }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    LaunchedEffect(key1 = Unit) {
        viewModel.uiEventFlow.collect {
            when(it) {
                is UiEvent.Navigate -> {
                    navController.navigate(it.route)
                }
                is UiEvent.HandleLinkClick -> {
                    if (!it.link.target.isNullOrBlank()) {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(it.link.target))
                        context.startActivity(intent)
                    }
                }
                is UiEvent.OpenUri -> {
                    try {
                        if (it.uri.isNotBlank()) {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(it.uri))
                        context.startActivity(intent)
                    }
                    } catch (e: Exception) {
                        // Handle exception, e.g., show a snackbar
                    }
                }
                else -> {}
            }
        }
    }

    if (uiState.showAddWebLinkDialog) {
        AddWebLinkDialog(
            onDismiss = { viewModel.inputHandler.onDismissLinkDialogs() },
            onConfirm = { url, name -> viewModel.inputHandler.onAddWebLinkConfirm(url, name) }
        )
    }

    if (uiState.showAddObsidianLinkDialog) {
        AddObsidianLinkDialog(
            onDismiss = { viewModel.inputHandler.onDismissLinkDialogs() },
            onConfirm = { noteName -> viewModel.inputHandler.onAddObsidianLinkConfirm(noteName) },
            onCreateNew = { noteName -> viewModel.inputHandler.onAddObsidianLinkAndCreateNewConfirm(noteName) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attachments") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(attachments, key = { it.hashCode() }) {
                AttachmentItemCard(
                    item = it,
                    onItemClick = { viewModel.itemActionHandler.onItemClick(it) },
                    onDeleteItem = { viewModel.itemActionHandler.deleteItem(it) },
                    onCopyContentRequest = { viewModel.itemActionHandler.copyContentRequest(it) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                AddAttachmentButton(onAddAttachment = { viewModel.onAddAttachment(it) })
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
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = { onItemClick(item) }
    ) {
        val endAction = @Composable {
            IconButton(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDeleteItem(item) 
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.delete_attachment_description),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        when (item) {
            is ListItemContent.LinkItem -> {
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
            is ListItemContent.NoteItem -> {
                NoteItemRow(
                    noteItem = item,
                    onClick = { onItemClick(item) },
                    onDelete = { onDeleteItem(item) },
                )
            }
            is ListItemContent.CustomListItem -> {
                CustomListItemRow(
                    customListItem = item,
                    onClick = { onItemClick(item) },
                    onDelete = { onDeleteItem(item) },
                )
            }
            else -> {}
        }
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
            AttachmentTypeMenuItem(R.string.attachment_type_note, AttachmentType.NOTE) { type ->
                onAddAttachment(type)
                showAddMenu = false
            }
            AttachmentTypeMenuItem(R.string.attachment_type_custom_list, AttachmentType.CUSTOM_LIST) { type ->
                onAddAttachment(type)
                showAddMenu = false
            }
            AttachmentTypeMenuItem(R.string.attachment_type_web_link, AttachmentType.WEB_LINK) { type ->
                onAddAttachment(type)
                showAddMenu = false
            }
            AttachmentTypeMenuItem(R.string.attachment_type_obsidian, AttachmentType.OBSIDIAN_LINK) { type ->
                onAddAttachment(type)
                showAddMenu = false
            }
            AttachmentTypeMenuItem(R.string.attachment_type_list_link, AttachmentType.LIST_LINK) { type ->
                onAddAttachment(type)
                showAddMenu = false
            }
            HorizontalDivider()
            AttachmentTypeMenuItem(R.string.menu_add_list_shortcut, AttachmentType.SHORTCUT) { type ->
                onAddAttachment(type)
                showAddMenu = false
            }
        }
    }
}

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