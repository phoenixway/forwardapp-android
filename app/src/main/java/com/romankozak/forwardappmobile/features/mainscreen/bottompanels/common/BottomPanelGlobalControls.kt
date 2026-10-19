package com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun BottomPanelMoreActionButton(
    actions: BottomPanelGlobalActions,
    modifier: Modifier = Modifier.size(BottomPanelTokens.ActionButtonSize),
    additionalActions: List<MoreSheetAction> = emptyList(),
    onShowRecents: (() -> Unit)? = null,
) {
    CommandDeckMoreActionButton(
        onNavigateToProjectHierarchy = actions.onNavigateToProjectHierarchy,
        onShowContextMarkersSheet = actions.onShowContextMarkersSheet,
        onNavigateToInbox = actions.onNavigateToInbox,
        onNavigateToReminders = actions.onNavigateToReminders,
        onNavigateToPresets = actions.onNavigateToPresets,
        onNavigateToAiChat = actions.onNavigateToAiChat,
        onNavigateToAiInsights = actions.onNavigateToAiInsights,
        onNavigateToAiLifeManagement = actions.onNavigateToAiLifeManagement,
        onNavigateToSettings = actions.onNavigateToSettings,
        onExportToFile = actions.onExportToFile,
        onImportFromFileRequest = actions.onImportFromFileRequest,
        onSelectiveImportFromFileRequest = actions.onSelectiveImportFromFileRequest,
        onExportAttachments = actions.onExportAttachments,
        onImportAttachmentsFromFileRequest = actions.onImportAttachmentsFromFileRequest,
        onWifiPush = actions.onWifiPush,
        onShowWifiServer = actions.onShowWifiServer,
        onShowWifiImport = actions.onShowWifiImport,
        onNavigateToAttachments = actions.onNavigateToAttachments,
        onNavigateToScripts = actions.onNavigateToScripts,
        onNavigateToEntityLibrary = actions.onNavigateToEntityLibrary,
        onShowAbout = actions.onShowAbout,
        additionalActions = additionalActions,
        onShowRecents = onShowRecents,
        featureToggles = actions.featureToggles,
        modifier = modifier,
    )
}
