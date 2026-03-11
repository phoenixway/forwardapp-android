package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.features.mainscreen.DashboardBottomBar
import com.romankozak.forwardappmobile.features.recent.RecentViewModel
import com.romankozak.forwardappmobile.ui.components.CommonBottomPanelLayout
import com.romankozak.forwardappmobile.ui.components.header.CommandDeckBackgroundModifier

@Composable
fun CoreBottomPanel(
    navController: NavController,
    onNavigateToProjectHierarchy: () -> Unit,
    onNavigateToPresets: () -> Unit,
    onNavigateToCharacter: () -> Unit,
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
    featureToggles: Map<FeatureFlag, Boolean>,
    onNavigateToRecentItem: (RecentItem) -> Unit,
    recentViewModel: RecentViewModel = hiltViewModel(),
) {
    CommonBottomPanelLayout {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .then(CommandDeckBackgroundModifier())
                    .padding(horizontal = 22.dp, vertical = 12.dp),
        ) {
            DashboardBottomBar(
                onNavigateToProjectHierarchy = onNavigateToProjectHierarchy,
                onNavigateToProjectSearch = onNavigateToGlobalSearch,
                onNavigateToTracker = onNavigateToTracker,
                onNavigateToInbox = onNavigateToInbox,
                onNavigateToReminders = onNavigateToReminders,
                onNavigateToPresets = onNavigateToPresets,
                onNavigateToAiChat = onNavigateToAiChat,
                onNavigateToAiInsights = onNavigateToAiInsights,
                onNavigateToAiLifeManagement = onNavigateToAiLifeManagement,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToRecentItem = onNavigateToRecentItem,
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
                recentViewModel = recentViewModel,
            )
        }
    }
}
