package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.core.config.FeatureToggles
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.core.theme.LocalInputPanelColors
import com.romankozak.forwardappmobile.domain.ner.ReminderParseResult
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Controller
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ModernInputPanel(
    modifier: Modifier = Modifier,
    holdMenuController: HoldMenu2Controller,
    inputValue: TextFieldValue,
    inputMode: InputMode,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    onInputModeSelected: (InputMode) -> Unit,
    onRecentsClick: () -> Unit,
    onAddNestedProjectClick: () -> Unit,
    onShowAddWebLinkDialog: () -> Unit,
    onShowAddObsidianLinkDialog: () -> Unit,
    onAddListShortcutClick: () -> Unit,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onShowProjectHierarchy: () -> Unit,
    onNavigateHome: () -> Unit,
    onEditList: () -> Unit,
    onShareList: () -> Unit,
    onDeleteList: () -> Unit,
    onSetReminder: () -> Unit,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    currentView: ContextViewMode,
    onViewChange: (ContextViewMode) -> Unit,
    onImportFromMarkdown: () -> Unit,
    onExportToMarkdown: () -> Unit,
    onImportBacklogFromMarkdown: () -> Unit,
    onExportBacklogToMarkdown: () -> Unit,
    onExportProjectState: () -> Unit,
    reminderParseResult: ReminderParseResult?,
    onClearReminder: () -> Unit,
    isNerActive: Boolean,
    onStartTrackingCurrentProject: () -> Unit,
    isProjectManagementEnabled: Boolean,
    enableInbox: Boolean,
    enableLog: Boolean,
    enableArtifact: Boolean,
    enableBacklog: Boolean,
    enableDashboard: Boolean,
    enableAttachments: Boolean,
    onToggleProjectManagement: () -> Unit,
    onAddProjectToDayPlan: () -> Unit,
    onCloseSearch: () -> Unit,
    onAddMilestone: () -> Unit,
    onShowCreateNoteDocumentDialog: () -> Unit,
    onCreateChecklist: () -> Unit,
    onShowDisplayPropertiesClick: () -> Unit,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    onAddScript: (() -> Unit)? = null,
) {
    // 1. Динамічний розрахунок дозволів
    val activeCapabilities = remember(enableInbox, enableLog, enableArtifact, enableBacklog, enableDashboard, enableAttachments, isProjectManagementEnabled) {
        setOfNotNull(
            if (enableInbox) CapabilityId("inbox") else null,
            if (enableLog) CapabilityId("log") else null,
            if (enableArtifact) CapabilityId("artifact") else null,
            if (enableBacklog) CapabilityId("backlog") else null,
            if (enableDashboard) CapabilityId("dashboard") else null,
            if (enableAttachments) CapabilityId("attachments") else null,
            if (isProjectManagementEnabled) CapabilityId("advanced") else null
        )
    }

    val state = NavPanelState(
        canGoBack = canGoBack,
        canGoForward = canGoForward,
        menuExpanded = menuExpanded,
        currentView = currentView,
        isProjectManagementEnabled = isProjectManagementEnabled,
        activeCapabilities = activeCapabilities,
        inputMode = inputMode,
    )

    val actions = NavPanelActions(
        onBackClick = onBackClick,
        onForwardClick = onForwardClick,
        onShowProjectHierarchy = onShowProjectHierarchy,
        onNavigateHome = onNavigateHome,
        onRecentsClick = onRecentsClick,
        onCloseSearch = onCloseSearch,
        onViewChange = onViewChange,
        onInputModeSelected = onInputModeSelected,
        onMenuExpandedChange = onMenuExpandedChange,
        onAddProjectToDayPlan = onAddProjectToDayPlan,
        menuActions = OptionsMenuActions(
            onEditList = onEditList,
            onToggleProjectManagement = onToggleProjectManagement,
            onStartTrackingCurrentProject = onStartTrackingCurrentProject,
            onShareList = onShareList,
            onImportFromMarkdown = onImportFromMarkdown,
            onExportToMarkdown = onExportToMarkdown,
            onImportBacklogFromMarkdown = onImportBacklogFromMarkdown,
            onExportBacklogToMarkdown = onExportBacklogToMarkdown,
            onExportProjectState = onExportProjectState,
            onDeleteList = onDeleteList,
            onSetReminder = onSetReminder,
            onShowDisplayPropertiesClick = onShowDisplayPropertiesClick,
        ),
    )

    val focusRequester = remember { FocusRequester() }
    val panelColors = getPanelColors(inputMode, LocalInputPanelColors.current)
    
    val animatedContainerColor by animateColorAsState(
        targetValue = panelColors.containerColor,
        animationSpec = tween(400),
        label = "panel_bg"
    )

    var showModeMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = animatedContainerColor,
        border = BorderStroke(1.dp, panelColors.contentColor.copy(alpha = 0.1f)),
    ) {
        Column {
            // 1. Автодоповнення
            AnimatedVisibility(visible = suggestions.isNotEmpty()) {
                AutocompleteSuggestions(suggestions = suggestions, onSuggestionClick = onSuggestionClick)
            }

            // 2. Верхня панель навігації (Back, Search, ViewToggle, Menu)
            NavigationBar(state, actions, panelColors.contentColor, holdMenuController)

            // 3. Індикатор нагадування
            if (reminderParseResult != null) {
                ReminderParseResultView(reminderParseResult, onClearReminder)
            }

            // 4. Основний рядок введення
            Row(
                modifier = Modifier.defaultMinSize(minHeight = 64.dp).padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                // Кнопка перемикання режимів (+, Inbox, Search)
                ModeSelectorButton(
                    inputMode = inputMode,
                    panelColors = panelColors,
                    onOpenMenu = { showModeMenu = true },
                    onModeChange = onInputModeSelected,
                    isProjectManagementEnabled = isProjectManagementEnabled,
                    currentView = currentView
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Текстове поле
                Box(modifier = Modifier.weight(1f)) {
                    InputTextField(
                        inputValue = inputValue,
                        inputMode = inputMode,
                        panelColors = panelColors,
                        focusRequester = focusRequester,
                        onValueChange = onValueChange,
                        onSubmit = onSubmit,
                        isNerActive = isNerActive
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Кнопка Send (з'являється при наявності тексту)
                AnimatedSendButton(
                    isVisible = inputValue.text.isNotBlank(),
                    panelColors = panelColors,
                    onClick = onSubmit
                )
            }
        }
    }

    // Діалог швидких дій
    if (showModeMenu) {
        InputPanelAddToProjectActionsDialog(
            currentInputMode = inputMode,
            isProjectManagementEnabled = isProjectManagementEnabled,
            onDismiss = { showModeMenu = false },
            onInputModeSelected = onInputModeSelected,
            onAddNestedProjectClick = onAddNestedProjectClick,
            onShowAddWebLinkDialog = onShowAddWebLinkDialog,
            onShowAddObsidianLinkDialog = onShowAddObsidianLinkDialog,
            onAddListShortcutClick = onAddListShortcutClick,
            onShowCreateNoteDocumentDialog = onShowCreateNoteDocumentDialog,
            onCreateChecklist = onCreateChecklist,
            onAddScript = if (FeatureToggles.isEnabled(FeatureFlag.ScriptsLibrary)) onAddScript else null,
        )
    }
}

// ------------------- ВНУТРІШНІ КОМПОНЕНТИ (LOCAL HELPERS) ---------------------

@Composable
private fun ReminderParseResultView(result: ReminderParseResult, onClear: () -> Unit) {
    AnimatedVisibility(visible = true, enter = fadeIn() + expandVertically()) {
        if (result.success) {
            ReminderChip(suggestionText = result.suggestionText ?: "", onClear = onClear)
        } else {
            Text(
                text = "Помилка розпізнавання: ${result.errorMessage}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun ModeSelectorButton(
    inputMode: InputMode,
    panelColors: PanelColors,
    onOpenMenu: () -> Unit,
    onModeChange: (InputMode) -> Unit,
    isProjectManagementEnabled: Boolean,
    currentView: ContextViewMode
) {
    val modes = remember(isProjectManagementEnabled, currentView) {
        listOfNotNull(
            InputMode.AddGoal,
            InputMode.AddQuickRecord,
            if (isProjectManagementEnabled) InputMode.AddProjectLog else null,
            InputMode.SearchGlobal
        )
    }
    
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Surface(
        onClick = onOpenMenu,
        shape = CircleShape,
        color = panelColors.contentColor.copy(alpha = 0.1f),
        modifier = Modifier.size(48.dp).pointerInput(inputMode) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (dragOffset > 50f) {
                        val prev = (modes.indexOf(inputMode) - 1 + modes.size) % modes.size
                        onModeChange(modes[prev])
                    } else if (dragOffset < -50f) {
                        val next = (modes.indexOf(inputMode) + 1) % modes.size
                        onModeChange(modes[next])
                    }
                    dragOffset = 0f
                }
            ) { _, amount -> dragOffset += amount }
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = when (inputMode) {
                    InputMode.AddGoal -> Icons.Default.Add
                    InputMode.AddQuickRecord -> Icons.Default.Inbox
                    InputMode.SearchGlobal -> Icons.Default.TravelExplore
                    else -> Icons.Default.PostAdd
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = panelColors.contentColor
            )
        }
    }
}

@Composable
private fun InputTextField(
    inputValue: TextFieldValue,
    inputMode: InputMode,
    panelColors: PanelColors,
    focusRequester: FocusRequester,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    isNerActive: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp, max = 200.dp),
        shape = RoundedCornerShape(20.dp),
        color = panelColors.inputFieldColor,
        border = BorderStroke(1.dp, panelColors.accentColor.copy(alpha = 0.2f))
    ) {
        BasicTextField(
            value = inputValue,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = panelColors.contentColor),
            cursorBrush = SolidColor(panelColors.accentColor),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSubmit() }),
            decorationBox = { innerTextField ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        if (inputValue.text.isEmpty()) {
                            Text(
                                text = when(inputMode) {
                                    InputMode.AddGoal -> stringResource(R.string.hint_add_goal)
                                    InputMode.AddQuickRecord -> stringResource(R.string.hint_add_quick_record)
                                    InputMode.SearchInList -> stringResource(R.string.hint_search_in_list)
                                    else -> "Введіть текст..."
                                },
                                color = panelColors.contentColor.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                    NerIndicator(isActive = isNerActive, hasText = inputValue.text.isNotBlank())
                }
            }
        )
    }
}

@Composable
private fun AnimatedSendButton(isVisible: Boolean, panelColors: PanelColors, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp).background(panelColors.accentColor, CircleShape),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = if (panelColors.accentColor.luminance() > 0.5f) Color.Black else Color.White
            )
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(20.dp))
        }
    }
}
