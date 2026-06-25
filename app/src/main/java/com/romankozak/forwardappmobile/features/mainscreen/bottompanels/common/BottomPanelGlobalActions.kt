package com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem

@Immutable
data class BottomPanelGlobalActions(
    val onNavigateToProjectHierarchy: () -> Unit,
    val onShowContextMarkersSheet: () -> Unit,
    val onNavigateToPresets: () -> Unit,
    val onNavigateToGlobalSearch: () -> Unit,
    val onNavigateToSettings: () -> Unit,
    val onNavigateToInbox: () -> Unit,
    val onNavigateToTracker: () -> Unit,
    val onNavigateToReminders: () -> Unit,
    val onNavigateToAiChat: () -> Unit,
    val onNavigateToAiInsights: () -> Unit,
    val onNavigateToAiLifeManagement: () -> Unit,
    val onExportToFile: () -> Unit,
    val onImportFromFileRequest: (Uri) -> Unit,
    val onSelectiveImportFromFileRequest: (Uri) -> Unit,
    val onExportAttachments: () -> Unit,
    val onImportAttachmentsFromFileRequest: (Uri) -> Unit,
    val onWifiPush: (String) -> Unit,
    val onShowWifiServer: () -> Unit,
    val onShowWifiImport: () -> Unit,
    val onNavigateToAttachments: () -> Unit,
    val onNavigateToScripts: () -> Unit,
    val onShowAbout: () -> Unit,
    val featureToggles: Map<FeatureFlag, Boolean>,
    val onNavigateToRecentItem: (RecentItem) -> Unit,
)
