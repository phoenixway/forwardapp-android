package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
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

/**
 * Головна вхідна точка панелі введення та навігації.
 * Тепер повністю керується динамічним набором activeCapabilities.
 */
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
    // Новий єдиний параметр замість 7 булевих
    activeCapabilities: Set<CapabilityId>, 
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
    // 1. Формуємо стан та дії для дочірніх компонентів
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
    val inputPanelColors = LocalInputPanelColors.current
    val panelColors = getPanelColors(inputMode, inputPanelColors)
    
    val animatedContainerColor by animateColorAsState(
        targetValue = panelColors.containerColor,
        animationSpec = tween(400),
        label = "panel_color_animation"
    )

    var showModeMenu by remember { mutableStateOf(false) }

    // 2. Основний UI
    Surface(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = animatedContainerColor,
        border = BorderStroke(1.dp, panelColors.contentColor.copy(alpha = 0.1f)),
    ) {
        Column {
            // Підказки автодоповнення
            if (suggestions.isNotEmpty()) {
                AutocompleteSuggestions(suggestions, onSuggestionClick)
            }

            // Рядок навігації (Верхній)
            NavigationBar(state, actions, panelColors.contentColor, holdMenuController)

            // Результати розпізнавання нагадувань
            if (reminderParseResult != null) {
                ReminderParseResultView(reminderParseResult, onClearReminder)
            }

            // Рядок введення (Нижній)
            Row(
                modifier = Modifier.defaultMinSize(minHeight = 64.dp).padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                // Кнопка перемикання режимів (Add Goal, Inbox, Log)
                ModeSelectorButton(
                    inputMode = inputMode,
                    panelColors = panelColors,
                    onOpenMenu = { showModeMenu = true },
                    onModeChange = onInputModeSelected,
                    isProjectManagementEnabled = isProjectManagementEnabled,
                    currentView = currentView
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Поле введення тексту
                InputTextField(
                    modifier = Modifier.weight(1f),
                    inputValue = inputValue,
                    inputMode = inputMode,
                    panelColors = panelColors,
                    focusRequester = focusRequester,
                    onValueChange = onValueChange,
                    onSubmit = onSubmit,
                    isNerActive = isNerActive
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Кнопка Send
                AnimatedSendButton(
                    isVisible = inputValue.text.isNotBlank(),
                    panelColors = panelColors,
                    onClick = onSubmit
                )
            }
        }
    }

    // Діалоги
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

// ------------------- ДОПОМІЖНІ UI-ФУНКЦІЇ ---------------------

@Composable
private fun ReminderParseResultView(result: ReminderParseResult, onClear: () -> Unit) {
    AnimatedVisibility(visible = true, enter = fadeIn() + expandVertically()) {
        if (result.success) {
            ReminderChip(suggestionText = result.suggestionText ?: "", onClear = onClear)
        } else {
            Text(
                text = "Не вдалося розпізнати дату/час: ${result.errorMessage}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun AnimatedSendButton(isVisible: Boolean, panelColors: PanelColors, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
    ) {
        val backgroundColor = panelColors.accentColor
        val iconColor = remember(backgroundColor) {
            if (backgroundColor.luminance() > 0.55f) Color(0xFF1C1B1F) else Color.White
        }

        IconButton(
            onClick = onClick,
            modifier = Modifier.size(44.dp).background(backgroundColor, CircleShape),
            colors = IconButtonDefaults.iconButtonColors(contentColor = iconColor),
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.send), modifier = Modifier.size(20.dp))
        }
    }
}

private fun getPanelColors(mode: InputMode, theme: com.romankozak.forwardappmobile.core.theme.InputPanelColors): PanelColors {
    val style = when (mode) {
        InputMode.AddGoal -> theme.addGoal
        InputMode.AddQuickRecord -> theme.addQuickRecord
        InputMode.SearchInList -> theme.searchInList
        InputMode.SearchGlobal -> theme.searchGlobal
        InputMode.AddProjectLog, InputMode.AddMilestone -> theme.addProjectLog
    }
    return PanelColors(
        containerColor = style.backgroundColor,
        contentColor = style.textColor,
        accentColor = style.textColor,
        inputFieldColor = style.inputFieldColor
    )
}

private data class PanelColors(val containerColor: Color, val contentColor: Color, val accentColor: Color, val inputFieldColor: Color)
