package com.romankozak.forwardappmobile.ui.screens.daymanagement.dayplan.tasklist

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.romankozak.forwardappmobile.data.database.models.ScoringStatusValues


@Composable
fun EnhancedScoreStatusBadge(
    scoringStatus: String,
    displayScore: Double?,
) {


    if (scoringStatus != ScoringStatusValues.NOT_ASSESSED) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text(
                text = displayScore?.toString() ?: scoringStatus,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}