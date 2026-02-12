package com.romankozak.forwardappmobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class ConnectionType {
    CONTEXT,
    ATTACHMENT,
    URL,
    OBSIDIAN_NOTE,
}

enum class AddConnectionType {
    CONTEXT,
    ATTACHMENT,
    EXTERNAL_LINK,
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
    onAddConnection: (AddConnectionType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var addMenuExpanded by remember { mutableStateOf(false) }

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

            Box {
                FilledTonalIconButton(
                    onClick = { addMenuExpanded = true },
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Додати зв'язок")
                }
                DropdownMenu(
                    expanded = addMenuExpanded,
                    onDismissRequest = { addMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Контекст") },
                        onClick = {
                            addMenuExpanded = false
                            onAddConnection(AddConnectionType.CONTEXT)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Вкладення") },
                        onClick = {
                            addMenuExpanded = false
                            onAddConnection(AddConnectionType.ATTACHMENT)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Зовнішнє посилання") },
                        onClick = {
                            addMenuExpanded = false
                            onAddConnection(AddConnectionType.EXTERNAL_LINK)
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (items.isEmpty()) {
                Text(
                    text = "Поки немає зв'язків. Додай перший через +",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 440.dp)) {
                    items(items, key = { "${it.type}-${it.id}" }) { item ->
                        ConnectionRow(
                            item = item,
                            onOpen = { onConnectionClick(item) },
                            onRemove = { onConnectionRemove(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionRow(
    item: ConnectionItemUi,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TypeIcon(type = item.type)
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
            }
            IconButton(onClick = onOpen) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = "Відкрити",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Видалити",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 12.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        )
    }
}

@Composable
private fun TypeIcon(type: ConnectionType) {
    Box(
        modifier =
            Modifier
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
            ConnectionType.URL -> Icons.Outlined.Language
            ConnectionType.OBSIDIAN_NOTE -> Icons.Outlined.StickyNote2
        }

private val ConnectionType.tint: Color
    get() =
        when (this) {
            ConnectionType.CONTEXT -> Color(0xFF2E7D32)
            ConnectionType.ATTACHMENT -> Color(0xFF1565C0)
            ConnectionType.URL -> Color(0xFF6A1B9A)
            ConnectionType.OBSIDIAN_NOTE -> Color(0xFF455A64)
        }

private val ConnectionType.label: String
    get() =
        when (this) {
            ConnectionType.CONTEXT -> "Контекст"
            ConnectionType.ATTACHMENT -> "Вкладення"
            ConnectionType.URL -> "URL"
            ConnectionType.OBSIDIAN_NOTE -> "Obsidian"
        }
