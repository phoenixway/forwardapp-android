package com.romankozak.forwardappmobile.features.missions.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIteration
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIterationStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

@Composable
internal fun TacticalIterationStatusPanel(
    activeIteration: TacticalIteration?,
    iterationDurationDays: Int?,
    onOpenIterationArchive: () -> Unit,
    onSetIterationDuration: () -> Unit,
    onPlanCycle: () -> Unit,
    onStartCycle: () -> Unit,
    onStartNewCycle: () -> Unit,
    onStartOpenEndedIteration: () -> Unit,
    onFinishIteration: () -> Unit,
) {
    val status = buildTacticalIterationStatus(activeIteration)
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("tactical-iteration-status"),
            shape = RoundedCornerShape(16.dp),
            color = status.containerColor(),
            contentColor = status.contentColor(),
            border = BorderStroke(1.dp, status.contentColor().copy(alpha = 0.26f)),
        ) {
            Row(
                Modifier.padding(start = 8.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(6.dp).background(status.contentColor(), CircleShape))
                Column(Modifier.weight(1f)) {
                    Text(status.label, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        buildTacticalIterationStatusSubtitle(activeIteration, iterationDurationDays),
                        style = MaterialTheme.typography.labelSmall,
                        color = status.contentColor().copy(alpha = 0.74f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                TacticalIterationStatusMoreMenu(
                    iterationStatus = activeIteration?.status,
                    onOpenIterationArchive = onOpenIterationArchive,
                    onSetIterationDuration = onSetIterationDuration,
                    onPlanCycle = onPlanCycle,
                    onStartCycle = onStartCycle,
                    onStartNewCycle = onStartNewCycle,
                    onStartOpenEndedIteration = onStartOpenEndedIteration,
                    onFinishIteration = onFinishIteration,
                )
            }
        }
    }
}

@Composable
private fun TacticalIterationStatusMoreMenu(
    iterationStatus: TacticalIterationStatus?,
    onOpenIterationArchive: () -> Unit,
    onSetIterationDuration: () -> Unit,
    onPlanCycle: () -> Unit,
    onStartCycle: () -> Unit,
    onStartNewCycle: () -> Unit,
    onStartOpenEndedIteration: () -> Unit,
    onFinishIteration: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val isDraft = iterationStatus == TacticalIterationStatus.DRAFT
    val isActive = iterationStatus == TacticalIterationStatus.ACTIVE
    val hasIteration = iterationStatus != null
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Дії ітерації")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (!hasIteration) {
                DropdownMenuItem(
                    text = { Text("Draft new cycle") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    },
                    onClick = {
                        expanded = false
                        onPlanCycle()
                    },
                )
            }
            if (!isActive) {
                DropdownMenuItem(
                    text = { Text(if (isDraft) "Start cycle" else "Start cycle now") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    },
                    onClick = {
                        expanded = false
                        onStartCycle()
                    },
                )
            }
            if (isActive) {
                DropdownMenuItem(
                    text = { Text("Finish cycle") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.CheckCircle, contentDescription = null)
                    },
                    onClick = {
                        expanded = false
                        onFinishIteration()
                    },
                )
            }
            if (hasIteration) {
                DropdownMenuItem(
                    text = { Text("New cycle") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    },
                    onClick = {
                        expanded = false
                        onStartNewCycle()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("New open draft") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    onStartOpenEndedIteration()
                },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Set duration") },
                leadingIcon = {
                    Icon(imageVector = Icons.Outlined.Today, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    onSetIterationDuration()
                },
            )
            DropdownMenuItem(
                text = { Text("Cycle history") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Archive, contentDescription = null)
                },
                onClick = {
                    expanded = false
                    onOpenIterationArchive()
                },
            )
        }
    }
}

private data class TacticalIterationStatusUi(
    val label: String,
    val state: TacticalIterationStatusState,
)

private enum class TacticalIterationStatusState {
    DEVELOPMENT,
    EXECUTION,
    ABSENT,
}

private fun buildTacticalIterationStatus(
    activeIteration: TacticalIteration?,
): TacticalIterationStatusUi =
    when {
        activeIteration == null ->
            TacticalIterationStatusUi(
                label = "Ітерація відсутня",
                state = TacticalIterationStatusState.ABSENT,
            )
        activeIteration.status == TacticalIterationStatus.DRAFT ->
            TacticalIterationStatusUi(
                label = "Ітерація в розробці",
                state = TacticalIterationStatusState.DEVELOPMENT,
            )
        activeIteration.status == TacticalIterationStatus.ACTIVE ->
            TacticalIterationStatusUi(
                label = "Ітерація у виконанні",
                state = TacticalIterationStatusState.EXECUTION,
            )
        else ->
            TacticalIterationStatusUi(
                label = "Ітерація завершена",
                state = TacticalIterationStatusState.ABSENT,
            )
    }

private fun buildTacticalIterationStatusSubtitle(
    activeIteration: TacticalIteration?,
    iterationDurationDays: Int?,
): String {
    if (activeIteration == null) {
        return "Тактичну ітерацію ще не запущено"
    }
    if (activeIteration.status != TacticalIterationStatus.DRAFT) {
        return activeIteration.title
    }
    val today = System.currentTimeMillis()
    val draftEndAt =
        iterationDurationDays
            ?.takeIf { it > 0 }
            ?.let { today + it * MILLIS_PER_DAY }
    return if (draftEndAt != null) {
        "${formatCyclePanelDate(today)} - ${formatCyclePanelDate(draftEndAt)}"
    } else {
        "Сьогодні -"
    }
}

private fun formatCyclePanelDate(timestamp: Long): String =
    SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestamp))

@Composable
private fun TacticalIterationStatusUi.containerColor() =
    when (state) {
        TacticalIterationStatusState.DEVELOPMENT -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f)
        TacticalIterationStatusState.EXECUTION -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f)
        TacticalIterationStatusState.ABSENT -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

@Composable
private fun TacticalIterationStatusUi.contentColor() =
    when (state) {
        TacticalIterationStatusState.DEVELOPMENT -> MaterialTheme.colorScheme.onSecondaryContainer
        TacticalIterationStatusState.EXECUTION -> MaterialTheme.colorScheme.onPrimaryContainer
        TacticalIterationStatusState.ABSENT -> MaterialTheme.colorScheme.onSurfaceVariant
    }
