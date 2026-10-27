package com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.RecurrenceFrequency
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskExecutionStrictness
import com.romankozak.forwardappmobile.core.navigation.routes.NavigationRoutes
import com.romankozak.forwardappmobile.features.daymanagement.taskexecution.domain.TaskExecutionTimingCalculator
import com.romankozak.forwardappmobile.features.daymanagement.taskexecution.domain.TaskExecutionTimingRequest
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.tasklist.TaskExecutionPolicyEditor
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

// ─── Actions / tab model ──────────────────────────────────────────────────────

private data class EditTaskActions(
    val onTitleChange: (String) -> Unit,
    val onDescriptionChange: (String) -> Unit,
    val onNotesChange: (String) -> Unit,
    val onPointsChange: (Int) -> Unit,
    val onPriorityChange: (TaskPriority) -> Unit,
    val onDurationChange: (Long?) -> Unit,
    val onScheduledTimeChange: (Long?) -> Unit,
    val onDueTimeChange: (Long?) -> Unit,
    val onExecutionStrictnessChange: (TaskExecutionStrictness) -> Unit,
    val onRecurringChange: (Boolean) -> Unit,
    val onRecurrenceFrequencyChange: (RecurrenceFrequency) -> Unit,
    val onRecurrenceIntervalChange: (Int) -> Unit,
    val onRecurrenceDayOfWeekToggle: (DayOfWeek) -> Unit,
    val onAddContextLink: () -> Unit,
    val onOpenContextLink: (String) -> Unit,
    val onRemoveContextLink: (String) -> Unit,
)

// ─── Full-screen edit ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTaskScreen(
    viewModel: EditTaskViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
    navController: NavController? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val requestClose = rememberTaskNotesCloseRequest(uiState, viewModel, onNavigateUp)
    var selectedTab by rememberSaveable { mutableStateOf(EditTaskTab.Main) }
    var isContextChooserOpen by remember { mutableStateOf(false) }
    EditTaskContextChooserResultEffect(
        navController = navController,
        viewModel = viewModel,
        onResultHandled = { isContextChooserOpen = false },
    )
    EditTaskContextChooserRouteEffect(
        navController = navController,
        onChooserClosed = { isContextChooserOpen = false },
    )

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is EditTaskUiEvent.NavigateUp -> if (viewModel.uiState.value.task?.id == event.taskId) { viewModel.reset(); onNavigateUp() }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Завдання дня",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = requestClose) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    FilledTonalIconButton(
                        onClick = viewModel::saveTask,
                        enabled = uiState.task != null && !uiState.isSaving && !uiState.isLoadingDetails && uiState.loadError == null,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(48.dp),
                    ) {
                        if (uiState.isSaving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(
                            Icons.Default.Save,
                            contentDescription = "Зберегти",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        val actions =
            rememberActions(
                viewModel = viewModel,
                navController = navController,
                uiState = uiState,
                onContextChooserOpened = { isContextChooserOpen = true },
            )
        EditTaskBody(
            uiState = uiState,
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
            actions = actions,
            onRetryLoad = viewModel::retryTaskLoad,
        )
    }
}


// ─── Full-screen body ─────────────────────────────────────────────────────────

@Composable
private fun EditTaskBody(
    uiState: EditTaskUiState,
    selectedTab: EditTaskTab,
    onTabSelected: (EditTaskTab) -> Unit,
    modifier: Modifier = Modifier,
    actions: EditTaskActions,
    onRetryLoad: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TaskPropertiesTabs(selectedTab = selectedTab, onTabSelected = onTabSelected)
        if ((uiState.task == null || uiState.isLoadingDetails) && uiState.loadError == null) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (selectedTab != EditTaskTab.Notes) uiState.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            uiState.loadError?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetryLoad) { Text("Повторити завантаження") }
            }
            when (selectedTab) {
                EditTaskTab.Main -> {
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = actions.onTitleChange,
                        enabled = uiState.task != null,
                        label = { Text("Назва завдання") },
                        textStyle = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 1,
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp),
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Параметри завдання", style = MaterialTheme.typography.titleSmall)
                            SummaryChips(uiState = uiState)
                            TextButton(onClick = { onTabSelected(EditTaskTab.Details) }) {
                                Text("Налаштувати час і пріоритет")
                            }
                        }
                    }
                }
                EditTaskTab.Details -> if (!uiState.isLoadingDetails) DetailsContent(uiState = uiState, actions = actions)
                EditTaskTab.Repeat -> if (!uiState.isLoadingDetails) RepeatContent(uiState = uiState, actions = actions)
                EditTaskTab.Notes -> TaskNotesTab(uiState, actions.onNotesChange, actions.onDescriptionChange)
                EditTaskTab.Realizes -> TaskRealizesTab(uiState.task, uiState.contextLinks.map { it.id to it.name }, actions.onRemoveContextLink, actions.onOpenContextLink)
            }
        }
    }
}

// ─── Summary chips ────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryChips(
    uiState: EditTaskUiState,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val priorityColor = when (uiState.priority) {
        TaskPriority.LOW      -> cs.secondary
        TaskPriority.MEDIUM   -> cs.tertiary
        TaskPriority.HIGH     -> cs.primary
        TaskPriority.CRITICAL -> cs.error
        TaskPriority.NONE     -> cs.onSurfaceVariant
    }
    val priorityBg = when (uiState.priority) {
        TaskPriority.LOW      -> cs.secondaryContainer.copy(alpha = 0.45f)
        TaskPriority.MEDIUM   -> cs.tertiaryContainer.copy(alpha = 0.45f)
        TaskPriority.HIGH     -> cs.primaryContainer.copy(alpha = 0.45f)
        TaskPriority.CRITICAL -> cs.errorContainer.copy(alpha = 0.45f)
        TaskPriority.NONE     -> Color.Transparent
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CompactChip(
            label = uiState.priority.taskPropertiesLabel(),
            textColor = priorityColor,
            borderColor = priorityColor,
            bg = priorityBg,
        )
        CompactChip(label = "Бали: ${uiState.points}")
        uiState.duration?.let {
            CompactChip(
                label = "$it хв",
                textColor = cs.secondary,
                borderColor = cs.secondary,
                bg = cs.secondaryContainer.copy(alpha = 0.45f),
            )
        }
        if (uiState.isRecurring) {
            CompactChip(label = "Повторюється")
        }
    }
}

@Composable
private fun CompactChip(
    label: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    bg: Color = Color.Transparent,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .then(if (bg != Color.Transparent) Modifier.background(bg) else Modifier)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContextLinksSection(
    contextLinks: List<TaskContextLinkUi>,
    onAddContextLink: () -> Unit,
    onOpenContextLink: (String) -> Unit,
    onRemoveContextLink: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Контексти", modifier = Modifier.weight(1f))
            OutlinedIconButton(
                onClick = onAddContextLink,
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Додати контекст",
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        if (contextLinks.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cs.surfaceContainerHigh)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = cs.onSurfaceVariant,
                )
                Text(
                    text = "Немає зв'язаних контекстів",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
            return
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            contextLinks.forEach { context ->
                Row(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onOpenContextLink(context.id) }
                            .background(cs.secondaryContainer.copy(alpha = 0.55f))
                            .padding(start = 9.dp, top = 5.dp, end = 3.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = cs.onSecondaryContainer,
                    )
                    Box(modifier = Modifier.width(140.dp)) {
                        Text(
                            text = context.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium,
                            color = cs.onSecondaryContainer,
                        )
                    }
                    IconButton(
                        onClick = { onRemoveContextLink(context.id) },
                        modifier = Modifier.size(22.dp),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Прибрати контекст",
                            modifier = Modifier.size(14.dp),
                            tint = cs.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}

// ─── Details tab ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailsContent(
    uiState: EditTaskUiState,
    actions: EditTaskActions,
) {
    val cs = MaterialTheme.colorScheme
    val timingResolution =
        remember(uiState.scheduledTime, uiState.dueTime, uiState.duration) {
            TaskExecutionTimingCalculator().resolve(
                TaskExecutionTimingRequest(
                    scheduledTime = uiState.scheduledTime,
                    dueTime = uiState.dueTime,
                    durationMinutes = uiState.duration,
                ),
            )
        }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("Пріоритет")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TaskPriority.values().forEach { priority ->
                FilterChip(
                    selected = uiState.priority == priority,
                    onClick = { actions.onPriorityChange(priority) },
                    label = {
                        Text(priority.taskPropertiesLabel(), softWrap = false, maxLines = 1)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = cs.primary,
                        selectedLabelColor = cs.onPrimary,
                        containerColor = cs.surfaceContainerHigh,
                        labelColor = cs.onSurfaceVariant,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = cs.outlineVariant,
                        selectedBorderColor = cs.primary,
                        enabled = true,
                        selected = uiState.priority == priority,
                    ),
                )
            }
        }

        HorizontalDivider(color = cs.outlineVariant)

        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Бали", modifier = Modifier.weight(1f))
            Text(
                text = uiState.points.toString(),
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = cs.onSurface,
                    fontFamily = FontFamily.Serif,
                    fontSize = 30.sp,
                ),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                SmallStepButton("+") { actions.onPointsChange(uiState.points + 1) }
                SmallStepButton("−") { actions.onPointsChange((uiState.points - 1).coerceAtLeast(0)) }
            }
        }

        HorizontalDivider(color = cs.outlineVariant)

        TaskExecutionPolicyEditor(
            dayAnchorTime = uiState.dayAnchorTime,
            durationMinutes = uiState.duration,
            scheduledTime = uiState.scheduledTime,
            dueTime = uiState.dueTime,
            resolvedScheduledTime = timingResolution.scheduledTime,
            resolvedDueTime = timingResolution.dueTime,
            strictness = uiState.executionStrictness,
            onDurationChange = actions.onDurationChange,
            onScheduledTimeChange = actions.onScheduledTimeChange,
            onDueTimeChange = actions.onDueTimeChange,
            onStrictnessChange = actions.onExecutionStrictnessChange,
        )
    }
}

// ─── Repeat tab ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RepeatContent(
    uiState: EditTaskUiState,
    actions: EditTaskActions,
) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Повторювана",
                fontSize = 13.sp,
                color = cs.onSurface,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = uiState.isRecurring,
                onCheckedChange = actions.onRecurringChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = cs.onPrimary,
                    checkedTrackColor = cs.primary,
                    uncheckedThumbColor = cs.surface,
                    uncheckedTrackColor = cs.surfaceVariant,
                    uncheckedBorderColor = cs.outlineVariant,
                ),
            )
        }

        AnimatedVisibility(visible = uiState.isRecurring) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                ) {
                    OutlinedTextField(
                        value = uiState.recurrenceFrequency.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Частота", fontSize = 11.sp) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        colors = outlinedFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        RecurrenceFrequency.values()
                            .filter { frequency -> frequency != RecurrenceFrequency.HOURLY }
                            .forEach { frequency ->
                                DropdownMenuItem(
                                    text = { Text(frequency.name, fontSize = 13.sp) },
                                    onClick = {
                                        actions.onRecurrenceFrequencyChange(frequency)
                                        expanded = false
                                    },
                                )
                            }
                    }
                }

                OutlinedTextField(
                    value = uiState.recurrenceInterval.toString(),
                    onValueChange = { actions.onRecurrenceIntervalChange(it.toIntOrNull() ?: 1) },
                    label = { Text("Інтервал", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = outlinedFieldColors(),
                )

                if (uiState.recurrenceFrequency == RecurrenceFrequency.WEEKLY) {
                    SectionLabel("Дні тижня")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        DayOfWeek.values().forEach { day ->
                            FilterChip(
                                selected = uiState.recurrenceDaysOfWeek.contains(day),
                                onClick = { actions.onRecurrenceDayOfWeekToggle(day) },
                                label = {
                                    Text(
                                        day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = cs.primary,
                                    selectedLabelColor = cs.onPrimary,
                                    containerColor = cs.surfaceContainerHigh,
                                    labelColor = cs.onSurfaceVariant,
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = cs.outlineVariant,
                                    selectedBorderColor = cs.primary,
                                    enabled = true,
                                    selected = uiState.recurrenceDaysOfWeek.contains(day),
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        fontSize = 9.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 0.07.sp,
        modifier = modifier,
    )
}

@Composable
private fun SmallStepButton(label: String, onClick: () -> Unit) {
    OutlinedIconButton(
        onClick = onClick,
        modifier = Modifier.size(width = 26.dp, height = 20.dp),
        shape = RoundedCornerShape(3.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
)

@Composable
private fun EditTaskContextChooserResultEffect(
    navController: NavController?,
    viewModel: EditTaskViewModel,
    onResultHandled: () -> Unit,
) {
    LaunchedEffect(navController) {
        val savedStateHandle = navController?.currentBackStackEntry?.savedStateHandle ?: return@LaunchedEffect
        savedStateHandle
            .getStateFlow<String?>("list_chooser_result", null)
            .collect { result ->
                if (result != null) {
                    savedStateHandle["list_chooser_result"] = null
                    viewModel.onContextChooserResult(result)
                    onResultHandled()
                }
            }
    }
}

@Composable
private fun EditTaskContextChooserRouteEffect(
    navController: NavController?,
    onChooserClosed: () -> Unit,
) {
    LaunchedEffect(navController) {
        navController?.currentBackStackEntryFlow?.collect { entry ->
            val route = entry.destination.route.orEmpty()
            if (!route.startsWith(NavigationRoutes.LIST_CHOOSER)) {
                onChooserClosed()
            }
        }
    }
}

@Composable
private fun rememberActions(
    viewModel: EditTaskViewModel,
    navController: NavController?,
    uiState: EditTaskUiState,
    onContextChooserOpened: () -> Unit = {},
) = EditTaskActions(
    onTitleChange = viewModel::onTitleChange,
    onDescriptionChange = viewModel::onDescriptionChange,
    onNotesChange = viewModel::onNotesChange,
    onPointsChange = viewModel::onPointsChange,
    onPriorityChange = viewModel::onPriorityChange,
    onDurationChange = viewModel::onDurationChange,
    onScheduledTimeChange = viewModel::onScheduledTimeChange,
    onDueTimeChange = viewModel::onDueTimeChange,
    onExecutionStrictnessChange = viewModel::onExecutionStrictnessChange,
    onRecurringChange = viewModel::onRecurringChange,
    onRecurrenceFrequencyChange = viewModel::onRecurrenceFrequencyChange,
    onRecurrenceIntervalChange = viewModel::onRecurrenceIntervalChange,
    onRecurrenceDayOfWeekToggle = viewModel::onRecurrenceDayOfWeekToggle,
    onAddContextLink = {
        navController?.let { controller ->
            onContextChooserOpened()
            controller.navigate(
                NavigationRoutes.listChooser(
                    title = "Вибрати контекст",
                    disabledIds = uiState.contextLinks.joinToString(",") { it.id }.ifBlank { null },
                ),
            )
        }
    },
    onOpenContextLink = { contextId ->
        navController?.navigate(NavigationRoutes.contextDetail(contextId = contextId))
    },
    onRemoveContextLink = viewModel::removeContextLink,
)
