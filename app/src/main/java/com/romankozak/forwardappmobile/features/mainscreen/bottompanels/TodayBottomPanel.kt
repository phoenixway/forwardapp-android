package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.core.data.models.entities.TaskPriority
import com.romankozak.forwardappmobile.core.theme.LocalInputPanelColors
import com.romankozak.forwardappmobile.features.daymanagement.ui.dayplan.DayPlanViewModel
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.actions.InputSuggestionActions
import com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel.AutocompleteSuggestions
import com.romankozak.forwardappmobile.features.missions.presentation.LinkPickerTab
import com.romankozak.forwardappmobile.features.missions.presentation.LinkedTargetsPickerDialog
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption
import com.romankozak.forwardappmobile.features.mainscreen.CommandDeckMoreActionButton
import com.romankozak.forwardappmobile.features.daymanagement.ui.DayManagementTab
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.text.style.TextOverflow
import com.romankozak.forwardappmobile.features.recent.RecentViewModel
import com.romankozak.forwardappmobile.ui.components.CommonBottomPanelLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class TodayQuickTaskDraft(
    val title: String,
    val description: String,
)

private const val TODAY_QUICK_TASK_TITLE_LIMIT = 100
private const val SURFACE_LUMINANCE_THRESHOLD = 0.5f

private fun buildTodayQuickTaskDraft(rawInput: String): TodayQuickTaskDraft? {
    val normalizedInput =
        rawInput
            .replace("\r\n", "\n")
            .trim()
    val lines = normalizedInput.takeIf { it.isNotBlank() }?.lines().orEmpty()
    val firstLine = lines.firstOrNull()?.trim().orEmpty()
    val remainingText =
        lines
            .drop(1)
            .joinToString("\n")
            .trim()

    return firstLine.takeIf { it.isNotBlank() }?.let { title ->
        if (title.length <= TODAY_QUICK_TASK_TITLE_LIMIT) {
            TodayQuickTaskDraft(
                title = title,
                description = remainingText,
            )
        } else {
            TodayQuickTaskDraft(
                title = title.take(TODAY_QUICK_TASK_TITLE_LIMIT).trimEnd(),
                description = listOf(title.drop(TODAY_QUICK_TASK_TITLE_LIMIT).trim(), remainingText)
                    .filter { it.isNotBlank() }
                    .joinToString("\n"),
            )
        }
    }
}

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
    onNavigateToDayManagementTab: (DayManagementTab) -> Unit,
    featureToggles: Map<FeatureFlag, Boolean>,
    onNavigateToRecentItem: (RecentItem) -> Unit,
    recentViewModel: RecentViewModel = hiltViewModel(),
    dayPlanViewModel: DayPlanViewModel = hiltViewModel(),
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
    val panelStyle = LocalInputPanelColors.current.addGoal
    val colorScheme = MaterialTheme.colorScheme
    val inputSuggestionActions = remember { InputSuggestionActions() }
    val dateChipBackground =
        if (colorScheme.surface.luminance() > SURFACE_LUMINANCE_THRESHOLD) {
            colorScheme.surfaceContainerHighest
        } else {
            colorScheme.surfaceContainerHigh
        }
    var inputValue by remember { mutableStateOf(TextFieldValue("")) }
    var showContextPicker by remember { mutableStateOf(false) }
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
    val dateLabel =
        remember(dayPlanUiState.dayPlan?.date, dayPlanUiState.isToday) {
            val date = dayPlanUiState.dayPlan?.date ?: return@remember ""
            val prefix = if (dayPlanUiState.isToday) "Today" else ""
            val formatted = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(date))
            listOf(prefix, formatted).filter { it.isNotBlank() }.joinToString(" ")
        }

    fun submitTask() {
        val dayPlanId = dayPlanUiState.dayPlan?.id ?: return
        val taskDraft = buildTodayQuickTaskDraft(inputValue.text) ?: return

        dayPlanViewModel.addTask(
            dayPlanId = dayPlanId,
            title = taskDraft.title,
            description = taskDraft.description,
            duration = null,
            priority = TaskPriority.MEDIUM,
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
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { showContextPicker = true },
                        modifier = Modifier.size(38.dp),
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                contentColor = panelStyle.textColor.copy(alpha = 0.8f),
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Додати задачу",
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    IconButton(
                        onClick = { dayPlanViewModel.toggleScopeLinksSheet() },
                        modifier = Modifier.size(38.dp),
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                contentColor = panelStyle.textColor.copy(alpha = 0.8f),
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = "Показати зв'язки",
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    IconButton(
                        onClick = { dayPlanViewModel.navigateToPreviousDay() },
                        modifier = Modifier.size(38.dp),
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                contentColor = panelStyle.textColor.copy(alpha = 0.8f),
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Попередній день",
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    IconButton(
                        onClick = { dayPlanViewModel.navigateToNextDay() },
                        modifier = Modifier.size(38.dp),
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                contentColor = panelStyle.textColor.copy(alpha = 0.8f),
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Наступний день",
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    if (dateLabel.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = dateChipBackground,
                            border = BorderStroke(1.dp, panelStyle.textColor.copy(alpha = 0.12f)),
                        ) {
                            Text(
                                text = dateLabel,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.onSurface.copy(alpha = 0.88f),
                            )
                        }
                    }

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
                        featureToggles = featureToggles,
                        modifier = Modifier.size(38.dp),
                    )
                }

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

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 64.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Surface(
                        modifier =
                            Modifier
                                .weight(1f)
                                .heightIn(max = LocalConfiguration.current.screenHeightDp.dp / 3)
                                .defaultMinSize(minHeight = 44.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = panelStyle.inputFieldColor,
                        border = BorderStroke(1.dp, panelStyle.textColor.copy(alpha = 0.3f)),
                    ) {
                        BasicTextField(
                            value = inputValue,
                            onValueChange = { inputValue = it },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            textStyle =
                                MaterialTheme.typography.bodyLarge.copy(
                                    color = panelStyle.textColor,
                                ),
                            singleLine = false,
                            maxLines = 6,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { submitTask() }),
                            cursorBrush = SolidColor(panelStyle.textColor),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (inputValue.text.isBlank()) {
                                        Text(
                                            text = "Нове завдання...",
                                            color = panelStyle.textColor.copy(alpha = 0.6f),
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    AnimatedVisibility(
                        visible = inputValue.text.isNotBlank(),
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                    ) {
                        IconButton(
                            onClick = ::submitTask,
                            modifier =
                                Modifier
                                    .size(44.dp)
                                    .background(
                                        color = panelStyle.textColor,
                                        shape = RoundedCornerShape(22.dp),
                                    ),
                            colors =
                                IconButtonDefaults.iconButtonColors(
                                    contentColor =
                                        if (panelStyle.textColor.luminance() > 0.5f) {
                                            Color.Black
                                        } else {
                                            Color.White
                                        },
                                ),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Створити задачу",
                            )
                        }
                    }
                }

                // Day Management Sub-tabs
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DayManagementTab.entries.filter { it != DayManagementTab.TRACK && it != DayManagementTab.DASHBOARD && it != DayManagementTab.ANALYTICS }.forEach {
                        IconButton(
                            onClick = { onNavigateToDayManagementTab(it) },
                            modifier = Modifier.size(48.dp),
                            colors =
                                IconButtonDefaults.iconButtonColors(
                                    contentColor = panelStyle.textColor.copy(alpha = 0.8f),
                                ),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = it.icon,
                                    contentDescription = it.title,
                                    modifier = Modifier.size(24.dp),
                                )
                                Text(
                                    text = it.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = panelStyle.textColor.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }
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
}
