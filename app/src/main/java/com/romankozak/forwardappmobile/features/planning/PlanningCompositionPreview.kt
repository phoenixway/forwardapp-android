package com.romankozak.forwardappmobile.features.planning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwner
import com.romankozak.forwardappmobile.data.planning.*

internal data class PlanningCompositionListUi(val frame: PlanningCommitmentCompositionProjection,
    val open: (PlanningParticipantSummary) -> Unit, val context: (PlanningParticipantSummary) -> Unit)

/** Call once per list, not from each card. All cards share the same observable graph snapshot. */
@Composable
internal fun rememberPlanningCompositionList(): PlanningCompositionListUi {
    val model: PlanningCompositionViewModel = hiltViewModel(key = "planning-composition-list")
    val frame by model.projection.collectAsStateWithLifecycle()
    return remember(frame, model) { PlanningCompositionListUi(frame, model::open, model::context) }
}

@Composable
internal fun PlanningCompositionPreview(owner: PlanningReferenceOwner, ui: PlanningCompositionListUi,
    compact: Boolean = false, modifier: Modifier = Modifier) {
    val current = ui.frame.forOwner(owner) ?: return
    val parents = ui.frame.outgoing(current.commitment.id)
    val children = ui.frame.incoming(current.commitment.id)
    if (parents.isEmpty() && (compact || children.isEmpty())) return
    Column(modifier.padding(top = 4.dp)) {
        if (parents.isNotEmpty()) {
            val title = parents.first().participant.entity.title
            Text("↑ Part of · $title${if (parents.size > 1) " +${parents.size - 1}" else ""}",
                modifier = Modifier.clickable {
                    if (parents.size == 1) ui.open(parents.single().participant) else ui.context(current)
                }.padding(vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (!compact && children.isNotEmpty()) {
            Text("Components · ${children.size}", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { ui.context(current) }.padding(vertical = 4.dp))
        }
    }
}
