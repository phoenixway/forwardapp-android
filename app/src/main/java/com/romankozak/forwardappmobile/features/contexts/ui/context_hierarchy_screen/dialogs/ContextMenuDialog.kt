package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.MoveUp
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.romankozak.forwardappmobile.core.data.models.entities.Context

private data class ContextActionItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit,
)

private enum class AddEntityType {
    CONTEXT,
    NOTE_DOCUMENT,
    CHECKLIST,
}

@Composable
fun ContextMenuDialog(
    project: Context,
    isUserFocused: Boolean,
    onDismissRequest: () -> Unit,
    onMoveRequest: (Context) -> Unit,
    onAddSubprojectRequest: (Context) -> Unit,
    onDeleteRequest: (Context) -> Unit,
    onEditRequest: (Context) -> Unit,
    onAddToDayPlanRequest: (Context) -> Unit,
    onSetReminderRequest: (Context) -> Unit,
    onToggleUserFocusRequest: (Context) -> Unit,
    onCopyContextLinkRequest: (Context) -> Unit,
    onCutContextLinkRequest: (Context) -> Unit,
    onPasteContextLinkRequest: (Context) -> Unit,
    onAddNoteDocumentRequest: (Context) -> Unit,
    onAddChecklistRequest: (Context) -> Unit,
    canPasteContextLinks: Boolean,
) {
    var showAddActionsDialog by remember { mutableStateOf(false) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val menuItems =
        buildList {
            add(
                ContextActionItem(
                    title = "Додати",
                    icon = Icons.Default.Add,
                    color = primaryColor,
                    onClick = { showAddActionsDialog = true },
                ),
            )
            add(
                ContextActionItem(
                    title = if (isUserFocused) "Зняти фокус" else "Фокус",
                    icon = Icons.Default.FilterCenterFocus,
                    color = tertiaryColor,
                    onClick = { onToggleUserFocusRequest(project) },
                ),
            )
            add(
                ContextActionItem(
                    title = "План дня",
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    color = secondaryColor,
                    onClick = { onAddToDayPlanRequest(project) },
                ),
            )
            add(
                ContextActionItem(
                    title = "Нагадування",
                    icon = Icons.Default.Alarm,
                    color = secondaryColor,
                    onClick = { onSetReminderRequest(project) },
                ),
            )
            add(
                ContextActionItem(
                    title = "Копіювати",
                    icon = Icons.Default.ContentCopy,
                    color = secondaryColor,
                    onClick = { onCopyContextLinkRequest(project) },
                ),
            )
            add(
                ContextActionItem(
                    title = "Вирізати",
                    icon = Icons.Default.ContentCut,
                    color = secondaryColor,
                    onClick = { onCutContextLinkRequest(project) },
                ),
            )
            if (canPasteContextLinks) {
                add(
                    ContextActionItem(
                        title = "Вставити",
                        icon = Icons.Default.ContentPaste,
                        color = secondaryColor,
                        onClick = { onPasteContextLinkRequest(project) },
                    ),
                )
            }
            add(
                ContextActionItem(
                    title = "Перемістити",
                    icon = Icons.Default.MoveUp,
                    color = secondaryColor,
                    onClick = { onMoveRequest(project) },
                ),
            )
            add(
                ContextActionItem(
                    title = "Редагувати",
                    icon = Icons.Default.Edit,
                    color = secondaryColor,
                    onClick = { onEditRequest(project) },
                ),
            )
        }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 2.dp),
                    modifier = Modifier.height(336.dp),
                ) {
                    items(menuItems) { item ->
                        ActionGridItem(
                            title = item.title,
                            icon = item.icon,
                            color = item.color,
                            onClick = item.onClick,
                        )
                    }
                }
                FilledTonalButton(
                    onClick = { onDeleteRequest(project) },
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Видалити")
                }
            }
        }
    }

    if (showAddActionsDialog) {
        AddContextEntityDialog(
            onDismiss = { showAddActionsDialog = false },
            onSelect = { type ->
                showAddActionsDialog = false
                when (type) {
                    AddEntityType.CONTEXT -> onAddSubprojectRequest(project)
                    AddEntityType.NOTE_DOCUMENT -> onAddNoteDocumentRequest(project)
                    AddEntityType.CHECKLIST -> onAddChecklistRequest(project)
                }
            },
        )
    }
}

@Composable
private fun ActionGridItem(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .background(color.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun AddContextEntityDialog(
    onDismiss: () -> Unit,
    onSelect: (AddEntityType) -> Unit,
) {
    val items =
        listOf(
            Triple("Контекст", Icons.Outlined.AccountTree, AddEntityType.CONTEXT),
            Triple("Нотатка", Icons.Outlined.Description, AddEntityType.NOTE_DOCUMENT),
            Triple("Чекліст", Icons.Outlined.Checklist, AddEntityType.CHECKLIST),
        )

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Додати",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 2.dp),
                    modifier = Modifier.height(140.dp),
                ) {
                    items(items) { item ->
                        ActionGridItem(
                            title = item.first,
                            icon = item.second,
                            color = MaterialTheme.colorScheme.secondary,
                            onClick = { onSelect(item.third) },
                        )
                    }
                }
            }
        }
    }
}
