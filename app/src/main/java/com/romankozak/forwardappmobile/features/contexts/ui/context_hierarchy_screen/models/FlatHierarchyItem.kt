package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus

/**
 * Read-only normal-hierarchy display item.
 *
 * Its project is never a Room Context and must not be routed into Context
 * mutation APIs. Legacy actions are enabled separately by stable-id backing
 * capability; the display payload remains presentation-only.
 */
data class FlatHierarchyPresentationItem(
    val project: HierarchyContextPresentationNode,
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
        /** Read-only presentation; never reconstructed as a persistable Context. */
        val presentation: HierarchyContextPresentationNode

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
        override val presentation: HierarchyContextPresentationNode,
        override val linkedBeaconIds: Set<String>,
        override val isLinkedAppearance: Boolean = false,
    ) : ProjectLike {
        override val id: String = presentation.id
        override val title: String = presentation.name
        override val isCanonicalWorkspace: Boolean = false
    }

    data class WorkspaceNode(
        override val presentation: HierarchyContextPresentationNode,
        override val linkedBeaconIds: Set<String>,
        override val isLinkedAppearance: Boolean = false,
    ) : ProjectLike {
        override val id: String = presentation.id
        override val title: String = presentation.name
        override val isCanonicalWorkspace: Boolean = true
    }
}

const val NO_GROUP_NODE_ID = "virtual:no-group"
const val NO_BEACON_NODE_ID = "virtual:no-beacon"
