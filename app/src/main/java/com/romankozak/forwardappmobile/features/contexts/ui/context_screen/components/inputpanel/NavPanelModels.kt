package com.romankozak.forwardappmobile.features.contexts.ui.context_screen.components.inputpanel

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
    val onToggleProjectManagement: () -> Unit,
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
)

// Допоміжна модель для кольорів (внутрішня)
internal data class PanelColors(
    val containerColor: androidx.compose.ui.graphics.Color,
    val contentColor: androidx.compose.ui.graphics.Color,
    val accentColor: androidx.compose.ui.graphics.Color,
    val inputFieldColor: androidx.compose.ui.graphics.Color,
)
