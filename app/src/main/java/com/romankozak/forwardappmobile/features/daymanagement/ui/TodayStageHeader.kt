package com.romankozak.forwardappmobile.features.daymanagement.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementRuntimeState
import kotlin.math.abs

@Composable
internal fun TodayStageHeader(
    selectedTab: DayManagementTab,
    runtimeState: DayManagementRuntimeState,
    onSelectTab: (DayManagementTab) -> Unit,
    onReady: (DayManagementTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stages = DayManagementTab.todaySubTabs()
    if (selectedTab !in stages) return
    var menuExpanded by remember { mutableStateOf(false) }
    val stageColor = MaterialTheme.colorScheme.onSurface
    val swipeThreshold = with(LocalDensity.current) { 28.dp.toPx() }

    Surface(
        modifier = modifier.fillMaxWidth().testTag("today-stage-controller")
            .pointerInput(selectedTab, onSelectTab, swipeThreshold) {
            var dragDistance = 0f
            detectHorizontalDragGestures(
                onDragStart = { dragDistance = 0f },
                onHorizontalDrag = { change, amount -> dragDistance += amount; change.consume() },
                onDragEnd = {
                    stageAfterSwipe(selectedTab, dragDistance, swipeThreshold)?.let(onSelectTab)
                },
            )
        },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = stageColor,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.clickable { menuExpanded = true }
                        .semantics { contentDescription = "Вибрати стадію дня" }
                        .height(40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(selectedTab.icon, contentDescription = null, modifier = Modifier.size(17.dp))
                    Text(
                        text = selectedTab.stageTitle(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    stages.forEach { stage ->
                        DropdownMenuItem(
                            text = { Text(stage.stageTitle()) },
                            leadingIcon = { Icon(stage.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (stage == selectedTab) Icon(Icons.Filled.Check, contentDescription = "Поточна стадія")
                            },
                            onClick = { menuExpanded = false; onSelectTab(stage) },
                        )
                    }
                }
            }
            TodayStageIndicators(stages, selectedTab, runtimeState)
            Surface(
                onClick = { onReady(selectedTab) },
                enabled = selectedTab.readyAction(runtimeState) != null || selectedTab.nextTodayStage() != null,
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Text(
                    text = "Ready",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun TodayStageIndicators(
    stages: List<DayManagementTab>,
    selectedTab: DayManagementTab,
    runtimeState: DayManagementRuntimeState,
) {
    val readyColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    val emptyColor = MaterialTheme.colorScheme.surfaceContainerLow
    val readyCount = stages.count { it.isReady(runtimeState) }
    Canvas(
        modifier = Modifier.width(104.dp).height(30.dp).semantics {
            contentDescription = "$readyCount із 6 стадій готові; поточна: ${selectedTab.stageTitle()}"
        },
    ) {
        val centerY = size.height / 2f
        val radius = 3.5.dp.toPx()
        val step = (size.width - radius * 2) / (stages.size - 1)
        drawLine(outlineColor.copy(alpha = 0.55f), Offset(radius, centerY), Offset(size.width - radius, centerY),
            strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
        stages.forEachIndexed { index, stage ->
            val center = Offset(radius + step * index, centerY)
            val ready = stage.isReady(runtimeState)
            drawCircle(if (ready) readyColor else emptyColor, radius, center)
            drawCircle(if (ready) readyColor else outlineColor, radius, center, style = Stroke(1.5.dp.toPx()))
            if (stage == selectedTab) {
                drawCircle(readyColor, radius + 3.dp.toPx(), center, style = Stroke(1.5.dp.toPx()))
            }
        }
    }
}

internal fun stageAfterSwipe(selected: DayManagementTab, distance: Float, threshold: Float): DayManagementTab? {
    if (abs(distance) < threshold) return null
    val stages = DayManagementTab.todaySubTabs()
    val index = stages.indexOf(selected)
    if (index < 0) return null
    return stages.getOrNull(index + if (distance < 0f) 1 else -1)
}

internal fun DayManagementTab.nextTodayStage(): DayManagementTab? {
    val stages = DayManagementTab.todaySubTabs()
    val index = stages.indexOf(this)
    return if (index >= 0) stages.getOrNull(index + 1) else null
}

internal fun DayManagementTab.isReady(state: DayManagementRuntimeState): Boolean =
    when (this) {
        DayManagementTab.DAY_START -> state.wokeAt != null
        DayManagementTab.DAY_THEMES -> state.dayThemesFinalizedAt != null
        DayManagementTab.DAY_FOCUSES -> state.dayFocusFinalizedAt != null
        DayManagementTab.DAY_PLAN -> state.dayPlanFinalizedAt != null
        DayManagementTab.JOURNAL -> state.finalizationStartedAt != null
        DayManagementTab.FINALIZATION -> state.sleepAt != null
        else -> false
    }

internal enum class TodayStageReadyAction {
    WAKE_UP,
    FINALIZE_THEMES,
    FINALIZE_FOCUS,
    FINALIZE_PLAN,
    START_FINALIZATION,
    SLEEP,
}

internal fun DayManagementTab.readyAction(state: DayManagementRuntimeState): TodayStageReadyAction? =
    when (this) {
        DayManagementTab.DAY_START -> if (!state.hasOpenOperationalDay) TodayStageReadyAction.WAKE_UP else null
        DayManagementTab.DAY_THEMES -> if (!isReady(state)) TodayStageReadyAction.FINALIZE_THEMES else null
        DayManagementTab.DAY_FOCUSES -> if (!isReady(state)) TodayStageReadyAction.FINALIZE_FOCUS else null
        DayManagementTab.DAY_PLAN -> if (!isReady(state)) TodayStageReadyAction.FINALIZE_PLAN else null
        DayManagementTab.JOURNAL -> if (!isReady(state)) TodayStageReadyAction.START_FINALIZATION else null
        DayManagementTab.FINALIZATION -> if (state.hasOpenOperationalDay) TodayStageReadyAction.SLEEP else null
        else -> null
    }

internal fun DayManagementTab.stageTitle(): String =
    when (this) {
        DayManagementTab.DAY_START -> "Start"
        DayManagementTab.DAY_THEMES -> "Themes"
        DayManagementTab.DAY_FOCUSES -> "Priorities"
        DayManagementTab.DAY_PLAN -> "Plan"
        DayManagementTab.JOURNAL -> "Journal"
        DayManagementTab.FINALIZATION -> "Finalization"
        else -> title
    }
