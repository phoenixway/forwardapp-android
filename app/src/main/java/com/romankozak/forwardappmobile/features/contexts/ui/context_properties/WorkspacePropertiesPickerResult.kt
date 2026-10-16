package com.romankozak.forwardappmobile.features.contexts.ui.context_properties

import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitypicker.CANONICAL_ENTITY_PICKER_RESULT_KEY
import com.romankozak.forwardappmobile.features.entitypicker.CanonicalEntityRefCodec

internal const val PENDING_WORKSPACE_BINDING_PICKER_KEY = "workspace_binding_picker_pending"

/** Removes the result before dispatch so recreation cannot replay a binding mutation. */
internal fun consumeWorkspaceBindingPickerResult(
    savedStateHandle: SavedStateHandle,
    onResult: (Set<CanonicalEntityRef>) -> Unit,
): Boolean {
    if (savedStateHandle.get<Boolean>(PENDING_WORKSPACE_BINDING_PICKER_KEY) != true) return false
    val encoded = savedStateHandle.get<List<String>>(CANONICAL_ENTITY_PICKER_RESULT_KEY) ?: return false
    savedStateHandle.remove<List<String>>(CANONICAL_ENTITY_PICKER_RESULT_KEY)
    savedStateHandle.remove<Boolean>(PENDING_WORKSPACE_BINDING_PICKER_KEY)
    onResult(encoded.map(CanonicalEntityRefCodec::decode).toSet())
    return true
}
