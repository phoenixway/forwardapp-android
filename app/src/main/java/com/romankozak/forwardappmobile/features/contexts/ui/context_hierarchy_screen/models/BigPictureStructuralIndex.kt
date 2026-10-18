package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.BigPictureUiState

data class BigPictureStructuralItem(val ref: H1TypedNode, val title: String)

/** Index of persisted identities, not a second UI tree or structural authority. */
class BigPictureStructuralIndex(read: BigPictureReadComposition) {
    private val graph = read.structuralGraph
    private val titles = buildMap<H1TypedNode, String> {
        read.nodes.forEach { put(H1TypedNode.Occurrence(it.placement.id.value), it.entity?.title ?: "Сутність недоступна") }
        read.genericContainerTitles.forEach { (id, title) ->
            put(H1TypedNode.Container(id), title)
        }
    }
    val items = graph.edges.map { BigPictureStructuralItem(it.node, titles.getValue(it.node)) }
    val sections = read.genericContainerTitles.keys.associate { "presentation:$it" to H1TypedNode.Container(it) }

    fun children(parent: H1StructuralParent): List<BigPictureStructuralItem> =
        graph.childrenOf((parent as? H1StructuralParent.Node)?.ref).map {
            BigPictureStructuralItem(it.node, titles.getValue(it.node))
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
