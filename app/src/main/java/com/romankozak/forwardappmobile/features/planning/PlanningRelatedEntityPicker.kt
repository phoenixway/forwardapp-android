package com.romankozak.forwardappmobile.features.planning

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceOwner

@Composable
fun PlanningRelatedEntityPicker(owner: PlanningReferenceOwner, onDismiss: () -> Unit) {
    val model: PlanningReferencesViewModel = hiltViewModel()
    PlanningEntityPickerDialog(onDismiss, onSelected = { model.attachRelated(owner, it) }, host = model)
}
