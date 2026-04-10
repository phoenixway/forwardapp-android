@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.romankozak.forwardappmobile.features.activitytracker

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecord
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.NavTargetRouter
import com.romankozak.forwardappmobile.features.activitytracker.dialogs.TimePickerDialog
import com.romankozak.forwardappmobile.features.activitytracker.dialogs.formatDuration
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Button
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Controller
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Overlay
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenuItem
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.IconPosition
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.MenuAlignment
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.rememberHoldMenu2
import com.romankozak.forwardappmobile.features.reminders.dialogs.ReminderPropertiesDialog
import com.romankozak.forwardappmobile.ui.shared.InProgressIndicator
import com.romankozak.forwardappmobile.ui.shared.InProgressIndicatorState
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

private val HashTagRegex = Regex("""(^|\s)(#[\p{L}\p{N}_-]+)""")
private const val JournalTagAnnotation = "journal_tag"

private data class ScrollRestoreAnchor(
    val key: Any,
    val offset: Int,
)

private data class JournalUiConfig(
    val isCompactPhone: Boolean,
    val listHorizontalPadding: Dp,
    val listVerticalPadding: Dp,
    val itemSpacing: Dp,
    val headerBottomSpacing: Dp,
    val sectionSpacing: Dp,
    val entryHorizontalPadding: Dp,
    val entryVerticalPadding: Dp,
    val sectionHorizontalPadding: Dp,
    val sectionVerticalPadding: Dp,
    val dayHeaderBackgroundHorizontalPadding: Dp,
    val dayHeaderBackgroundVerticalPadding: Dp,
    val cornerRadius: Dp,
    val metadataSpacing: Dp,
    val textMaxLines: Int,
    val maxVisibleTags: Int,
    val inputMinHeight: Dp,
    val inputBarPadding: Dp,
    val pillHorizontalPadding: Dp,
    val pillVerticalPadding: Dp,
)

@Composable
private fun rememberJournalUiConfig(): JournalUiConfig {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    return remember(screenWidthDp) {
        if (screenWidthDp < 390) {
            JournalUiConfig(
                isCompactPhone = true,
                listHorizontalPadding = 10.dp,
                listVerticalPadding = 8.dp,
                itemSpacing = 8.dp,
                headerBottomSpacing = 8.dp,
                sectionSpacing = 22.dp,
                entryHorizontalPadding = 12.dp,
                entryVerticalPadding = 10.dp,
                sectionHorizontalPadding = 6.dp,
                sectionVerticalPadding = 4.dp,
                dayHeaderBackgroundHorizontalPadding = 10.dp,
                dayHeaderBackgroundVerticalPadding = 8.dp,
                cornerRadius = 16.dp,
                metadataSpacing = 6.dp,
                textMaxLines = 2,
                maxVisibleTags = 2,
                inputMinHeight = 42.dp,
                inputBarPadding = 8.dp,
                pillHorizontalPadding = 7.dp,
                pillVerticalPadding = 3.dp,
            )
        } else {
            JournalUiConfig(
                isCompactPhone = false,
                listHorizontalPadding = 12.dp,
                listVerticalPadding = 10.dp,
                itemSpacing = 10.dp,
                headerBottomSpacing = 10.dp,
                sectionSpacing = 26.dp,
                entryHorizontalPadding = 14.dp,
                entryVerticalPadding = 12.dp,
                sectionHorizontalPadding = 8.dp,
                sectionVerticalPadding = 6.dp,
                dayHeaderBackgroundHorizontalPadding = 12.dp,
                dayHeaderBackgroundVerticalPadding = 9.dp,
                cornerRadius = 18.dp,
                metadataSpacing = 8.dp,
                textMaxLines = 3,
                maxVisibleTags = 2,
                inputMinHeight = 46.dp,
                inputBarPadding = 10.dp,
                pillHorizontalPadding = 8.dp,
                pillVerticalPadding = 4.dp,
            )
        }
    }
}

private val ActivityRecord.isTimeless: Boolean
    get() = this.startTime == null

private val ActivityRecord.isOngoing: Boolean
    get() = this.startTime != null && this.endTime == null

private enum class ActivityRecordType { COMMENT, TIMED, INSTANT }

@Composable
fun ActivityTrackerScreen(
    navController: NavController,
    viewModel: ActivityTrackerViewModel = hiltViewModel(),
) {
    val groupedByDate by viewModel.groupedActivityLog.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val lastOngoingActivity by viewModel.lastOngoingActivity.collectAsStateWithLifecycle()
    val editingRecord by viewModel.editingRecord.collectAsStateWithLifecycle()
    val recordToDelete by viewModel.recordToDelete.collectAsStateWithLifecycle()
    val isEditingLastTimedRecord by viewModel.isEditingLastTimedRecord.collectAsStateWithLifecycle()
    val recordForReminder by viewModel.recordForReminder.collectAsStateWithLifecycle()
    val isLoadingOlderRecords by viewModel.isLoadingOlderRecords.collectAsStateWithLifecycle()
    val hasMoreOlderRecords by viewModel.hasMoreOlderRecords.collectAsStateWithLifecycle()
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }

    val filteredGroupedByDate =
        remember(groupedByDate, selectedTag) {
            val tag = selectedTag
            if (tag.isNullOrBlank()) {
                groupedByDate
            } else {
                groupedByDate.mapValues { (_, records) ->
                    records.filter { record -> recordHasTag(record.text, tag) }
                }.filterValues { it.isNotEmpty() }
            }
        }

    var quickDoneDialogState by remember { mutableStateOf<String?>(null) }
    val holdMenuController = rememberHoldMenu2()

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                ActivityTrackerTopAppBar(
                    onNavigateBack = { navController.popBackStack() },
                    onClearLogRequest = { showClearConfirmDialog = true },
                    onExportRequest = {
                        val markdown = exportLogToMarkdown(groupedByDate.values.flatten())
                        copyToClipboard(context, markdown)
                    },
                )
            },
            bottomBar = {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding(),
                ) {
                    val indicatorState = remember { InProgressIndicatorState(isInitiallyExpanded = true) }
                    InProgressIndicator(
                        ongoingActivity = lastOngoingActivity,
                        onStopClick = viewModel::onToggleStartStop,
                        onReminderClick = { lastOngoingActivity?.let { viewModel.onSetReminder(it) } },
                        onIndicatorClick = { },
                        indicatorState = indicatorState,
                    )
                    ActivityInputBar(
                        text = inputText,
                        isActivityOngoing = lastOngoingActivity != null,
                        onTextChange = viewModel::onInputTextChanged,
                        onToggleStartStop = viewModel::onToggleStartStop,
                        onTimelessClick = viewModel::onTimelessRecordClick,
                        onQuickDoneClick = { textValue -> quickDoneDialogState = textValue },
                        holdMenuController = holdMenuController,
                    )
                }
            },
        ) { paddingValues ->
            ActivityLog(
                groupedByDate = filteredGroupedByDate,
                modifier = Modifier.padding(paddingValues),
                onEdit = viewModel::onEditRequest,
                onRestart = viewModel::onRestartActivity,
                onDelete = viewModel::onDeleteRequest,
                onSetReminder = viewModel::onSetReminder,
                selectedTag = selectedTag,
                onTagSelected = { selectedTag = it },
                onClearTagFilter = { selectedTag = null },
                isLoadingOlderRecords = isLoadingOlderRecords,
                hasMoreOlderRecords = hasMoreOlderRecords,
                onLoadOlderRecords = viewModel::loadOlderRecords,
                onSearchTagGlobally = { tag ->
                    val encoded = URLEncoder.encode(tag, "UTF-8")
                    navController.navigate(NavTargetRouter.routeOf(NavTarget.GlobalSearch(query = encoded)))
                },
            )

            editingRecord?.let { recordToEdit ->
                EditRecordDialog(
                    record = recordToEdit,
                    onDismiss = viewModel::onEditDialogDismiss,
                    onConfirm = viewModel::onRecordUpdated,
                    isLastTimedRecord = isEditingLastTimedRecord,
                )
            }

            recordToDelete?.let { record ->
                AlertDialog(
                    onDismissRequest = viewModel::onDeleteDismiss,
                    title = { Text("Видалити запис?") },
                    text = { Text("Ви впевнені, що хочете видалити запис: \"${record.text}\"?") },
                    confirmButton = { Button(onClick = viewModel::onDeleteConfirm) { Text("Видалити") } },
                    dismissButton = { TextButton(onClick = viewModel::onDeleteDismiss) { Text("Скасувати") } },
                )
            }

            if (showClearConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showClearConfirmDialog = false },
                    title = { Text("Очистити лог?") },
                    text = { Text("Ви впевнені, що хочете видалити всі записи? Цю дію неможливо буде скасувати.") },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.onClearLogConfirm()
                            showClearConfirmDialog = false
                        }) { Text("Видалити") }
                    },
                    dismissButton = { TextButton(onClick = { showClearConfirmDialog = false }) { Text("Скасувати") } },
                )
            }

            recordForReminder?.let { record ->
                ReminderPropertiesDialog(
                    onDismiss = viewModel::onReminderDialogDismiss,
                    onSetReminder = { time -> viewModel.onSetReminder(time) },
                    onRemoveReminder =
                        if (record.reminderTime != null) {
                            { _: String -> viewModel.onClearReminder() }
                        } else {
                            null
                        },
                    currentReminders =
                        listOfNotNull(record.reminderTime).map {
                            Reminder(
                                entityId = record.id,
                                entityType = "TASK",
                                reminderTime = it,
                                status = "SCHEDULED",
                                creationTime = System.currentTimeMillis(),
                            )
                        },
                )
            }

            quickDoneDialogState?.let { presetText ->
                QuickCompletedActionDialog(
                    initialText = presetText,
                    onDismiss = { quickDoneDialogState = null },
                    onConfirm = { desc, xp, antyXp ->
                        viewModel.onAddCompletedAction(desc, xp, antyXp)
                        viewModel.onInputTextChanged("")
                        quickDoneDialogState = null
                    },
                )
            }
        }

        HoldMenu2Overlay(controller = holdMenuController)
    }
}

@Composable
private fun ActivityTrackerTopAppBar(
    onNavigateBack: () -> Unit,
    onClearLogRequest: () -> Unit,
    onExportRequest: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text("Life Journal") },
        navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
        actions = {
            IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, "Меню") }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(text = { Text("Експорт в Markdown") }, onClick = {
                    onExportRequest()
                    menuExpanded = false
                })
                DropdownMenuItem(text = { Text("Очистити лог") }, onClick = {
                    onClearLogRequest()
                    menuExpanded = false
                })
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ActivityLog(
    groupedByDate: Map<String, List<ActivityRecord>>,
    modifier: Modifier = Modifier,
    onEdit: (ActivityRecord) -> Unit,
    onRestart: (ActivityRecord) -> Unit,
    onDelete: (ActivityRecord) -> Unit,
    onSetReminder: (ActivityRecord) -> Unit,
    selectedTag: String?,
    onTagSelected: (String) -> Unit,
    onClearTagFilter: () -> Unit,
    isLoadingOlderRecords: Boolean,
    hasMoreOlderRecords: Boolean,
    onLoadOlderRecords: () -> Unit,
    onSearchTagGlobally: (String) -> Unit,
) {
    val uiConfig = rememberJournalUiConfig()
    val lazyListState = rememberLazyListState()
    var didInitialScroll by rememberSaveable { mutableStateOf(false) }
    var pendingScrollRestore by remember { mutableStateOf<ScrollRestoreAnchor?>(null) }
    val listKeys =
        remember(groupedByDate, selectedTag) {
            buildJournalListKeys(
                groupedByDate = groupedByDate,
                selectedTag = selectedTag,
            )
        }

    LaunchedEffect(groupedByDate.isNotEmpty()) {
        if (groupedByDate.isNotEmpty() && !didInitialScroll) {
            val lastIndex = (lazyListState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
            lazyListState.scrollToItem(lastIndex)
            didInitialScroll = true
        }
    }

    LaunchedEffect(groupedByDate.isEmpty()) {
        if (groupedByDate.isEmpty()) {
            didInitialScroll = false
            pendingScrollRestore = null
        }
    }

    LaunchedEffect(didInitialScroll, hasMoreOlderRecords, isLoadingOlderRecords) {
        if (!didInitialScroll || !hasMoreOlderRecords) return@LaunchedEffect

        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .collect { firstVisibleItemIndex ->
                if (firstVisibleItemIndex > 24 || isLoadingOlderRecords) return@collect

                val anchorItem = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull() ?: return@collect
                pendingScrollRestore =
                    ScrollRestoreAnchor(
                        key = anchorItem.key,
                        offset = lazyListState.firstVisibleItemScrollOffset,
                    )
                onLoadOlderRecords()
            }
    }

    LaunchedEffect(listKeys, isLoadingOlderRecords, pendingScrollRestore) {
        val anchor = pendingScrollRestore ?: return@LaunchedEffect
        if (isLoadingOlderRecords) return@LaunchedEffect

        val restoredIndex = listKeys.indexOf(anchor.key)
        if (restoredIndex >= 0) {
            lazyListState.scrollToItem(restoredIndex, anchor.offset)
        }
        pendingScrollRestore = null
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.padding(horizontal = uiConfig.listHorizontalPadding),
        contentPadding = PaddingValues(vertical = uiConfig.listVerticalPadding),
    ) {
        selectedTag?.let { tag ->
            item(key = "tag_filter_banner") {
                ActiveTagFilterCard(
                    tag = tag,
                    onClear = onClearTagFilter,
                    onSearchGlobally = { onSearchTagGlobally(tag) },
                    uiConfig = uiConfig,
                )
            }
        }
        if (groupedByDate.isEmpty()) {
            item(key = "empty_state") {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (selectedTag == null) {
                            "Лог порожній. Почніть першу активність!"
                        } else {
                            "За тегом $selectedTag нічого не знайдено."
                        },
                    )
                }
            }
        } else {
            groupedByDate.entries.forEachIndexed { sectionIndex, (dateHeader, records) ->
                if (sectionIndex > 0) {
                    item(key = "day_section_gap_$dateHeader") {
                        Spacer(modifier = Modifier.height(uiConfig.sectionSpacing))
                    }
                }
                stickyHeader(key = "day_header_$dateHeader") {
                    Box(modifier = Modifier.padding(top = 2.dp)) {
                        JournalDayHeader(
                            dateHeader = dateHeader,
                            entryCount = records.size,
                            uiConfig = uiConfig,
                        )
                    }
                }
                item(key = "day_header_gap_$dateHeader") {
                    Spacer(modifier = Modifier.height(uiConfig.headerBottomSpacing))
                }
                itemsIndexed(records, key = { _, record -> record.id }) { recordIndex, record ->
                    JournalEntryCard(
                        record = record,
                        onEdit = onEdit,
                        onRestart = onRestart,
                        onDelete = onDelete,
                        onSetReminder = onSetReminder,
                        onTagClick = onTagSelected,
                        uiConfig = uiConfig,
                    )
                    if (recordIndex < records.lastIndex) {
                        Spacer(modifier = Modifier.height(uiConfig.itemSpacing))
                    }
                }
            }
        }
    }
}

private fun buildJournalListKeys(
    groupedByDate: Map<String, List<ActivityRecord>>,
    selectedTag: String?,
): List<Any> {
    val keys = mutableListOf<Any>()
    if (selectedTag != null) {
        keys += "tag_filter_banner"
    }
    if (groupedByDate.isEmpty()) {
        keys += "empty_state"
        return keys
    }

    groupedByDate.entries.forEachIndexed { sectionIndex, (dateHeader, records) ->
        if (sectionIndex > 0) {
            keys += "day_section_gap_$dateHeader"
        }
        keys += "day_header_$dateHeader"
        keys += "day_header_gap_$dateHeader"
        keys.addAll(records.map { it.id })
    }
    return keys
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickCompletedActionDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Int?, Int?) -> Unit,
) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    var xpText by remember { mutableStateOf("") }
    var antyXpText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Швидке додавання події") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Опис активності") },
                )
                OutlinedTextField(
                    value = xpText,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() } || value.isBlank()) xpText = value
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Отримана експа (опціонально)") },
                    placeholder = { Text("0") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = antyXpText,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() } || value.isBlank()) antyXpText = value
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Втрачена експа (опціонально)") },
                    placeholder = { Text("0") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val xp = xpText.toIntOrNull()
                    val antyXp = antyXpText.toIntOrNull()
                    onConfirm(text.trim(), xp, antyXp)
                },
                enabled = text.isNotBlank(),
            ) { Text("Зберегти") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
    )
}

@Composable
private fun JournalEntryCard(
    record: ActivityRecord,
    onEdit: (ActivityRecord) -> Unit,
    onRestart: (ActivityRecord) -> Unit,
    onDelete: (ActivityRecord) -> Unit,
    onSetReminder: (ActivityRecord) -> Unit,
    onTagClick: (String) -> Unit,
    uiConfig: JournalUiConfig,
) {
    val annotatedText = rememberJournalEntryAnnotatedString(record.text)
    var menuExpanded by remember(record.id) { mutableStateOf(false) }
    val cardColor = lerp(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.secondaryContainer, 0.08f)
    val cardBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.11f)
    val dismissState =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                when (value) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        onEdit(record)
                        false
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        onDelete(record)
                        false
                    }
                    SwipeToDismissBoxValue.Settled -> true
                }
            },
            positionalThreshold = { totalDistance -> totalDistance * 0.33f },
        )

    Box {
        SwipeToDismissBox(
            state = dismissState,
            modifier = Modifier.fillMaxWidth(),
            backgroundContent = {
                JournalSwipeBackground(
                    dismissState = dismissState,
                    shape = RoundedCornerShape(uiConfig.cornerRadius),
                )
            },
            content = {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onEdit(record) },
                                onLongClick = { menuExpanded = true },
                            ),
                    shape = RoundedCornerShape(uiConfig.cornerRadius),
                    color = cardColor,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = BorderStroke(1.dp, cardBorderColor),
                ) {
                    Column(
                        modifier =
                            Modifier.padding(
                                horizontal = uiConfig.entryHorizontalPadding,
                                vertical = uiConfig.entryVerticalPadding,
                            ),
                        verticalArrangement = Arrangement.spacedBy(uiConfig.metadataSpacing),
                    ) {
                        JournalEntryText(
                            text = annotatedText,
                            style = MaterialTheme.typography.bodyLarge,
                            onTagClick = onTagClick,
                            onTextClick = { onEdit(record) },
                            maxLines = uiConfig.textMaxLines,
                            overflow = TextOverflow.Ellipsis,
                        )
                        JournalMetadataRow(
                            record = record,
                            onTagClick = onTagClick,
                            uiConfig = uiConfig,
                        )
                    }
                }
            },
        )

        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
            DropdownMenuItem(
                text = { Text("Редагувати") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onEdit(record)
                },
            )
            DropdownMenuItem(
                text = { Text("Запустити знову") },
                leadingIcon = { Icon(Icons.Default.Replay, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onRestart(record)
                },
            )
            if (record.isOngoing) {
                DropdownMenuItem(
                    text = { Text(if (record.reminderTime != null) "Редагувати нагадування" else "Додати нагадування") },
                    leadingIcon = {
                        Icon(
                            if (record.reminderTime != null) Icons.Default.NotificationImportant else Icons.Default.Notifications,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onSetReminder(record)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Видалити") },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                onClick = {
                    menuExpanded = false
                    onDelete(record)
                },
            )
        }
    }
}

@Composable
private fun JournalEntryText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    onTagClick: (String) -> Unit = {},
    onTextClick: (() -> Unit)? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    var layoutResult by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    Text(
        text = text,
        style = style,
        modifier =
            modifier.pointerInput(text, onTagClick) {
                detectTapGestures { position ->
                    val clickedOffset = layoutResult?.getOffsetForPosition(position) ?: return@detectTapGestures
                    text
                        .getStringAnnotations(tag = JournalTagAnnotation, start = clickedOffset, end = clickedOffset)
                        .firstOrNull()
                        ?.let { onTagClick(it.item) }
                        ?: onTextClick?.invoke()
                }
            },
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = { layoutResult = it },
    )
}

@Composable
private fun rememberJournalEntryAnnotatedString(text: String): AnnotatedString {
    val colorScheme = MaterialTheme.colorScheme
    return remember(text, colorScheme.primary, colorScheme.onSurface) {
        buildJournalEntryAnnotatedString(
            text = text,
            tagStyle = SpanStyle(
                color = colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

private fun buildJournalEntryAnnotatedString(
    text: String,
    tagStyle: SpanStyle,
): AnnotatedString =
    buildAnnotatedString {
        var currentIndex = 0
        HashTagRegex.findAll(text).forEach { match ->
            val prefixRange = match.groups[1]?.range
            val tagRange = match.groups[2]?.range ?: return@forEach

            val plainTextEnd = prefixRange?.last?.plus(1) ?: tagRange.first
            if (plainTextEnd > currentIndex) {
                append(text.substring(currentIndex, plainTextEnd))
            }

            withStyle(tagStyle) {
                append(text.substring(tagRange.first, tagRange.last + 1))
            }
            addStringAnnotation(
                tag = JournalTagAnnotation,
                annotation = text.substring(tagRange.first, tagRange.last + 1),
                start = length - (tagRange.last - tagRange.first + 1),
                end = length,
            )
            currentIndex = tagRange.last + 1
        }

        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }

@Composable
private fun ActiveTagFilterCard(
    tag: String,
    onClear: () -> Unit,
    onSearchGlobally: () -> Unit,
    uiConfig: JournalUiConfig,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(uiConfig.cornerRadius),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = uiConfig.entryHorizontalPadding, vertical = uiConfig.entryVerticalPadding),
            verticalArrangement = Arrangement.spacedBy(uiConfig.metadataSpacing),
        ) {
            Text(
                text = "Фільтр за тегом",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = true,
                    onClick = onClear,
                    label = { Text(tag) },
                    leadingIcon = { Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Очистити фільтр", modifier = Modifier.size(18.dp)) },
                )
                FilledTonalButton(onClick = onSearchGlobally) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Шукати глобально")
                }
            }
        }
    }
}

@Composable
private fun JournalDayHeader(
    dateHeader: String,
    entryCount: Int,
    uiConfig: JournalUiConfig,
) {
    val headerBackgroundColor = MaterialTheme.colorScheme.surface
    val chipColor = MaterialTheme.colorScheme.secondaryContainer
    val headerShape = RoundedCornerShape(14.dp)
    val headerTextColor = lerp(
        MaterialTheme.colorScheme.onSurface,
        MaterialTheme.colorScheme.primary,
        0.32f,
    )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = headerBackgroundColor,
                    shape = headerShape,
                )
                .padding(
                    horizontal = uiConfig.sectionHorizontalPadding,
                    vertical = uiConfig.sectionVerticalPadding,
                ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = dateHeader,
            style = MaterialTheme.typography.titleSmall,
            color = headerTextColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Surface(
            color = chipColor,
            shape = RoundedCornerShape(999.dp),
        ) {
            Text(
                text = recordCountLabel(entryCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier =
                    Modifier.padding(
                        horizontal = uiConfig.dayHeaderBackgroundHorizontalPadding,
                        vertical = uiConfig.dayHeaderBackgroundVerticalPadding,
                    ),
            )
        }
    }
}

@Composable
private fun JournalSwipeBackground(
    dismissState: SwipeToDismissBoxState,
    shape: RoundedCornerShape,
) {
    val isSwipeActive =
        dismissState.currentValue != SwipeToDismissBoxValue.Settled ||
            dismissState.targetValue != SwipeToDismissBoxValue.Settled
    if (!isSwipeActive) return

    val direction = dismissState.dismissDirection ?: return
    val isEdit = direction == SwipeToDismissBoxValue.StartToEnd
    val backgroundColor =
        if (isEdit) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
        }
    val contentColor =
        if (isEdit) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onErrorContainer
        }
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(backgroundColor, shape),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            horizontalArrangement =
                if (isEdit) {
                    Arrangement.Start
                } else {
                    Arrangement.End
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isEdit) Icons.Default.Edit else Icons.Default.Delete,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isEdit) "Редагувати" else "Видалити",
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun JournalMetadataRow(
    record: ActivityRecord,
    onTagClick: (String) -> Unit,
    uiConfig: JournalUiConfig,
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val tags = remember(record.text) { extractTags(record.text) }
    val durationText =
        remember(record.startTime, record.endTime, record.isOngoing) {
            val startTime = record.startTime
            val endTime = record.endTime
            if (record.isOngoing || startTime == null || endTime == null) {
                null
            } else {
                val duration = endTime - startTime
                if (duration > 0) formatDuration(duration) else null
            }
        }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(uiConfig.metadataSpacing),
        verticalArrangement = Arrangement.spacedBy(uiConfig.metadataSpacing),
    ) {
        CompactMetadataPill(
            text = buildRecordTimeLabel(record, timeFormat),
            uiConfig = uiConfig,
            emphasized = record.isOngoing,
        )
        recordTypeLabel(record)?.let { label ->
            CompactMetadataPill(text = label, uiConfig = uiConfig)
        }
        durationText?.let { duration ->
            CompactMetadataPill(text = duration, uiConfig = uiConfig)
        }
        if (record.reminderTime != null) {
            CompactMetadataPill(text = "нагадування", uiConfig = uiConfig)
        }
        if ((record.xpGained ?: 0) > 0) {
            CompactMetadataPill(text = "+${record.xpGained} xp", uiConfig = uiConfig, positive = true)
        }
        if ((record.antyXp ?: 0) > 0) {
            CompactMetadataPill(text = "-${record.antyXp} xp", uiConfig = uiConfig, negative = true)
        }
        tags.take(uiConfig.maxVisibleTags).forEach { tag ->
            CompactTagPill(tag = tag, onClick = { onTagClick(tag) }, uiConfig = uiConfig)
        }
        if (tags.size > uiConfig.maxVisibleTags) {
            CompactMetadataPill(text = "+${tags.size - uiConfig.maxVisibleTags}", uiConfig = uiConfig)
        }
    }
}

@Composable
private fun CompactMetadataPill(
    text: String,
    uiConfig: JournalUiConfig,
    emphasized: Boolean = false,
    positive: Boolean = false,
    negative: Boolean = false,
) {
    val containerColor =
        when {
            positive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            negative -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
            emphasized -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.65f)
        }
    val contentColor =
        when {
            positive -> MaterialTheme.colorScheme.onPrimaryContainer
            negative -> MaterialTheme.colorScheme.onErrorContainer
            emphasized -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier =
                Modifier.padding(
                    horizontal = uiConfig.pillHorizontalPadding,
                    vertical = uiConfig.pillVerticalPadding,
                ),
            maxLines = 1,
        )
    }
}

@Composable
private fun CompactTagPill(
    tag: String,
    onClick: () -> Unit,
    uiConfig: JournalUiConfig,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier =
                Modifier.padding(
                    horizontal = uiConfig.pillHorizontalPadding,
                    vertical = uiConfig.pillVerticalPadding,
                ),
            maxLines = 1,
        )
    }
}

private fun recordHasTag(
    text: String,
    tag: String,
): Boolean = extractTags(text).any { it.equals(tag, ignoreCase = true) }

private fun extractTags(text: String): List<String> = HashTagRegex.findAll(text).mapNotNull { it.groups[2]?.value }.toList()

private fun buildRecordTimeLabel(
    record: ActivityRecord,
    timeFormat: SimpleDateFormat,
): String =
    when {
        record.isTimeless -> timeFormat.format(Date(record.createdAt))
        record.isOngoing -> "${timeFormat.format(Date(record.startTime!!))} → ..."
        record.endTime != null && record.startTime == record.endTime -> timeFormat.format(Date(record.startTime!!))
        else -> "${timeFormat.format(Date(record.startTime!!))} → ${timeFormat.format(Date(record.endTime!!))}"
    }

private fun recordTypeLabel(record: ActivityRecord): String? =
    when {
        record.isTimeless -> "коментар"
        record.isOngoing -> "триває"
        record.startTime != null && record.endTime != null && record.startTime == record.endTime -> "подія"
        else -> null
    }

private fun recordCountLabel(count: Int): String {
    val suffix =
        when {
            count % 10 == 1 && count % 100 != 11 -> "запис"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "записи"
            else -> "записів"
    }
    return "$count $suffix"
}

@Composable
private fun ActivityInputBar(
    text: String,
    isActivityOngoing: Boolean,
    onTextChange: (String) -> Unit,
    onToggleStartStop: () -> Unit,
    onTimelessClick: () -> Unit,
    onQuickDoneClick: (String) -> Unit,
    holdMenuController: HoldMenu2Controller,
) {
    val uiConfig = rememberJournalUiConfig()
    var moreMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = uiConfig.inputBarPadding, vertical = uiConfig.inputBarPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(uiConfig.cornerRadius),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = uiConfig.inputMinHeight)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (text.isBlank()) {
                        Text(
                            text = "Що зараз?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    )
                }
            }

            val menuItems =
                remember {
                    listOf(
                        HoldMenuItem(label = "Події", icon = Icons.Default.CheckCircle),
                        HoldMenuItem(label = "Коментар", icon = Icons.Default.AddComment),
                    )
                }
            val onMenuSelect: (Int) -> Unit = { index ->
                when (index) {
                    0 -> if (text.isNotBlank()) onQuickDoneClick(text)
                    1 -> onTimelessClick()
                }
            }

            val icon: ImageVector
            val tint: Color
            val description: String

            if (isActivityOngoing) {
                if (text.isNotBlank()) {
                    icon = Icons.Default.Sync
                    tint = MaterialTheme.colorScheme.tertiary
                    description = "Зупинити поточну та почати нову"
                } else {
                    icon = Icons.Default.StopCircle
                    tint = MaterialTheme.colorScheme.error
                    description = "Зупинити"
                }
            } else {
                icon = Icons.Default.PlayCircle
                tint = MaterialTheme.colorScheme.primary
                description = "Почати"
            }

            HoldMenu2Button(
                items = menuItems,
                controller = holdMenuController,
                onSelect = onMenuSelect,
                menuAlignment = MenuAlignment.END,
                iconPosition = IconPosition.END,
            ) {
                FilledTonalIconButton(
                    onClick = {
                        if (text.isNotBlank() || isActivityOngoing) onToggleStartStop()
                    },
                    modifier = Modifier.size(if (uiConfig.isCompactPhone) 40.dp else 44.dp),
                    colors =
                        IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor =
                                when {
                                    isActivityOngoing && text.isNotBlank() -> MaterialTheme.colorScheme.tertiaryContainer
                                    isActivityOngoing -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.primaryContainer
                                },
                            contentColor =
                                when {
                                    isActivityOngoing && text.isNotBlank() -> MaterialTheme.colorScheme.onTertiaryContainer
                                    isActivityOngoing -> MaterialTheme.colorScheme.onErrorContainer
                                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                                },
                        ),
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = description,
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { moreMenuExpanded = true },
                    modifier = Modifier.size(if (uiConfig.isCompactPhone) 36.dp else 40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Додаткові дії",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = moreMenuExpanded,
                    onDismissRequest = { moreMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Події") },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                        onClick = {
                            moreMenuExpanded = false
                            if (text.isNotBlank()) onQuickDoneClick(text)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Коментар") },
                        leadingIcon = { Icon(Icons.Default.AddComment, contentDescription = null) },
                        onClick = {
                            moreMenuExpanded = false
                            onTimelessClick()
                        },
                    )
                }
            }
        }
    }
}

private fun exportLogToMarkdown(log: List<ActivityRecord>): String {
    val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
    val groupedByDate = log.sortedByDescending { it.createdAt }.groupBy { toDateHeader(it.createdAt) }
    return buildString {
        groupedByDate.forEach { (dateHeader, records) ->
            append("## $dateHeader\n\n")
            val (timeless, timed) = records.partition { it.isTimeless }
            timeless.forEach { record ->
                append("- ${record.text}\n")
            }
            timed.forEach { record ->
                val timeText =
                    when {
                        record.isOngoing -> "`${sdfTime.format(Date(record.startTime!!))} - ...`"
                        else -> "`${sdfTime.format(Date(record.startTime!!))} - ${sdfTime.format(Date(record.endTime!!))}`"
                    }
                append("- $timeText ${record.text}\n".trim())
            }
            append("\n")
        }
    }
}

private fun copyToClipboard(
    context: Context,
    text: String,
) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Activity Log", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Лог скопійовано!", Toast.LENGTH_SHORT).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditRecordDialog(
    record: ActivityRecord,
    onDismiss: () -> Unit,
    onConfirm: (String, Long?, Long?, Int?, Int?) -> Unit,
    isLastTimedRecord: Boolean,
) {
    val initialType =
        when {
            record.isTimeless -> ActivityRecordType.COMMENT
            record.endTime != null && record.startTime == record.endTime -> ActivityRecordType.INSTANT
            else -> ActivityRecordType.TIMED
        }

    var text by remember(record) { mutableStateOf(record.text) }
    var startTime by remember(record) { mutableStateOf(record.startTime ?: record.createdAt) }
    var endTime by remember(record) { mutableStateOf(record.endTime) }
    var recordType by remember(record) { mutableStateOf(initialType) }
    var xpText by remember(record) { mutableStateOf(record.xpGained?.toString().orEmpty()) }
    var antyXpText by remember(record) { mutableStateOf(record.antyXp?.toString().orEmpty()) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val chipColors =
        FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редагувати запис") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = recordType == ActivityRecordType.COMMENT,
                        onClick = { recordType = ActivityRecordType.COMMENT },
                        label = { Text("Коментар") },
                        colors = chipColors,
                    )
                    FilterChip(
                        selected = recordType == ActivityRecordType.INSTANT,
                        onClick = { recordType = ActivityRecordType.INSTANT },
                        label = { Text("Минулі дії") },
                        colors = chipColors,
                    )
                    FilterChip(
                        selected = recordType == ActivityRecordType.TIMED,
                        onClick = { recordType = ActivityRecordType.TIMED },
                        label = { Text("З часом") },
                        colors = chipColors,
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Текст запису") },
                )
                if (recordType != ActivityRecordType.COMMENT) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = { showStartTimePicker = true },
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .heightIn(min = 44.dp),
                        ) {
                            Text(startTime?.let { timeFormatter.format(Date(it)) } ?: "Start")
                        }
                        if (recordType == ActivityRecordType.TIMED) {
                            Text("-")
                            OutlinedButton(
                                onClick = { showEndTimePicker = true },
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .heightIn(min = 44.dp),
                                enabled = !record.isOngoing,
                            ) {
                                Text(endTime?.let { timeFormatter.format(Date(it)) } ?: "Зараз")
                            }
                            if (isLastTimedRecord && endTime != null) {
                                IconButton(onClick = { endTime = null }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Зробити поточним")
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = xpText,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() } || value.isBlank()) xpText = value
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Отримана експа (опціонально)") },
                    singleLine = true,
                    placeholder = { Text("0") },
                )
                OutlinedTextField(
                    value = antyXpText,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() } || value.isBlank()) antyXpText = value
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Втрачена експа (опціонально)") },
                    singleLine = true,
                    placeholder = { Text("0") },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val actualStart =
                        when (recordType) {
                            ActivityRecordType.COMMENT -> null
                            ActivityRecordType.INSTANT -> startTime ?: record.createdAt
                            ActivityRecordType.TIMED -> startTime ?: record.createdAt
                        }
                    val actualEnd =
                        when (recordType) {
                            ActivityRecordType.COMMENT -> null
                            ActivityRecordType.INSTANT -> actualStart
                            ActivityRecordType.TIMED -> endTime
                        }
                    val isTimeInvalid =
                        recordType == ActivityRecordType.TIMED &&
                            actualStart != null &&
                            actualEnd != null &&
                            actualEnd < actualStart
                    if (isTimeInvalid) {
                        Toast.makeText(context, "Час закінчення не може бути раніше часу початку", Toast.LENGTH_SHORT).show()
                    } else {
                        val xp = xpText.toIntOrNull()
                        val antyXp = antyXpText.toIntOrNull()
                        onConfirm(text, actualStart, actualEnd, xp, antyXp)
                    }
                },
                enabled = text.isNotBlank(),
            ) {
                Text("Зберегти")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
    )

    if (showStartTimePicker) {
        TimePickerDialog(
            initialTime = startTime ?: System.currentTimeMillis(),
            onDismiss = { showStartTimePicker = false },
            onConfirm = { newTime ->
                startTime = newTime
                showStartTimePicker = false
            },
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            initialTime = endTime ?: System.currentTimeMillis(),
            onDismiss = { showEndTimePicker = false },
            onConfirm = { newTime ->
                endTime = newTime
                showEndTimePicker = false
            },
        )
    }
}
