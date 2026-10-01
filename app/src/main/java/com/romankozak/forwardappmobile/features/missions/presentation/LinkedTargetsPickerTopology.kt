package com.romankozak.forwardappmobile.features.missions.presentation

import com.romankozak.forwardappmobile.data.hierarchy.ChooserHierarchyItem

/**
 * A target-only picker may display several V2 appearances of one Workspace.
 * The tree key is occurrence-specific; selection and preselection use targetId.
 */
internal data class LinkedPickerNode(
    val key: String,
    val targetId: String,
    val title: String,
    val parentKey: String?,
)

internal fun buildLinkedPickerNodes(
    options: List<ProjectOption>,
    occurrences: List<ChooserHierarchyItem>,
): List<LinkedPickerNode> {
    val optionsById = options.associateBy { it.id }
    val admittedOccurrences = occurrences.filter { it.id in optionsById }
    val occurrenceKeys = admittedOccurrences
        .mapTo(hashSetOf()) { it.occurrence.placementId.value }

    val occurrenceNodes = admittedOccurrences.map { item ->
        val placementId = item.occurrence.placementId.value
        val parentPlacementId = item.occurrence.parentPlacementId?.value
        LinkedPickerNode(
            key = "occurrence:$placementId",
            targetId = item.id,
            title = optionsById.getValue(item.id).name,
            parentKey = parentPlacementId
                ?.takeIf { it in occurrenceKeys }
                ?.let { "occurrence:$it" },
        )
    }

    // Compatibility for semantic targets without a presented V2 occurrence:
    // flat and target-only, with no inferred V1 or WorkspaceBinding parent.
    val presentedTargetIds = admittedOccurrences.mapTo(hashSetOf()) { it.id }
    val unplacedTargets = options
        .distinctBy { it.id }
        .filter { it.id !in presentedTargetIds }
        .map { option ->
            LinkedPickerNode(
                key = "target-only:${option.id}",
                targetId = option.id,
                title = option.name,
                parentKey = null,
            )
        }

    return occurrenceNodes + unplacedTargets
}

internal data class LinkedPickerTree(
    val roots: List<LinkedPickerNode>,
    val childrenByKey: Map<String, List<LinkedPickerNode>>,
    val visibleKeys: Set<String>,
)

internal fun buildLinkedPickerTree(
    nodes: List<LinkedPickerNode>,
    query: String,
    showDescendants: Boolean,
): LinkedPickerTree {
    val byKey = nodes.associateBy { it.key }
    val children = nodes
        .filter { it.parentKey in byKey }
        .groupBy { requireNotNull(it.parentKey) }
        .mapValues { (_, children) -> children.sortedBy { it.title.lowercase() } }
    val roots = nodes
        .filter { it.parentKey !in byKey }
        .sortedBy { it.title.lowercase() }

    if (query.isBlank()) {
        return LinkedPickerTree(
            roots = roots,
            childrenByKey = children,
            visibleKeys = nodes.mapTo(hashSetOf()) { it.key },
        )
    }

    val matches = nodes.filter { it.title.contains(query, ignoreCase = true) }
    val visibleKeys = mutableSetOf<String>()
    matches.forEach { match ->
        var current: LinkedPickerNode? = match
        while (current != null && visibleKeys.add(current.key)) {
            current = current.parentKey?.let(byKey::get)
        }
    }

    if (showDescendants) {
        val queue = ArrayDeque(matches)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            children[current.key].orEmpty().forEach { child ->
                if (visibleKeys.add(child.key)) {
                    queue.add(child)
                }
            }
        }
    }

    return LinkedPickerTree(
        roots = roots,
        childrenByKey = children,
        visibleKeys = visibleKeys,
    )
}

internal fun linkedPickerExpandedKeysForQuery(
    nodes: List<LinkedPickerNode>,
    query: String,
): Set<String> {
    if (query.isBlank()) return emptySet()

    val byKey = nodes.associateBy { it.key }
    val expandedKeys = mutableSetOf<String>()
    nodes.filter { it.title.contains(query, ignoreCase = true) }.forEach { node ->
        var parentKey = node.parentKey
        while (parentKey != null && expandedKeys.add(parentKey)) {
            parentKey = byKey[parentKey]?.parentKey
        }
    }
    return expandedKeys
}
