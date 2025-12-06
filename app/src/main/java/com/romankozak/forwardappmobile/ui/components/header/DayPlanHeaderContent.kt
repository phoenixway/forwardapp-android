package com.romankozak.forwardappmobile.ui.components.header

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.DayPlan
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayPlanHeaderContent(
    dayPlan: DayPlan?,
    totalPointsEarned: Int,
    totalPointsAvailable: Int,
    bestCompletedPoints: Int,
    completedTasks: Int,
    totalTasks: Int,
) {
    val colorScheme = MaterialTheme.colorScheme

    val formattedDate =
        remember(dayPlan?.date) {
            dayPlan?.date?.let { dateMillis ->
                val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("uk"))
                Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).format(formatter)
            } ?: "План дня"
        }
    val progress =
        remember(totalPointsEarned, totalPointsAvailable) {
            if (totalPointsAvailable > 0) {
                (totalPointsEarned.toFloat() / totalPointsAvailable.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    val pointsLabel =
        buildString {
            val bestDayPoints = max(bestCompletedPoints, totalPointsEarned)
            append(totalPointsEarned)
            append(" / ")
            append(totalPointsAvailable)
            append(" / ")
            append(bestDayPoints)
            append(" балів")
        }
    val tasksLabel =
        if (totalTasks > 0) {
            "$completedTasks / $totalTasks задач"
        } else {
            "Завдань поки немає"
        }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth() // Fill the weight given by FAHeader
    ) {
        Text(
            text =
            formattedDate.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.Center,
        ) {
            HeaderInfoChip(
                icon = Icons.Filled.CheckCircle,
                text = pointsLabel,
                contentColor = colorScheme.primary,
            )
            HeaderInfoChip(
                icon = Icons.Outlined.Checklist,
                text = tasksLabel,
                contentColor = colorScheme.onSurface,
            )
        }

        if (totalPointsAvailable > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                trackColor = colorScheme.surfaceVariant,
                color = colorScheme.primary,
            )
        }
    }
}

@Composable
fun HeaderInfoChip(icon: ImageVector, text: String, contentColor: Color) {
  Surface(
    color = contentColor.copy(alpha = 0.12f),
    contentColor = contentColor,
    shape = RoundedCornerShape(12.dp),
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Icon(
        icon,
        contentDescription = null,
        modifier = Modifier.size(14.dp),
        tint = contentColor,
      )
      Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
      )
    }
  }
}
