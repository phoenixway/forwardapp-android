package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.romankozak.forwardappmobile.core.data.models.entities.tactical.TacticalIterationStatus
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.BottomPanelTokens
import com.romankozak.forwardappmobile.features.mainscreen.bottompanels.common.bottomPanelColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
internal fun TacticalIterationDeadlineLine(
    iterationStatus: TacticalIterationStatus?,
    iterationDurationDays: Int?,
) {
    val durationDays = iterationDurationDays?.takeIf { it > 0 } ?: return
    val colors = bottomPanelColors()
    if (iterationStatus != TacticalIterationStatus.ACTIVE) {
        Text(
            text = "Тривалість: $durationDays дн після старту циклу",
            modifier = Modifier.padding(horizontal = BottomPanelTokens.ContentHorizontalPadding),
            style = MaterialTheme.typography.labelSmall,
            color = colors.mutedContent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        return
    }
    val deadline = calculateTacticalIterationDeadlineMillis(durationDays)
    val now = System.currentTimeMillis()
    val isUrgent = deadline - now <= 24L * 60L * 60L * 1000L
    val textColor =
        if (isUrgent) {
            MaterialTheme.colorScheme.error
        } else {
            colors.mutedContent
        }
    Text(
        text = "Дедлайн: ${formatTacticalDeadline(deadline)}",
        modifier = Modifier.padding(horizontal = BottomPanelTokens.ContentHorizontalPadding),
        style = MaterialTheme.typography.labelSmall,
        color = textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

internal fun buildStreamBudgetLabel(
    budgetPercent: Int,
    iterationDurationHours: Int?,
): String {
    val totalHours = iterationDurationHours?.takeIf { it > 0 } ?: return "$budgetPercent%"
    val streamHours = totalHours * budgetPercent / 100.0
    return "${formatBudgetHours(streamHours)} год"
}

internal fun formatBudgetHours(hours: Double): String =
    if (hours % 1.0 == 0.0) {
        hours.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", hours)
    }

internal fun calculateTacticalIterationDeadlineMillis(durationDays: Int): Long =
    Calendar.getInstance()
        .apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
            add(Calendar.DAY_OF_YEAR, durationDays - 1)
        }.timeInMillis

internal fun formatTacticalDeadline(timestamp: Long): String =
    SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(timestamp)
