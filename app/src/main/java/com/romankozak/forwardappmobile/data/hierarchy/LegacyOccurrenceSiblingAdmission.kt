package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.data.hierarchy.container.BigPicturePresentationRepository
import com.romankozak.forwardappmobile.data.hierarchy.container.H1TypedNode
import com.romankozak.forwardappmobile.data.hierarchy.container.projectH1UnifiedStructuralGraph
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*

/** Legacy occurrence-only admission, not an alternative mixed structural writer. */
internal suspend fun AppDatabase.requireLegacyOccurrenceSiblingLevels(
    state: Collection<HierarchyPlacement>,
    parents: Collection<PlacementId?>,
    allowUniformMembershipSource: Boolean,
) {
    val presentation = BigPicturePresentationRepository(this).snapshot()
    if (presentation.containers.none { !it.isDeleted }) return
    val graph = projectH1UnifiedStructuralGraph(state, presentation)
    parents.distinct().forEach { parent ->
        val physicalChildren = state.filter {
            !it.isDeleted && it.hierarchyId == HierarchyId.GENERAL && it.parentPlacementId == parent
        }.map { H1TypedNode.Occurrence(it.id.value) }
        val typedParent = parent?.value?.let(H1TypedNode::Occurrence)
        val typedChildren = graph.childrenOf(typedParent).map { it.node }
        val physicalSet = physicalChildren.toSet()
        val pureOccurrenceLevel = physicalChildren.size == typedChildren.size && physicalSet == typedChildren.toSet()
        val uniformMembershipSource = if (!allowUniformMembershipSource || physicalChildren.isEmpty()) false else {
            val immediateParent = graph.parentOf(physicalChildren.first())
            immediateParent is H1TypedNode.Container &&
                physicalChildren.all { graph.parentOf(it) == immediateParent } &&
                graph.childrenOf(immediateParent).let { children ->
                    children.size == physicalChildren.size && children.map { it.node }.toSet() == physicalSet
                }
        }
        if (!pureOccurrenceLevel && !uniformMembershipSource) {
            throw HierarchySiblingSetMismatchException("Legacy structural write cannot address mixed typed siblings")
        }
    }
}
