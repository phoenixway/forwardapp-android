package com.romankozak.forwardappmobile.ui.reminders.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow

data class ReminderAction(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val color: Color = Color.Unspecified
)

 @Composable
fun ReminderActionsDialog(
    onDismiss: () -> Unit,
    actions: List<ReminderAction>
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Дії",
                style = MaterialTheme.typography.titleMedium
            ) 
        },
        text = {
            FlowRow(
                mainAxisSpacing = 6.dp,
                crossAxisSpacing = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                actions.forEach { action ->
                    ActionButton(action)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрити")
            }
        }
    )
}

 @Composable
fun ActionButton(action: ReminderAction) {
    val contentColor = if (action.color != Color.Unspecified) {
        action.color
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Surface(
        modifier = Modifier.size(72.dp),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        onClick = action.onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.text,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = action.text,
                textAlign = TextAlign.Center,
                color = contentColor,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2
            )
        }
    }
}