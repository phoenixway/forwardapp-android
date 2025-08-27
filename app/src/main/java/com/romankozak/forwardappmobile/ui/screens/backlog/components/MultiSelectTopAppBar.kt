package com.romankozak.forwardappmobile.ui.screens.backlog.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.romankozak.forwardappmobile.ui.screens.backlog.GoalActionType

@OptIn(ExperimentalMaterial3Api::class)
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

    TopAppBar(
        title = { Text("$selectedCount виділено") },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = "Закрити режим виділення")
            }
        },
        actions = {
            IconButton(onClick = onSelectAll, enabled = !areAllSelected) {
                Icon(Icons.Default.SelectAll, contentDescription = "Вибрати все")
            }
            IconButton(onClick = onToggleComplete) {
                Icon(Icons.Default.DoneAll, contentDescription = "Відмітити виконаними/невиконаними")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Видалити виділені")
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Додаткові дії")
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
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}