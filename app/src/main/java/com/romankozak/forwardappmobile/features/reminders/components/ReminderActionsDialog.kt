package com.romankozak.forwardappmobile.features.reminders.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow

private val ReminderActionButtonSize = 72.dp
private val ReminderActionIconSize = 24.dp
private val ReminderActionSpacing = 6.dp

data class ReminderAction(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val color: Color = Color.Unspecified,
)

@Composable
fun ReminderActionsDialog(
    onDismiss: () -> Unit,
    actions: List<ReminderAction>,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Дії",
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            FlowRow(
                mainAxisSpacing = ReminderActionSpacing,
                crossAxisSpacing = ReminderActionSpacing,
                modifier = Modifier.fillMaxWidth(),
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
        },
    )
}

@Composable
fun ActionButton(action: ReminderAction) {
    val contentColor =
        if (action.color != Color.Unspecified) {
            action.color
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    Surface(
        modifier = Modifier.size(ReminderActionButtonSize),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        onClick = action.onClick,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.text,
                tint = contentColor,
                modifier = Modifier.size(ReminderActionIconSize),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = action.text,
                textAlign = TextAlign.Center,
                color = contentColor,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
            )
        }
    }
}
