package com.romankozak.forwardappmobile.ui.screens.backlog.components.projectrealization

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.GoalList
import com.romankozak.forwardappmobile.data.database.models.ProjectStatus
import com.romankozak.forwardappmobile.data.database.models.ProjectTimeMetrics
import com.romankozak.forwardappmobile.ui.utils.formatDurationForUi

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DashboardContent(
    goalList: GoalList,
    onStatusUpdate: (ProjectStatus, String?) -> Unit,
    onToggleProjectManagement: (Boolean) -> Unit,
    onRecalculateTime: () -> Unit,
    projectTimeMetrics: ProjectTimeMetrics?,
) {
    var showStatusDialog by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedContent(
            targetState = goalList.isProjectManagementEnabled == true,
            label = "DashboardContentAnimation",
        ) { isManagementEnabled ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (!isManagementEnabled) {
                    EnableSupportCard(onEnable = { onToggleProjectManagement(true) })
                } else {
                    StatusDisplayCard(status = goalList.projectStatus ?: ProjectStatus.NO_PLAN, statusText = goalList.projectStatusText, onClick = {
                        showStatusDialog = true
                    })

                    projectTimeMetrics?.let { metrics ->
                        Spacer(modifier = Modifier.height(8.dp))
                        MetricsDisplayCard(metrics = metrics)
                    }

                    OutlinedButton(onClick = onRecalculateTime) {
                        Text("Recalculate")
                    }
                    Text(
                        "Тут будуть метрики та рекомендовані дії",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showStatusDialog) {
        UpdateStatusDialog(
            currentStatus = goalList.projectStatus ?: ProjectStatus.NO_PLAN,
            currentStatusText = goalList.projectStatusText ?: "",
            onDismissRequest = {
                showStatusDialog = false
            },
            onSave = { newStatus, newText ->
                onStatusUpdate(newStatus, newText)
                showStatusDialog = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusDisplayCard(
    status: ProjectStatus,
    statusText: String?,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Поточний статус", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(status.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (!statusText.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Редагувати статус",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun EnableSupportCard(onEnable: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Підтримка реалізації вимкнена", style = MaterialTheme.typography.titleMedium)
            Text(
                "Увімкніть, щоб відстежувати історію, статуси та інсайти по проекту.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onEnable) {
                Text("Увімкнути підтримку")
            }
        }
    }
}

@Composable
private fun MetricsDisplayCard(metrics: ProjectTimeMetrics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            MetricItem(label = "Час за сьогодні", value = formatDurationForUi(metrics.timeToday))
            MetricItem(label = "Загальний час", value = formatDurationForUi(metrics.timeTotal))
        }
    }
}

@Composable
private fun MetricItem(
    label: String,
    value: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
