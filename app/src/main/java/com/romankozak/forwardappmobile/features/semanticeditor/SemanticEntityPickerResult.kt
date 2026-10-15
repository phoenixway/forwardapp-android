package com.romankozak.forwardappmobile.features.semanticeditor

import androidx.lifecycle.SavedStateHandle
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitypicker.CANONICAL_ENTITY_PICKER_RESULT_KEY
import com.romankozak.forwardappmobile.features.entitypicker.CanonicalEntityRefCodec

internal const val PENDING_SEMANTIC_RELATION_PICKER_KIND_KEY = "semantic_relation_picker_kind"

/** Removes the navigation result before dispatch so recreation cannot replay a mutation. */
internal fun consumeSemanticRelationPickerResult(
    savedStateHandle: SavedStateHandle,
    onResult: (SemanticRelationPickerKind, Set<CanonicalEntityRef>) -> Unit,
): Boolean {
    val encoded = savedStateHandle.get<List<String>>(CANONICAL_ENTITY_PICKER_RESULT_KEY) ?: return false
    val kindName = savedStateHandle.get<String>(PENDING_SEMANTIC_RELATION_PICKER_KIND_KEY) ?: return false
    val kind = SemanticRelationPickerKind.valueOf(kindName)
    savedStateHandle.remove<List<String>>(CANONICAL_ENTITY_PICKER_RESULT_KEY)
    savedStateHandle.remove<String>(PENDING_SEMANTIC_RELATION_PICKER_KIND_KEY)
    onResult(kind, encoded.map(CanonicalEntityRefCodec::decode).toSet())
    return true
}
