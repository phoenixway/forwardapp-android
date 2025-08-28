package com.romankozak.forwardappmobile.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.romankozak.forwardappmobile.data.database.models.GoalList

@Composable
fun ContextMenuDialog(
    list: GoalList,
    onDismissRequest: () -> Unit,
    onMoveRequest: (GoalList) -> Unit,
    onAddSublistRequest: (GoalList) -> Unit,
    onDeleteRequest: (GoalList) -> Unit,
    onEditRequest: (GoalList) -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier.width(300.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Column {
                Text(
                    text = list.name,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
                HorizontalDivider()
                DialogActionItem(
                    text = "Перемістити",
                    icon = Icons.Default.MoveUp,
                    onClick = { onMoveRequest(list) },
                )
                HorizontalDivider()
                DialogActionItem(
                    text = "Редагувати",
                    icon = Icons.Default.Edit,
                    onClick = { onEditRequest(list) },
                )
                HorizontalDivider()
                DialogActionItem(
                    text = "Додати підсписок",
                    icon = Icons.Default.Add,
                    onClick = { onAddSublistRequest(list) },
                )
                HorizontalDivider()
                DialogActionItem(
                    text = "Видалити",
                    icon = Icons.Default.Delete,
                    color = MaterialTheme.colorScheme.error,
                    onClick = { onDeleteRequest(list) },
                )
            }
        }
    }
}

@Composable
private fun DialogActionItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    color: Color = LocalContentColor.current,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = color.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}