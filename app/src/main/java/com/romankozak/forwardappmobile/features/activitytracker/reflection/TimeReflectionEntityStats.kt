package com.romankozak.forwardappmobile.features.activitytracker.reflection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.core.data.models.entities.ActivityEntityType
import com.romankozak.forwardappmobile.features.activitytracker.entities.displayName

@Composable
fun TimeReflectionEntitySections(stats: List<EntityTimeStat>) {
    val dayTypes =
        setOf(
            ActivityEntityType.DAY_TASK,
            ActivityEntityType.DAY_FOCUS,
            ActivityEntityType.DAY_RESPONSIBILITY,
            ActivityEntityType.DAY_THEME,
        )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EntityStatSection("Сутності дня", stats.filter { it.link.entityType in dayTypes })
        EntityStatSection("Контексти", stats.filter { it.link.entityType == ActivityEntityType.CONTEXT })
        EntityStatSection("Цілі беклогу", stats.filter { it.link.entityType == ActivityEntityType.GOAL })
        if (stats.isNotEmpty()) {
            Text(
                "Одна активність враховується в кожній пов’язаній сутності.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EntityStatSection(
    title: String,
    stats: List<EntityTimeStat>,
) {
    if (stats.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Spacer(Modifier.height(4.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        stats.forEach { stat ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stat.title, fontWeight = FontWeight.Medium)
                        Text(
                            buildString {
                                append(stat.link.entityType.displayName())
                                if (stat.trackedDayCount > 1) append(" · ${stat.trackedDayCount} дні")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(formatReflectionDuration(stat.durationMillis))
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { stat.share.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
