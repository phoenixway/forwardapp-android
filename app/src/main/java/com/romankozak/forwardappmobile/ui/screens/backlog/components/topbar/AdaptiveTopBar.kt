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
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.ui.screens.backlog.GoalActionType
import com.romankozak.forwardappmobile.ui.screens.backlog.ProjectViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveTopBar(
    isSelectionModeActive: Boolean,
    title: String,
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
    selectedCount: Int,
    areAllSelected: Boolean,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onToggleComplete: () -> Unit,
    onMoreActions: (GoalActionType) -> Unit,
    currentView: ProjectViewMode,
    onViewChange: (ProjectViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        shadowElevation = if (isSelectionModeActive) 4.dp else 1.dp,
        modifier = modifier.statusBarsPadding()
    ) {
        if (isSelectionModeActive) {
            Column(modifier = Modifier.statusBarsPadding()) {
                ListTitleBar(title = title)
                MultiSelectTopAppBar(
                    selectedCount = selectedCount,
                    areAllSelected = areAllSelected,
                    onClearSelection = onClearSelection,
                    onSelectAll = onSelectAll,
                    onDelete = onDelete,
                    onToggleComplete = onToggleComplete,
                    onMoreActions = onMoreActions
                )
            }
        } else {
            SubcomposeLayout(modifier = Modifier.statusBarsPadding()) { constraints ->
                val leftButtonsPlaceable = subcompose(AdaptiveTopBarSlot.LEFT_BUTTONS) {
                    LeftButtons(canGoBack, onBackClick, onForwardClick, onHomeClick)
                }.first().measure(constraints)

                val rightButtonsPlaceable = subcompose(AdaptiveTopBarSlot.RIGHT_BUTTONS) {
                    RightButtons(
                        isAttachmentsExpanded = isAttachmentsExpanded,
                        onToggleAttachments = onToggleAttachments,
                        menuExpanded = menuExpanded,
                        onMenuExpandedChange = onMenuExpandedChange,
                        onEditList = onEditList,
                        onShareList = onShareList,
                        onDeleteList = onDeleteList,
                        currentView = currentView,
                        onViewChange = onViewChange
                    )
                }.first().measure(constraints)

                val titleTextPlaceable = subcompose(AdaptiveTopBarSlot.TITLE_TEXT) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }.first().measure(constraints)

                val availableSpace = constraints.maxWidth - leftButtonsPlaceable.width - rightButtonsPlaceable.width
                val horizontalPadding = 32.dp.toPx()
                val showInlineTitle = titleTextPlaceable.width < availableSpace - horizontalPadding

                if (showInlineTitle) {
                    val navBarHeight = maxOf(leftButtonsPlaceable.height, rightButtonsPlaceable.height, 48.dp.toPx().toInt())
                    layout(constraints.maxWidth, navBarHeight) {
                        leftButtonsPlaceable.placeRelative(0, (navBarHeight - leftButtonsPlaceable.height) / 2)

                        rightButtonsPlaceable.placeRelative(
                            x = constraints.maxWidth - rightButtonsPlaceable.width,
                            y = (navBarHeight - rightButtonsPlaceable.height) / 2
                        )

                        titleTextPlaceable.placeRelative(
                            x = leftButtonsPlaceable.width + (availableSpace - titleTextPlaceable.width) / 2,
                            y = (navBarHeight - titleTextPlaceable.height) / 2
                        )
                    }
                } else {
                    val titleBarPlaceable = subcompose(AdaptiveTopBarSlot.TITLE_BAR) {
                        ListTitleBar(title = title)
                    }.first().measure(constraints)

                    val navBarPlaceable = subcompose(AdaptiveTopBarSlot.FULL_NAV_BAR) {
                        BrowserNavigationBar(
                            canGoBack = canGoBack,
                            onBackClick = onBackClick,
                            onForwardClick = onForwardClick,
                            onHomeClick = onHomeClick,
                            isAttachmentsExpanded = isAttachmentsExpanded,
                            onToggleAttachments = onToggleAttachments,
                            onEditList = onEditList,
                            onShareList = onShareList,
                            onDeleteList = onDeleteList,
                            menuExpanded = menuExpanded,
                            onMenuExpandedChange = onMenuExpandedChange,
                            currentView = currentView,
                            onViewChange = onViewChange
                        )
                    }.first().measure(constraints)

                    val totalHeight = titleBarPlaceable.height + navBarPlaceable.height
                    layout(constraints.maxWidth, totalHeight) {
                        titleBarPlaceable.placeRelative(0, 0)
                        navBarPlaceable.placeRelative(0, titleBarPlaceable.height)
                    }
                }
            }
        }
    }
}

@Composable
private fun LeftButtons(
    canGoBack: Boolean,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onHomeClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 2.dp)
    ) {
        val backButtonAlpha by animateFloatAsState(
            targetValue = if (canGoBack) 1f else 0.6f,
            label = "backButtonAlpha"
        )

        IconButton(
            onClick = onBackClick,
            enabled = canGoBack,
            modifier = Modifier.alpha(backButtonAlpha)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = if (canGoBack) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }

        IconButton(
            onClick = onForwardClick,
            enabled = false,
            modifier = Modifier.alpha(0.38f)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(R.string.forward),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }

        Spacer(modifier = Modifier.width(2.dp))

        IconButton(
            onClick = onHomeClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = stringResource(R.string.go_to_home_list),
                tint = MaterialTheme.colorScheme.primary
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
    onViewChange: (ProjectViewMode) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 2.dp)
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.height(36.dp)
        ) {
            SegmentedButton(
                selected = currentView == ProjectViewMode.BACKLOG,
                onClick = { onViewChange(ProjectViewMode.BACKLOG) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                // --- ПОЧАТОК ЗМІН ---
                icon = { }, // Прибираємо стандартну іконку-галочку
                // --- КІНЕЦЬ ЗМІН ---
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.List,
                    contentDescription = "Backlog",
                    modifier = Modifier.size(18.dp)
                )
            }

            SegmentedButton(
                selected = currentView == ProjectViewMode.INBOX,
                onClick = { onViewChange(ProjectViewMode.INBOX) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                // --- ПОЧАТОК ЗМІН ---
                icon = { }, // Прибираємо стандартну іконку-галочку
                // --- КІНЕЦЬ ЗМІН ---
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Inbox,
                    contentDescription = "Inbox",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        val attachmentIconColor by animateColorAsState(
            targetValue = if (isAttachmentsExpanded)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            label = "attachmentIconColor"
        )

        val attachmentScale by animateFloatAsState(
            targetValue = if (isAttachmentsExpanded) 1.2f else 1f,
            label = "attachmentScale"
        )

        IconButton(
            onClick = onToggleAttachments,
            modifier = Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = attachmentScale
                    scaleY = attachmentScale
                }
        ) {
            Icon(
                imageVector = Icons.Default.Attachment,
                contentDescription = stringResource(R.string.toggle_attachments),
                tint = attachmentIconColor
            )
        }

        Box {
            IconButton(onClick = { onMenuExpandedChange(true) }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.more_options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { onMenuExpandedChange(false) },
                modifier = Modifier.width(180.dp)
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.edit_list),
                            style = MaterialTheme.typography.bodyMedium
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
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.share_list),
                            style = MaterialTheme.typography.bodyMedium
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
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.delete_list),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
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


private enum class AdaptiveTopBarSlot {
    LEFT_BUTTONS,
    RIGHT_BUTTONS,
    TITLE_TEXT,
    TITLE_BAR,
    FULL_NAV_BAR
}