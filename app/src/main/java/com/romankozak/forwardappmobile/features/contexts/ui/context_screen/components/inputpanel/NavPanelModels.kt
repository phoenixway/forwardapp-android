package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

import androidx.compose.ui.graphics.Color
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode

data class NavPanelState(
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val menuExpanded: Boolean,
    val currentView: ContextViewMode,
    val isProjectManagementEnabled: Boolean,
    val activeCapabilities: Set<CapabilityId>,
    val inputMode: InputMode,
)

data class NavPanelActions(
    val onBackClick: () -> Unit,
    val onForwardClick: () -> Unit,
    val onShowProjectHierarchy: () -> Unit,
    val onNavigateHome: () -> Unit,
    val onRecentsClick: () -> Unit,
    val onCloseSearch: () -> Unit,
    val onViewChange: (ContextViewMode) -> Unit,
    val onInputModeSelected: (InputMode) -> Unit,
    val onMenuExpandedChange: (Boolean) -> Unit,
    val onAddProjectToDayPlan: () -> Unit,
    val menuActions: OptionsMenuActions,
)

data class OptionsMenuActions(
    val onEditList: () -> Unit,
    val onToggleProjectManagement: (Boolean) -> Unit,
    val onStartTrackingCurrentProject: () -> Unit,
    val onShareList: () -> Unit,
    val onImportFromMarkdown: () -> Unit,
    val onExportToMarkdown: () -> Unit,
    val onImportBacklogFromMarkdown: () -> Unit,
    val onExportBacklogToMarkdown: () -> Unit,
    val onExportProjectState: () -> Unit,
    val onDeleteList: () -> Unit,
    val onSetReminder: () -> Unit,
    val onShowDisplayPropertiesClick: () -> Unit,
    val onAddMilestone: (String) -> Unit,
)

// Ця модель тепер доступна у всьому пакеті
internal data class PanelColors(
    val containerColor: Color,
    val contentColor: Color,
    val accentColor: Color,
    val inputFieldColor: Color,
)
