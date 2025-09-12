package com.romankozak.forwardappmobile.ui.screens.backlog.components.topbar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.data.database.models.ProjectViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserNavigationBar(
    canGoBack: Boolean,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onHomeClick: () -> Unit,
    isAttachmentsExpanded: Boolean,
    onToggleAttachments: () -> Unit,
    onEditList: () -> Unit,
    onShareList: () -> Unit,
    onDeleteList: () -> Unit,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    currentView: ProjectViewMode,
    onViewChange: (ProjectViewMode) -> Unit,
    onImportFromMarkdown: () -> Unit,
    onExportToMarkdown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val backButtonAlpha by animateFloatAsState(
                    targetValue = if (canGoBack) 1f else 0.6f,
                    label = "backButtonAlpha",
                )

                IconButton(
                    onClick = onBackClick,
                    enabled = canGoBack,
                    modifier = Modifier.alpha(backButtonAlpha),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint =
                            if (canGoBack) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                    )
                }

                IconButton(
                    onClick = onForwardClick,
                    enabled = false,
                    modifier = Modifier.alpha(0.38f),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.forward),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onHomeClick,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = stringResource(R.string.go_to_home_list),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            RightButtons(
                isAttachmentsExpanded = isAttachmentsExpanded,
                onToggleAttachments = onToggleAttachments,
                menuExpanded = menuExpanded,
                onMenuExpandedChange = onMenuExpandedChange,
                onEditList = onEditList,
                onShareList = onShareList,
                onDeleteList = onDeleteList,
                currentView = currentView,
                onViewChange = onViewChange,
                onImportFromMarkdown = onImportFromMarkdown,
                onExportToMarkdown = onExportToMarkdown,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RightButtons(
    isAttachmentsExpanded: Boolean,
    onToggleAttachments: () -> Unit,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onEditList: () -> Unit,
    onShareList: () -> Unit,
    onDeleteList: () -> Unit,
    currentView: ProjectViewMode,
    onViewChange: (ProjectViewMode) -> Unit,
    onImportFromMarkdown: () -> Unit,
    onExportToMarkdown: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SingleChoiceSegmentedButtonRow(
            modifier =
                Modifier
                    .height(36.dp)
                    .padding(end = 8.dp),
        ) {
            SegmentedButton(
                selected = currentView == ProjectViewMode.BACKLOG,
                onClick = { onViewChange(ProjectViewMode.BACKLOG) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = { },
                colors =
                    SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                        inactiveContainerColor = MaterialTheme.colorScheme.surface,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.List,
                    contentDescription = "Backlog",
                    modifier = Modifier.size(18.dp),
                )
            }

            SegmentedButton(
                selected = currentView == ProjectViewMode.INBOX,
                onClick = { onViewChange(ProjectViewMode.INBOX) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = { },
                colors =
                    SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                        inactiveContainerColor = MaterialTheme.colorScheme.surface,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Inbox,
                    contentDescription = "Inbox",
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        val attachmentIconColor by animateColorAsState(
            targetValue =
                if (isAttachmentsExpanded) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            label = "attachmentIconColor",
        )

        val attachmentScale by animateFloatAsState(
            targetValue = if (isAttachmentsExpanded) 1.2f else 1f,
            label = "attachmentScale",
        )

        IconButton(
            onClick = onToggleAttachments,
            modifier =
                Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        scaleX = attachmentScale
                        scaleY = attachmentScale
                    },
        ) {
            Icon(
                imageVector = Icons.Default.Attachment,
                contentDescription = stringResource(R.string.toggle_attachments),
                tint = attachmentIconColor,
            )
        }

        Box {
            IconButton(onClick = { onMenuExpandedChange(true) }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.more_options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { onMenuExpandedChange(false) },
                modifier = Modifier.width(220.dp),
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.edit_list),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    onClick = {
                        onEditList()
                        onMenuExpandedChange(false)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.share_list),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    onClick = {
                        onShareList()
                        onMenuExpandedChange(false)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Поділитися списком",
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                    },
                )

                if (currentView == ProjectViewMode.INBOX) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    DropdownMenuItem(
                        text = { Text("Імпортувати з Markdown", style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            onImportFromMarkdown()
                            onMenuExpandedChange(false)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Upload, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Експортувати в Markdown", style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            onExportToMarkdown()
                            onMenuExpandedChange(false)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Download, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        },
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.delete_list),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
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
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                )
            }
        }
    }
}
