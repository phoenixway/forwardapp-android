// .../components/topbar/BrowserNavigationBar.kt
package com.romankozak.forwardappmobile.ui.screens.backlog.components.topbar

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R

@Composable
fun BrowserNavigationBar(
    canGoBack: Boolean,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onHomeClick: () -> Unit,
    isAttachmentsExpanded: Boolean,
    onToggleAttachments: () -> Unit,
    onEditList: () -> Unit,
    onShareList: () -> Unit,      // <-- Додано
    onDeleteList: () -> Unit,     // <-- Додано
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Ліва група кнопок
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBackClick, enabled = canGoBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
            }
            IconButton(onClick = onForwardClick, enabled = false) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = stringResource(R.string.forward))
            }
            IconButton(onClick = onHomeClick) {
                Icon(imageVector = Icons.Default.Home, contentDescription = stringResource(R.string.go_to_home_list))
            }
        }

        // Права група кнопок (використовує нові параметри)
        RightButtons(
            isAttachmentsExpanded = isAttachmentsExpanded,
            onToggleAttachments = onToggleAttachments,
            menuExpanded = menuExpanded,
            onMenuExpandedChange = onMenuExpandedChange,
            onEditList = onEditList,
            onShareList = onShareList,
            onDeleteList = onDeleteList
        )
    }
}

// RightButtons залишається тут як приватний хелпер для BrowserNavigationBar
@Composable
private fun RightButtons(
    isAttachmentsExpanded: Boolean,
    onToggleAttachments: () -> Unit,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onEditList: () -> Unit,
    onShareList: () -> Unit,
    onDeleteList: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val attachmentIconColor by animateColorAsState(
            targetValue = if (isAttachmentsExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            label = "attachmentIconColor"
        )
        IconButton(onClick = onToggleAttachments) {
            Icon(
                imageVector = Icons.Default.Attachment,
                contentDescription = stringResource(R.string.toggle_attachments),
                tint = attachmentIconColor
            )
        }
        Box {
            IconButton(onClick = { onMenuExpandedChange(true) }) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.more_options))
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { onMenuExpandedChange(false) }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.edit_list)) },
                    onClick = {
                        onEditList()
                        onMenuExpandedChange(false)
                    },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.share_list)) },
                    onClick = {
                        onShareList()
                        onMenuExpandedChange(false)
                    },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = "Поділитися списком") }
                )
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.delete_list),
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        onDeleteList()
                        onMenuExpandedChange(false)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                )
            }
        }
    }
}