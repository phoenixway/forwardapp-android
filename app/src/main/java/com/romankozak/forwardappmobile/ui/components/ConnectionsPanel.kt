@file:Suppress("FunctionNaming", "MagicNumber", "LongParameterList")

package com.romankozak.forwardappmobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.romankozak.forwardappmobile.ui.components.connectionspanel.ConnectionsCreateActionsDialog
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

enum class ConnectionType {
    CONTEXT,
    ATTACHMENT,
    NOTE_DOCUMENT,
    JOURNAL_DOCUMENT,
    CHECKLIST,
    MUSIC_NOTE,
    SCRIPT,
    URL,
    OBSIDIAN_NOTE,
}

enum class AddConnectionType {
    CONTEXT,
    ATTACHMENT,
    EXTERNAL_LINK,
    OBSIDIAN_NOTE,
}

enum class CreateConnectionType {
    CONTEXT,
    NOTE_DOCUMENT,
    JOURNAL_DOCUMENT,
    MUSIC_NOTE,
    CHECKLIST,
    SCRIPT,
    EXTERNAL_LINK,
    OBSIDIAN_NOTE,
}

enum class ConnectionPanelMode {
    NORMAL,
    COMPACT,
}

data class ConnectionItemUi(
    val id: String,
    val title: String,
    val type: ConnectionType,
    val vault: String? = null,
)

private const val NORMAL_TITLE_MAX_LINES = 4
private const val COMPACT_TITLE_MAX_LINES = 2
private const val CONNECTION_DIVIDER_ALPHA = 0.08f
private const val TYPE_ICON_BG_ALPHA = 0.18f
private const val WRAP_CONTENT_LIST_MAX_HEIGHT_DP = 360

private val ContextTypeTint = Color(0xFF2E7D32)
private val AttachmentTypeTint = Color(0xFF1565C0)
private val NoteDocumentTypeTint = Color(0xFF00897B)
private val ChecklistTypeTint = Color(0xFF5E35B1)
private val MusicNoteTypeTint = Color(0xFFE65100)
private val ScriptTypeTint = Color(0xFF00695C)
private val UrlTypeTint = Color(0xFF3949AB)
private val ObsidianTypeTint = Color(0xFF455A64)
private val WideLayoutMinWidth = 420.dp

@Composable
@Suppress("LongParameterList", "LongMethod")
fun ConnectionsPanel(
    items: List<ConnectionItemUi>,
    onConnectionClick: (ConnectionItemUi) -> Unit,
    onConnectionRemove: (ConnectionItemUi) -> Unit,
    onConnectionDeleteEverywhere: ((ConnectionItemUi) -> Unit)? = null,
    onConnectionCopy: ((ConnectionItemUi) -> Unit)? = null,
    onConnectionCut: ((ConnectionItemUi) -> Unit)? = null,
    onAddConnection: (AddConnectionType) -> Unit,
    onAddButtonClick: (() -> Unit)? = null,
    onCreateConnection: ((CreateConnectionType) -> Unit)? = null,
    mode: ConnectionPanelMode = ConnectionPanelMode.COMPACT,
    preferActionsBesideTitleWhenWide: Boolean = false,
    wrapContentHeight: Boolean = false,
    showTitle: Boolean = true,
    onConnectionsReordered: (List<ConnectionItemUi>) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    var showCreateActionsDialog by remember { mutableStateOf(false) }
    var pendingRemovalItem by remember { mutableStateOf<ConnectionItemUi?>(null) }
    var internalItems by remember { mutableStateOf(items) }
    val lazyListState = rememberLazyListState()
    val reorderableState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            internalItems =
                internalItems.toMutableList().apply {
                    add(to.index, removeAt(from.index))
                }
            onConnectionsReordered(internalItems)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }

    LaunchedEffect(items) {
        internalItems = items
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
    ) {
        ConnectionsHeader(
            showTitle = showTitle,
            onShowCreateActions = onCreateConnection?.let { { showCreateActionsDialog = true } },
            onAddExistingConnection = {
                if (onAddButtonClick != null) {
                    onAddButtonClick()
                } else {
                    onAddConnection(AddConnectionType.CONTEXT)
                }
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        ConnectionsListCard(
            items = internalItems,
            lazyListState = lazyListState,
            reorderableState = reorderableState,
            wrapContentHeight = wrapContentHeight,
            onConnectionClick = onConnectionClick,
            onPendingRemovalChange = { pendingRemovalItem = it },
            onConnectionCopy = onConnectionCopy,
            onConnectionCut = onConnectionCut,
            mode = mode,
            preferActionsBesideTitleWhenWide = preferActionsBesideTitleWhenWide,
            hapticFeedback = hapticFeedback,
        )
    }

    ConnectionsFooterDialogs(
        showCreateActionsDialog = showCreateActionsDialog,
        onShowCreateActionsDialogChange = { showCreateActionsDialog = it },
        onCreateConnection = onCreateConnection,
        pendingRemovalItem = pendingRemovalItem,
        onPendingRemovalChange = { pendingRemovalItem = it },
        onConnectionDeleteEverywhere = onConnectionDeleteEverywhere,
        onConnectionRemove = onConnectionRemove,
    )
}

@Composable
private fun ConnectionsHeader(
    showTitle: Boolean,
    onShowCreateActions: (() -> Unit)?,
    onAddExistingConnection: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showTitle) {
            Text(
                text = "Connections",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onShowCreateActions != null) {
                FilledTonalIconButton(
                    onClick = onShowCreateActions,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Створити новий зв'язок",
                    )
                }
            }
            FilledTonalIconButton(
                onClick = onAddExistingConnection,
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = "Додати існуючий зв'язок",
                )
            }
        }
    }
}

@Composable
private fun ConnectionsListCard(
    items: List<ConnectionItemUi>,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    reorderableState: sh.calvin.reorderable.ReorderableLazyListState,
    wrapContentHeight: Boolean,
    onConnectionClick: (ConnectionItemUi) -> Unit,
    onPendingRemovalChange: (ConnectionItemUi?) -> Unit,
    onConnectionCopy: ((ConnectionItemUi) -> Unit)?,
    onConnectionCut: ((ConnectionItemUi) -> Unit)?,
    mode: ConnectionPanelMode,
    preferActionsBesideTitleWhenWide: Boolean,
    hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback,
) {
    ElevatedCard(
        modifier =
            if (wrapContentHeight) {
                Modifier.fillMaxWidth()
            } else {
                Modifier.fillMaxWidth()
            },
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        ConnectionsListContent(
            items = items,
            lazyListState = lazyListState,
            reorderableState = reorderableState,
            wrapContentHeight = wrapContentHeight,
            onConnectionClick = onConnectionClick,
            onPendingRemovalChange = onPendingRemovalChange,
            onConnectionCopy = onConnectionCopy,
            onConnectionCut = onConnectionCut,
            mode = mode,
            preferActionsBesideTitleWhenWide = preferActionsBesideTitleWhenWide,
            hapticFeedback = hapticFeedback,
        )
    }
}

@Composable
private fun ConnectionsListContent(
    items: List<ConnectionItemUi>,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    reorderableState: sh.calvin.reorderable.ReorderableLazyListState,
    wrapContentHeight: Boolean,
    onConnectionClick: (ConnectionItemUi) -> Unit,
    onPendingRemovalChange: (ConnectionItemUi?) -> Unit,
    onConnectionCopy: ((ConnectionItemUi) -> Unit)?,
    onConnectionCut: ((ConnectionItemUi) -> Unit)?,
    mode: ConnectionPanelMode,
    preferActionsBesideTitleWhenWide: Boolean,
    hapticFeedback: androidx.compose.ui.hapticfeedback.HapticFeedback,
) {
    if (items.isEmpty()) {
        Text(
            text = "Поки немає зв'язків. Додай перший через +",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        )
        return
    }

    LazyColumn(
        state = lazyListState,
        modifier =
            if (wrapContentHeight) {
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = WRAP_CONTENT_LIST_MAX_HEIGHT_DP.dp)
            } else {
                Modifier.fillMaxSize()
            },
    ) {
        items(items, key = { "${it.type}-${it.id}" }) { item ->
            ReorderableItem(reorderableState, key = "${item.type}-${item.id}") {
                ConnectionRow(
                    item = item,
                    onOpen = { onConnectionClick(item) },
                    onRemoveRequest = { onPendingRemovalChange(item) },
                    onCopy = onConnectionCopy?.let { { it(item) } },
                    onCut = onConnectionCut?.let { { it(item) } },
                    mode = mode,
                    preferActionsBesideTitleWhenWide = preferActionsBesideTitleWhenWide,
                    typeIconModifier =
                        with(this@ReorderableItem) {
                            Modifier.longPressDraggableHandle(
                                onDragStarted = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                            )
                        },
                )
            }
        }
    }
}

@Composable
private fun ConnectionsFooterDialogs(
    showCreateActionsDialog: Boolean,
    onShowCreateActionsDialogChange: (Boolean) -> Unit,
    onCreateConnection: ((CreateConnectionType) -> Unit)?,
    pendingRemovalItem: ConnectionItemUi?,
    onPendingRemovalChange: (ConnectionItemUi?) -> Unit,
    onConnectionDeleteEverywhere: ((ConnectionItemUi) -> Unit)?,
    onConnectionRemove: (ConnectionItemUi) -> Unit,
) {
    if (showCreateActionsDialog && onCreateConnection != null) {
        ConnectionsCreateActionsDialog(
            onDismiss = { onShowCreateActionsDialogChange(false) },
            onActionSelected = { type ->
                onShowCreateActionsDialogChange(false)
                onCreateConnection(type)
            },
        )
    }

    pendingRemovalItem?.let { item ->
        DeleteConnectionDialog(
            item = item,
            onDismiss = { onPendingRemovalChange(null) },
            onDeleteEverywhere = {
                (onConnectionDeleteEverywhere ?: onConnectionRemove)(item)
                onPendingRemovalChange(null)
            },
            onDeleteFromListOnly = {
                onConnectionRemove(item)
                onPendingRemovalChange(null)
            },
        )
    }
}

@Composable
@Suppress("LongParameterList", "LongMethod")
private fun ConnectionRow(
    item: ConnectionItemUi,
    onOpen: () -> Unit,
    onRemoveRequest: () -> Unit,
    onCopy: (() -> Unit)? = null,
    onCut: (() -> Unit)? = null,
    mode: ConnectionPanelMode = ConnectionPanelMode.COMPACT,
    preferActionsBesideTitleWhenWide: Boolean = false,
    typeIconModifier: Modifier = Modifier,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val showActionsOnRight = mode.shouldShowActionsOnRight(preferActionsBesideTitleWhenWide, maxWidth)
            val titleMaxLines = mode.titleMaxLines

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpen)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = if (showActionsOnRight) Alignment.CenterVertically else Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TypeIcon(type = item.type, modifier = typeIconModifier)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = titleMaxLines,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = item.type.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (item.type == ConnectionType.OBSIDIAN_NOTE && !item.vault.isNullOrBlank()) {
                        Text(
                            text = "Vault: ${item.vault}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }

                    if (!showActionsOnRight) {
                        Spacer(modifier = Modifier.height(6.dp))
                        ConnectionActionsRow(
                            onCopy = onCopy,
                            onCut = onCut,
                            onRemove = onRemoveRequest,
                        )
                    }
                }

                if (showActionsOnRight) {
                    ConnectionActionsRow(
                        onCopy = onCopy,
                        onCut = onCut,
                        onRemove = onRemoveRequest,
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = CONNECTION_DIVIDER_ALPHA),
        )
    }
}

@Composable
private fun DeleteConnectionDialog(
    item: ConnectionItemUi,
    onDismiss: () -> Unit,
    onDeleteEverywhere: () -> Unit,
    onDeleteFromListOnly: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Видалити зв'язок?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDeleteEverywhere) {
                        Text(text = "Видалити всюди", color = MaterialTheme.colorScheme.error)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDeleteFromListOnly) {
                        Text(text = "Видалити з цього списку")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Відмінити")
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionActionsRow(
    onCopy: (() -> Unit)?,
    onCut: (() -> Unit)?,
    onRemove: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onCopy != null) {
            IconButton(onClick = onCopy) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Копіювати",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (onCut != null) {
            IconButton(onClick = onCut) {
                Icon(
                    imageVector = Icons.Outlined.ContentCut,
                    contentDescription = "Вирізати",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "Видалити",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun TypeIcon(
    type: ConnectionType,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(type.tint.copy(alpha = TYPE_ICON_BG_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = type.icon,
            contentDescription = null,
            tint = type.tint,
            modifier = Modifier.size(16.dp),
        )
    }
}

private val ConnectionType.icon: ImageVector
    get() =
        when (this) {
            ConnectionType.CONTEXT -> Icons.Outlined.AccountTree
            ConnectionType.ATTACHMENT -> Icons.Outlined.AttachFile
            ConnectionType.NOTE_DOCUMENT -> Icons.Outlined.Description
            ConnectionType.JOURNAL_DOCUMENT -> Icons.AutoMirrored.Outlined.StickyNote2
            ConnectionType.CHECKLIST -> Icons.Outlined.Checklist
            ConnectionType.MUSIC_NOTE -> Icons.Outlined.MusicNote
            ConnectionType.SCRIPT -> Icons.Outlined.Code
            ConnectionType.URL -> Icons.Outlined.Language
            ConnectionType.OBSIDIAN_NOTE -> Icons.AutoMirrored.Outlined.StickyNote2
        }

private val ConnectionType.tint: Color
    get() =
        when (this) {
            ConnectionType.CONTEXT -> ContextTypeTint
            ConnectionType.ATTACHMENT -> AttachmentTypeTint
            ConnectionType.NOTE_DOCUMENT -> NoteDocumentTypeTint
            ConnectionType.JOURNAL_DOCUMENT -> NoteDocumentTypeTint
            ConnectionType.CHECKLIST -> ChecklistTypeTint
            ConnectionType.MUSIC_NOTE -> MusicNoteTypeTint
            ConnectionType.SCRIPT -> ScriptTypeTint
            ConnectionType.URL -> UrlTypeTint
            ConnectionType.OBSIDIAN_NOTE -> ObsidianTypeTint
        }

private val ConnectionType.label: String
    get() =
        when (this) {
            ConnectionType.CONTEXT -> "Контекст"
            ConnectionType.ATTACHMENT -> "Вкладення"
            ConnectionType.NOTE_DOCUMENT -> "Нотатка"
            ConnectionType.JOURNAL_DOCUMENT -> "Журнал"
            ConnectionType.CHECKLIST -> "Чекліст"
            ConnectionType.MUSIC_NOTE -> "Музичні ноти"
            ConnectionType.SCRIPT -> "Скрипт"
            ConnectionType.URL -> "Веб-посилання"
            ConnectionType.OBSIDIAN_NOTE -> "Посилання Obsidian"
        }

private fun ConnectionPanelMode.shouldShowActionsOnRight(
    preferActionsBesideTitleWhenWide: Boolean,
    maxWidth: androidx.compose.ui.unit.Dp,
): Boolean =
    when (this) {
        ConnectionPanelMode.COMPACT -> true
        ConnectionPanelMode.NORMAL -> preferActionsBesideTitleWhenWide && maxWidth >= WideLayoutMinWidth
    }

private val ConnectionPanelMode.titleMaxLines: Int
    get() =
        when (this) {
            ConnectionPanelMode.NORMAL -> NORMAL_TITLE_MAX_LINES
            ConnectionPanelMode.COMPACT -> COMPACT_TITLE_MAX_LINES
        }
