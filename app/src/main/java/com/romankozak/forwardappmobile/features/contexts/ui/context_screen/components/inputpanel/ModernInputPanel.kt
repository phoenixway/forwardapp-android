package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.core.theme.LocalInputPanelColors
import com.romankozak.forwardappmobile.domain.ner.ReminderParseResult
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Controller

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
    // --- ДОДАНО: Список експериментальних фіч (для ветеринара, нотаток тощо) ---
    experimentalCapabilityIds: List<CapabilityId> = emptyList(),
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
    // Об'єднуємо старі прапорці та нові ID в єдиний Set можливостей
    val activeCapabilities = remember(
        enableInbox, enableLog, enableArtifact, enableBacklog, 
        enableDashboard, enableAttachments, isProjectManagementEnabled, 
        experimentalCapabilityIds
    ) {
        val base = mutableSetOf<CapabilityId>()
        if (enableInbox) base.add(CapabilityId("inbox"))
        if (enableLog) base.add(CapabilityId("log"))
        if (enableArtifact) base.add(CapabilityId("artifact"))
        if (enableBacklog) base.add(CapabilityId("backlog"))
        if (enableDashboard) base.add(CapabilityId("dashboard"))
        if (enableAttachments) base.add(CapabilityId("attachments"))
        if (isProjectManagementEnabled) base.add(CapabilityId("advanced"))
        
        // Додаємо динамічні фічі (ветеринар, нотатки), щоб вони не відфільтровувалися в UI
        base.addAll(experimentalCapabilityIds)
        base
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

    Surface(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = panelColors.containerColor,
        border = BorderStroke(1.dp, panelColors.contentColor.copy(alpha = 0.1f)),
    ) {
        Column {
            // Верхня панель (NavigationBar автоматично відфільтрує доступні вкладки через activeCapabilities)
            NavigationBar(state, actions, panelColors.contentColor, holdMenuController)

            Row(
                modifier = Modifier.defaultMinSize(minHeight = 64.dp).padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                ModeSelectorButton(
                    inputMode = inputMode,
                    panelColors = panelColors,
                    onOpenMenu = { onMenuExpandedChange(true) },
                    onModeChange = onInputModeSelected,
                    isProjectManagementEnabled = isProjectManagementEnabled,
                    currentView = currentView
                )

                Spacer(modifier = Modifier.width(8.dp))

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

                AnimatedSendButton(
                    isVisible = inputValue.text.isNotBlank(),
                    panelColors = panelColors,
                    onClick = onSubmit
                )
            }
        }
    }
}
