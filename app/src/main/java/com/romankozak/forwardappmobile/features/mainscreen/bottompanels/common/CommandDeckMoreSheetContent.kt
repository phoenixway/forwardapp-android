package com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onNavigateToInbox: () -> Unit,
    onNavigateToAiChat: () -> Unit,
    onNavigateToAiInsights: () -> Unit,
    onNavigateToAiLifeManagement: () -> Unit,
    onShowImportExportSheet: () -> Unit,
    onNavigateToAttachments: () -> Unit,
    onNavigateToScripts: () -> Unit,
    onNavigateToEntityLibrary: () -> Unit,
    onShowAbout: () -> Unit,
    additionalActions: List<MoreSheetAction>,
    onNavigateToSettings: () -> Unit,
    featureToggles: Map<FeatureFlag, Boolean>,
    containerColor: Color,
    onShowRecents: (() -> Unit)? = null,
) {
    var showAiFeatures by remember { mutableStateOf(false) }
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(containerColor)
                .padding(16.dp),
    ) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("More Options", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            MoreSheetMenuItem(
                icon = if (onShowRecents != null) Icons.Outlined.History else Icons.Outlined.AccountTree,
                label = if (onShowRecents != null) "Недавні" else "Navigation",
                onClick = onShowRecents ?: onNavigateToProjectHierarchy,
            )
            // Context Markers and Structure presets are hidden, not retired.
            // Their existing callbacks/routes remain available to other consumers.
            MoreSheetMenuItem(
                icon = Icons.Outlined.MoveToInbox,
                label = "Inbox",
                onClick = onNavigateToInbox,
            )
            Box {
                MoreSheetMenuItem(
                    icon = Icons.Outlined.AutoAwesome,
                    label = "AI features",
                    onClick = { showAiFeatures = true },
                    opensSubmenu = true,
                )
                DropdownMenu(expanded = showAiFeatures, onDismissRequest = { showAiFeatures = false },
                    modifier = Modifier.widthIn(min = 240.dp)) {
                    DropdownMenuItem(text = { Text("Назад") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) },
                        onClick = { showAiFeatures = false })
                    MoreSheetAiItems(featureToggles,
                        onNavigateToAiLifeManagement = { showAiFeatures = false; onNavigateToAiLifeManagement() },
                        onNavigateToAiChat = { showAiFeatures = false; onNavigateToAiChat() },
                        onNavigateToAiInsights = { showAiFeatures = false; onNavigateToAiInsights() })
                }
            }
            MoreSheetMenuItem(
                icon = Icons.Outlined.Notifications,
                label = "Reminders",
                onClick = onNavigateToReminders,
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
                onNavigateToEntityLibrary = onNavigateToEntityLibrary,
                onNavigateToAttachments = onNavigateToAttachments,
                onNavigateToScripts = onNavigateToScripts,
            )
            MoreSheetMenuItem(
                icon = Icons.Outlined.SwapVert,
                label = "Import/Export",
                onClick = {
                    Log.e("FullJsonImport", "MoreSheetMenuItem Import/Export click")
                    onShowImportExportSheet()
                },
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
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null) },
            text = { Text("AI Life-Management") },
            onClick = onNavigateToAiLifeManagement,
        )
    }
    if (featureToggles[FeatureFlag.AiChat] == true) {
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null) },
            text = { Text("AI-Chat") },
            onClick = onNavigateToAiChat,
        )
    }
    DropdownMenuItem(
        leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null) },
        text = { Text("AI Insights") },
        onClick = onNavigateToAiInsights,
    )
}

@Composable
private fun MoreSheetLibraryItems(
    featureToggles: Map<FeatureFlag, Boolean>,
    onNavigateToEntityLibrary: () -> Unit,
    onNavigateToAttachments: () -> Unit,
    onNavigateToScripts: () -> Unit,
) {
    MoreSheetMenuItem(
        icon = Icons.Default.CollectionsBookmark,
        label = "Library",
        onClick = onNavigateToEntityLibrary,
    )
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
    opensSubmenu: Boolean = false,
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
        Text(label, modifier = Modifier.weight(1f))
        if (opensSubmenu) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
    }
}
