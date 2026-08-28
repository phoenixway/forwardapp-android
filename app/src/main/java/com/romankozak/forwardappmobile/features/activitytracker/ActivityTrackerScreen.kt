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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityEntityLink
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityRecordKind
import com.romankozak.forwardappmobile.core.data.models.entities.Reminder
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.core.navigation.NavTargetRouter
import com.romankozak.forwardappmobile.core.navigation.routes.NavigationRoutes
import com.romankozak.forwardappmobile.features.activitytracker.dialogs.TimePickerDialog
import com.romankozak.forwardappmobile.features.activitytracker.entities.ActivityEntityDescriptor
import com.romankozak.forwardappmobile.features.activitytracker.entities.ActivityEntityLinksEditor
import com.romankozak.forwardappmobile.features.activitytracker.entities.effectiveEntityLinks
import com.romankozak.forwardappmobile.features.reminders.components.DateTimePickerDialog
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Button
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Controller
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Overlay
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenuItem
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.IconPosition
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.MenuAlignment
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.rememberHoldMenu2
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.AutocompleteSuggestions
import com.romankozak.forwardappmobile.features.reminders.dialogs.ReminderPropertiesDialog
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

private val HashTagRegex = Regex("""(^|\s)(#[\p{L}\p{N}_-]+)""")
private const val JournalTagAnnotation = "journal_tag"

private data class ScrollRestoreAnchor(
    val key: Any,
    val offset: Int,
)

internal data class JournalUiConfig(
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
    val cornerRadius: Dp,
    val metadataSpacing: Dp,
    val textMaxLines: Int,
    val maxVisibleTags: Int,
    val inputMinHeight: Dp,
    val inputBarPadding: Dp,
    val pillHorizontalPadding: Dp,
)

@Composable
private fun rememberJournalUiConfig(): JournalUiConfig {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    return remember(screenWidthDp) {
        if (screenWidthDp < 390) {
            JournalUiConfig(
                isCompactPhone = true,
                listHorizontalPadding = 10.dp,
                listVerticalPadding = 6.dp,
                itemSpacing = 6.dp,
                headerBottomSpacing = 6.dp,
                sectionSpacing = 18.dp,
                entryHorizontalPadding = 12.dp,
                entryVerticalPadding = 8.dp,
                sectionHorizontalPadding = 6.dp,
                sectionVerticalPadding = 3.dp,
                dayHeaderBackgroundHorizontalPadding = 8.dp,
                cornerRadius = 14.dp,
                metadataSpacing = 4.dp,
                textMaxLines = 2,
                maxVisibleTags = 2,
                inputMinHeight = 42.dp,
                inputBarPadding = 8.dp,
                pillHorizontalPadding = 7.dp,
            )
        } else {
            JournalUiConfig(
                isCompactPhone = false,
                listHorizontalPadding = 12.dp,
                listVerticalPadding = 8.dp,
                itemSpacing = 8.dp,
                headerBottomSpacing = 8.dp,
                sectionSpacing = 22.dp,
                entryHorizontalPadding = 14.dp,
                entryVerticalPadding = 9.dp,
                sectionHorizontalPadding = 8.dp,
                sectionVerticalPadding = 4.dp,
                dayHeaderBackgroundHorizontalPadding = 9.dp,
                cornerRadius = 16.dp,
                metadataSpacing = 5.dp,
                textMaxLines = 3,
                maxVisibleTags = 2,
                inputMinHeight = 46.dp,
                inputBarPadding = 10.dp,
                pillHorizontalPadding = 8.dp,
            )
        }
    }
}

private val ActivityRecord.isDaySummary: Boolean
    get() = this.recordKind == ActivityRecordKind.DAY_SUMMARY

private enum class ActivityRecordType { COMMENT, TIMED, INSTANT, DAY_SUMMARY }

@Composable
fun ActivityTrackerScreen(
    navController: NavController,
    viewModel: ActivityTrackerViewModel = hiltViewModel(),
    dayPlanId: String? = null,
    showTopBar: Boolean = true,
    showInputBar: Boolean = true,
) {
    val groupedByDate by viewModel.groupedActivityLog.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val tagSuggestions by viewModel.tagSuggestions.collectAsStateWithLifecycle()
    val entityCatalog by viewModel.entityCatalog.collectAsStateWithLifecycle()
    val availableEntities by viewModel.availableEntities.collectAsStateWithLifecycle()
    val pendingEntityLinks by viewModel.pendingEntityLinks.collectAsStateWithLifecycle()
    val lastOngoingActivity by viewModel.lastOngoingActivity.collectAsStateWithLifecycle()
    val editingRecord by viewModel.editingRecord.collectAsStateWithLifecycle()
    val recordToDelete by viewModel.recordToDelete.collectAsStateWithLifecycle()
    val isEditingLastTimedRecord by viewModel.isEditingLastTimedRecord.collectAsStateWithLifecycle()
    val recordForReminder by viewModel.recordForReminder.collectAsStateWithLifecycle()
    val isLoadingOlderRecords by viewModel.isLoadingOlderRecords.collectAsStateWithLifecycle()
    val hasMoreOlderRecords by viewModel.hasMoreOlderRecords.collectAsStateWithLifecycle()
    val activeElapsedState = rememberActiveElapsedState(lastOngoingActivity)
    var liveEntryVisibility by
        remember(lastOngoingActivity?.id) {
            mutableStateOf(JournalLiveEntryVisibility.UNKNOWN)
        }
    var scrollToLiveEntryRequest by remember { mutableIntStateOf(0) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTag by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(dayPlanId) {
        viewModel.setDayPlanScope(dayPlanId)
    }

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
    var backdatedDraft by remember { mutableStateOf<BackdatedActivityDraft?>(null) }
    val holdMenuController = rememberHoldMenu2()

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = journalBackgroundColor(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar =
                if (showTopBar) {
                    {
                        ActivityTrackerTopAppBar(
                            onNavigateBack = { navController.popBackStack() },
                            onReflectionRequest = { navController.navigate(NavigationRoutes.TIME_REFLECTION) },
                            onClearLogRequest = { showClearConfirmDialog = true },
                            onExportRequest = {
                                val markdown = exportLogToMarkdown(groupedByDate.values.flatten())
                                copyToClipboard(context, markdown)
                            },
                        )
                    }
                } else {
                    {}
                },
            bottomBar = {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding(),
                ) {
                    ActiveTrackingStickyStrip(
                        activity = lastOngoingActivity,
                        elapsedState = activeElapsedState,
                        liveEntryVisibility = liveEntryVisibility,
                        onOpen = {
                            selectedTag = null
                            scrollToLiveEntryRequest++
                        },
                        onStop = viewModel::onStopTracking,
                    )
                    if (showInputBar) {
                        ActivityInputBar(
                            text = inputText,
                            tagSuggestions = tagSuggestions,
                            selectedEntityLinks = pendingEntityLinks,
                            entityOptions = availableEntities,
                            isActivityOngoing = lastOngoingActivity != null,
                            onTextChange = viewModel::onInputTextChanged,
                            onTagSuggestionClick = viewModel::onTagSuggestionSelected,
                            onEntityLinksChanged = viewModel::onPendingEntityLinksChanged,
                            onToggleStartStop = viewModel::onToggleStartStop,
                            onTimelessClick = viewModel::onTimelessRecordClick,
                            onQuickDoneClick = { textValue -> quickDoneDialogState = textValue },
                            onBackdatedClick = { textValue ->
                                backdatedDraft = BackdatedActivityDraft(textValue, entityLinks = pendingEntityLinks)
                            },
                            onDaySummaryClick = viewModel::onAddTodaySummary,
                            holdMenuController = holdMenuController,
                        )
                    }
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
                onAddBackdated = { record ->
                    backdatedDraft =
                        BackdatedActivityDraft(
                            text = record.text,
                            entityLinks = record.effectiveEntityLinks(),
                        )
                },
                onSearchTagGlobally = { tag ->
                    val encoded = URLEncoder.encode(tag, "UTF-8")
                    navController.navigate(NavTargetRouter.routeOf(NavTarget.GlobalSearch(query = encoded)))
                },
                entityCatalog = entityCatalog,
                activeActivity = lastOngoingActivity,
                activeElapsedState = activeElapsedState,
                onStopActive = viewModel::onStopTracking,
                onLiveEntryVisibilityChanged = { liveEntryVisibility = it },
                scrollToLiveEntryRequest = scrollToLiveEntryRequest,
            )

            editingRecord?.let { recordToEdit ->
                EditRecordDialog(
                    record = recordToEdit,
                    onDismiss = viewModel::onEditDialogDismiss,
                    onConfirm = viewModel::onRecordUpdated,
                    isLastTimedRecord = isEditingLastTimedRecord,
                    entityOptions = availableEntities,
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
                        viewModel.clearActivityComposer()
                        quickDoneDialogState = null
                    },
                )
            }

            backdatedDraft?.let { draft ->
                BackdatedActivityDialog(
                    draft = draft,
                    entityOptions = availableEntities,
                    onDismiss = { backdatedDraft = null },
                    onConfirm = { text, startTime, endTime, links ->
                        viewModel.onAddBackdatedActivity(text, startTime, endTime, links)
                        backdatedDraft = null
                    },
                )
            }
        }

        if (showInputBar) {
            HoldMenu2Overlay(controller = holdMenuController)
        }
    }
}

@Composable
private fun ActivityTrackerTopAppBar(
    onNavigateBack: () -> Unit,
    onReflectionRequest: () -> Unit,
    onClearLogRequest: () -> Unit,
    onExportRequest: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    TopAppBar(
        title = { Text("Life Journal") },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor =
                    lerp(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.colorScheme.primaryContainer,
                        0.14f,
                    ),
                scrolledContainerColor =
                    lerp(
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.colorScheme.primaryContainer,
                        0.18f,
                    ),
            ),
        navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
        actions = {
            IconButton(onClick = { menuExpanded = true }) { Icon(Icons.Default.MoreVert, "Меню") }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Reflection") },
                    leadingIcon = { Icon(Icons.Default.Insights, contentDescription = null) },
                    onClick = {
                        onReflectionRequest()
                        menuExpanded = false
                    },
                )
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
    entityCatalog: List<ActivityEntityDescriptor>,
    onAddBackdated: (ActivityRecord) -> Unit,
    activeActivity: ActivityRecord?,
    activeElapsedState: State<Long>,
    onStopActive: () -> Unit,
    onLiveEntryVisibilityChanged: (JournalLiveEntryVisibility) -> Unit,
    scrollToLiveEntryRequest: Int,
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
    ObserveLiveEntryVisibility(
        lazyListState = lazyListState,
        activeKey = activeActivity?.id,
        knownItemKeys = listKeys,
        onVisibilityChanged = onLiveEntryVisibilityChanged,
    )
    ScrollToLiveEntryEffect(
        lazyListState = lazyListState,
        activeKey = activeActivity?.id,
        knownItemKeys = listKeys,
        request = scrollToLiveEntryRequest,
    )

    LaunchedEffect(listKeys, groupedByDate.isNotEmpty()) {
        if (groupedByDate.isNotEmpty() && !didInitialScroll) {
            val lastIndex = listKeys.lastIndex.coerceAtLeast(0)
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
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(journalDayHeaderColor())
                                .padding(top = 2.dp),
                    ) {
                        JournalDayHeader(
                            dateHeader = dateHeader,
                            entryCount = records.size,
                            uiConfig = uiConfig,
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 3.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                        )
                    }
                }
                item(key = "day_header_gap_$dateHeader") {
                    Spacer(modifier = Modifier.height(uiConfig.headerBottomSpacing))
                }
                val sortedRecords = sortRecordsForDay(records)
                itemsIndexed(sortedRecords, key = { _, record -> record.id }) { recordIndex, record ->
                    JournalEntryCard(
                        record = record,
                        onEdit = onEdit,
                        onRestart = onRestart,
                        onDelete = onDelete,
                        onSetReminder = onSetReminder,
                        onTagClick = onTagSelected,
                        uiConfig = uiConfig,
                        entityCatalog = entityCatalog,
                        onAddBackdated = onAddBackdated,
                        isActive = record.id == activeActivity?.id && record.isOngoing,
                        activeElapsedState = activeElapsedState,
                        onStopActive = onStopActive,
                    )
                    if (recordIndex < sortedRecords.lastIndex) {
                        Spacer(modifier = Modifier.height(uiConfig.itemSpacing))
                    }
                }
            }
        }
    }
}

private fun sortRecordsForDay(records: List<ActivityRecord>): List<ActivityRecord> =
    records.sortedWith(
        compareByDescending<ActivityRecord> { it.isDaySummary }
            .thenBy { it.startTime ?: it.createdAt },
    )

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
        keys.addAll(sortRecordsForDay(records).map { it.id })
    }
    return keys
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCompletedActionDialog(
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
    entityCatalog: List<ActivityEntityDescriptor>,
    onAddBackdated: (ActivityRecord) -> Unit,
    isActive: Boolean,
    activeElapsedState: State<Long>,
    onStopActive: () -> Unit,
) {
    val annotatedText = rememberJournalEntryAnnotatedString(record.text)
    var menuExpanded by remember(record.id) { mutableStateOf(false) }
    val tonalCardBase =
        lerp(
            MaterialTheme.colorScheme.surfaceContainerLow,
            MaterialTheme.colorScheme.surfaceContainerHigh,
            0.58f,
        )
    val cardColor =
        lerp(
            tonalCardBase,
            MaterialTheme.colorScheme.primaryContainer,
            0.07f,
        )
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
                            .then(
                                if (isActive) {
                                    Modifier.semantics {
                                        stateDescription = "Активна активність, відстеження триває"
                                    }
                                } else {
                                    Modifier
                                },
                            )
                            .combinedClickable(
                                onClick = { onEdit(record) },
                                onLongClick = { menuExpanded = true },
                            ),
                    shape = RoundedCornerShape(uiConfig.cornerRadius),
                    color = cardColor,
                    tonalElevation = 0.dp,
                    shadowElevation = 1.dp,
                ) {
                    Box {
                        if (isActive) {
                            Box(modifier = Modifier.matchParentSize()) {
                                Box(
                                    modifier =
                                        Modifier
                                            .align(Alignment.CenterStart)
                                            .fillMaxHeight()
                                            .width(3.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.58f),
                                                RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp),
                                            ),
                                )
                            }
                        }
                        Column(
                            modifier =
                                Modifier.padding(
                                    horizontal = uiConfig.entryHorizontalPadding,
                                    vertical = uiConfig.entryVerticalPadding,
                                ),
                            verticalArrangement = Arrangement.spacedBy(uiConfig.metadataSpacing),
                        ) {
                            if (isActive) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    ActiveTrackingDot()
                                    JournalEntryText(
                                        text = annotatedText,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        modifier = Modifier.weight(1f),
                                        onTagClick = onTagClick,
                                        onTextClick = { onEdit(record) },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            } else {
                                JournalEntryText(
                                    text = annotatedText,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    onTagClick = onTagClick,
                                    onTextClick = { onEdit(record) },
                                    maxLines = uiConfig.textMaxLines,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            JournalMetadataRow(
                                record = record,
                                onTagClick = onTagClick,
                                uiConfig = uiConfig,
                                entityCatalog = entityCatalog,
                                activeElapsedState = if (isActive) activeElapsedState else null,
                                onStopActive = onStopActive,
                            )
                        }
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
            DropdownMenuItem(
                text = { Text("Додати ще часу") },
                leadingIcon = { Icon(Icons.Default.MoreTime, contentDescription = null) },
                onClick = {
                    menuExpanded = false
                    onAddBackdated(record)
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
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
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
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f),
            shape = RoundedCornerShape(999.dp),
        ) {
            Text(
                text = recordCountLabel(entryCount),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier.padding(
                        horizontal = uiConfig.dayHeaderBackgroundHorizontalPadding,
                        vertical = 3.dp,
                    ),
            )
        }
    }
}

@Composable
private fun journalBackgroundColor(): Color =
    lerp(
        MaterialTheme.colorScheme.background,
        MaterialTheme.colorScheme.primaryContainer,
        0.04f,
    )

@Composable
private fun journalDayHeaderColor(): Color =
    lerp(
        journalBackgroundColor(),
        MaterialTheme.colorScheme.surfaceContainerLow,
        0.58f,
    )

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

private fun recordHasTag(
    text: String,
    tag: String,
): Boolean = extractTags(text).any { it.equals(tag, ignoreCase = true) }

private fun extractTags(text: String): List<String> = extractActivityTags(text)

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
fun ActivityInputBar(
    text: String,
    tagSuggestions: List<String> = emptyList(),
    selectedEntityLinks: List<ActivityEntityLink> = emptyList(),
    entityOptions: List<ActivityEntityDescriptor> = emptyList(),
    isActivityOngoing: Boolean,
    onTextChange: (String) -> Unit,
    onTagSuggestionClick: (String) -> Unit = {},
    onEntityLinksChanged: (List<ActivityEntityLink>) -> Unit = {},
    onToggleStartStop: () -> Unit,
    onTimelessClick: () -> Unit,
    onQuickDoneClick: (String) -> Unit,
    onBackdatedClick: (String) -> Unit = {},
    onDaySummaryClick: (String) -> Unit,
    holdMenuController: HoldMenu2Controller,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val uiConfig = rememberJournalUiConfig()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color =
            lerp(
                MaterialTheme.colorScheme.surfaceContainerLow,
                MaterialTheme.colorScheme.primaryContainer,
                0.04f,
            ),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
    ) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            AutocompleteSuggestions(
                suggestions = tagSuggestions,
                onSuggestionClick = onTagSuggestionClick,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier =
                    Modifier.padding(
                        horizontal = uiConfig.inputBarPadding,
                        vertical = 8.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(if (uiConfig.isCompactPhone) 15.dp else 17.dp),
                    color =
                        lerp(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.colorScheme.primaryContainer,
                            0.10f,
                        ),
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
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
                            HoldMenuItem(label = "Подія", icon = Icons.Default.CheckCircle),
                            HoldMenuItem(label = "Минула активність", icon = Icons.Default.MoreTime),
                            HoldMenuItem(label = "Коментар", icon = Icons.Default.AddComment),
                            HoldMenuItem(label = "Резюме дня", icon = Icons.Default.Summarize),
                        )
                    }
                val onMenuSelect: (Int) -> Unit = { index ->
                    when (index) {
                        0 -> if (text.isNotBlank()) onQuickDoneClick(text)
                        1 -> if (text.isNotBlank()) onBackdatedClick(text)
                        2 -> onTimelessClick()
                        3 -> if (text.isNotBlank()) onDaySummaryClick(text)
                    }
                }

                if (!isActivityOngoing || text.isNotBlank()) {
                    val icon: ImageVector
                    val tint: Color
                    val description: String

                    if (isActivityOngoing) {
                        icon = Icons.Default.Sync
                        tint = MaterialTheme.colorScheme.tertiary
                        description = "Зупинити поточну та почати нову"
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
                            onClick = onToggleStartStop,
                            modifier = Modifier.size(if (uiConfig.isCompactPhone) 40.dp else 44.dp),
                            colors =
                                IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor =
                                        if (isActivityOngoing) {
                                            MaterialTheme.colorScheme.tertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.primaryContainer
                                        },
                                    contentColor =
                                        if (isActivityOngoing) {
                                            MaterialTheme.colorScheme.onTertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onPrimaryContainer
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
                }
            }
            ActivityInputActionsRow(
                text = text,
                selectedEntityLinks = selectedEntityLinks,
                entityOptions = entityOptions,
                onEntityLinksChanged = onEntityLinksChanged,
                onQuickDoneClick = onQuickDoneClick,
                onBackdatedClick = onBackdatedClick,
                onTimelessClick = onTimelessClick,
                onDaySummaryClick = onDaySummaryClick,
                trailingContent = trailingContent,
            )
        }
    }
}

fun exportLogToMarkdown(log: List<ActivityRecord>): String {
    val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
    val groupedByDate = log.sortedByDescending { it.createdAt }.groupBy { toDateHeader(it.createdAt) }
    return buildString {
        groupedByDate.forEach { (dateHeader, records) ->
            append("## $dateHeader\n\n")
            val summaries = records.filter { it.isDaySummary }
            if (summaries.isNotEmpty()) {
                append("### Summary\n\n")
                summaries.forEach { record ->
                    append("- ${record.text}\n")
                }
                append("\n")
            }
            append("### Timeline\n\n")
            val (timeless, timed) = records.filterNot { it.isDaySummary }.partition { it.isTimeless }
            timeless.forEach { record ->
                append("- ${record.text}\n")
            }
            timed.forEach { record ->
                val timeText =
                    when {
                        record.startTime == null -> "`${sdfTime.format(Date(record.createdAt))}`"
                        record.isOngoing -> "`${sdfTime.format(Date(record.startTime!!))} - ...`"
                        record.startTime == record.endTime -> "`${sdfTime.format(Date(record.startTime!!))}`"
                        else -> "`${sdfTime.format(Date(record.startTime!!))} - ${sdfTime.format(Date(record.endTime!!))}`"
                    }
                append("- $timeText ${record.text}\n".trim())
            }
            append("\n")
        }
    }
}

fun copyToClipboard(
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
    onConfirm: (String, String, Long?, Long?, Int?, Int?, List<ActivityEntityLink>) -> Unit,
    isLastTimedRecord: Boolean,
    entityOptions: List<ActivityEntityDescriptor>,
) {
    val initialType =
        when {
            record.recordKind == ActivityRecordKind.DAY_SUMMARY -> ActivityRecordType.DAY_SUMMARY
            record.recordKind == ActivityRecordKind.COMMENT -> ActivityRecordType.COMMENT
            record.recordKind == ActivityRecordKind.EVENT -> ActivityRecordType.INSTANT
            record.endTime != null && record.startTime == record.endTime -> ActivityRecordType.INSTANT
            else -> ActivityRecordType.TIMED
        }

    var text by remember(record) { mutableStateOf(record.text) }
    var startTime by remember(record) { mutableStateOf<Long?>(record.startTime ?: record.createdAt) }
    var endTime by remember(record) { mutableStateOf(record.endTime) }
    var recordType by remember(record) { mutableStateOf(initialType) }
    var xpText by remember(record) { mutableStateOf(record.xpGained?.toString().orEmpty()) }
    var antyXpText by remember(record) { mutableStateOf(record.antyXp?.toString().orEmpty()) }
    var entityLinks by remember(record) { mutableStateOf(record.effectiveEntityLinks()) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val currentStartTime = startTime
    val currentEndTime = endTime
    val isTimeInvalid =
        recordType == ActivityRecordType.TIMED &&
            currentStartTime != null &&
            currentEndTime != null &&
            currentEndTime < currentStartTime
    val chipColors =
        FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редагувати запис") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
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
                        selected = recordType == ActivityRecordType.DAY_SUMMARY,
                        onClick = { recordType = ActivityRecordType.DAY_SUMMARY },
                        label = { Text("Резюме дня") },
                        colors = chipColors,
                    )
                    FilterChip(
                        selected = recordType == ActivityRecordType.INSTANT,
                        onClick = { recordType = ActivityRecordType.INSTANT },
                        label = { Text("Подія") },
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
                if (recordType != ActivityRecordType.COMMENT && recordType != ActivityRecordType.DAY_SUMMARY) {
                    ActivityRecordTimeEditor(
                        startTime = startTime ?: record.createdAt,
                        endTime = endTime,
                        isTimed = recordType == ActivityRecordType.TIMED,
                        canRemainOngoing = isLastTimedRecord,
                        onStartClick = { showStartTimePicker = true },
                        onEndClick = { showEndTimePicker = true },
                        onMakeOngoing = { endTime = null },
                    )
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
                ActivityEntityLinksEditor(
                    selectedLinks = entityLinks,
                    options = entityOptions,
                    onLinksChanged = { entityLinks = it },
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
                            ActivityRecordType.DAY_SUMMARY -> null
                            ActivityRecordType.INSTANT -> startTime ?: record.createdAt
                            ActivityRecordType.TIMED -> startTime ?: record.createdAt
                        }
                    val actualEnd =
                        when (recordType) {
                            ActivityRecordType.COMMENT -> null
                            ActivityRecordType.DAY_SUMMARY -> null
                            ActivityRecordType.INSTANT -> actualStart
                            ActivityRecordType.TIMED -> endTime
                        }
                    val recordKind =
                        when (recordType) {
                            ActivityRecordType.COMMENT -> ActivityRecordKind.COMMENT
                            ActivityRecordType.DAY_SUMMARY -> ActivityRecordKind.DAY_SUMMARY
                            ActivityRecordType.INSTANT -> ActivityRecordKind.EVENT
                            ActivityRecordType.TIMED -> ActivityRecordKind.TIMED_ACTIVITY
                        }
                    val xp = xpText.toIntOrNull()
                    val antyXp = antyXpText.toIntOrNull()
                    onConfirm(text, recordKind, actualStart, actualEnd, xp, antyXp, entityLinks)
                },
                enabled = text.isNotBlank() && !isTimeInvalid,
            ) {
                Text("Зберегти")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } },
    )

    if (showStartTimePicker) {
        DateTimePickerDialog(
            initialDateTime = startTime ?: System.currentTimeMillis(),
            onDismiss = { showStartTimePicker = false },
            onConfirm = { newTime ->
                startTime = newTime
                showStartTimePicker = false
            },
            enablePastValues = true,
            title = if (recordType == ActivityRecordType.INSTANT) "Час події" else "Початок активності",
            summaryLabel = "Обраний час",
        )
    }

    if (showEndTimePicker) {
        DateTimePickerDialog(
            initialDateTime = endTime ?: System.currentTimeMillis(),
            onDismiss = { showEndTimePicker = false },
            onConfirm = { newDateTime ->
                endTime = newDateTime
                showEndTimePicker = false
            },
            enablePastValues = true,
            title = "Завершення активності",
            summaryLabel = "Обраний час",
        )
    }
}
