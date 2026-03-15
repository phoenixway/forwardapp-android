package com.romankozak.forwardappmobile.ui.components.connectionspanel

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.romankozak.forwardappmobile.ui.components.CreateConnectionType

private const val DIALOG_WIDTH_FRACTION = 0.92f
private const val ACTION_GRID_COLUMNS = 3

private data class ConnectionCreateItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val type: CreateConnectionType,
)

@Composable
fun ConnectionsCreateActionsDialog(
    onDismiss: () -> Unit,
    onActionSelected: (CreateConnectionType) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val items = createConnectionItems()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(DIALOG_WIDTH_FRACTION),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Create",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(ACTION_GRID_COLUMNS),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 2.dp),
                    modifier = Modifier.height(320.dp),
                ) {
                    items(items) { item ->
                        ConnectionCreateGridItem(
                            item = item,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onActionSelected(item.type)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionCreateGridItem(
    item: ConnectionCreateItem,
    onClick: () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
        label = "connection_create_scale",
    )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .scale(scale)
                .background(MaterialTheme.colorScheme.surfaceContainerLow, RoundedCornerShape(14.dp))
                .clickable(
                    onClick = {
                        isPressed = true
                        onClick()
                        isPressed = false
                    },
                )
                .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        androidx.compose.foundation.layout.Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .background(item.color.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.color,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun createConnectionItems(): List<ConnectionCreateItem> =
    listOf(
        ConnectionCreateItem(
            "Контекст",
            Icons.Outlined.AccountTree,
            MaterialTheme.colorScheme.tertiary,
            CreateConnectionType.CONTEXT,
        ),
        ConnectionCreateItem(
            "Нотатка",
            Icons.AutoMirrored.Outlined.StickyNote2,
            MaterialTheme.colorScheme.primary,
            CreateConnectionType.NOTE_DOCUMENT,
        ),
        ConnectionCreateItem(
            "Ноти",
            Icons.Outlined.MusicNote,
            MaterialTheme.colorScheme.primary,
            CreateConnectionType.MUSIC_NOTE,
        ),
        ConnectionCreateItem(
            "Чекліст",
            Icons.Outlined.Checklist,
            MaterialTheme.colorScheme.secondary,
            CreateConnectionType.CHECKLIST,
        ),
        ConnectionCreateItem(
            "Скрипт",
            Icons.Outlined.Code,
            MaterialTheme.colorScheme.secondary,
            CreateConnectionType.SCRIPT,
        ),
        ConnectionCreateItem(
            "Web посилання",
            Icons.Outlined.Public,
            MaterialTheme.colorScheme.secondary,
            CreateConnectionType.EXTERNAL_LINK,
        ),
        ConnectionCreateItem(
            "Obsidian note",
            Icons.Outlined.DataObject,
            MaterialTheme.colorScheme.secondary,
            CreateConnectionType.OBSIDIAN_NOTE,
        ),
    )
