package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.BigPictureUiState

data class BigPictureStructuralItem(val ref: H1TypedNode, val title: String)

/** Index of persisted identities, not a second UI tree or structural authority. */
class BigPictureStructuralIndex(read: BigPictureReadComposition) {
    private val graph = read.structuralGraph
    private val childCounts = graph.edges.groupingBy { it.parent }.eachCount()
    private val titles = buildMap<H1TypedNode, String> {
        read.nodes.forEach { put(H1TypedNode.Occurrence(it.placement.id.value), it.entity?.title ?: "Сутність недоступна") }
        read.genericContainerTitles.forEach { (id, title) ->
            put(H1TypedNode.Container(id), title)
        }
    }
    val items = graph.edges.map { BigPictureStructuralItem(it.node, titles.getValue(it.node)) }
    val sections = read.genericContainerTitles.keys.associate { "presentation:$it" to H1TypedNode.Container(it) }

    fun childCount(node: H1TypedNode): Int = childCounts[node] ?: 0

    fun children(parent: H1StructuralParent): List<BigPictureStructuralItem> =
        graph.childrenOf((parent as? H1StructuralParent.Node)?.ref).map {
            BigPictureStructuralItem(it.node, titles.getValue(it.node))
        }

    /** Complete mixed parent chain, including sections between occurrence ancestors. */
    fun breadcrumbs(destination: H1StructuralParent): List<BreadcrumbItem> {
        var cursor = (destination as? H1StructuralParent.Node)?.ref
        val path = mutableListOf<H1TypedNode>()
        while (cursor != null) {
            path += cursor
            cursor = graph.parentOf(cursor)
        }
        return path.asReversed().mapIndexed { level, ref ->
            BreadcrumbItem(
                id = when (ref) {
                    is H1TypedNode.Occurrence -> ref.id
                    is H1TypedNode.Container -> "presentation:${ref.id}"
                },
                name = titles.getValue(ref),
                level = level,
                target = BreadcrumbTarget.OrientationNode,
                placementId = (ref as? H1TypedNode.Occurrence)?.id,
            )
        }
    }
}

/** Historical scopes are not canonical mutation destinations. */
fun BigPictureUiState.Ready.structuralDestination(): H1StructuralParent? = when {
    activeNode != null -> H1StructuralParent.Node(H1TypedNode.Occurrence(activeNode.placement.id.value))
    activeScope == null -> H1StructuralParent.Root
    else -> structure?.sections?.get(activeScope.id)?.let(H1StructuralParent::Node)
}

fun BigPictureReadEntry.structuralRef(index: BigPictureStructuralIndex): H1TypedNode? = when (this) {
    is BigPictureReadEntry.Occurrence -> H1TypedNode.Occurrence(node.placement.id.value)
    is BigPictureReadEntry.Container -> index.sections[section.id]
}
