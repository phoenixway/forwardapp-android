package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.GENERAL_MISSION_STREAM_ID
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStatus
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.MissionStream
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIteration
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalMission
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.InputSuggestionActions
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelGlobalActions
import com.romankozak.forwardappmobile.features.missions.presentation.TacticsWorkspaceMode
import com.romankozak.forwardappmobile.features.missions.presentation.TacticalMissionViewModel
import com.romankozak.forwardappmobile.features.missions.presentation.isInCurrentIteration
import com.romankozak.forwardappmobile.features.missions.presentation.normalizedMissionStreamId
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun TacticsBottomPanel(
    globalActions: BottomPanelGlobalActions,
    viewModel: TacticalMissionViewModel = hiltViewModel(),
) {
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val contextMarkerNames by viewModel.contextMarkerNames.collectAsStateWithLifecycle()
    val projectOptions by viewModel.projectOptions.collectAsStateWithLifecycle()
    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val missionStreams by viewModel.missionStreams.collectAsStateWithLifecycle()
    val allMissions by viewModel.missions.collectAsStateWithLifecycle()
    val tacticalIterations by viewModel.tacticalIterations.collectAsStateWithLifecycle()
    val activeIteration by viewModel.activeIteration.collectAsStateWithLifecycle()
    val recentMissionStreams by viewModel.recentMissionStreams.collectAsStateWithLifecycle()
    val selectedMissionStreamId by viewModel.selectedMissionStreamId.collectAsStateWithLifecycle()
    val missionStreamCounts by viewModel.missionStreamCounts.collectAsStateWithLifecycle()
    val iterationDurationDays by viewModel.iterationDurationDays.collectAsStateWithLifecycle()
    val iterationDurationHours by viewModel.iterationDurationHours.collectAsStateWithLifecycle()
    val activitySlotContexts by viewModel.activitySlotContexts.collectAsStateWithLifecycle()
    val selectedPlanningContextId by viewModel.selectedPlanningContextId.collectAsStateWithLifecycle()
    val canPasteAsMissions by viewModel.canPasteAsMissions.collectAsStateWithLifecycle()
    val inputSuggestionActions = remember { InputSuggestionActions() }
    var inputValue by remember { mutableStateOf(TextFieldValue("")) }
    var showContextPicker by remember { mutableStateOf(false) }
    var showIterationDurationDialog by remember { mutableStateOf(false) }
    var showIterationArchiveSheet by remember { mutableStateOf(false) }
    val autocompleteSuggestions =
        remember(inputValue, allTags, contextMarkerNames) {
            inputSuggestionActions.buildSuggestions(
                currentText = inputValue.text,
                cursorPosition = inputValue.selection.start.coerceAtLeast(0),
                contextMarkerNames = contextMarkerNames,
                tags = allTags,
            )
        }

    fun submitMission() {
        val title = inputValue.text.trim()
        if (title.isBlank()) return
        viewModel.addQuickMissionForCurrentStream(title)
        inputValue = TextFieldValue("")
    }

    val selectedStreamName = missionStreams.firstOrNull { it.id == selectedMissionStreamId }?.title
    val placeholder =
        if (selectedMode == TacticsWorkspaceMode.STREAMS && selectedStreamName != null) {
            "Нова місія в потік: $selectedStreamName..."
        } else {
            "Нова місія тижня..."
    }

    TacticsBottomPanelContent(
        inputValue = inputValue,
        onValueChange = { inputValue = it },
        onSubmit = ::submitMission,
        placeholder = placeholder,
        autocompleteSuggestions = autocompleteSuggestions,
        onSuggestionClick = { suggestion ->
            inputSuggestionActions
                .applySuggestion(
                    currentText = inputValue.text,
                    cursorPosition = inputValue.selection.start.coerceAtLeast(0),
                    suggestion = suggestion,
                )?.let { result ->
                    inputValue =
                        TextFieldValue(
                            text = result.text,
                            selection = TextRange(result.cursorPosition),
                        )
                }
        },
        onAddMissionFromContext = { showContextPicker = true },
        onToggleScopeLinksSheet = viewModel::toggleScopeLinksSheet,
        selectedMode = selectedMode,
        missionStreams = recentMissionStreams,
        allMissionStreams = missionStreams,
        selectedMissionStreamId = selectedMissionStreamId,
        missionStreamCounts = missionStreamCounts,
        iterationDurationDays = iterationDurationDays,
        iterationDurationHours = iterationDurationHours,
        activitySlotContexts = activitySlotContexts,
        selectedPlanningContextId = selectedPlanningContextId,
        projectOptions = projectOptions,
        canPasteAsMissions = canPasteAsMissions,
        onModeSelected = viewModel::selectMode,
        onMissionStreamSelected = viewModel::selectMissionStream,
        onPlanningContextSelected = viewModel::selectPlanningContext,
        onOpenMissionStreamsSheet = viewModel::openMissionStreamsSheet,
        onPasteMissions = viewModel::pasteClipboardAsMissions,
        onSetIterationDuration = { showIterationDurationDialog = true },
        onOpenIterationArchive = { showIterationArchiveSheet = true },
        onStartTimeboxedIteration = viewModel::startTimeboxedIteration,
        onStartOpenEndedIteration = viewModel::startOpenEndedIteration,
        globalActions = globalActions,
    )

    TacticsContextPickerHost(
        visible = showContextPicker,
        projectOptions = projectOptions,
        onDismiss = { showContextPicker = false },
        onContextSelected = { contextId ->
            viewModel.addWeeklyMissionFromContext(contextId)
            showContextPicker = false
        },
    )

    if (showIterationDurationDialog) {
        TacticalIterationDurationDialog(
            currentDays = iterationDurationDays,
            currentHours = iterationDurationHours,
            onDismiss = { showIterationDurationDialog = false },
            onSave = { days, hours ->
                viewModel.setIterationDuration(days, hours)
                showIterationDurationDialog = false
            },
            onClear = {
                viewModel.setIterationDuration(null, null)
                showIterationDurationDialog = false
            },
        )
    }

    if (showIterationArchiveSheet) {
        TacticalIterationArchiveSheet(
            missions = allMissions,
            missionStreams = missionStreams,
            iterations = tacticalIterations,
            activeIterationId = activeIteration?.id,
            currentWeekKey = viewModel.currentWeekKey,
            actions =
                ArchivedMissionActions(
                    onMoveToCurrentIteration = viewModel::moveMissionToCurrentIteration,
                    onComplete = viewModel::completeMission,
                    onPause = viewModel::pauseMission,
                    onActivate = viewModel::activateMission,
                    onDelete = { mission -> viewModel.deleteMission(mission.id) },
                ),
            onDismiss = { showIterationArchiveSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TacticalIterationArchiveSheet(
    missions: List<TacticalMission>,
    missionStreams: List<MissionStream>,
    iterations: List<TacticalIteration>,
    activeIterationId: String?,
    currentWeekKey: String,
    actions: ArchivedMissionActions,
    onDismiss: () -> Unit,
) {
    val streamTitleById =
        remember(missionStreams) {
            missionStreams.associate { it.id to it.title } + (GENERAL_MISSION_STREAM_ID to "General")
        }
    val archivedIterations =
        remember(missions, missionStreams, iterations, activeIterationId, currentWeekKey) {
            val iterationById = iterations.associateBy { it.id }
            missions
                .filterNot { it.isInCurrentIteration(activeIterationId, currentWeekKey) }
                .filter { it.iterationId != null || it.weekKey.isNotBlank() }
                .groupBy { it.iterationId ?: it.weekKey }
                .toSortedMap(compareByDescending { it })
                .map { (iterationKey, weekMissions) ->
                    val iteration = iterationById[iterationKey]
                    ArchivedTacticalIterationUi(
                        id = iterationKey,
                        title = iteration?.title ?: formatTacticalWeekTitle(iterationKey),
                        streams =
                            weekMissions
                                .groupBy { it.normalizedMissionStreamId() }
                                .map { (streamId, streamMissions) ->
                                    ArchivedTacticalStreamUi(
                                        title =
                                            streamTitleById[streamId]
                                                ?: "Потік ${streamId.takeLast(STREAM_ID_SUFFIX_LENGTH)}",
                                        missions =
                                            streamMissions.sortedWith(
                                                compareBy<TacticalMission> { it.status != MissionStatus.COMPLETED }
                                                    .thenBy { it.orderInWeek }
                                                    .thenBy { it.createdAt },
                                            ),
                                    )
                                }
                                .sortedBy { it.title.lowercase(Locale.getDefault()) },
                    )
                }
        }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Минулі ітерації",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (archivedIterations.isEmpty()) {
                Text(
                    text = "Поки немає місій з попередніх тактичних ітерацій.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(archivedIterations, key = { it.id }) { iteration ->
                        ArchivedTacticalIterationCard(
                            iteration = iteration,
                            actions = actions,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchivedTacticalIterationCard(
    iteration: ArchivedTacticalIterationUi,
    actions: ArchivedMissionActions,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = iteration.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            iteration.streams.forEachIndexed { index, stream ->
                if (index > 0) HorizontalDivider()
                ArchivedTacticalStreamSection(
                    stream = stream,
                    actions = actions,
                )
            }
        }
    }
}

@Composable
private fun ArchivedTacticalStreamSection(
    stream: ArchivedTacticalStreamUi,
    actions: ArchivedMissionActions,
) {
    val completedCount = stream.missions.count { it.status == MissionStatus.COMPLETED }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stream.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "$completedCount/${stream.missions.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        stream.missions.forEach { mission ->
            ArchivedTacticalMissionRow(
                mission = mission,
                actions = actions,
            )
        }
    }
}

@Composable
private fun ArchivedTacticalMissionRow(
    mission: TacticalMission,
    actions: ArchivedMissionActions,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "${missionStatusPrefix(mission.status)} ${mission.title}",
            style = MaterialTheme.typography.bodyMedium,
            color =
                if (mission.status == MissionStatus.COMPLETED) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = { menuExpanded = true }) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Дії місії")
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("В поточну ітерацію") },
                onClick = {
                    menuExpanded = false
                    actions.onMoveToCurrentIteration(mission)
                },
            )
            if (mission.status == MissionStatus.COMPLETED) {
                DropdownMenuItem(
                    text = { Text("Активувати") },
                    onClick = {
                        menuExpanded = false
                        actions.onActivate(mission)
                    },
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Завершити") },
                    onClick = {
                        menuExpanded = false
                        actions.onComplete(mission)
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("Пауза") },
                onClick = {
                    menuExpanded = false
                    actions.onPause(mission)
                },
            )
            DropdownMenuItem(
                text = { Text("Видалити") },
                onClick = {
                    menuExpanded = false
                    actions.onDelete(mission)
                },
            )
        }
    }
}

private data class ArchivedTacticalIterationUi(
    val id: String,
    val title: String,
    val streams: List<ArchivedTacticalStreamUi>,
)

private data class ArchivedTacticalStreamUi(
    val title: String,
    val missions: List<TacticalMission>,
)

private data class ArchivedMissionActions(
    val onMoveToCurrentIteration: (TacticalMission) -> Unit,
    val onComplete: (TacticalMission) -> Unit,
    val onPause: (TacticalMission) -> Unit,
    val onActivate: (TacticalMission) -> Unit,
    val onDelete: (TacticalMission) -> Unit,
)

private fun missionStatusPrefix(status: MissionStatus): String =
    when (status) {
        MissionStatus.COMPLETED -> "✓"
        MissionStatus.ACTIVE -> "•"
        MissionStatus.PAUSED -> "Ⅱ"
        MissionStatus.INACTIVE -> "○"
    }

private fun formatTacticalWeekTitle(weekKey: String): String {
    return runCatching {
        val match = Regex("""^(\d{4})-W(\d{2})$""").matchEntire(weekKey) ?: error("Invalid week key")
        val year = match.groupValues[1].toInt()
        val week = match.groupValues[2].toInt()
        val weekFields = WeekFields.ISO
        val start =
            LocalDate.now()
                .with(weekFields.weekBasedYear(), year.toLong())
                .with(weekFields.weekOfWeekBasedYear(), week.toLong())
                .with(DayOfWeek.MONDAY)
        val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
        "$weekKey · ${start.format(formatter)} – ${start.plusDays(DAYS_IN_WEEK - 1).format(formatter)}"
    }.getOrDefault(weekKey)
}

private const val STREAM_ID_SUFFIX_LENGTH = 4
private const val DAYS_IN_WEEK = 7L
private const val HOURS_PER_DAY = 24
private const val MAX_DAYS_DIGITS = 3
private const val MAX_HOURS_DIGITS = 4

@Composable
private fun TacticalIterationDurationDialog(
    currentDays: Int?,
    currentHours: Int?,
    onDismiss: () -> Unit,
    onSave: (Int, Int?) -> Unit,
    onClear: () -> Unit,
) {
    var daysText by remember(currentDays) { mutableStateOf(currentDays?.toString().orEmpty()) }
    var hoursText by remember(currentHours) { mutableStateOf(currentHours?.toString().orEmpty()) }
    val parsedDays = daysText.trim().toIntOrNull()?.takeIf { it > 0 }
    val parsedHours = hoursText.trim().toIntOrNull()?.takeIf { it > 0 }
    val maxHours = parsedDays?.times(HOURS_PER_DAY)
    val isHoursValid = hoursText.isBlank() || (parsedHours != null && maxHours != null && parsedHours <= maxHours)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Тривалість тактичної ітерації") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it.filter(Char::isDigit).take(MAX_DAYS_DIGITS) },
                    label = { Text("Днів від сьогодні") },
                    placeholder = { Text("7") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("Дні задають дедлайн тактичної ітерації.") },
                )
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it.filter(Char::isDigit).take(MAX_HOURS_DIGITS) },
                    label = { Text("Робочих годин в ітерації") },
                    placeholder = { Text("40") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = !isHoursValid,
                    supportingText = {
                        Text(
                            text =
                                if (maxHours == null) {
                                    "Спершу вкажи дні."
                                } else {
                                    "Не більше $maxHours год для $parsedDays дн."
                                },
                        )
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = parsedDays != null && isHoursValid,
                onClick = { parsedDays?.let { onSave(it, parsedHours) } },
            ) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
            TextButton(onClick = onClear) {
                Text("Очистити")
            }
        },
    )
}
