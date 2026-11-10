package com.romankozak.forwardappmobile.features.mainscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.features.recent.RecentViewModel
import com.romankozak.forwardappmobile.R
import com.romankozak.forwardappmobile.core.data.models.entities.GeneralStatus
import com.romankozak.forwardappmobile.core.data.models.entities.RecentItem
import com.romankozak.forwardappmobile.features.mainscreen.lifemanagement.*
import com.romankozak.forwardappmobile.features.mainscreen.session.SessionMode
import com.romankozak.forwardappmobile.features.mainscreen.session.SessionModeDashboard
import com.romankozak.forwardappmobile.features.mainscreen.session.SessionModeState

private const val SESSION = "session"
private const val LEVELS = "levels"
private const val INSIGHTS = "insights"
private const val RECENTS = "recents"

@Composable
internal fun CommandDeckAccordion(
    session: SessionModeState,
    latestSessionReason: String?,
    onModeSelected: (SessionMode) -> Unit,
    onOpenModeContext: (String) -> Unit,
    commandDeckViewModel: CommandDeckViewModel,
    onRecentItemClick: (RecentItem) -> Unit,
    insightsContent: @Composable () -> Unit,
    levelsViewModel: LifeManagementStatusPanelViewModel = hiltViewModel(),
    recentViewModel: RecentViewModel = hiltViewModel(),
) {
    val statuses by levelsViewModel.statuses.collectAsStateWithLifecycle()
    var expanded by rememberSaveable {
        mutableStateOf(
            when {
                commandDeckViewModel.isSessionModeCardExpanded() -> SESSION
                commandDeckViewModel.isCategoryExpanded("dashboard_life_management_status_expanded") -> LEVELS
                commandDeckViewModel.isCategoryExpanded("dashboard_ai_insights_expanded") -> INSIGHTS
                commandDeckViewModel.isCategoryExpanded("dashboard_recents_expanded", true) -> RECENTS
                else -> null
            },
        )
    }
    fun toggle(section: String) {
        expanded = if (expanded == section) null else section
        commandDeckViewModel.setSessionModeCardExpanded(expanded == SESSION)
        commandDeckViewModel.setCategoryExpanded("dashboard_life_management_status_expanded", expanded == LEVELS)
        commandDeckViewModel.setCategoryExpanded("dashboard_ai_insights_expanded", expanded == INSIGHTS)
        commandDeckViewModel.setCategoryExpanded("dashboard_recents_expanded", expanded == RECENTS)
    }
    val blocked = statuses.count { it.generalStatus == GeneralStatus.BLOCKED }
    val defected = statuses.count { it.generalStatus == GeneralStatus.DEFECTED }
    val healthy = statuses.isNotEmpty() && blocked + defected == 0
    val levelColor = when {
        blocked + defected > 0 -> MaterialTheme.colorScheme.error
        healthy -> if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) ReadyDarkContentColor else ReadyLightContentColor
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            AccordionHeader(
                icon = Icons.Outlined.SwapVert,
                title = stringResource(R.string.deck_session),
                expanded = expanded == SESSION,
                onClick = { toggle(SESSION) },
                summary = {
                    Text(if (session.mode == SessionMode.UNSET) stringResource(R.string.deck_mode_unset) else session.mode.title,
                        color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                },
            )
            if (expanded == SESSION) {
                SessionModeDashboard(
                    state = session,
                    latestSessionReason = latestSessionReason,
                    onModeSelected = onModeSelected,
                    isExpanded = true,
                    onExpandedChange = {},
                    onOpenModeContext = onOpenModeContext,
                    showHeader = false,
                )
            }
            AccordionDivider()
            AccordionHeader(
                icon = Icons.Outlined.Layers,
                title = stringResource(R.string.deck_levels_short),
                expanded = expanded == LEVELS,
                onClick = { toggle(LEVELS) },
                summary = {
                    if (healthy) Icon(Icons.Outlined.CheckCircle, null, Modifier.size(16.dp), tint = levelColor)
                    Text(when {
                        blocked + defected > 0 -> stringResource(R.string.deck_levels_problem, blocked, defected)
                        healthy -> stringResource(R.string.deck_ok)
                        else -> stringResource(R.string.deck_loading)
                    }, color = levelColor, style = MaterialTheme.typography.labelMedium)
                },
            )
            if (expanded == LEVELS) {
                LifeManagementStatusPanelSection(
                    statuses = statuses,
                    isExpanded = true,
                    onToggleExpanded = {},
                    onSave = levelsViewModel::save,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    showSummary = false,
                )
            }
            AccordionDivider()
            AccordionHeader(
                icon = Icons.Outlined.AutoAwesome,
                title = stringResource(R.string.deck_ai_insights),
                expanded = expanded == INSIGHTS,
                onClick = { toggle(INSIGHTS) },
            )
            if (expanded == INSIGHTS) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { insightsContent() }
            }
            AccordionDivider()
            DashboardRecentsSection(
                onItemClick = onRecentItemClick,
                recentViewModel = recentViewModel,
                commandDeckViewModel = commandDeckViewModel,
                expandedOverride = expanded == RECENTS,
                contentModifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                onToggleOverride = { toggle(RECENTS) },
                header = { isExpanded, onToggle, onShowAll ->
                    AccordionHeader(
                        icon = Icons.Outlined.History,
                        title = stringResource(R.string.deck_recent),
                        expanded = isExpanded,
                        onClick = onToggle,
                        action = { TextButton(onClick = onShowAll) { Text(stringResource(R.string.deck_all)) } },
                    )
                },
            )
        }
    }
}

@Composable
private fun AccordionDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
}

@Composable
internal fun AccordionHeader(
    icon: ImageVector,
    title: String,
    expanded: Boolean,
    onClick: () -> Unit,
    summary: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val primary = MaterialTheme.colorScheme.primary
    val headerColor = MaterialTheme.colorScheme.primaryContainer
        .copy(alpha = if (expanded) 0.70f else 0.40f)
        .compositeOver(MaterialTheme.colorScheme.surface)
    Row(
        modifier = Modifier.fillMaxWidth().background(headerColor)
            .drawBehind {
                if (expanded) drawLine(primary,
                    start = Offset(2.dp.toPx(), 12.dp.toPx()),
                    end = Offset(2.dp.toPx(), size.height - 12.dp.toPx()),
                    strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
            }
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 50.dp).padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = if (expanded) primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.SansSerif,
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
        // Keep long problem summaries from squeezing the section title off screen.
        if (summary != null) Row(Modifier.widthIn(max = 130.dp), horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically) { summary() }
        action?.invoke()
        IconButton(onClick = onClick) {
            Icon(if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = stringResource(if (expanded) R.string.deck_collapse else R.string.deck_expand),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
