package com.romankozak.forwardappmobile.ui.screens.projectscreen.components.projectrealization

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ProjectExecutionLog
import com.romankozak.forwardappmobile.data.database.models.ProjectLogEntryTypeValues
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogContent(
    logs: List<ProjectExecutionLog>,
    isManagementEnabled: Boolean,
) {
    if (!isManagementEnabled) {
        PlaceholderContent(text = "Увімкніть підтримку реалізації на Дашборді, щоб бачити історію.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (logs.isEmpty()) {
            item {
                Text(
                    "Історія проекту порожня.",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            items(logs.sortedByDescending { it.timestamp }) { log ->
                LogEntryItem(log)
            }
        }
    }
}

@Composable
private fun LogEntryItem(log: ProjectExecutionLog) {
    val icon =
        when (log.type) {
            ProjectLogEntryTypeValues.STATUS_CHANGE -> Icons.Default.TrendingUp
            ProjectLogEntryTypeValues.COMMENT -> Icons.Default.Comment
            ProjectLogEntryTypeValues.AUTOMATIC -> Icons.Default.ReceiptLong
            ProjectLogEntryTypeValues.INSIGHT -> Icons.Default.Lightbulb
            ProjectLogEntryTypeValues.MILESTONE -> Icons.Default.Flag
            else -> Icons.Default.Info
        }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = log.type,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                log.details?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(log.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Composable
internal fun PlaceholderContent(text: String) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
