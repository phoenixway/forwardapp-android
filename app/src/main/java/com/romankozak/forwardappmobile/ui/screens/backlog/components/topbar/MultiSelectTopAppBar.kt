package com.romankozak.forwardappmobile.ui.screens.backlog.components.topbar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.ui.screens.backlog.GoalActionType

@Composable
fun MultiSelectTopAppBar(
    selectedCount: Int,
    areAllSelected: Boolean,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onDelete: () -> Unit,
    onToggleComplete: () -> Unit,
    onMoreActions: (GoalActionType) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    // Використовуємо Surface для фону та тіні, як у BrowserNavigationBar
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer, // Залишаємо виділяючий фон
        tonalElevation = 3.dp
    ) {
        // Використовуємо Row для гнучкого розташування елементів
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Ліва частина ---
            IconButton(onClick = onClearSelection) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Закрити режим виділення",
                    // Колір контенту для читабельності на фоні primaryContainer
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Text(
                text = "$selectedCount виділено",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(start = 8.dp)
            )

            // --- Розпірка, що заповнює простір ---
            Spacer(modifier = Modifier.weight(1f))

            // --- Права частина (кнопки дій) ---
            IconButton(onClick = onSelectAll, enabled = !areAllSelected) {
                Icon(
                    imageVector = Icons.Default.SelectAll,
                    contentDescription = "Вибрати все",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            IconButton(onClick = onToggleComplete) {
                Icon(
                    imageVector = Icons.Default.DoneAll,
                    contentDescription = "Відмітити виконаними/невиконаними",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Видалити виділені",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Додаткові дії",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Створити екземпляр в...") },
                        leadingIcon = { Icon(Icons.Default.AddBox, null) },
                        onClick = {
                            onMoreActions(GoalActionType.CreateInstance)
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Перемістити в...") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null) },
                        onClick = {
                            onMoreActions(GoalActionType.MoveInstance)
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Копіювати в...") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                        onClick = {
                            onMoreActions(GoalActionType.CopyGoal)
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}