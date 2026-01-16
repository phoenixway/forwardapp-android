package com.romankozak.forwardappmobile.features.context.ui.contextcreen.components.inputpanel

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.romankozak.forwardappmobile.config.FeatureFlag
import com.romankozak.forwardappmobile.config.FeatureToggles

data class ActionItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val isSelected: Boolean = false,
    val action: () -> Unit,
)

@Composable
fun InputPanelAddToProjectActionsDialog(
    currentInputMode: InputMode,
    isProjectManagementEnabled: Boolean,
    onDismiss: () -> Unit,
    onInputModeSelected: (InputMode) -> Unit,
    onAddNestedProjectClick: () -> Unit,
    onShowAddWebLinkDialog: () -> Unit,
    onShowAddObsidianLinkDialog: () -> Unit,
    onAddListShortcutClick: () -> Unit,
    onShowCreateNoteDocumentDialog: () -> Unit,
    onCreateChecklist: () -> Unit,
    onAddScript: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val scriptsEnabled = FeatureToggles.isEnabled(FeatureFlag.ScriptsLibrary)

    val linkActions =
        buildList {
            add(
            ActionItem(
                    title = "Project link",
                icon = Icons.Outlined.AccountTree,
                color = MaterialTheme.colorScheme.secondary,
                action = {
                    onAddNestedProjectClick()
                    onDismiss()
                },
            ),
            )
            add(
                ActionItem(
                    title = "Project to backlog",
                    icon = Icons.Outlined.PlaylistAdd,
                    color = MaterialTheme.colorScheme.tertiary,
                    action = {
                        onAddListShortcutClick()
                        onDismiss()
                    },
                ),
            )
            add(
                ActionItem(
                title = "Web посилання",
                icon = Icons.Outlined.Public,
                color = MaterialTheme.colorScheme.secondary,
                action = {
                    onShowAddWebLinkDialog()
                    onDismiss()
                },
            ),
            )
            add(
                ActionItem(
                title = "Obsidian нотатка",
                icon = Icons.Outlined.DataObject,
                color = MaterialTheme.colorScheme.secondary,
                action = {
                    onShowAddObsidianLinkDialog()
                    onDismiss()
                },
            ),
            )
            add(
                ActionItem(
                title = "Документ",
                icon = Icons.Outlined.List,
                color = MaterialTheme.colorScheme.secondary,
                action = {
                    onShowCreateNoteDocumentDialog()
                    onDismiss()
                },
            ),
            )
            add(
                ActionItem(
                title = "Чекліст",
                icon = Icons.Outlined.Checklist,
                color = MaterialTheme.colorScheme.secondary,
                action = {
                    onCreateChecklist()
                    onDismiss()
                },
            ),
            )
            if (scriptsEnabled && onAddScript != null) {
                add(
                    ActionItem(
                        title = "Скрипт",
                        icon = Icons.Outlined.Code,
                        color = MaterialTheme.colorScheme.secondary,
                        action = {
                            onAddScript()
                            onDismiss()
                        },
                    ),
                )
            }
        }

    val addActions =
        buildList {
            add(
                ActionItem(
                    title = "Add nested project",
                    icon = Icons.Outlined.PlaylistAdd,
                    color = MaterialTheme.colorScheme.tertiary,
                    action = {
                        onAddNestedProjectClick()
                        onDismiss()
                    },
                ),
            )
            add(
                ActionItem(
                    title = "Документ",
                    icon = Icons.Outlined.List,
                    color = MaterialTheme.colorScheme.secondary,
                    action = {
                        onShowCreateNoteDocumentDialog()
                        onDismiss()
                    },
                ),
            )
            add(
                ActionItem(
                    title = "Web посилання",
                    icon = Icons.Outlined.Public,
                    color = MaterialTheme.colorScheme.secondary,
                    action = {
                        onShowAddWebLinkDialog()
                        onDismiss()
                    },
                ),
            )
            add(
                ActionItem(
                    title = "Obsidian нотатка",
                    icon = Icons.Outlined.DataObject,
                    color = MaterialTheme.colorScheme.secondary,
                    action = {
                        onShowAddObsidianLinkDialog()
                        onDismiss()
                    },
                ),
            )
            add(
                ActionItem(
                    title = "Вкладений проект",
                    icon = Icons.Outlined.AccountTree,
                color = MaterialTheme.colorScheme.secondary,
                action = {
                    onAddNestedProjectClick()
                    onDismiss()
                },
            ),
            )
            add(
                ActionItem(
                    title = "Чекліст",
                    icon = Icons.Outlined.Checklist,
                    color = MaterialTheme.colorScheme.secondary,
                    action = {
                        onCreateChecklist()
                        onDismiss()
                    },
                ),
            )
            if (scriptsEnabled && onAddScript != null) {
                add(
                    ActionItem(
                        title = "Скрипт",
                        icon = Icons.Outlined.Code,
                        color = MaterialTheme.colorScheme.secondary,
                        action = {
                            onAddScript()
                            onDismiss()
                        },
                    ),
                )
            }
            if (isProjectManagementEnabled) {
                add(
                    ActionItem(
                        title = "Віха",
                        icon = Icons.Outlined.Flag,
                        color = MaterialTheme.colorScheme.tertiary,
                        isSelected = currentInputMode == InputMode.AddMilestone,
                        action = {
                            onInputModeSelected(InputMode.AddMilestone)
                            onDismiss()
                        },
                    ),
                )
            }
        }

    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            ),
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ActionGrid(
                    title = "Add",
                    items = linkActions,
                    haptic = haptic,
                )
            }
        }
    }
}

@Composable
private fun ActionGrid(
    title: String,
    items: List<ActionItem>,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height((items.size + 2) / 3 * 120.dp),
        ) {
            items(items) { item ->
                ActionGridItem(
                    item = item,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        item.action()
                    },
                )
            }
        }
    }
}

@Composable
private fun ActionGridItem(
    item: ActionItem,
    onClick: () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue =
            when {
                isPressed -> 0.88f
                item.isSelected -> 1.05f
                else -> 1f
            },
        animationSpec =
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        label = "item_scale",
    )

    val backgroundColor by animateColorAsState(
        targetValue =
            when {
                item.isSelected -> item.color.copy(alpha = 0.15f)
                else -> Color.Transparent
            },
        animationSpec = tween(200),
        label = "item_background",
    )

    val iconColor by animateColorAsState(
        targetValue =
            when {
                item.isSelected -> item.color
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        animationSpec = tween(200),
        label = "icon_color",
    )

    Surface(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier =
            Modifier
                .size(width = 90.dp, height = 120.dp)
                .scale(scale),
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        tonalElevation = if (item.isSelected) 3.dp else 0.dp,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier =
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.isSelected) {
                                item.color.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                        ),
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp),
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Visible,
                color =
                    if (item.isSelected) {
                        item.color
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                fontWeight = if (item.isSelected) FontWeight.Medium else FontWeight.Normal,
                lineHeight = 14.sp,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}
