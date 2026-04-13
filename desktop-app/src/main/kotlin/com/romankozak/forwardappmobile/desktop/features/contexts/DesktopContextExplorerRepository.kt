package com.romankozak.forwardappmobile.desktop.features.contexts

import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextSummary
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextTreeNode
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextStatus
import com.romankozak.forwardappmobile.shared.contracts.contexts.SharedContextView

interface DesktopContextExplorerRepository {
    fun loadTree(query: String): List<SharedContextTreeNode>
}

class InMemoryDesktopContextExplorerRepository : DesktopContextExplorerRepository {
    private val contexts =
        listOf(
            SharedContextSummary(
                id = "core",
                name = "Core Level",
                description = "Ключові системи і стратегічні контексти верхнього рівня.",
                parentId = null,
                status = SharedContextStatus.InProgress,
                defaultView = SharedContextView.Dashboard,
                score = 91,
                isCompleted = false,
            ),
            SharedContextSummary(
                id = "work",
                name = "Work System",
                description = "Операційний workbench для задач, backlog і tactical flow.",
                parentId = "core",
                status = SharedContextStatus.InProgress,
                defaultView = SharedContextView.Backlog,
                score = 84,
                isCompleted = false,
            ),
            SharedContextSummary(
                id = "health",
                name = "Health",
                description = "Контури self-management і регулярних review.",
                parentId = "core",
                status = SharedContextStatus.Planning,
                defaultView = SharedContextView.Dashboard,
                score = 66,
                isCompleted = false,
            ),
            SharedContextSummary(
                id = "scripts",
                name = "Scripts Library",
                description = "Автоматизації, tooling, migration utilities.",
                parentId = "work",
                status = SharedContextStatus.NoPlan,
                defaultView = SharedContextView.Connections,
                score = 53,
                isCompleted = false,
            ),
            SharedContextSummary(
                id = "desktop",
                name = "Desktop App",
                description = "Новий desktop shell, shared contracts і desktop feature slices.",
                parentId = "work",
                status = SharedContextStatus.InProgress,
                defaultView = SharedContextView.Backlog,
                score = 78,
                isCompleted = false,
            ),
        )

    override fun loadTree(query: String): List<SharedContextTreeNode> {
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
        val childCountByParentId = filtered.groupingBy { it.parentId }.eachCount()
        val sorted = filtered.sortedWith(compareBy<SharedContextSummary> { it.parentId ?: "" }.thenBy { it.name })

        return sorted.map { context ->
            SharedContextTreeNode(
                context = context,
                depth = depthOf(context, filtered.associateBy { it.id }),
                childCount = childCountByParentId[context.id] ?: 0,
            )
        }
    }

    private fun depthOf(
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
