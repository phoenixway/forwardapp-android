package com.romankozak.forwardappmobile.ui.screens.mainscreen.components

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.shared.features.projects.core.domain.model.Project
import com.romankozak.forwardappmobile.ui.screens.mainscreen.MainScreenContent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenUiState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.ProjectEditorState
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.MainScreenEvent
import com.romankozak.forwardappmobile.ui.screens.mainscreen.models.PlanningMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenScaffold(
    state: MainScreenUiState,
    onEvent: (MainScreenEvent) -> Unit,
) {
    val context = LocalContext.current
    val showPlaceholderToast = remember {
        {
            Toast
                .makeText(context, "Функція ще не готова, працюємо над нею", Toast.LENGTH_SHORT)
                .show()
        }
    }

    var isBottomNavExpanded by rememberSaveable { mutableStateOf(false) }
    var planningMode by rememberSaveable { mutableStateOf(PlanningMode.All) }

    Scaffold(
        topBar = {
            MainScreenTopAppBar(
                projectCount = state.projects.size,
                isLoading = state.isLoading,
                onPlaceholderAction = showPlaceholderToast,
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(MainScreenEvent.ShowCreateDialog()) },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Додати проєкт")
            }
        },
        bottomBar = {
            MainScreenBottomBar(
                isExpanded = isBottomNavExpanded,
                onExpandedChange = { isBottomNavExpanded = it },
                planningMode = planningMode,
                onPlanningModeChange = { planningMode = it },
                onPlaceholderAction = showPlaceholderToast,
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxWidth(),
        ) {
            MainScreenContent(
                state = state,
                onEvent = onEvent,
            )

            if (state.isActionInProgress) {
                LinearProgressIndicator(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth(),
                )
            }
        }
    }

    val parentProjectName =
        (state.activeDialog as? ProjectEditorState.Create)
            ?.parentId
            ?.let { parentId -> state.projects.firstOrNull { it.id == parentId }?.name }

    ProjectEditorDialog(
        dialogState = state.activeDialog,
        parentProjectName = parentProjectName,
        onDismiss = { onEvent(MainScreenEvent.HideDialog) },
        onConfirm = { name, description ->
            onEvent(MainScreenEvent.SubmitProject(name, description))
        },
    )

    DeleteProjectDialog(
        project = state.pendingDeletion,
        onDismiss = { onEvent(MainScreenEvent.CancelDeletion) },
        onConfirm = { onEvent(MainScreenEvent.ConfirmDeletion) },
    )
}

@Composable
private fun ProjectEditorDialog(
    dialogState: ProjectEditorState,
    parentProjectName: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    if (dialogState is ProjectEditorState.Hidden) return

    val (initialName, initialDescription) =
        when (dialogState) {
            is ProjectEditorState.Edit -> dialogState.project.name to (dialogState.project.description ?: "")
            is ProjectEditorState.Create -> "" to ""
            ProjectEditorState.Hidden -> "" to ""
        }

    var name by rememberSaveable(dialogState) { mutableStateOf(initialName) }
    var description by rememberSaveable(dialogState) { mutableStateOf(initialDescription) }

    val title =
        when (dialogState) {
            is ProjectEditorState.Edit -> "Редагувати проєкт"
            is ProjectEditorState.Create -> "Новий проєкт"
            ProjectEditorState.Hidden -> ""
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            androidx.compose.foundation.layout.Column {
                if (dialogState is ProjectEditorState.Create && parentProjectName != null) {
                    Text(
                        text = "Батьківський проєкт: $parentProjectName",
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Назва") },
                    singleLine = true,
                    supportingText = { Text("Обов'язкове поле") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Опис") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description) },
                enabled = name.isNotBlank(),
            ) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        },
    )
}

@Composable
private fun DeleteProjectDialog(
    project: Project?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (project == null) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Видалити проєкт?") },
        text = { Text("\"${project.name}\" та всі його дані буде видалено. Продовжити?") },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Видалити")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        },
    )
}
