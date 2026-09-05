package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity

/**
 * Represents a single project inside the flattened hierarchy list along with its depth level.
 */
data class FlatHierarchyItem(
    val project: Context,
    val level: Int,
    val isLinkedAppearance: Boolean = false,
    val isCanonicalWorkspace: Boolean = false,
)

data class OrientationHierarchyItem(
    val node: OrientationHierarchyNode,
    val level: Int,
)

sealed interface OrientationHierarchyNode {
    val id: String
    val title: String

    sealed interface ProjectLike : OrientationHierarchyNode {
        val contextProjection: Context
        val linkedBeaconIds: Set<String>
        val isLinkedAppearance: Boolean
        val isCanonicalWorkspace: Boolean
    }

    data class Group(
        override val id: String,
        override val title: String,
        val beaconCount: Int,
    ) : OrientationHierarchyNode

    data class Beacon(
        override val id: String,
        override val title: String,
        val readinessStatus: MainBeaconReadinessStatus,
        val relatedContextCount: Int,
    ) : OrientationHierarchyNode

    data object NoBeacon : OrientationHierarchyNode {
        override val id: String = NO_BEACON_NODE_ID
        override val title: String = "No beacon"
    }

    data object NoGroup : OrientationHierarchyNode {
        override val id: String = NO_GROUP_NODE_ID
        override val title: String = "No group"
    }

    data class ContextNode(
        val context: Context,
        override val linkedBeaconIds: Set<String>,
        override val isLinkedAppearance: Boolean = false,
    ) : ProjectLike {
        override val id: String = context.id
        override val title: String = context.name
        override val contextProjection: Context = context
        override val isCanonicalWorkspace: Boolean = false
    }

    data class WorkspaceNode(
        val workspace: WorkspaceEntity,
        override val linkedBeaconIds: Set<String>,
        override val isLinkedAppearance: Boolean = false,
    ) : ProjectLike {
        override val id: String = workspace.id
        override val title: String =
            workspace.nameOverride?.trim()?.takeIf { it.isNotEmpty() } ?: workspace.id
        override val contextProjection: Context =
            Context(
                id = workspace.id,
                name = title,
                description = workspace.descriptionOverride,
                parentId = workspace.parentWorkspaceId,
                createdAt = workspace.createdAt,
                updatedAt = workspace.updatedAt,
                order = workspace.workspaceOrder,
            )
        override val isCanonicalWorkspace: Boolean = true
    }
}

const val NO_GROUP_NODE_ID = "virtual:no-group"
const val NO_BEACON_NODE_ID = "virtual:no-beacon"
