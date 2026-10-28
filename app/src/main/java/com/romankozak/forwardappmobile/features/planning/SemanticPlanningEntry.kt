package com.romankozak.forwardappmobile.features.planning

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef

/** Call only for exact Orientation identities; Aspect never gains implicit participation. */
@Composable
internal fun SemanticPlanningEntry(ref: CanonicalEntityRef, onOpen: () -> Unit) {
    if (orientationPlanningTarget(ref) != null && ref.id.isNotBlank()) TextButton(onClick = onOpen) { Text("Planning") }
}
