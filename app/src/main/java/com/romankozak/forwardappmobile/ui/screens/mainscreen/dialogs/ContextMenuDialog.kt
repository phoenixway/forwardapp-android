package com.romankozak.forwardappmobile.ui.screens.mainscreen.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.romankozak.forwardappmobile.data.database.models.Project

@Composable
fun ContextMenuDialog(
    project: Project,
    onDismissRequest: () -> Unit,
    onMoveRequest: (Project) -> Unit,
    onAddSubprojectRequest: (Project) -> Unit,
    onDeleteRequest: (Project) -> Unit,
    onEditRequest: (Project) -> Unit,
    onAddToDayPlanRequest: (Project) -> Unit,
    onSetReminderRequest: (Project) -> Unit,
    onFocusRequest: (Project) -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier.width(300.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Column {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                )
                HorizontalDivider()
                // Планування та фокус
                DialogActionItem(
                    text = "Фокусуватись на проекті",
                    icon = Icons.Default.FilterCenterFocus,
                    onClick = { onFocusRequest(project) },
                )
                HorizontalDivider()
                DialogActionItem(
                    text = "Додати в план дня",
                    icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                    onClick = { onAddToDayPlanRequest(project) },
                )
                HorizontalDivider()
                DialogActionItem(
                    text = "Встановити нагадування",
                    icon = Icons.Default.Alarm,
                    onClick = { onSetReminderRequest(project) },
                )
                HorizontalDivider()
                // Структурні зміни
                DialogActionItem(
                    text = "Перемістити",
                    icon = Icons.Default.MoveUp,
                    onClick = { onMoveRequest(project) },
                )
                HorizontalDivider()
                DialogActionItem(
                    text = "Додати підпроект",
                    icon = Icons.Default.Add,
                    onClick = { onAddSubprojectRequest(project) },
                )
                HorizontalDivider()
                DialogActionItem(
                    text = "Редагувати",
                    icon = Icons.Default.Edit,
                    onClick = {
                        onEditRequest(project)
                        onDismissRequest()
                    },
                )
                HorizontalDivider()
                // Небезпечна дія
                DialogActionItem(
                    text = "Видалити",
                    icon = Icons.Default.Delete,
                    color = MaterialTheme.colorScheme.error,
                    onClick = { onDeleteRequest(project) },
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
            tint = color.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}
