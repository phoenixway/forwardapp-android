package com.romankozak.forwardappmobile.ui.screens.contextcreen.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoveDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import androidx.compose.material.icons.filled.Notifications
import com.romankozak.forwardappmobile.ui.screens.contextcreen.GoalActionType

@Composable
fun GoalActionChoiceDialog(
    itemContent: ListItemContent,
    onDismiss: () -> Unit,
    onActionSelected: (GoalActionType) -> Unit,
    onSetReminder: () -> Unit,
    onOpenRemindersDialog: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(300.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Column {
                DialogActionItem(
                    text = "Перемістити елемент",
                    icon = Icons.Default.MoveDown,
                    onClick = {
                        onActionSelected(GoalActionType.MoveInstance)
                        onDismiss()
                    },
                )

                if (itemContent is ListItemContent.GoalItem) {
                    HorizontalDivider()
                    DialogActionItem(
                        text = "Створити посилання (ярлик)",
                        icon = Icons.Default.Link,
                        onClick = {
                            onActionSelected(GoalActionType.CreateInstance)
                            onDismiss()
                        },
                    )
                    HorizontalDivider()
                    DialogActionItem(
                        text = "Клонувати (повна копія)",
                        icon = Icons.Default.ContentCopy,
                        onClick = {
                            onActionSelected(GoalActionType.CopyGoal)
                            onDismiss()
                        },
                    )
                }
                HorizontalDivider()
                DialogActionItem(
                    text = "Нагадування...",
                    icon = Icons.Default.Notifications,
                    onClick = {
                        onOpenRemindersDialog()
                        onDismiss()
                    },
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
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
