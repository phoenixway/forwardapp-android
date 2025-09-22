package com.romankozak.forwardappmobile.ui.screens.goaledit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.romankozak.forwardappmobile.data.database.models.ScoringStatus

@Composable
fun ScoringStatusSelector(
    selectedStatus: ScoringStatus,
    onStatusSelected: (ScoringStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statuses = ScoringStatus.entries.toTypedArray()
    val labels =
        mapOf(
            ScoringStatus.NOT_ASSESSED to "Unset",
            ScoringStatus.ASSESSED to "Set",
            ScoringStatus.IMPOSSIBLE_TO_ASSESS to "Impossible",
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
