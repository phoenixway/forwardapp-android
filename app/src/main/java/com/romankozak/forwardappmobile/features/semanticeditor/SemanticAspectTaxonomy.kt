package com.romankozak.forwardappmobile.features.semanticeditor

import com.romankozak.forwardappmobile.data.orientation.CanonicalAspect
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityClass
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitypicker.CanonicalEntityPickerRequest
import com.romankozak.forwardappmobile.features.entitypicker.CanonicalEntitySelectionMode
import com.romankozak.forwardappmobile.features.semantic.SemanticEntityClass
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType

data class AspectChildItem(val aspectId: String, val title: String) {
    fun destination() = NavTarget.SemanticEntityEditor(aspectId)
}

/** Direct semantic children only; canonical sibling order, with stable identity tie-break. */
internal fun aspectDirectChildren(parentId: String, live: List<CanonicalAspect>): List<AspectChildItem> =
    live.filter { !it.subject.isDeleted && it.subject.subjectType == ManagedSubjectType.ASPECT.name && it.node.parentAspectId == parentId }
        .sortedWith(compareBy<CanonicalAspect>({ it.node.aspectOrder }, { it.subject.id }))
        .map { AspectChildItem(it.subject.id, it.subject.title) }

/** SEMANTIC_NOT_HIERARCHY: canonical Aspect taxonomy, never H1/Navigation ancestry. */
internal fun aspectParentExclusions(current: CanonicalAspect, live: List<CanonicalAspect>): Set<String> {
    val children = live.groupBy { it.node.parentAspectId }
    val excluded = linkedSetOf<String>()
    val pending = ArrayDeque<String>()
    pending.add(current.subject.id)
    while (pending.isNotEmpty()) {
        val id = pending.removeFirst()
        if (!excluded.add(id)) continue
        children[id].orEmpty().forEach { pending.addLast(it.subject.id) }
    }
    current.node.parentAspectId?.let(excluded::add)
    return excluded
}

internal fun SemanticEntityEditorModel.aspectParentRequest(): CanonicalEntityPickerRequest? {
    if (entityClass != SemanticEntityClass.ASPECT || isSaving || isRelationMutationInFlight) return null
    return CanonicalEntityPickerRequest(
        allowedClasses = setOf(CanonicalEntityClass.ASPECT),
        excludedRefs = (aspectParentExcludedIds + id).mapTo(hashSetOf()) { CanonicalEntityRef.Aspect(it) },
        selectionMode = CanonicalEntitySelectionMode.SINGLE,
    )
}
