package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context

private data class ContextActionItem(
    val title: String,
    val subtitle: String? = null,
    val icon: ImageVector,
    val tint: Color,
    val onClick: () -> Unit,
)

private data class ContextActionSection(
    val title: String,
    val items: List<ContextActionItem>,
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
    onAddContextAppearanceRequest: (Context) -> Unit,
    onAddNoteDocumentRequest: (Context) -> Unit,
    onAddChecklistRequest: (Context) -> Unit,
    canPasteContextLinks: Boolean,
) {
    var showAddActionsDialog by remember { mutableStateOf(false) }
    val contextId = remember(project.id) { ContextId(project.id) }
    val isSystemContext = remember(project.id) { SystemContexts.isSystem(contextId) }
    val canRenameOrMove = remember(project.id) { SystemContexts.canRenameOrMove(contextId) }
    val colorScheme = MaterialTheme.colorScheme

    val createSection =
        ContextActionSection(
            title = "Створення",
            items =
                listOf(
                    ContextActionItem(
                        title = "Додати в контекст",
                        icon = Icons.Default.Add,
                        tint = colorScheme.primary,
                        onClick = { showAddActionsDialog = true },
                    ),
                ),
        )

    val organizeSection =
        ContextActionSection(
            title = "Організація",
            items =
                buildList {
                    if (canRenameOrMove) {
                        add(
                            ContextActionItem(
                                title = "Редагувати",
                                icon = Icons.Default.Edit,
                                tint = colorScheme.secondary,
                                onClick = { onEditRequest(project) },
                            ),
                        )
                        add(
                            ContextActionItem(
                                title = "Перемістити",
                                icon = Icons.Default.FolderOpen,
                                tint = colorScheme.secondary,
                                onClick = { onMoveRequest(project) },
                            ),
                        )
                    }
                    add(
                        ContextActionItem(
                            title = if (isUserFocused) "Зняти з фокусу" else "Додати у фокус",
                            icon = Icons.Default.FilterCenterFocus,
                            tint = colorScheme.tertiary,
                            onClick = { onToggleUserFocusRequest(project) },
                        ),
                    )
                },
        )

    val planningSection =
        ContextActionSection(
            title = "Планування",
            items =
                listOf(
                    ContextActionItem(
                        title = "Додати в план дня",
                        icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                        tint = colorScheme.secondary,
                        onClick = { onAddToDayPlanRequest(project) },
                    ),
                    ContextActionItem(
                        title = "Нагадування",
                        icon = Icons.Default.Alarm,
                        tint = colorScheme.secondary,
                        onClick = { onSetReminderRequest(project) },
                    ),
                ),
        )

    val clipboardItems =
        buildList {
            add(
                ContextActionItem(
                    title = "Копіювати посилання",
                    icon = Icons.Default.ContentCopy,
                    tint = colorScheme.secondary,
                    onClick = { onCopyContextLinkRequest(project) },
                ),
            )
            add(
                ContextActionItem(
                    title = "Вирізати посилання",
                    icon = Icons.Default.ContentCut,
                    tint = colorScheme.secondary,
                    onClick = { onCutContextLinkRequest(project) },
                ),
            )
            if (canPasteContextLinks) {
                add(
                    ContextActionItem(
                        title = "Додати появу тут",
                        icon = Icons.Outlined.AccountTree,
                        tint = colorScheme.tertiary,
                        onClick = { onAddContextAppearanceRequest(project) },
                    ),
                )
                add(
                    ContextActionItem(
                        title = "Вставити посилання",
                        icon = Icons.Default.ContentPaste,
                        tint = colorScheme.secondary,
                        onClick = { onPasteContextLinkRequest(project) },
                    ),
                )
            }
        }

    val sections =
        listOf(
            createSection,
            organizeSection,
            planningSection,
            ContextActionSection(title = "Буфер обміну", items = clipboardItems),
        ).filter { it.items.isNotEmpty() }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 460.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ContextMenuHeader(
                    title = project.name,
                    isSystemContext = isSystemContext,
                    onDismissRequest = onDismissRequest,
                )

                sections.forEachIndexed { index, section ->
                    ContextActionSectionCard(section = section)
                    if (index != sections.lastIndex) {
                        HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }

                if (!isSystemContext) {
                    DestructiveContextAction(
                        title = "Видалити контекст",
                        subtitle = "Разом із вкладеним вмістом",
                        onClick = { onDeleteRequest(project) },
                    )
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
private fun ContextMenuHeader(
    title: String,
    isSystemContext: Boolean,
    onDismissRequest: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "Дії з контекстом",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(onClick = onDismissRequest) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Закрити",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ContextActionSectionCard(section: ContextActionSection) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = section.title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            section.items.forEach { item ->
                ContextActionRow(
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    tint = item.tint,
                    onClick = item.onClick,
                )
            }
        }
    }
}

@Composable
private fun ContextActionRow(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .background(tint.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun DestructiveContextAction(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f),
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Що додати?",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Оберіть тип сутності для цього контексту",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрити",
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { item ->
                        ContextActionRow(
                            title = item.first,
                            subtitle =
                                when (item.third) {
                                    AddEntityType.CONTEXT -> null
                                    AddEntityType.NOTE_DOCUMENT -> null
                                    AddEntityType.CHECKLIST -> null
                                },
                            icon = item.second,
                            tint = MaterialTheme.colorScheme.secondary,
                            onClick = { onSelect(item.third) },
                        )
                    }
                }
            }
        }
    }
}
