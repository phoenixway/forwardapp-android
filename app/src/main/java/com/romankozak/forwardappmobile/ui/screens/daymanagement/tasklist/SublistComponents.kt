package com.romankozak.forwardappmobile.ui.screens.daymanagement.tasklist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class SubTask(val id: Int, val title: String, val isCompleted: Boolean)

@Composable
fun TaskSublist(
    subTasks: List<SubTask>,
    onSubTaskToggled: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        subTasks.forEach { subTask ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSubTaskToggled(subTask.id) }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = subTask.isCompleted,
                    onCheckedChange = { onSubTaskToggled(subTask.id) }
                )
                Text(
                    text = subTask.title,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}