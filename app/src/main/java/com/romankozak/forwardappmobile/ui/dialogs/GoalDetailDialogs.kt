package com.romankozak.forwardappmobile.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.ui.screens.backlog.GoalActionType
import com.romankozak.forwardappmobile.ui.screens.backlog.components.inputpanel.InputMode

@Composable
fun GoalActionChoiceDialog(onDismiss: () -> Unit, onActionSelected: (GoalActionType) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column {
                Text("Create instance in another list", modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onActionSelected(GoalActionType.CreateInstance) }
                    .padding(16.dp))
                HorizontalDivider()
                Text("Move instance to another list", modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onActionSelected(GoalActionType.MoveInstance) }
                    .padding(16.dp))
                HorizontalDivider()
                Text("Copy goal to another list", modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onActionSelected(GoalActionType.CopyGoal) }
                    .padding(16.dp))
/*                HorizontalDivider()
                Text("Перемістити на вершину списку", modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onActionSelected(GoalActionType.MoveToTop) }
                    .padding(16.dp))*/
            }
        }
    }
}

@Composable
fun ListChooserDialog(
    topLevelLists: List<GoalList>,
    childMap: Map<String, List<GoalList>>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column {
                Text("Choose a list", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    fun renderList(list: GoalList, level: Int) {
                        item(key = list.id) {
                            Text(
                                text = list.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onConfirm(list.id) }
                                    .padding(start = (level * 24 + 16).dp, top = 8.dp, bottom = 8.dp, end = 16.dp)
                            )
                        }
                        childMap[list.id]?.forEach { child ->
                            renderList(child, level + 1)
                        }
                    }
                    topLevelLists.forEach { renderList(it, 0) }
                }
            }
        }
    }
}

@Composable
fun InputModeDialog(onDismiss: () -> Unit, onSelect: (InputMode) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column {
                // Option to Add Goal
                Text(
                    text = "Add Goal",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(InputMode.AddGoal) }
                        .padding(16.dp)
                )
                HorizontalDivider()

                 // Option to Search in the current list
                Text(
                    text = "Search in List",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(InputMode.SearchInList) }
                        .padding(16.dp)
                )

                // ✨ REMOVED: The "Search Globally" option which caused the error
            }
        }
    }
}