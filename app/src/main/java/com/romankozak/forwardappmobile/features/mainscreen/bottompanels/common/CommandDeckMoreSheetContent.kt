package com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.config.FeatureFlag

@Composable
internal fun MoreBottomSheetContent(
    onNavigateToReminders: () -> Unit,
    onNavigateToPresets: () -> Unit,
    onNavigateToProjectHierarchy: () -> Unit,
    onShowContextMarkersSheet: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onNavigateToAiInsights: () -> Unit,
    onNavigateToAiLifeManagement: () -> Unit,
    onShowImportExportSheet: () -> Unit,
    onNavigateToAttachments: () -> Unit,
    onNavigateToScripts: () -> Unit,
    onShowAbout: () -> Unit,
    additionalActions: List<MoreSheetAction>,
    onNavigateToSettings: () -> Unit,
    featureToggles: Map<FeatureFlag, Boolean>,
    containerColor: Color,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(containerColor)
                .padding(16.dp),
    ) {
        Column {
            Text("More Options", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            MoreSheetMenuItem(
                icon = Icons.Outlined.AccountTree,
                label = "Contexts",
                onClick = onNavigateToProjectHierarchy,
            )
            MoreSheetMenuItem(
                icon = Icons.Outlined.AccountTree,
                label = "Context Markers",
                onClick = onShowContextMarkersSheet,
            )
            MoreSheetAiItems(
                featureToggles = featureToggles,
                onNavigateToAiLifeManagement = onNavigateToAiLifeManagement,
                onNavigateToAiChat = onNavigateToAiChat,
                onNavigateToAiInsights = onNavigateToAiInsights,
            )
            MoreSheetMenuItem(
                icon = Icons.Outlined.Notifications,
                label = "Reminders",
                onClick = onNavigateToReminders,
            )
            MoreSheetMenuItem(
                icon = Icons.Outlined.DashboardCustomize,
                label = "Structure presets",
                contentDescription = "Presets",
                onClick = onNavigateToPresets,
            )
            additionalActions.forEach { action ->
                MoreSheetMenuItem(
                    icon = Icons.Outlined.DashboardCustomize,
                    label = action.label,
                    onClick = action.onClick,
                )
            }
            MoreSheetLibraryItems(
                featureToggles = featureToggles,
                onNavigateToAttachments = onNavigateToAttachments,
                onNavigateToScripts = onNavigateToScripts,
            )
            MoreSheetMenuItem(
                icon = Icons.Outlined.SwapVert,
                label = "Import/Export",
                onClick = onShowImportExportSheet,
            )
            MoreSheetMenuItem(
                icon = Icons.Outlined.Info,
                label = "About",
                onClick = onShowAbout,
            )
            MoreSheetMenuItem(
                icon = Icons.Outlined.Settings,
                label = "Settings",
                onClick = onNavigateToSettings,
            )
        }
    }
}

@Composable
private fun MoreSheetAiItems(
    featureToggles: Map<FeatureFlag, Boolean>,
    onNavigateToAiLifeManagement: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onNavigateToAiInsights: () -> Unit,
) {
    if (featureToggles[FeatureFlag.AiLifeManagement] == true) {
        MoreSheetMenuItem(
            icon = Icons.Outlined.AutoAwesome,
            label = "AI Life-Management",
            onClick = onNavigateToAiLifeManagement,
        )
    }
    if (featureToggles[FeatureFlag.AiChat] == true) {
        MoreSheetMenuItem(
            icon = Icons.Outlined.AutoAwesome,
            label = "AI-Chat",
            onClick = onNavigateToAiChat,
        )
    }
    MoreSheetMenuItem(
        icon = Icons.Outlined.AutoAwesome,
        label = "AI Insights",
        onClick = onNavigateToAiInsights,
    )
}

@Composable
private fun MoreSheetLibraryItems(
    featureToggles: Map<FeatureFlag, Boolean>,
    onNavigateToAttachments: () -> Unit,
    onNavigateToScripts: () -> Unit,
) {
    if (featureToggles[FeatureFlag.AttachmentsLibrary] == true) {
        MoreSheetMenuItem(
            icon = Icons.Default.Description,
            label = "Attachments",
            contentDescription = "Attachments Library",
            onClick = onNavigateToAttachments,
        )
    }
    if (featureToggles[FeatureFlag.ScriptsLibrary] == true) {
        MoreSheetMenuItem(
            icon = Icons.Default.Code,
            label = "Scripts",
            contentDescription = "Scripts Library",
            onClick = onNavigateToScripts,
        )
    }
}

@Composable
private fun MoreSheetMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    contentDescription: String = label,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = contentDescription)
        Spacer(modifier = Modifier.width(16.dp))
        Text(label)
    }
}
