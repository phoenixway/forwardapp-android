package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.tasklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayTask

private const val DIALOG_PADDING = 16
private const val CARD_CORNER_RADIUS = 20
private const val CARD_HORIZONTAL_PADDING = 12
private const val DESCRIPTION_MIN_HEIGHT = 80
private const val CONTENT_SPACING = 20
private const val PRIORITY_SPACING = 10
private const val PRIORITY_LABEL_BOTTOM_PADDING = 8
private const val PRIORITY_CHIP_CORNER_RADIUS = 14
private const val SELECTED_CHIP_BORDER_WIDTH = 2
private const val TITLE_CARD_ALPHA = 0.35f
private const val FIELD_CARD_ALPHA = 0.28f
private const val UNSELECTED_CHIP_ALPHA = 0.20f
private const val CONFIRM_ICON_SPACING = 6
private const val DELETE_ICON_SPACING = 4
private const val ACTIONS_SPACING = 12
private const val CONTENT_VERTICAL_PADDING = 8

private data class EditTaskDraft(
    val title: String,
    val description: String,
    val durationText: String,
    val priority: TaskPriority,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: DayTask,
    onDismissRequest: () -> Unit,
    onConfirm: (title: String, description: String, duration: Long?, priority: TaskPriority) -> Unit,
    onDelete: () -> Unit,
) {
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description ?: "") }
    var durationText by remember { mutableStateOf(task.estimatedDurationMinutes?.toString() ?: "") }
    var priority by remember { mutableStateOf(task.priority) }

    val draft =
        EditTaskDraft(
            title = title,
            description = description,
            durationText = durationText,
            priority = priority,
        )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(DIALOG_PADDING.dp),
        title = {
            Text(
                text = "Редагувати завдання",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        text = {
            EditTaskDialogContent(
                draft = draft,
                onTitleChange = { title = it },
                onDescriptionChange = { description = it },
                onDurationTextChange = { newValue ->
                    if (newValue.all(Char::isDigit) || newValue.isEmpty()) {
                        durationText = newValue
                    }
                },
                onPrioritySelected = { priority = it },
            )
        },
        confirmButton = {
            ConfirmEditTaskButton(
                draft = draft,
                onConfirm = onConfirm,
            )
        },
        dismissButton = {
            EditTaskDialogActions(
                onDelete = onDelete,
                onDismissRequest = onDismissRequest,
            )
        },
    )
}

@Composable
private fun EditTaskDialogContent(
    draft: EditTaskDraft,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDurationTextChange: (String) -> Unit,
    onPrioritySelected: (TaskPriority) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = CONTENT_VERTICAL_PADDING.dp),
        verticalArrangement = Arrangement.spacedBy(CONTENT_SPACING.dp),
    ) {
        TitleFieldCard(
            title = draft.title,
            onTitleChange = onTitleChange,
        )
        DescriptionFieldCard(
            description = draft.description,
            onDescriptionChange = onDescriptionChange,
        )
        DurationFieldCard(
            durationText = draft.durationText,
            onDurationTextChange = onDurationTextChange,
        )

        PrioritySelector(
            selectedPriority = draft.priority,
            onPrioritySelected = onPrioritySelected,
        )
    }
}

@Composable
private fun TitleFieldCard(
    title: String,
    onTitleChange: (String) -> Unit,
) {
    EditTaskFieldCard(containerAlpha = TITLE_CARD_ALPHA) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Назва") },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(CARD_HORIZONTAL_PADDING.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
            isError = title.isBlank(),
            supportingText = {
                if (title.isBlank()) {
                    Text("Обов'язкове поле")
                }
            },
        )
    }
}

@Composable
private fun DescriptionFieldCard(
    description: String,
    onDescriptionChange: (String) -> Unit,
) {
    EditTaskFieldCard(containerAlpha = FIELD_CARD_ALPHA) {
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Опис") },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(CARD_HORIZONTAL_PADDING.dp)
                    .heightIn(DESCRIPTION_MIN_HEIGHT.dp),
            maxLines = 5,
            leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
        )
    }
}

@Composable
private fun DurationFieldCard(
    durationText: String,
    onDurationTextChange: (String) -> Unit,
) {
    EditTaskFieldCard(containerAlpha = FIELD_CARD_ALPHA) {
        OutlinedTextField(
            value = durationText,
            onValueChange = onDurationTextChange,
            label = { Text("Тривалість") },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(CARD_HORIZONTAL_PADDING.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
            suffix = {
                if (durationText.isNotEmpty()) {
                    Text("хв")
                }
            },
        )
    }
}

@Composable
private fun EditTaskFieldCard(
    containerAlpha: Float,
    content: @Composable () -> Unit,
) {
    ElevatedCard(
        shape = RoundedCornerShape(CARD_CORNER_RADIUS.dp),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = containerAlpha),
            ),
    ) {
        content()
    }
}

@Composable
private fun PrioritySelector(
    selectedPriority: TaskPriority,
    onPrioritySelected: (TaskPriority) -> Unit,
) {
    Column {
        Text(
            "Пріоритет",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = PRIORITY_LABEL_BOTTOM_PADDING.dp),
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(PRIORITY_SPACING.dp),
            verticalArrangement = Arrangement.spacedBy(PRIORITY_SPACING.dp),
        ) {
            TaskPriority.values().forEach { priority ->
                PriorityChip(
                    priority = priority,
                    selected = priority == selectedPriority,
                    onClick = { onPrioritySelected(priority) },
                )
            }
        }
    }
}

@Composable
private fun PriorityChip(
    priority: TaskPriority,
    selected: Boolean,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(priority.getDisplayName()) },
        leadingIcon = {
            if (selected) {
                Icon(Icons.Default.Check, contentDescription = null)
            }
        },
        shape = RoundedCornerShape(PRIORITY_CHIP_CORNER_RADIUS.dp),
        colors =
            AssistChipDefaults.assistChipColors(
                containerColor =
                    if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = UNSELECTED_CHIP_ALPHA)
                    },
            ),
        border =
            if (selected) {
                BorderStroke(SELECTED_CHIP_BORDER_WIDTH.dp, MaterialTheme.colorScheme.primary)
            } else {
                null
            },
    )
}

@Composable
private fun ConfirmEditTaskButton(
    draft: EditTaskDraft,
    onConfirm: (title: String, description: String, duration: Long?, priority: TaskPriority) -> Unit,
) {
    FilledTonalButton(
        onClick = {
            onConfirm(
                draft.title,
                draft.description,
                draft.durationText.toLongOrNull(),
                draft.priority,
            )
        },
        enabled = draft.title.isNotBlank(),
    ) {
        Icon(Icons.Default.Done, contentDescription = null)
        Spacer(Modifier.width(CONFIRM_ICON_SPACING.dp))
        Text("Зберегти")
    }
}

@Composable
private fun EditTaskDialogActions(
    onDelete: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(ACTIONS_SPACING.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onDelete,
            colors =
                ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
        ) {
            Icon(Icons.Default.Delete, contentDescription = null)
            Spacer(Modifier.width(DELETE_ICON_SPACING.dp))
            Text("Видалити")
        }

        OutlinedButton(onClick = onDismissRequest) {
            Text("Скасувати")
        }
    }
}
