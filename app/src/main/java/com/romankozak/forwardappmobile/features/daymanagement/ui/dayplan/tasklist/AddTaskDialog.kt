package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.tasklist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.google.accompanist.flowlayout.FlowRow
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceRule
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.components.AdvancedRecurrencePickerDialog

private const val PRIORITY_LOW_COLOR_HEX = 0xFF4CAF50
private const val PRIORITY_MEDIUM_COLOR_HEX = 0xFF2196F3
private const val PRIORITY_HIGH_COLOR_HEX = 0xFFFF9800
private const val PRIORITY_CRITICAL_COLOR_HEX = 0xFFF44336
private val PRIORITY_LOW_COLOR = Color(PRIORITY_LOW_COLOR_HEX)
private val PRIORITY_MEDIUM_COLOR = Color(PRIORITY_MEDIUM_COLOR_HEX)
private val PRIORITY_HIGH_COLOR = Color(PRIORITY_HIGH_COLOR_HEX)
private val PRIORITY_CRITICAL_COLOR = Color(PRIORITY_CRITICAL_COLOR_HEX)
private val PRIORITY_NONE_COLOR = Color.Gray
private const val DIALOG_HORIZONTAL_PADDING = 24
private const val HEADER_ICON_SIZE = 48
private const val HEADER_ICON_CORNER_RADIUS = 12
private const val HEADER_SYMBOL_SIZE = 26
private const val HEADER_SPACING = 16
private const val CONTENT_TOP_PADDING = 20
private const val CONTENT_SPACING = 18
private const val FIELD_LABEL_SPACING = 6
private const val ROW_FIELD_SPACING = 16
private const val SURFACE_CORNER_RADIUS = 12
private const val FIELD_MIN_HEIGHT = 100
private const val RECURRENCE_ICON_SIZE = 20
private const val PRIORITY_CHIP_SPACING = 10
private const val PRIORITY_CHECK_ICON_SIZE = 18
private const val SELECTED_CHIP_ALPHA = 0.15f
private const val BORDER_ALPHA = 0.3f
private const val RECURRENCE_CHEVRON_ALPHA = 0.4f
private const val LABEL_ALPHA = 0.7f
private const val ACTION_BUTTON_HEIGHT = 48
private const val ACTION_HORIZONTAL_PADDING = 24
private const val ACTION_BUTTON_SPACING = 12
private const val CANCEL_BORDER_ALPHA = 0.5f
private const val CANCEL_BORDER_WIDTH = 1.5f
private val PRIORITY_BORDER_WIDTH = 1.5.dp
private const val ACTION_FONT_SIZE = 15
private const val ADD_ICON_SPACING = 8

private data class AddTaskDraft(
    val title: String,
    val description: String,
    val durationText: String,
    val pointsText: String,
    val priority: TaskPriority,
    val recurrenceRule: RecurrenceRule?,
)

private data class AddTaskFormActions(
    val onTitleChange: (String) -> Unit,
    val onDescriptionChange: (String) -> Unit,
    val onDurationChange: (String) -> Unit,
    val onPointsChange: (String) -> Unit,
    val onOpenRecurrencePicker: () -> Unit,
    val onPrioritySelected: (TaskPriority) -> Unit,
)

private data class TextFieldConfig(
    val label: String,
    val value: String,
    val placeholder: String,
    val onValueChange: (String) -> Unit,
    val singleLine: Boolean = true,
    val minHeight: Int? = null,
    val maxLines: Int = 1,
    val isError: Boolean = false,
    val supportingText: String? = null,
)

private data class NumericFieldConfig(
    val label: String,
    val value: String,
    val placeholder: String,
    val tint: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val suffixText: String? = null,
    val onValueChange: (String) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        duration: Long?,
        priority: TaskPriority,
        recurrenceRule: RecurrenceRule?,
        points: Int,
    ) -> Unit,
    initialPriority: TaskPriority = TaskPriority.MEDIUM,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("") }
    var pointsText by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(initialPriority) }
    var recurrenceRule by remember { mutableStateOf<RecurrenceRule?>(null) }
    var showRecurrencePicker by remember { mutableStateOf(false) }

    val draft =
        AddTaskDraft(
            title = title,
            description = description,
            durationText = durationText,
            pointsText = pointsText,
            priority = priority,
            recurrenceRule = recurrenceRule,
        )

    if (showRecurrencePicker) {
        AdvancedRecurrencePickerDialog(
            onDismiss = { showRecurrencePicker = false },
            onConfirm = { rule ->
                recurrenceRule = rule
                showRecurrencePicker = false
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(DIALOG_HORIZONTAL_PADDING.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = { AddTaskDialogHeader() },
        text = {
            val actions =
                AddTaskFormActions(
                    onTitleChange = { title = it },
                    onDescriptionChange = { description = it },
                    onDurationChange = { durationText = it },
                    onPointsChange = { pointsText = it },
                    onOpenRecurrencePicker = { showRecurrencePicker = true },
                    onPrioritySelected = { priority = it },
                )
            AddTaskDialogContent(
                draft = draft,
                actions = actions,
            )
        },
        confirmButton = {
            AddTaskDialogButtons(
                draft = draft,
                onDismissRequest = onDismissRequest,
                onConfirm = onConfirm,
            )
        },
        dismissButton = {},
    )
}

@Composable
private fun AddTaskDialogHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Box(
            modifier =
                Modifier
                    .size(HEADER_ICON_SIZE.dp)
                    .clip(RoundedCornerShape(HEADER_ICON_CORNER_RADIUS.dp))
                    .background(
                        brush =
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary,
                                    ),
                            ),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(HEADER_SYMBOL_SIZE.dp),
            )
        }
        Spacer(Modifier.width(HEADER_SPACING.dp))
        Text(
            text = "Нове завдання",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AddTaskDialogContent(
    draft: AddTaskDraft,
    actions: AddTaskFormActions,
) {
    Column(
        modifier =
            Modifier
                .padding(top = CONTENT_TOP_PADDING.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(CONTENT_SPACING.dp),
    ) {
        AddTaskTextSection(
            config =
                TextFieldConfig(
                    label = "Назва",
                    value = draft.title,
                    placeholder = "Введіть назву завдання",
                    onValueChange = actions.onTitleChange,
                    isError = draft.title.isBlank(),
                    supportingText = if (draft.title.isBlank()) "Обов'язкове поле" else null,
                ),
        )
        AddTaskTextSection(
            config =
                TextFieldConfig(
                    label = "Опис",
                    value = draft.description,
                    placeholder = "Додаткова інформація (необов'язково)",
                    onValueChange = actions.onDescriptionChange,
                    singleLine = false,
                    minHeight = FIELD_MIN_HEIGHT,
                    maxLines = 4,
                ),
        )
        AddTaskNumericSection(
            pointsText = draft.pointsText,
            durationText = draft.durationText,
            onPointsChange = actions.onPointsChange,
            onDurationChange = actions.onDurationChange,
        )
        AddTaskRecurrenceSection(
            recurrenceRule = draft.recurrenceRule,
            onClick = actions.onOpenRecurrencePicker,
        )
        AddTaskPrioritySection(
            selectedPriority = draft.priority,
            onPrioritySelected = actions.onPrioritySelected,
        )
    }
}

@Composable
private fun AddTaskTextSection(
    config: TextFieldConfig,
) {
    Column(verticalArrangement = Arrangement.spacedBy(FIELD_LABEL_SPACING.dp)) {
        Text(
            text = config.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = LABEL_ALPHA),
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value = config.value,
            onValueChange = config.onValueChange,
            placeholder = { Text(config.placeholder) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .let { base ->
                        config.minHeight?.let { minHeight ->
                            base.heightIn(min = minHeight.dp)
                        } ?: base
                    },
            singleLine = config.singleLine,
            maxLines = config.maxLines,
            shape = RoundedCornerShape(SURFACE_CORNER_RADIUS.dp),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = BORDER_ALPHA),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
            isError = config.isError,
            supportingText = {
                if (config.supportingText != null) {
                    Text(config.supportingText, color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }
}

@Composable
private fun AddTaskNumericSection(
    pointsText: String,
    durationText: String,
    onPointsChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(ROW_FIELD_SPACING.dp)) {
        NumericField(
            config =
                NumericFieldConfig(
                    label = "Бали",
                    value = pointsText,
                    placeholder = "0",
                    tint = MaterialTheme.colorScheme.primary,
                    icon = Icons.Default.Star,
                    onValueChange = onPointsChange,
                ),
            modifier = Modifier.weight(1f),
        )
        NumericField(
            config =
                NumericFieldConfig(
                    label = "Тривалість",
                    value = durationText,
                    placeholder = "0",
                    tint = MaterialTheme.colorScheme.tertiary,
                    icon = Icons.Default.AccessTime,
                    suffixText = "хв",
                    onValueChange = onDurationChange,
                ),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun NumericField(
    config: NumericFieldConfig,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(FIELD_LABEL_SPACING.dp),
    ) {
        Text(
            text = config.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = LABEL_ALPHA),
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value = config.value,
            onValueChange = {
                if (it.all(Char::isDigit) || it.isEmpty()) {
                    config.onValueChange(it)
                }
            },
            placeholder = { Text(config.placeholder) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(SURFACE_CORNER_RADIUS.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            leadingIcon = {
                Icon(
                    imageVector = config.icon,
                    contentDescription = null,
                    tint = config.tint,
                )
            },
            suffix = {
                if (config.suffixText != null && config.value.isNotBlank()) {
                    Text(config.suffixText)
                }
            },
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = BORDER_ALPHA),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
        )
    }
}

@Composable
private fun AddTaskRecurrenceSection(
    recurrenceRule: RecurrenceRule?,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(FIELD_LABEL_SPACING.dp)) {
        Text(
            text = "Повторення",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = LABEL_ALPHA),
            fontWeight = FontWeight.SemiBold,
        )
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            shape = RoundedCornerShape(SURFACE_CORNER_RADIUS.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = BORDER_ALPHA)),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(HEADER_ICON_CORNER_RADIUS.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(RECURRENCE_ICON_SIZE.dp),
                    )
                    Text(
                        text = recurrenceRule?.let { it.frequency.name } ?: "Не повторюється",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = RECURRENCE_CHEVRON_ALPHA),
                )
            }
        }
    }
}

@Composable
private fun AddTaskPrioritySection(
    selectedPriority: TaskPriority,
    onPrioritySelected: (TaskPriority) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Пріоритет",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = LABEL_ALPHA),
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(
            mainAxisSpacing = PRIORITY_CHIP_SPACING.dp,
            crossAxisSpacing = PRIORITY_CHIP_SPACING.dp,
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
    val chipColor =
        when (priority) {
            TaskPriority.LOW -> PRIORITY_LOW_COLOR
            TaskPriority.MEDIUM -> PRIORITY_MEDIUM_COLOR
            TaskPriority.HIGH -> PRIORITY_HIGH_COLOR
            TaskPriority.CRITICAL -> PRIORITY_CRITICAL_COLOR
            TaskPriority.NONE -> PRIORITY_NONE_COLOR
        }
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = priority.name,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        },
        leadingIcon =
            if (selected) {
                {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(PRIORITY_CHECK_ICON_SIZE.dp),
                    )
                }
            } else {
                null
            },
        colors =
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = chipColor.copy(alpha = SELECTED_CHIP_ALPHA),
                selectedLabelColor = chipColor,
                selectedLeadingIconColor = chipColor,
            ),
        border =
            if (selected) {
                FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = true,
                    borderColor = chipColor,
                    selectedBorderColor = chipColor,
                    borderWidth = PRIORITY_BORDER_WIDTH,
                    selectedBorderWidth = PRIORITY_BORDER_WIDTH,
                )
            } else {
                FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = false,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = BORDER_ALPHA),
                )
            },
    )
}

@Composable
private fun AddTaskDialogButtons(
    draft: AddTaskDraft,
    onDismissRequest: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        duration: Long?,
        priority: TaskPriority,
        recurrenceRule: RecurrenceRule?,
        points: Int,
    ) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        OutlinedButton(
            onClick = onDismissRequest,
            shape = RoundedCornerShape(SURFACE_CORNER_RADIUS.dp),
            border =
                BorderStroke(
                    CANCEL_BORDER_WIDTH.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = CANCEL_BORDER_ALPHA),
                ),
            modifier = Modifier.height(ACTION_BUTTON_HEIGHT.dp),
            contentPadding = PaddingValues(horizontal = ACTION_HORIZONTAL_PADDING.dp),
        ) {
            Text(
                text = "Скасувати",
                fontWeight = FontWeight.Medium,
                fontSize = ACTION_FONT_SIZE.sp,
            )
        }
        Spacer(Modifier.width(ACTION_BUTTON_SPACING.dp))
        Button(
            onClick = {
                onConfirm(
                    draft.title,
                    draft.description,
                    draft.durationText.toLongOrNull(),
                    draft.priority,
                    draft.recurrenceRule,
                    draft.pointsText.toIntOrNull() ?: 0,
                )
            },
            enabled = draft.title.isNotBlank(),
            shape = RoundedCornerShape(SURFACE_CORNER_RADIUS.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.height(ACTION_BUTTON_HEIGHT.dp),
            contentPadding = PaddingValues(horizontal = ACTION_HORIZONTAL_PADDING.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(RECURRENCE_ICON_SIZE.dp),
            )
            Spacer(Modifier.width(ADD_ICON_SPACING.dp))
            Text(
                text = "Додати",
                fontWeight = FontWeight.SemiBold,
                fontSize = ACTION_FONT_SIZE.sp,
            )
        }
    }
}
