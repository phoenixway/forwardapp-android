package com.romankozak.forwardappmobile.shared.domain.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextSummary
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextTreeNode

class ObserveContextTreeUseCase(
    private val repository: DesktopWorkspaceRepository,
) {
    suspend operator fun invoke(query: String): List<SharedContextTreeNode> {
        val contexts = repository.getContexts()
        val normalizedQuery = query.trim()
        val filtered =
            if (normalizedQuery.isBlank()) {
                contexts
            } else {
                contexts.filter { context ->
                    context.name.contains(normalizedQuery, ignoreCase = true) ||
                        context.description.orEmpty().contains(normalizedQuery, ignoreCase = true)
                }
            }
        val byId = filtered.associateBy { it.id }
        val childCountByParentId = filtered.groupingBy { it.parentId }.eachCount()
        val sorted =
            filtered.sortedWith(
                compareBy<SharedContextSummary> { it.parentId ?: "" }
                    .thenBy { it.name },
            )

        return sorted.map { context ->
            SharedContextTreeNode(
                context = context,
                depth = calculateDepth(context, byId),
                childCount = childCountByParentId[context.id] ?: 0,
            )
        }
    }

    private fun calculateDepth(
        context: SharedContextSummary,
        byId: Map<String, SharedContextSummary>,
    ): Int {
        var depth = 0
        var currentParentId = context.parentId
        while (currentParentId != null) {
            depth += 1
            currentParentId = byId[currentParentId]?.parentId
        }
        return depth
    }
}
