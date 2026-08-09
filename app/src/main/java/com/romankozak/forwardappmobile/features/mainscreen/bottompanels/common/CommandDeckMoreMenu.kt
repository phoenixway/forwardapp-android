package com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common

import android.net.Uri
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.config.FeatureFlag
import kotlinx.coroutines.launch

data class MoreSheetAction(
    val label: String,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandDeckMoreActionButton(
    onNavigateToProjectHierarchy: () -> Unit,
    onShowContextMarkersSheet: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToPresets: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onNavigateToAiInsights: () -> Unit,
    onNavigateToAiLifeManagement: () -> Unit,
    onNavigateToSettings: () -> Unit,
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
    additionalActions: List<MoreSheetAction> = emptyList(),
    featureToggles: Map<FeatureFlag, Boolean>,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = false,
        )
    var showMoreBottomSheet by remember { mutableStateOf(false) }
    var showImportExportSheet by remember { mutableStateOf(false) }
    fun runAfterSheetDismiss(action: () -> Unit) {
        coroutineScope.launch { modalSheetState.hide() }.invokeOnCompletion {
            if (!modalSheetState.isVisible) {
                showMoreBottomSheet = false
            }
            action()
        }
    }

    IconButton(
        onClick = { showMoreBottomSheet = true },
        modifier = modifier,
        colors =
            IconButtonDefaults.iconButtonColors(
                contentColor = bottomPanelColors().action,
            ),
    ) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Більше дій",
        )
    }

    if (showMoreBottomSheet) {
        val moreSheetColor = MaterialTheme.colorScheme.surfaceContainerLow
        ModalBottomSheet(
            onDismissRequest = { showMoreBottomSheet = false },
            sheetState = modalSheetState,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            containerColor = moreSheetColor,
        ) {
            MoreBottomSheetContent(
                onNavigateToReminders = { runAfterSheetDismiss(onNavigateToReminders) },
                onNavigateToPresets = { runAfterSheetDismiss(onNavigateToPresets) },
                onNavigateToAiInsights = { runAfterSheetDismiss(onNavigateToAiInsights) },
                onNavigateToProjectHierarchy = { runAfterSheetDismiss(onNavigateToProjectHierarchy) },
                onShowContextMarkersSheet = { runAfterSheetDismiss(onShowContextMarkersSheet) },
                onNavigateToInbox = { runAfterSheetDismiss(onNavigateToInbox) },
                onNavigateToAiChat = { runAfterSheetDismiss(onNavigateToAiChat) },
                onNavigateToAiLifeManagement = { runAfterSheetDismiss(onNavigateToAiLifeManagement) },
                onShowImportExportSheet = { runAfterSheetDismiss { showImportExportSheet = true } },
                onNavigateToAttachments = { runAfterSheetDismiss(onNavigateToAttachments) },
                onNavigateToScripts = { runAfterSheetDismiss(onNavigateToScripts) },
                onShowAbout = { runAfterSheetDismiss(onShowAbout) },
                additionalActions =
                    additionalActions.map { action ->
                        MoreSheetAction(
                            label = action.label,
                            onClick = { runAfterSheetDismiss(action.onClick) },
                        )
                    },
                onNavigateToSettings = { runAfterSheetDismiss(onNavigateToSettings) },
                featureToggles = featureToggles,
                containerColor = moreSheetColor,
            )
        }
    }

    if (showImportExportSheet) {
        CommandDeckImportExportSheet(
            onDismiss = { showImportExportSheet = false },
            onExportToFile = onExportToFile,
            onImportFromFileRequest = onImportFromFileRequest,
            onSelectiveImportFromFileRequest = onSelectiveImportFromFileRequest,
            onExportAttachments = onExportAttachments,
            onImportAttachmentsFromFileRequest = onImportAttachmentsFromFileRequest,
            onWifiPush = onWifiPush,
            onShowWifiServer = onShowWifiServer,
            onShowWifiImport = onShowWifiImport,
        )
    }
}
