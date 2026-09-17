package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation

/**
 * Read-only input for hierarchy presentation and tree construction.
 *
 * This deliberately excludes Room Context persistence and mutation fields.
 * Both legacy Context rows and shell-free canonical System Workspaces may
 * produce this DTO, but it can never be converted back into a Context entity.
 */
data class HierarchyContextPresentationNode(
    val id: String,
    val name: String,
    val description: String?,
    val parentId: String?,
    val order: Long,
    val roleCode: String?,
    val tags: List<String>,
)

/**
 * Read-only hierarchy tree contract. It is intentionally separate from
 * raw Context mutation contracts.
 */
data class HierarchyPresentationData(
    val allProjects: List<HierarchyContextPresentationNode> = emptyList(),
    val topLevelProjects: List<HierarchyContextPresentationNode> = emptyList(),
    val childMap: Map<String, List<HierarchyContextPresentationNode>> = emptyMap(),
)

/**
 * Raw Context backing admitted by the presentation hierarchy.
 *
 * This is intentionally not a display model: values are the original persisted
 * Context objects, joined by stable id only for legacy mutation and focused
 * compatibility paths. Presentation-only (including shell-free System) ids
 * never receive a backing entry.
 */
internal data class RawContextHierarchyBacking(
    val rawContexts: List<Context> = emptyList(),
    val rawContextsById: Map<String, Context> = emptyMap(),
    val rawChildMap: Map<String, List<Context>> = emptyMap(),
)

fun Context.toHierarchyPresentationNode(): HierarchyContextPresentationNode =
    HierarchyContextPresentationNode(
        id = id,
        name = name,
        description = description,
        parentId = parentId,
        order = order,
        roleCode = roleCode,
        tags = tags.orEmpty(),
    )

fun ContextPresentation.toHierarchyPresentationNode(): HierarchyContextPresentationNode =
    HierarchyContextPresentationNode(
        id = id,
        name = name,
        description = description,
        parentId = parentId,
        order = order,
        roleCode = roleCode,
        tags = tags.orEmpty(),
    )
