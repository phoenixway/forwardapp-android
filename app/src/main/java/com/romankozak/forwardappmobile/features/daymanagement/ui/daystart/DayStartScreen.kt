package com.romankozak.forwardappmobile.features.daymanagement.ui.daystart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementPhase
import com.romankozak.forwardappmobile.features.daymanagement.runtime.domain.DayManagementRuntimeState
import com.romankozak.forwardappmobile.features.daymanagement.ui.NeonTitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DayStartScreen(
    runtimeState: DayManagementRuntimeState,
    predictedDurationMinutes: Long?,
    onWakeUp: () -> Unit,
    onSleep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = System.currentTimeMillis()
    val phaseItems =
        listOf(
            PhaseStatusItem(
                title = "Теми дня",
                startAt = runtimeState.wokeAt,
                endAt = runtimeState.dayThemesFinalizedAt,
                currentPhase = runtimeState.currentPhase,
                activePhase = DayManagementPhase.PREPARATION,
                blockIfReached = runtimeState.dayThemesFinalizedAt == null &&
                    listOfNotNull(
                        runtimeState.dayFocusFinalizedAt,
                        runtimeState.dayPlanFinalizedAt,
                        runtimeState.implementationStartedAt,
                        runtimeState.finalizationStartedAt,
                        runtimeState.sleepAt,
                    ).isNotEmpty(),
            ),
            PhaseStatusItem(
                title = "Фокус дня",
                startAt =
                    runtimeState.dayThemesFinalizedAt
                        ?: runtimeState.wokeAt.takeIf { runtimeState.dayFocusFinalizedAt != null },
                endAt = runtimeState.dayFocusFinalizedAt,
                currentPhase = runtimeState.currentPhase,
                activePhase = DayManagementPhase.PREPARATION,
                blockIfReached = runtimeState.dayFocusFinalizedAt == null &&
                    listOfNotNull(
                        runtimeState.dayPlanFinalizedAt,
                        runtimeState.implementationStartedAt,
                        runtimeState.finalizationStartedAt,
                        runtimeState.sleepAt,
                    ).isNotEmpty(),
            ),
            PhaseStatusItem(
                title = "План дня",
                startAt = runtimeState.dayFocusFinalizedAt,
                endAt = runtimeState.dayPlanFinalizedAt,
                currentPhase = runtimeState.currentPhase,
                activePhase = DayManagementPhase.PREPARATION,
                blockIfReached = runtimeState.dayPlanFinalizedAt == null &&
                    listOfNotNull(
                        runtimeState.implementationStartedAt,
                        runtimeState.finalizationStartedAt,
                        runtimeState.sleepAt,
                    ).isNotEmpty(),
            ),
            PhaseStatusItem(
                title = "Реалізація цілей",
                startAt = runtimeState.implementationStartedAt,
                endAt = runtimeState.finalizationStartedAt ?: runtimeState.sleepAt,
                currentPhase = runtimeState.currentPhase,
                activePhase = DayManagementPhase.IMPLEMENTATION,
            ),
            PhaseStatusItem(
                title = "Фіналізація дня",
                startAt = runtimeState.finalizationStartedAt,
                endAt = runtimeState.sleepAt,
                currentPhase = runtimeState.currentPhase,
                activePhase = DayManagementPhase.FINALIZATION,
            ),
        )

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        NeonTitle(text = "Day Start")
        RuntimeStatusCard(
            runtimeState = runtimeState,
            phaseItems = phaseItems,
            predictedDurationMinutes = predictedDurationMinutes,
            now = now,
        )
        Button(
            onClick = onWakeUp,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
        ) {
            Text(
                text = "Проснувся!",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
        Button(
            onClick = onSleep,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
        ) {
            Text("Пішов спати")
        }
    }
}

private data class PhaseStatusItem(
    val title: String,
    val startAt: Long?,
    val endAt: Long?,
    val currentPhase: DayManagementPhase,
    val activePhase: DayManagementPhase,
    val blockIfReached: Boolean = false,
)

@Composable
private fun RuntimeStatusCard(
    runtimeState: DayManagementRuntimeState,
    phaseItems: List<PhaseStatusItem>,
    predictedDurationMinutes: Long?,
    now: Long,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Поточна фаза: ${runtimeState.currentPhase.title()}",
                style = MaterialTheme.typography.titleSmall,
            )
            StatusLine(
                title = "Прогноз тривалості дня",
                value = predictedDurationMinutes?.let(::formatDurationMinutes) ?: "Не задано",
            )
            if (!runtimeState.hasOpenOperationalDay) {
                Text(
                    text = "Поточний день ще не розпочато.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                StatusLine(
                    title = "Пробудження",
                    value = runtimeState.wokeAt?.let(::formatTimestamp) ?: "Ще не зафіксоване",
                )
                StatusLine(
                    title = "Теми дня",
                    value = runtimeState.dayThemesFinalizedAt?.let(::formatTimestamp) ?: "Ще не зафіксовані",
                )
                StatusLine(
                    title = "Фокус дня",
                    value = runtimeState.dayFocusFinalizedAt?.let(::formatTimestamp) ?: "Ще не зафіксований",
                )
                StatusLine(
                    title = "План дня",
                    value = runtimeState.dayPlanFinalizedAt?.let(::formatTimestamp) ?: "Ще не зафіксований",
                )
                Text(
                    text = "Фази дня",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                phaseItems.forEach { item ->
                    PhaseDurationRow(item = item, now = now)
                }
            }
        }
    }
}

@Composable
private fun StatusLine(
    title: String,
    value: String,
) {
    Text(
        text = "$title: $value",
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun PhaseDurationRow(
    item: PhaseStatusItem,
    now: Long,
) {
    val status = item.status(now)
    val duration = item.durationLabel(now)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        )
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Тривалість: $duration",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun PhaseStatusItem.status(now: Long): String =
    when {
        startAt == null && blockIfReached -> "Початок не був зафіксований"
        startAt == null -> "Ще не почалася"
        endAt != null -> "Завершена о ${formatTimestamp(endAt)}"
        currentPhase == activePhase -> "Триває зараз"
        currentPhase == DayManagementPhase.CLOSED -> "День завершено без окремої фіксації завершення"
        now >= startAt -> "Розпочата, але завершення ще не зафіксоване"
        else -> "Ще не почалася"
    }

private fun PhaseStatusItem.durationLabel(now: Long): String {
    val safeStart = startAt ?: return "ще не почалася"
    val effectiveEnd =
        when {
            endAt != null -> endAt
            currentPhase == activePhase -> now
            else -> null
        } ?: return "ще триває без фінальної мітки"
    return formatDuration(effectiveEnd - safeStart)
}

private fun DayManagementPhase.title(): String =
    when (this) {
        DayManagementPhase.PREPARATION -> "Підготовка"
        DayManagementPhase.IMPLEMENTATION -> "Реалізація"
        DayManagementPhase.FINALIZATION -> "Фіналізація"
        DayManagementPhase.CLOSED -> "День закрито"
    }

private fun formatTimestamp(timestamp: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun formatDuration(durationMillis: Long): String {
    val totalMinutes = (durationMillis.coerceAtLeast(0L) / 60_000L).toInt()
    return formatDurationMinutes(totalMinutes.toLong())
}

private fun formatDurationMinutes(totalMinutes: Long): String {
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "$hours год $minutes хв"
        hours > 0 -> "$hours год"
        else -> "$minutes хв"
    }
}
