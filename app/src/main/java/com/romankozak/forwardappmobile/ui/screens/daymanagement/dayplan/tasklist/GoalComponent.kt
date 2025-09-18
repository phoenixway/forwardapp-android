package com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.tasklist

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ScoringStatus

/**
 * Відображає бейдж зі статусом оцінки завдання або цілі.
 * @param scoringStatus Статус оцінки (наприклад, не оцінено, в процесі).
 * @param displayScore Конкретне значення оцінки для відображення.
 */
@Composable
fun EnhancedScoreStatusBadge(
    scoringStatus: ScoringStatus,
    displayScore: Double?
) {
    // Implementation depends on your ScoringStatus enum
    // This is a placeholder - adjust based on your actual implementation
    if (scoringStatus != ScoringStatus.NOT_ASSESSED) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = displayScore?.toString() ?: scoringStatus.name,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}