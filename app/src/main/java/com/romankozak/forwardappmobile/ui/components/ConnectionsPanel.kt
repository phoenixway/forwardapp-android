package com.romankozak.forwardappmobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.romankozak.forwardappmobile.ui.components.connectionspanel.ConnectionsCreateActionsDialog
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

enum class ConnectionType {
    CONTEXT,
    ATTACHMENT,
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
    CHECKLIST,
    SCRIPT,
    EXTERNAL_LINK,
    OBSIDIAN_NOTE,
}

data class ConnectionItemUi(
    val id: String,
    val title: String,
    val type: ConnectionType,
)

@Composable
fun ConnectionsPanel(
    items: List<ConnectionItemUi>,
    onConnectionClick: (ConnectionItemUi) -> Unit,
    onConnectionRemove: (ConnectionItemUi) -> Unit,
    onConnectionCopy: ((ConnectionItemUi) -> Unit)? = null,
    onConnectionCut: ((ConnectionItemUi) -> Unit)? = null,
    onAddConnection: (AddConnectionType) -> Unit,
    onAddButtonClick: (() -> Unit)? = null,
    onCreateConnection: ((CreateConnectionType) -> Unit)? = null,
    preferActionsBesideTitleWhenWide: Boolean = false,
    onConnectionsReordered: (List<ConnectionItemUi>) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    var showCreateActionsDialog by remember { mutableStateOf(false) }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Connections",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onCreateConnection != null) {
                    FilledTonalIconButton(
                        onClick = { showCreateActionsDialog = true },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Створити новий зв'язок",
                        )
                    }
                }
                FilledTonalIconButton(
                    onClick = {
                        if (onAddButtonClick != null) {
                            onAddButtonClick()
                        } else {
                            onAddConnection(AddConnectionType.CONTEXT)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = "Додати існуючий зв'язок",
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (internalItems.isEmpty()) {
                Text(
                    text = "Поки немає зв'язків. Додай перший через +",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
            } else {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.heightIn(max = 440.dp),
                ) {
                    items(internalItems, key = { "${it.type}-${it.id}" }) { item ->
                        ReorderableItem(reorderableState, key = "${item.type}-${item.id}") {
                            ConnectionRow(
                                item = item,
                                onOpen = { onConnectionClick(item) },
                                onRemove = { onConnectionRemove(item) },
                                onCopy = onConnectionCopy?.let { { it(item) } },
                                onCut = onConnectionCut?.let { { it(item) } },
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
        }
    }

    if (showCreateActionsDialog && onCreateConnection != null) {
        ConnectionsCreateActionsDialog(
            onDismiss = { showCreateActionsDialog = false },
            onActionSelected = { type ->
                showCreateActionsDialog = false
                onCreateConnection(type)
            },
        )
    }
}

@Composable
private fun ConnectionRow(
    item: ConnectionItemUi,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
    onCopy: (() -> Unit)? = null,
    onCut: (() -> Unit)? = null,
    preferActionsBesideTitleWhenWide: Boolean = false,
    typeIconModifier: Modifier = Modifier,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val showActionsOnRight = preferActionsBesideTitleWhenWide && maxWidth >= 420.dp

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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = item.type.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (!showActionsOnRight) {
                        Spacer(modifier = Modifier.height(6.dp))
                        ConnectionActionsRow(
                            onCopy = onCopy,
                            onCut = onCut,
                            onRemove = onRemove,
                        )
                    }
                }

                if (showActionsOnRight) {
                    ConnectionActionsRow(
                        onCopy = onCopy,
                        onCut = onCut,
                        onRemove = onRemove,
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        )
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
                .background(type.tint.copy(alpha = 0.18f)),
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
            ConnectionType.SCRIPT -> Icons.Outlined.Code
            ConnectionType.URL -> Icons.Outlined.Language
            ConnectionType.OBSIDIAN_NOTE -> Icons.Outlined.StickyNote2
        }

private val ConnectionType.tint: Color
    get() =
        when (this) {
            ConnectionType.CONTEXT -> Color(0xFF2E7D32)
            ConnectionType.ATTACHMENT -> Color(0xFF1565C0)
            ConnectionType.SCRIPT -> Color(0xFF00695C)
            ConnectionType.URL -> Color(0xFF6A1B9A)
            ConnectionType.OBSIDIAN_NOTE -> Color(0xFF455A64)
        }

private val ConnectionType.label: String
    get() =
        when (this) {
            ConnectionType.CONTEXT -> "Контекст"
            ConnectionType.ATTACHMENT -> "Вкладення"
            ConnectionType.SCRIPT -> "Скрипт"
            ConnectionType.URL -> "URL"
            ConnectionType.OBSIDIAN_NOTE -> "Obsidian"
        }
