package com.romankozak.forwardappmobile.ui.screens.daymanagement.tasklist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Проста модель даних для відображення підзавдання.
 */
data class SubTaskDisplayData(val title: String, val isCompleted: Boolean)

/**
 * Компонент для відображення списку підзавдань.
 * Цей компонент є презентаційним і не обробляє кліки.
 * @param subTasks Список підзавдань для відображення.
 */
@Composable
fun SublistComponent(
    subTasks: List<SubTaskDisplayData>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        subTasks.forEach { subTask ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Checkbox(
                    checked = subTask.isCompleted,
                    onCheckedChange = null // Компонент лише для читання
                )
                Text(
                    text = subTask.title,
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}