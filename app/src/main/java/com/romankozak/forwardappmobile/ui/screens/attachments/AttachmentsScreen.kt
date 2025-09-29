package com.romankozak.forwardappmobile.ui.screens.attachments

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.accompanist.flowlayout.FlowRow
import com.romankozak.forwardappmobile.data.database.models.ListItemContent
import com.romankozak.forwardappmobile.data.database.models.LinkType
import com.romankozak.forwardappmobile.data.database.models.ScoringStatus
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private object Scales {
    val effort = listOf(0f, 1f, 2f, 3f, 5f, 8f, 13f, 21f)
    val importance = (1..12).map { it.toFloat() }
    val impact = listOf(1f, 2f, 3f, 5f, 8f, 13f)
    val cost = (0..5).map { it.toFloat() }
    val risk = listOf(0f, 1f, 2f, 3f, 5f, 8f, 13f, 21f)
    val weights = (0..20).map { it * 0.1f }
    val costLabels = listOf("немає", "дуже низькі", "низькі", "середні", "високі", "дуже високі")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentsScreen(
    navController: NavController,
    viewModel: AttachmentsViewModel = hiltViewModel()
) {
    val attachments by viewModel.attachments.collectAsState()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddAttachmentDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var currentTagInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()



    if (showAddAttachmentDialog) {
        AddAttachmentDialog(
            onDismiss = { showAddAttachmentDialog = false },
            onAttachmentTypeSelected = {
                viewModel.onAddAttachment(it)
                showAddAttachmentDialog = false
            }
        )
    }

    when (uiState.showAddAttachmentDialog) {
        AddAttachmentDialogType.WEB_LINK -> {
            AddWebLinkDialog(
                onDismiss = { viewModel.onDismissAddAttachmentDialog() },
                onConfirm = { url, name -> viewModel.onAddWebLink(url, name) }
            )
        }
        AddAttachmentDialogType.OBSIDIAN_LINK -> {
            AddObsidianLinkDialog(
                onDismiss = { viewModel.onDismissAddAttachmentDialog() },
                onConfirm = { url, name -> viewModel.onAddObsidianLink(url, name) }
            )
        }
        AddAttachmentDialogType.NONE -> {}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.project?.name ?: "Редагування проекту") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            scope.launch {
                                val savedProject = viewModel.onSave()
                                if (savedProject != null) {
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set("needs_refresh", true)
                                    navController.popBackStack()
                                }
                            }
                        },
                        enabled = uiState.project != null && uiState.name.isNotBlank(),
                    ) {
                        Text("Зберегти")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddAttachmentDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add attachment")
            }
        }
    ) { paddingValues ->
        if (uiState.project == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = { viewModel.onNameChange(it) },
                        label = { Text("Назва проекту") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Теги", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = currentTagInput,
                                onValueChange = { currentTagInput = it },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions =
                                KeyboardOptions.Default.copy(
                                    capitalization = KeyboardCapitalization.None,
                                    imeAction = ImeAction.Done,
                                ),
                                keyboardActions =
                                KeyboardActions(onDone = {
                                    if (currentTagInput.isNotBlank()) {
                                        viewModel.onTagsChange(uiState.tags + currentTagInput.trim())
                                        currentTagInput = ""
                                    }
                                }),
                                label = { Text("Новий тег") },
                            )
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (currentTagInput.isNotBlank()) {
                                        viewModel.onTagsChange(uiState.tags + currentTagInput.trim())
                                        currentTagInput = ""
                                    }
                                },
                                enabled = currentTagInput.isNotBlank(),
                            ) {
                                Text("Додати")
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        if (uiState.tags.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                mainAxisSpacing = 8.dp,
                                crossAxisSpacing = 8.dp,
                            ) {
                                uiState.tags.forEach { tag ->
                                    TagChip(
                                        text = tag,
                                        onDismiss = { viewModel.onTagsChange(uiState.tags - tag) },
                                    )
                                }
                            }
                        } else {
                            Text(
                                "Теги ще не додані.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                item {
                    ReminderSection(
                        reminderTime = uiState.reminderTime,
                        onSetReminder = viewModel::onSetReminder,
                        onClearReminder = viewModel::onClearReminder,
                    )
                }

                item {
                    EvaluationSection(
                        uiState = uiState,
                        onViewModelAction = viewModel,
                    )
                }

                item {
                    Text("Attachments", style = MaterialTheme.typography.titleLarge)
                }

                items(attachments) { attachment ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clickable {
                                when (attachment) {
                                    is ListItemContent.LinkItem -> {
                                        viewModel.onLinkClick(attachment.link.linkData)
                                    }
                                    is ListItemContent.NoteItem -> {
                                        navController.navigate("note_edit_screen?noteId=${attachment.note.id}")
                                    }
                                    is ListItemContent.CustomListItem -> {
                                        navController.navigate("custom_list_screen/${attachment.customList.id}")
                                    }
                                    else -> {}
                                }
                            },
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (attachment) {
                                is ListItemContent.LinkItem -> {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        if (attachment.link.linkData.type == LinkType.OBSIDIAN) {
                                            Text(text = "[Obsidian]")
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text(text = attachment.link.linkData.displayName ?: attachment.link.linkData.target)
                                    }
                                }
                                is ListItemContent.NoteItem -> {
                                    Text(text = attachment.note.title, modifier = Modifier.weight(1f))
                                }
                                is ListItemContent.CustomListItem -> {
                                    Text(text = attachment.customList.name, modifier = Modifier.weight(1f))
                                }
                                else -> {}
                            }
                            IconButton(onClick = { viewModel.deleteAttachment(attachment) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete attachment")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderSection(
    reminderTime: Long?,
    onSetReminder: (year: Int, month: Int, day: Int, hour: Int, minute: Int) -> Unit,
    onClearReminder: () -> Unit,
) {
    val context = LocalContext.current
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val checkPermissionsAndShowDatePicker = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(context, "Потрібен дозвіл на точні нагадування", Toast.LENGTH_LONG).show()
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).also {
                context.startActivity(it)
            }
        } else {
            showDatePicker = true
        }
    }

    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState()

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Нагадування", style = MaterialTheme.typography.titleMedium)

            if (reminderTime != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = formatDateTime(reminderTime), style = MaterialTheme.typography.bodyLarge)
                    Row {
                        IconButton(onClick = { checkPermissionsAndShowDatePicker() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Змінити нагадування")
                        }
                        IconButton(onClick = onClearReminder) {
                            Icon(Icons.Default.Delete, contentDescription = "Видалити нагадування", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } else {
                OutlinedButton(onClick = { checkPermissionsAndShowDatePicker() }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AlarmAdd, contentDescription = null)
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("Додати нагадування")
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Скасувати") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Виберіть час") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                        datePickerState.selectedDateMillis?.let { dateMillis ->
                            val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }
                            val year = calendar.get(Calendar.YEAR)
                            val month = calendar.get(Calendar.MONTH)
                            val day = calendar.get(Calendar.DAY_OF_MONTH)
                            onSetReminder(year, month, day, timePickerState.hour, timePickerState.minute)
                        }
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Скасувати") }
            },
        )
    }
}

@Composable
private fun EvaluationSection(
    uiState: AttachmentsUiState,
    onViewModelAction: AttachmentsViewModel,
) {
    var isExpanded by remember { mutableStateOf(false) }

    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Оцінка", style = MaterialTheme.typography.titleLarge)
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Згорнути" else "Розгорнути",
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    Column(
                        modifier = Modifier.padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        ScoringStatusSelector(
                            selectedStatus = uiState.scoringStatus,
                            onStatusSelected = onViewModelAction::onScoringStatusChange,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )

                        if (uiState.scoringStatus == ScoringStatus.ASSESSED) {
                            val rawScore = uiState.rawScore
                            val balanceText = "Balance: ${if (rawScore >= 0) "+" else ""}" + "%.2f".format(rawScore)
                            val balanceColor =
                                when {
                                    rawScore > 0.2 -> Color(0xFF2E7D32)
                                    rawScore > -0.2 -> LocalContentColor.current
                                    else -> Color(0xFFC62828)
                                }
                            Text(
                                text = balanceText,
                                color = balanceColor,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }

                        EvaluationTabs(
                            uiState = uiState,
                            onViewModelAction = onViewModelAction,
                            isEnabled = uiState.isScoringEnabled,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoringStatusSelector(
    selectedStatus: ScoringStatus,
    onStatusSelected: (ScoringStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statuses = ScoringStatus.entries.toTypedArray()
    val labels =
        mapOf(
            ScoringStatus.NOT_ASSESSED to "Не задано",
            ScoringStatus.ASSESSED to "Задано",
            ScoringStatus.IMPOSSIBLE_TO_ASSESS to "Неможливо",
        )
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        statuses.forEachIndexed { index, status ->
            SegmentedButton(
                selected = selectedStatus == status,
                onClick = { onStatusSelected(status) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = statuses.size),
            ) {
                Text(labels[status] ?: "")
            }
        }
    }
}

@Composable
private fun EvaluationTabs(
    uiState: AttachmentsUiState,
    onViewModelAction: AttachmentsViewModel,
    isEnabled: Boolean,
) {
    val tabTitles = listOf("Вигода", "Втрати", "Ваги")
    val pagerState = rememberPagerState { tabTitles.size }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.alpha(if (isEnabled) 1.0f else 0.5f)) {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    enabled = isEnabled,
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(title) },
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            userScrollEnabled = isEnabled,
        ) { page ->
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (page) {
                    0 -> {
                        ParameterSlider(
                            label = "Важливість",
                            value = uiState.valueImportance,
                            onValueChange = onViewModelAction::onValueImportanceChange,
                            scale = Scales.importance,
                            enabled = isEnabled,
                        )
                        ParameterSlider(
                            label = "Вплив",
                            value = uiState.valueImpact,
                            onValueChange = onViewModelAction::onValueImpactChange,
                            scale = Scales.impact,
                            enabled = isEnabled,
                        )
                    }
                    1 -> {
                        ParameterSlider(
                            label = "Зусилля",
                            value = uiState.effort,
                            onValueChange = onViewModelAction::onEffortChange,
                            scale = Scales.effort,
                            enabled = isEnabled,
                        )
                        ParameterSlider(
                            label = "Витрати",
                            value = uiState.cost,
                            onValueChange = onViewModelAction::onCostChange,
                            scale = Scales.cost,
                            valueLabels = Scales.costLabels,
                            enabled = isEnabled,
                        )
                        ParameterSlider(
                            label = "Ризик",
                            value = uiState.risk,
                            onValueChange = onViewModelAction::onRiskChange,
                            scale = Scales.risk,
                            enabled = isEnabled,
                        )
                    }
                    2 -> {
                        ParameterSlider(
                            label = "Вага зусиль",
                            value = uiState.weightEffort,
                            onValueChange = onViewModelAction::onWeightEffortChange,
                            scale = Scales.weights,
                            enabled = isEnabled,
                        )
                        ParameterSlider(
                            label = "Вага витрат",
                            value = uiState.weightCost,
                            onValueChange = onViewModelAction::onWeightCostChange,
                            scale = Scales.weights,
                            enabled = isEnabled,
                        )
                        ParameterSlider(
                            label = "Вага ризику",
                            value = uiState.weightRisk,
                            onValueChange = onViewModelAction::onWeightRiskChange,
                            scale = Scales.weights,
                            enabled = isEnabled,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParameterSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    scale: List<Float>,
    enabled: Boolean,
    valueLabels: List<String>? = null,
) {
    val currentIndex = scale.indexOf(value).coerceAtLeast(0)
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            val displayText =
                when {
                    valueLabels != null -> valueLabels.getOrElse(currentIndex) { value.toString() }
                    scale == Scales.weights -> "x${"%.1f".format(value)}"
                    else -> value.toInt().toString()
                }
            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            enabled = enabled,
            value = currentIndex.toFloat(),
            onValueChange = { newIndex ->
                val roundedIndex = newIndex.roundToInt().coerceIn(0, scale.lastIndex)
                onValueChange(scale[roundedIndex])
            },
            valueRange = 0f..scale.lastIndex.toFloat(),
            steps = (scale.size - 2).coerceAtLeast(0),
        )
    }
}

private fun formatDateTime(millis: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}

@Composable
fun TagChip(
    text: String,
    onDismiss: () -> Unit,
) {
    Row(
        modifier =
        Modifier
            .padding(top = 4.dp)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.Cancel,
            contentDescription = "Видалити тег",
            modifier =
            Modifier
                .size(16.dp)
                .clickable(onClick = onDismiss),
        )
    }
}