package com.romankozak.forwardappmobile.features.mainscreen.bottompanels

import androidx.compose.runtime.Composable
import com.romankozak.forwardappmobile.features.missions.presentation.LinkPickerTab
import com.romankozak.forwardappmobile.features.missions.presentation.LinkedTargetsPickerDialog
import com.romankozak.forwardappmobile.features.missions.presentation.ProjectOption

@Composable
internal fun TodayContextPickerHost(
    visible: Boolean,
    contextOptions: List<ProjectOption>,
    onDismiss: () -> Unit,
    onEntitySelected: suspend (com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntitySummary) -> Unit,
) {
    if (!visible) return

    com.romankozak.forwardappmobile.features.planning.PlanningEntityPickerDialog(onDismiss, onEntitySelected)
}
