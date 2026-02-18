package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode
import com.romankozak.forwardappmobile.core.theme.LocalInputPanelColors
import com.romankozak.forwardappmobile.features.common.components.holdmenu2.HoldMenu2Controller
import kotlin.math.abs

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
    onShowCurrentContextInHierarchyFocus: () -> Unit,
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
    onToggleFocusContext: () -> Unit,
    isCurrentContextFocused: Boolean,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    currentView: ContextViewMode,
    onViewChange: (ContextViewMode) -> Unit,
    onImportFromMarkdown: () -> Unit,
    onExportToMarkdown: () -> Unit,
    onImportBacklogFromMarkdown: () -> Unit,
    onExportBacklogToMarkdown: () -> Unit,
    onExportProjectState: () -> Unit,
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
    onToggleProjectManagement: (Boolean) -> Unit,
    onAddProjectToDayPlan: () -> Unit,
    onCloseSearch: () -> Unit,
    onAddMilestone: (String) -> Unit,
    onShowDisplayPropertiesClick: () -> Unit,
    enabledCapabilitiesOverride: Set<CapabilityId>? = null,
) {
    // Об'єднуємо старі прапорці та нові ID в єдиний Set можливостей
    // У ModernInputPanel.kt

    val activeCapabilities =
        remember(
            enableInbox,
            enableLog,
            enableArtifact,
            enableBacklog,
            enableDashboard,
            enableAttachments,
            isProjectManagementEnabled,
            experimentalCapabilityIds,
            enabledCapabilitiesOverride,
        ) {
            buildSet {
                // 1. Додаємо старі прапорці (Legacy/UI)
                if (enableInbox) add(CapabilityId("inbox"))
                if (enableLog) add(CapabilityId("log"))
                if (enableArtifact) add(CapabilityId("advanced"))
                if (enableBacklog) add(CapabilityId("backlog"))
                if (enableDashboard) add(CapabilityId("dashboard"))
                if (enableAttachments) add(CapabilityId("connections"))
                if (isProjectManagementEnabled) add(CapabilityId("advanced"))

                // 2. Додаємо всі динамічні фічі (Ветеринар, Нотатки і т.д.), ігноруючи пошкоджені значення
                experimentalCapabilityIds.forEach { id ->
                    val normalized = runCatching { id.raw.trim() }.getOrNull()?.takeIf { it.isNotEmpty() } ?: return@forEach
                    add(CapabilityId(normalized))
                }

                // 3. Session override додаємо зверху, але не замінюємо локальні прапорці повністю.
                // Це дозволяє уникнути коротких станів, коли сесія ще не пересинхронизована.
                enabledCapabilitiesOverride?.forEach { add(it) }
            }
        }

    val state =
        NavPanelState(
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            menuExpanded = menuExpanded,
            currentView = currentView,
            isProjectManagementEnabled = isProjectManagementEnabled,
            activeCapabilities = activeCapabilities,
            inputMode = inputMode,
            isCurrentContextFocused = isCurrentContextFocused,
        )

    val actions =
        NavPanelActions(
            onBackClick = onBackClick,
            onForwardClick = onForwardClick,
            onShowProjectHierarchy = onShowProjectHierarchy,
            onAddContextLink = onAddNestedProjectClick,
            onShowCurrentContextInHierarchyFocus = onShowCurrentContextInHierarchyFocus,
            onNavigateHome = onNavigateHome,
            onRecentsClick = onRecentsClick,
            onCloseSearch = onCloseSearch,
            onViewChange = onViewChange,
            onInputModeSelected = onInputModeSelected,
            onMenuExpandedChange = onMenuExpandedChange,
            onAddProjectToDayPlan = onAddProjectToDayPlan,
            menuActions =
                OptionsMenuActions(
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
                    onAddMilestone = onAddMilestone,
                    onToggleFocusContext = onToggleFocusContext,
                ),
        )

    val focusRequester = remember { FocusRequester() }
    val panelColors = getPanelColors(inputMode, LocalInputPanelColors.current)
    val availableInputModes =
        remember(isProjectManagementEnabled, currentView) {
            listOfNotNull(
                InputMode.AddGoal,
                if (currentView == ContextViewMode.DIRECTION) InputMode.AddDirection else null,
                InputMode.AddQuickRecord,
                if (isProjectManagementEnabled) InputMode.AddProjectLog else null,
                InputMode.SearchGlobal,
            )
        }

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
                modifier =
                    Modifier
                        .defaultMinSize(minHeight = 64.dp)
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                        .pointerInput(inputMode, availableInputModes) {
                            var dragOffsetX = 0f
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { _, dragAmount -> dragOffsetX += dragAmount },
                                onDragEnd = {
                                    val threshold = 56f
                                    if (abs(dragOffsetX) >= threshold && availableInputModes.isNotEmpty()) {
                                        val currentIndex =
                                            availableInputModes.indexOf(inputMode).takeIf { it >= 0 } ?: 0
                                        val step = if (dragOffsetX < 0f) 1 else -1
                                        val nextIndex =
                                            (currentIndex + step + availableInputModes.size) %
                                                availableInputModes.size
                                        onInputModeSelected(availableInputModes[nextIndex])
                                    }
                                    dragOffsetX = 0f
                                },
                            )
                        },
                verticalAlignment = Alignment.Bottom,
            ) {
                InputTextField(
                    modifier = Modifier.weight(1f),
                    inputValue = inputValue,
                    inputMode = inputMode,
                    panelColors = panelColors,
                    focusRequester = focusRequester,
                    onValueChange = onValueChange,
                    onSubmit = onSubmit,
                    isNerActive = isNerActive,
                )

                Spacer(modifier = Modifier.width(8.dp))

                AnimatedSendButton(
                    isVisible = inputValue.text.isNotBlank(),
                    panelColors = panelColors,
                    onClick = onSubmit,
                )
            }
        }
    }
}
