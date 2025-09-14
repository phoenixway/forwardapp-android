// TaskOptionsBottomSheet.kt
package com.romankozak.forwardappmobile.ui.screens.daymanagement

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.DayTask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskOptionsBottomSheet(
    task: DayTask,
    onDismiss: () -> Unit,
    onEdit: (DayTask) -> Unit,
    onDelete: (DayTask) -> Unit,
    onSetReminder: (DayTask) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                "Опції завдання",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OptionItem(
                icon = Icons.Default.Edit,
                text = "Редагувати",
                onClick = { onEdit(task) }
            )

            OptionItem(
                icon = Icons.Default.Notifications,
                text = "Встановити нагадування",
                onClick = { onSetReminder(task) }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            OptionItem(
                icon = Icons.Default.Delete,
                text = "Видалити",
                onClick = { onDelete(task) },
                contentColor = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun OptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}