package com.romankozak.forwardappmobile.features.contexts.ui.context_screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.capability.CapabilityId
import com.romankozak.forwardappmobile.core.context.ContextViewPolicy
import com.romankozak.forwardappmobile.core.data.models.entities.BacklogItemContent
import com.romankozak.forwardappmobile.core.data.models.entities.ContextViewMode

internal data class WorkspaceDashboardTab(val mode: ContextViewMode, val label: String, val count: Int? = null)

/** Presentation order only; capability admission and persisted preferred view stay unchanged. */
internal fun workspaceDashboardTabs(enabled: Set<CapabilityId>, counts: Map<ContextViewMode, Int>): List<WorkspaceDashboardTab> {
    val available = ContextViewPolicy.availableViews(enabled).toSet()
    val order = listOf(ContextViewMode.DASHBOARD, ContextViewMode.CONNECTIONS, ContextViewMode.BACKLOG,
        ContextViewMode.INBOX, ContextViewMode.RELATIONS, ContextViewMode.DIRECTION, ContextViewMode.LOG, ContextViewMode.KEY_PROBLEMS)
    return order.filter { it in available }.map { mode ->
        WorkspaceDashboardTab(mode, when (mode) {
            ContextViewMode.DASHBOARD -> "Dashboard"
            ContextViewMode.CONNECTIONS -> "Connections"
            ContextViewMode.BACKLOG -> "Backlog"
            ContextViewMode.INBOX -> "Inbox"
            ContextViewMode.RELATIONS -> "Semantics"
            ContextViewMode.DIRECTION -> "Напрямок"
            ContextViewMode.LOG -> "Лог"
            ContextViewMode.KEY_PROBLEMS -> "Issues"
            else -> mode.name
        }, counts[mode])
    }
}

@Composable
internal fun WorkspaceDashboardOverview(
    modifier: Modifier,
    backlogCount: Int,
    inboxCount: Int,
    directionCount: Int,
    attachments: List<BacklogItemContent>,
    onAttachmentClick: (BacklogItemContent) -> Unit,
    currentViewMode: ContextViewMode,
    onSwitchView: (ContextViewMode) -> Unit,
    enabledCapabilities: Set<CapabilityId>,
    connectionAction: @Composable () -> Unit,
    entityNavigation: @Composable (() -> Unit)? = null,
) {
    val tabs = workspaceDashboardTabs(enabledCapabilities, mapOf(
        ContextViewMode.BACKLOG to backlogCount, ContextViewMode.INBOX to inboxCount,
        ContextViewMode.DIRECTION to directionCount, ContextViewMode.CONNECTIONS to attachments.size))
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Surface(Modifier.fillMaxWidth().padding(horizontal = 12.dp), shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))) {
            Row(Modifier.horizontalScroll(rememberScrollState()).selectableGroup().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEach { tab ->
                    val selected = tab.mode == currentViewMode
                    Surface(modifier = Modifier.clip(RoundedCornerShape(10.dp)).selectable(selected = selected, role = Role.Tab, onClick = { onSwitchView(tab.mode) }), shape = RoundedCornerShape(10.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant) {
                        Row(Modifier.defaultMinSize(minHeight = 44.dp).padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(tab.label, style = MaterialTheme.typography.labelLarge)
                            tab.count?.let { count ->
                                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                                    Text(count.toString(), Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
        entityNavigation?.let { content ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) { content() }
        }
        if (CapabilityId("connections") in enabledCapabilities) {
            WorkspaceDashboardConnectionsCard(
                attachments = attachments,
                onAttachmentClick = onAttachmentClick,
                onViewAll = { onSwitchView(ContextViewMode.CONNECTIONS) },
                connectionAction = connectionAction,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

