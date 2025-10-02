package com.romankozak.forwardappmobile.ui.screens.reminders.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowRow

data class ReminderAction(
    val text: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun ReminderActionsDialog(
    onDismiss: () -> Unit,
    actions: List<ReminderAction>
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Actions") },
        text = {
            FlowRow(
                mainAxisSpacing = 8.dp,
                crossAxisSpacing = 8.dp
            ) {
                actions.forEach { action ->
                    SquareButton(action)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun SquareButton(action: ReminderAction) {
    Column(
        modifier = Modifier
            .size(80.dp)
            .clickable(onClick = action.onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(action.icon, contentDescription = action.text)
        Spacer(modifier = Modifier.height(4.dp))
        Text(action.text, textAlign = TextAlign.Center)
    }
}
