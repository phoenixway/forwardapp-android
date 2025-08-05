package com.romankozak.forwardappmobile.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onEditRequest: (GoalList) -> Unit // ✨ Перейменовано
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier.width(300.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = list.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Перемістити список",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMoveRequest(list) }
                        .padding(vertical = 12.dp)
                )
                HorizontalDivider()

                // ✨ Змінено текст та виклик
                Text(
                    text = "Редагувати",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditRequest(list) }
                        .padding(vertical = 12.dp)
                )
                HorizontalDivider()

                Text(
                    text = "Додати підсписок",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddSublistRequest(list) }
                        .padding(vertical = 12.dp)
                )
                HorizontalDivider()

                Text(
                    text = "Видалити список",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDeleteRequest(list) }
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}