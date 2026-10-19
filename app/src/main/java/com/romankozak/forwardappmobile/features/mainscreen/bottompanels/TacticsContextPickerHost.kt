package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.features.missions.presentation.AttachmentOption
import com.romankozak.forwardappmobile.features.missions.presentation.LinkPickerTab
import com.romankozak.forwardappmobile.features.missions.presentation.LinkedTargetsPickerDialog
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption

@Composable
internal fun TacticsContextPickerHost(
    visible: Boolean,
    projectOptions: List<ProjectOption>,
    onDismiss: () -> Unit,
    onEntitySelected: suspend (com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary) -> Unit,
) {
    if (!visible) return

    com.romankozak.forwardappmobile.features.planning.PlanningEntityPickerDialog(onDismiss, onEntitySelected)
}
