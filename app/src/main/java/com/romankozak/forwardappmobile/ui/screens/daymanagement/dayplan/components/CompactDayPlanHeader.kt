package com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.DayPlan
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompactDayPlanHeader(
    dayPlan: DayPlan?,
    totalPointsEarned: Int,
    totalPointsAvailable: Int,
    bestCompletedPoints: Int,
    completedTasks: Int,
    totalTasks: Int,
    onNavigateToPreviousDay: () -> Unit,
    onNavigateToNextDay: () -> Unit,
    isNextDayNavigationEnabled: Boolean,
    onSettingsClick: () -> Unit,
    onAddTaskClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    val glowAlpha by animateFloatAsState(
        targetValue = 0.15f,
        animationSpec = tween(2000),
        label = "header_glow"
    )
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 0.dp,
                start = 16.dp,
                end = 16.dp
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.08f + glowAlpha),
                            primaryColor.copy(alpha = 0.03f),
                            primaryColor.copy(alpha = 0.08f + glowAlpha)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.3f),
                            primaryColor.copy(alpha = 0.1f),
                            primaryColor.copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateToPreviousDay) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Попередній день",
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
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
            IconButton(onClick = onNavigateToNextDay, enabled = isNextDayNavigationEnabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Наступний день",
                    tint =
                    if (isNextDayNavigationEnabled) {
                        colorScheme.onSurface
                    } else {
                        colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    },
                )
            }
        }
    }
}