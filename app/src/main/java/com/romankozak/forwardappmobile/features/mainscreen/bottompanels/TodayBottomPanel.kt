package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayFocusType
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.TaskExecutionStrictness
import com.romankozak.forwardappmobile.core.theme.LocalInputPanelColors
import com.romankozak.forwardappmobile.features.activitytracker.ActivityTrackerViewModel
import com.romankozak.forwardappmobile.features.activitytracker.ActivityInputBar
import com.romankozak.forwardappmobile.features.activitytracker.QuickCompletedActionDialog
import com.romankozak.forwardappmobile.features.activitytracker.copyToClipboard
import com.romankozak.forwardappmobile.features.activitytracker.exportLogToMarkdown
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.InputSuggestionActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.AutocompleteSuggestions
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Overlay
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.rememberHoldMenu2
import com.romankozak.forwardappmobile.features.daymanagement.runtime.presentation.DayManagementRuntimeUiState
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus.DayFocusDialogMode
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus.DayFocusItemEditorSheet
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayfocus.DayFocusesViewModel
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayPlanViewModel
import com.romankozak.forwardappmobile.features.missions.presentation.LinkPickerTab
import com.romankozak.forwardappmobile.features.missions.presentation.LinkedTargetsPickerDialog
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption
import com.romankozak.forwardappmobile.features.mainscreen.CommandDeckMoreActionButton
import com.romankozak.forwardappmobile.features.mainscreen.MoreSheetAction
import com.romankozak.forwardappmobile.features.recent.RecentViewModel
import com.romankozak.forwardappmobile.ui.components.CommonBottomPanelLayout
import java.util.Locale

@Composable
@Suppress("LongParameterList", "LongMethod")
fun TodayBottomPanel(
    onNavigateToProjectHierarchy: () -> Unit,
    onShowContextMarkersSheet: () -> Unit,
    onNavigateToPresets: () -> Unit,
    onNavigateToGlobalSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToTracker: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onNavigateToAiInsights: () -> Unit,
    onNavigateToAiLifeManagement: () -> Unit,
    onExportToFile: () -> Unit,
    onImportFromFileRequest: (Uri) -> Unit,
    onSelectiveImportFromFileRequest: (Uri) -> Unit,
    onExportAttachments: () -> Unit,
    onImportAttachmentsFromFileRequest: (Uri) -> Unit,
    onWifiPush: (String) -> Unit,
    onShowWifiServer: () -> Unit,
    onShowWifiImport: () -> Unit,
    onNavigateToAttachments: () -> Unit,
    onNavigateToScripts: () -> Unit,
    onShowAbout: () -> Unit,
    currentTab: DayManagementTab,
    onSelectTodayTab: (DayManagementTab) -> Unit,
    runtimeUiState: DayManagementRuntimeUiState,
    onWakeUp: () -> Unit,
    onFinalizeFocus: () -> Unit,
    onFinalizePlan: () -> Unit,
    onStartImplementation: () -> Unit,
    onStartFinalization: () -> Unit,
    onSleep: () -> Unit,
    featureToggles: Map<FeatureFlag, Boolean>,
    onNavigateToRecentItem: (RecentItem) -> Unit,
    recentViewModel: RecentViewModel = hiltViewModel(),
    dayPlanViewModel: DayPlanViewModel = hiltViewModel(),
    activityTrackerViewModel: ActivityTrackerViewModel = hiltViewModel(),
    dayFocusesViewModel: DayFocusesViewModel = hiltViewModel(),
) {
    @Suppress("UNUSED_VARIABLE")
    val unusedInputs =
        listOf(
            onNavigateToGlobalSearch,
            onNavigateToInbox,
            onNavigateToTracker,
            onNavigateToRecentItem,
            recentViewModel,
        )
    val dayPlanUiState by dayPlanViewModel.uiState.collectAsStateWithLifecycle()
    val allTags by dayPlanViewModel.allTags.collectAsStateWithLifecycle()
    val contextMarkerNames by dayPlanViewModel.contextMarkerNames.collectAsStateWithLifecycle()
    val groupedActivityLog by activityTrackerViewModel.groupedActivityLog.collectAsStateWithLifecycle()
    val activityInputText by activityTrackerViewModel.inputText.collectAsStateWithLifecycle()
    val lastOngoingActivity by activityTrackerViewModel.lastOngoingActivity.collectAsStateWithLifecycle()
    val dayFocusesUiState by dayFocusesViewModel.uiState.collectAsStateWithLifecycle()
    val panelStyle = LocalInputPanelColors.current.addGoal
    val inputSuggestionActions = remember { InputSuggestionActions() }
    val context = androidx.compose.ui.platform.LocalContext.current
    var inputValue by remember { mutableStateOf(TextFieldValue("")) }
    var showContextPicker by remember { mutableStateOf(false) }
    var showClearJournalConfirmDialog by remember { mutableStateOf(false) }
    var journalQuickDoneDialogState by remember { mutableStateOf<String?>(null) }
    val journalHoldMenuController = rememberHoldMenu2()

    LaunchedEffect(dayPlanUiState.dayPlan?.id) {
        dayPlanUiState.dayPlan?.id?.let(dayFocusesViewModel::loadDataForPlan)
    }
    val autocompleteSuggestions =
        remember(inputValue, allTags, contextMarkerNames) {
            inputSuggestionActions.buildSuggestions(
                currentText = inputValue.text,
                cursorPosition = inputValue.selection.start.coerceAtLeast(0),
                contextMarkerNames = contextMarkerNames,
                tags = allTags,
            )
        }
    val contextOptions =
        remember(dayPlanUiState.availableProjects) {
            dayPlanUiState.availableProjects.map { option ->
                ProjectOption(
                    id = option.id,
                    name = option.name,
                    parentId = null,
                )
            }
        }
    val additionalMoreActions =
        when (currentTab) {
            DayManagementTab.DAY_START ->
                listOf(
                    MoreSheetAction(label = "Проснувся!", onClick = onWakeUp),
                    MoreSheetAction(label = "Пішов спати", onClick = onSleep),
                )
            DayManagementTab.DAY_PLAN ->
                listOf(
                    MoreSheetAction(
                        label =
                            if (runtimeUiState.runtimeState.dayPlanFinalizedAt != null) {
                                "План дня зафіксовано"
                            } else {
                                "План дня готовий"
                            },
                        onClick = onFinalizePlan,
                    ),
                )
            DayManagementTab.DAY_FOCUSES ->
                listOf(
                    MoreSheetAction(
                        label =
                            if (runtimeUiState.runtimeState.dayFocusFinalizedAt != null) {
                                "Фокус дня зафіксований"
                            } else {
                                "Фокус дня зафіксувати"
                            },
                        onClick = onFinalizeFocus,
                    ),
                    MoreSheetAction(
                        label = "Додати фокус",
                        onClick = { dayFocusesViewModel.openCreateDialog(DayFocusType.FOCUS) },
                    ),
                    MoreSheetAction(
                        label = "Додати зону відповідальності",
                        onClick = { dayFocusesViewModel.openCreateDialog(DayFocusType.RESPONSIBILITY) },
                    ),
                )
            DayManagementTab.JOURNAL ->
                listOf(
                    MoreSheetAction(
                        label =
                            if (runtimeUiState.runtimeState.hasOpenOperationalDay) {
                                "Почати реалізацію"
                            } else {
                                "Стартувати день і реалізацію"
                            },
                        onClick = onStartImplementation,
                    ),
                    MoreSheetAction(
                        label = "Події",
                        onClick = {
                            if (activityInputText.isNotBlank()) {
                                journalQuickDoneDialogState = activityInputText
                            }
                        },
                    ),
                    MoreSheetAction(
                        label = "Коментар",
                        onClick = activityTrackerViewModel::onTimelessRecordClick,
                    ),
                    MoreSheetAction(
                        label = "Експорт в Markdown",
                        onClick = {
                            val markdown = exportLogToMarkdown(groupedActivityLog.values.flatten())
                            copyToClipboard(context, markdown)
                        },
                    ),
                    MoreSheetAction(
                        label = "Очистити лог",
                        onClick = { showClearJournalConfirmDialog = true },
                    ),
                )
            else -> emptyList()
        }

    fun submitTask() {
        val dayPlanId = dayPlanUiState.dayPlan?.id ?: return
        val taskDraft = buildTodayQuickTaskDraft(inputValue.text) ?: return

        dayPlanViewModel.addTask(
            dayPlanId = dayPlanId,
            title = taskDraft.title,
            description = taskDraft.description,
            duration = null,
            scheduledTime = null,
            dueTime = null,
            priority = TaskPriority.MEDIUM,
            strictness = TaskExecutionStrictness.NORMAL,
            recurrenceRule = null,
            points = 0,
        )
        inputValue = TextFieldValue("")
    }

    CommonBottomPanelLayout {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(28.dp),
            color = panelStyle.backgroundColor,
            border = BorderStroke(1.dp, panelStyle.textColor.copy(alpha = 0.1f)),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {}

                if (currentTab != DayManagementTab.JOURNAL && currentTab != DayManagementTab.DAY_FOCUSES) {
                    AutocompleteSuggestions(
                        suggestions = autocompleteSuggestions,
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
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (currentTab == DayManagementTab.JOURNAL) {
                    ActivityInputBar(
                        text = activityInputText,
                        isActivityOngoing = lastOngoingActivity != null,
                        onTextChange = activityTrackerViewModel::onInputTextChanged,
                        onToggleStartStop = activityTrackerViewModel::onToggleStartStop,
                        onTimelessClick = activityTrackerViewModel::onTimelessRecordClick,
                        onQuickDoneClick = { textValue -> journalQuickDoneDialogState = textValue },
                        onDaySummaryClick = activityTrackerViewModel::onAddTodaySummary,
                        holdMenuController = journalHoldMenuController,
                        showMoreMenu = false,
                        trailingContent = {
                            CommandDeckMoreActionButton(
                                onNavigateToProjectHierarchy = onNavigateToProjectHierarchy,
                                onShowContextMarkersSheet = onShowContextMarkersSheet,
                                onNavigateToReminders = onNavigateToReminders,
                                onNavigateToPresets = onNavigateToPresets,
                                onNavigateToAiChat = onNavigateToAiChat,
                                onNavigateToAiInsights = onNavigateToAiInsights,
                                onNavigateToAiLifeManagement = onNavigateToAiLifeManagement,
                                onNavigateToSettings = onNavigateToSettings,
                                onExportToFile = onExportToFile,
                                onImportFromFileRequest = onImportFromFileRequest,
                                onSelectiveImportFromFileRequest = onSelectiveImportFromFileRequest,
                                onExportAttachments = onExportAttachments,
                                onImportAttachmentsFromFileRequest = onImportAttachmentsFromFileRequest,
                                onWifiPush = onWifiPush,
                                onShowWifiServer = onShowWifiServer,
                                onShowWifiImport = onShowWifiImport,
                                onNavigateToAttachments = onNavigateToAttachments,
                                onNavigateToScripts = onNavigateToScripts,
                                onShowAbout = onShowAbout,
                                additionalActions = additionalMoreActions,
                                featureToggles = featureToggles,
                                modifier = Modifier.size(38.dp),
                            )
                        },
                    )
                } else {
                    TodayBottomPanelComposer(
                        inputValue = inputValue,
                        onValueChange = { inputValue = it },
                        onSubmit = {
                            when (currentTab) {
                                DayManagementTab.DAY_FOCUSES -> {
                                    dayFocusesViewModel.addQuickFocus(inputValue.text)
                                    inputValue = TextFieldValue("")
                                }
                                else -> submitTask()
                            }
                        },
                        panelStyle = panelStyle,
                        placeholderText =
                            if (currentTab == DayManagementTab.DAY_FOCUSES) {
                                "Новий фокус дня..."
                            } else {
                                "Нове завдання..."
                            },
                        trailingContent = {
                            CommandDeckMoreActionButton(
                                onNavigateToProjectHierarchy = onNavigateToProjectHierarchy,
                                onShowContextMarkersSheet = onShowContextMarkersSheet,
                                onNavigateToReminders = onNavigateToReminders,
                                onNavigateToPresets = onNavigateToPresets,
                                onNavigateToAiChat = onNavigateToAiChat,
                                onNavigateToAiInsights = onNavigateToAiInsights,
                                onNavigateToAiLifeManagement = onNavigateToAiLifeManagement,
                                onNavigateToSettings = onNavigateToSettings,
                                onExportToFile = onExportToFile,
                                onImportFromFileRequest = onImportFromFileRequest,
                                onSelectiveImportFromFileRequest = onSelectiveImportFromFileRequest,
                                onExportAttachments = onExportAttachments,
                                onImportAttachmentsFromFileRequest = onImportAttachmentsFromFileRequest,
                                onWifiPush = onWifiPush,
                                onShowWifiServer = onShowWifiServer,
                                onShowWifiImport = onShowWifiImport,
                                onNavigateToAttachments = onNavigateToAttachments,
                                onNavigateToScripts = onNavigateToScripts,
                                onShowAbout = onShowAbout,
                                additionalActions = additionalMoreActions,
                                featureToggles = featureToggles,
                                modifier = Modifier.size(38.dp),
                            )
                        },
                    )
                }

                TodaySubTabs(
                    selectedTab = currentTab,
                    onTabSelected = onSelectTodayTab,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                )
                TodayBottomPanelRuntimeActions(
                    currentTab = currentTab,
                    runtimeUiState = runtimeUiState,
                    onWakeUp = onWakeUp,
                    onFinalizeFocus = onFinalizeFocus,
                    onFinalizePlan = onFinalizePlan,
                    onStartImplementation = onStartImplementation,
                    onStartFinalization = onStartFinalization,
                    onSleep = onSleep,
                )
            }
        }
    }

    if (showContextPicker) {
        LinkedTargetsPickerDialog(
            contextOptions = contextOptions,
            attachmentOptions = emptyList(),
            preselectedContextIds = emptySet(),
            preselectedAttachmentIds = emptySet(),
            initialTab = LinkPickerTab.CONTEXTS,
            allowedTabs = setOf(LinkPickerTab.CONTEXTS),
            onDismiss = { showContextPicker = false },
            onContextSelected = { contextId ->
                dayPlanViewModel.addTaskFromContext(contextId)
                showContextPicker = false
            },
            onAttachmentSelected = {},
            onCreateRootContext = null,
            onCreateDocument = null,
        )
    }

    if (showClearJournalConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearJournalConfirmDialog = false },
            title = { Text("Очистити лог?") },
            text = { Text("Ви впевнені, що хочете видалити всі записи? Цю дію неможливо буде скасувати.") },
            confirmButton = {
                Button(
                    onClick = {
                        activityTrackerViewModel.onClearLogConfirm()
                        showClearJournalConfirmDialog = false
                    },
                ) { Text("Видалити") }
            },
            dismissButton = {
                TextButton(onClick = { showClearJournalConfirmDialog = false }) {
                    Text("Скасувати")
                }
            },
        )
    }

    journalQuickDoneDialogState?.let { presetText ->
        QuickCompletedActionDialog(
            initialText = presetText,
            onDismiss = { journalQuickDoneDialogState = null },
            onConfirm = { desc, xp, antyXp ->
                activityTrackerViewModel.onAddCompletedAction(desc, xp, antyXp)
                activityTrackerViewModel.onInputTextChanged("")
                journalQuickDoneDialogState = null
            },
        )
    }

    if (currentTab == DayManagementTab.JOURNAL) {
        HoldMenu2Overlay(controller = journalHoldMenuController)
    }

    when (val dialogMode = dayFocusesUiState.dialogMode) {
        is DayFocusDialogMode.Create ->
            DayFocusItemEditorSheet(
                initialType = dialogMode.type,
                availableContexts = dayFocusesUiState.availableContexts,
                availableAttachments = dayFocusesUiState.availableAttachments,
                onDismiss = dayFocusesViewModel::dismissDialog,
                onConfirm = dayFocusesViewModel::saveItem,
                onCreateDocumentForPicker = dayFocusesViewModel::createDocumentForPicker,
            )

        is DayFocusDialogMode.Edit ->
            DayFocusItemEditorSheet(
                existingItem = dialogMode.item,
                initialType = dialogMode.item.type,
                availableContexts = dayFocusesUiState.availableContexts,
                availableAttachments = dayFocusesUiState.availableAttachments,
                onDismiss = dayFocusesViewModel::dismissDialog,
                onConfirm = dayFocusesViewModel::saveItem,
                onCreateDocumentForPicker = dayFocusesViewModel::createDocumentForPicker,
            )

        null -> Unit
    }

    dayFocusesUiState.pendingDeleteItem?.let { item ->
        AlertDialog(
            onDismissRequest = dayFocusesViewModel::dismissDeleteRequest,
            title = { Text("Видалити елемент?") },
            text = {
                Text(
                    if (item.isEveryday) {
                        "Це everyday-фокус. Видалити його з усіх днів чи тільки з сьогодні?"
                    } else {
                        "Видалити \"${item.title}\" зі списку фокусів дня?"
                    },
                )
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.isEveryday) {
                        TextButton(onClick = dayFocusesViewModel::confirmDeleteCurrentOnly) {
                            Text("Лише сьогодні")
                        }
                    }
                    Button(
                        onClick =
                            if (item.isEveryday) {
                                dayFocusesViewModel::confirmDeleteEverywhere
                            } else {
                                dayFocusesViewModel::confirmDeleteCurrentOnly
                            },
                    ) {
                        Text(if (item.isEveryday) "З усіх днів" else "Видалити")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = dayFocusesViewModel::dismissDeleteRequest) {
                    Text("Скасувати")
                }
            },
        )
    }
}
