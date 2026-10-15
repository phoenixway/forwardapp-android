package com.romankozak.forwardappmobile.features.entitypicker

import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityClass
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef

enum class CanonicalEntitySelectionMode {
    SINGLE,
    MULTI,
}

data class CanonicalEntityPickerRequest(
    val allowedClasses: Set<CanonicalEntityClass>,
    val excludedRefs: Set<CanonicalEntityRef> = emptySet(),
    val selectionMode: CanonicalEntitySelectionMode = CanonicalEntitySelectionMode.SINGLE,
    val initiallySelected: Set<CanonicalEntityRef> = emptySet(),
) {
    init {
        require(allowedClasses.isNotEmpty()) { "Picker must allow at least one entity class" }
        require(initiallySelected.all { it.entityClass in allowedClasses && it !in excludedRefs }) {
            "Initial picker selection must be eligible"
        }
        require(selectionMode == CanonicalEntitySelectionMode.MULTI || initiallySelected.size <= 1) {
            "Single-select picker cannot start with multiple selections"
        }
    }
}

object CanonicalEntityRefCodec {
    fun encode(ref: CanonicalEntityRef): String = "${ref.entityClass.name}:${ref.id}"

    fun decode(value: String): CanonicalEntityRef {
        val (type, id) = value.split(':', limit = 2).also { require(it.size == 2 && it[1].isNotBlank()) }
        return when (CanonicalEntityClass.valueOf(type)) {
            CanonicalEntityClass.ORIENTATION -> CanonicalEntityRef.Orientation(id)
            CanonicalEntityClass.ASPECT -> CanonicalEntityRef.Aspect(id)
            CanonicalEntityClass.WORKSPACE -> CanonicalEntityRef.Workspace(id)
        }
    }
}

fun togglePickerSelection(
    request: CanonicalEntityPickerRequest,
    current: Set<CanonicalEntityRef>,
    ref: CanonicalEntityRef,
): Set<CanonicalEntityRef> {
    require(ref.entityClass in request.allowedClasses && ref !in request.excludedRefs) {
        "Picker cannot select an ineligible entity"
    }
    return when (request.selectionMode) {
        CanonicalEntitySelectionMode.SINGLE -> setOf(ref)
        CanonicalEntitySelectionMode.MULTI -> if (ref in current) current - ref else current + ref
    }
}

const val CANONICAL_ENTITY_PICKER_RESULT_KEY = "canonical_entity_picker_result"
