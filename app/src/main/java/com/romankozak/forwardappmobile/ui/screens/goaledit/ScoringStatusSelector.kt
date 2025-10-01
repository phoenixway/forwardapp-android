package com.romankozak.forwardappmobile.ui.screens.goaledit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.romankozak.forwardappmobile.data.database.models.ScoringStatusValues

@Composable
fun ScoringStatusSelector(
    selectedStatus: String,
    onStatusSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statuses = listOf(ScoringStatusValues.NOT_ASSESSED, ScoringStatusValues.ASSESSED, ScoringStatusValues.IMPOSSIBLE_TO_ASSESS)
    val labels =
        mapOf(
            ScoringStatusValues.NOT_ASSESSED to "Unset",
            ScoringStatusValues.ASSESSED to "Set",
            ScoringStatusValues.IMPOSSIBLE_TO_ASSESS to "Impossible",
        )
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        statuses.forEachIndexed { index, status ->
            SegmentedButton(
                selected = selectedStatus == status,
                onClick = { onStatusSelected(status) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = statuses.size),
            ) {
                Text(
                    text = labels[status] ?: "",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}