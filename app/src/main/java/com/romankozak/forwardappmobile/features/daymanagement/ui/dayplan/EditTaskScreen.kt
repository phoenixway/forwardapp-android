package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceFrequency
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private data class EditTaskActions(
    val onTitleChange: (String) -> Unit,
    val onDescriptionChange: (String) -> Unit,
    val onPointsChange: (Int) -> Unit,
    val onPriorityChange: (TaskPriority) -> Unit,
    val onDurationChange: (Long?) -> Unit,
    val onRecurringChange: (Boolean) -> Unit,
    val onRecurrenceFrequencyChange: (RecurrenceFrequency) -> Unit,
    val onRecurrenceIntervalChange: (Int) -> Unit,
    val onRecurrenceDayOfWeekToggle: (DayOfWeek) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTaskScreen(
    viewModel: EditTaskViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is EditTaskUiEvent.NavigateUp -> onNavigateUp()
            }
        }
    }

    Scaffold(
        topBar = {
            EditTaskTopBar(
                onNavigateUp = onNavigateUp,
                onSave = viewModel::saveTask,
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        val actions =
            EditTaskActions(
                onTitleChange = viewModel::onTitleChange,
                onDescriptionChange = viewModel::onDescriptionChange,
                onPointsChange = viewModel::onPointsChange,
                onPriorityChange = viewModel::onPriorityChange,
                onDurationChange = viewModel::onDurationChange,
                onRecurringChange = viewModel::onRecurringChange,
                onRecurrenceFrequencyChange = viewModel::onRecurrenceFrequencyChange,
                onRecurrenceIntervalChange = viewModel::onRecurrenceIntervalChange,
                onRecurrenceDayOfWeekToggle = viewModel::onRecurrenceDayOfWeekToggle,
            )
        EditTaskContent(
            uiState = uiState,
            paddingValues = paddingValues,
            actions = actions,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTaskTopBar(
    onNavigateUp: () -> Unit,
    onSave: () -> Unit,
) {
    TopAppBar(
        title = { Text("Edit Task") },
        navigationIcon = {
            IconButton(onClick = onNavigateUp) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                )
            }
        },
        actions = {
            Button(onClick = onSave) {
                Text("Save")
            }
        },
    )
}

@Composable
private fun EditTaskContent(
    uiState: EditTaskUiState,
    paddingValues: PaddingValues,
    actions: EditTaskActions,
) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            EditTaskBasicFields(
                uiState = uiState,
                onTitleChange = actions.onTitleChange,
                onDescriptionChange = actions.onDescriptionChange,
            )
        }
        item {
            NumericInputCard(
                value = uiState.points.toString(),
                label = "Points",
                onValueChange = { actions.onPointsChange(it.toIntOrNull() ?: 0) },
            )
        }
        item {
            PriorityCard(
                selectedPriority = uiState.priority,
                onPriorityChange = actions.onPriorityChange,
            )
        }
        item {
            NumericInputCard(
                value = uiState.duration?.toString().orEmpty(),
                label = "Duration (minutes)",
                onValueChange = { actions.onDurationChange(it.toLongOrNull()) },
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    RecurrenceSection(
                        uiState = uiState,
                        onRecurringChange = actions.onRecurringChange,
                        onRecurrenceFrequencyChange = actions.onRecurrenceFrequencyChange,
                        onRecurrenceIntervalChange = actions.onRecurrenceIntervalChange,
                        onRecurrenceDayOfWeekToggle = actions.onRecurrenceDayOfWeekToggle,
                    )
                }
            }
        }
    }
}

@Composable
private fun EditTaskBasicFields(
    uiState: EditTaskUiState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = uiState.title,
            onValueChange = onTitleChange,
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = uiState.description,
            onValueChange = onDescriptionChange,
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 5,
        )
    }
}

@Composable
private fun NumericInputCard(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PriorityCard(
    selectedPriority: TaskPriority,
    onPriorityChange: (TaskPriority) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Priority", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TaskPriority.values().forEach { priority ->
                    FilterChip(
                        selected = selectedPriority == priority,
                        onClick = { onPriorityChange(priority) },
                        label = { Text(priority.name, softWrap = false, maxLines = 1) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecurrenceSection(
    uiState: EditTaskUiState,
    onRecurringChange: (Boolean) -> Unit,
    onRecurrenceFrequencyChange: (RecurrenceFrequency) -> Unit,
    onRecurrenceIntervalChange: (Int) -> Unit,
    onRecurrenceDayOfWeekToggle: (DayOfWeek) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Recurrence", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.weight(1f))
        Checkbox(
            checked = uiState.isRecurring,
            onCheckedChange = onRecurringChange,
        )
    }
    AnimatedVisibility(visible = uiState.isRecurring) {
        RecurrenceFields(
            uiState = uiState,
            onRecurrenceFrequencyChange = onRecurrenceFrequencyChange,
            onRecurrenceIntervalChange = onRecurrenceIntervalChange,
            onRecurrenceDayOfWeekToggle = onRecurrenceDayOfWeekToggle,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurrenceFields(
    uiState: EditTaskUiState,
    onRecurrenceFrequencyChange: (RecurrenceFrequency) -> Unit,
    onRecurrenceIntervalChange: (Int) -> Unit,
    onRecurrenceDayOfWeekToggle: (DayOfWeek) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            OutlinedTextField(
                value = uiState.recurrenceFrequency.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Frequency") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                RecurrenceFrequency.values().forEach { frequency ->
                    DropdownMenuItem(
                        text = { Text(frequency.name) },
                        onClick = {
                            onRecurrenceFrequencyChange(frequency)
                            expanded = false
                        },
                    )
                }
            }
        }

        OutlinedTextField(
            value = uiState.recurrenceInterval.toString(),
            onValueChange = { onRecurrenceIntervalChange(it.toIntOrNull() ?: 1) },
            label = { Text("Interval") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        if (uiState.recurrenceFrequency == RecurrenceFrequency.WEEKLY) {
            WeeklyRecurrenceDays(
                selectedDays = uiState.recurrenceDaysOfWeek,
                onRecurrenceDayOfWeekToggle = onRecurrenceDayOfWeekToggle,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeeklyRecurrenceDays(
    selectedDays: Set<DayOfWeek>,
    onRecurrenceDayOfWeekToggle: (DayOfWeek) -> Unit,
) {
    Spacer(modifier = Modifier.height(8.dp))
    Text("Days of week", style = MaterialTheme.typography.labelMedium)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DayOfWeek.values().forEach { day ->
            FilterChip(
                selected = selectedDays.contains(day),
                onClick = { onRecurrenceDayOfWeekToggle(day) },
                label = {
                    Text(day.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                },
            )
        }
    }
}
