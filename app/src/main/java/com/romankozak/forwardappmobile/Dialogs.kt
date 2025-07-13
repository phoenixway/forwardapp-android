package com.romankozak.forwardappmobile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EditGoalDialog(
    goal: Goal,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember(goal) { mutableStateOf(goal.text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Редагувати ціль") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Текст цілі") }
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    // DEBUG: Перевіряємо, чи спрацьовує кнопка
                    println("DEBUG: Кнопка 'Зберегти' натиснута в EditGoalDialog.")
                    if (text.isNotBlank()) {
                        onConfirm(text)
                    }
                }
            ) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
    )
}

