package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus

/**
 * Represents a single project inside the flattened hierarchy list along with its depth level.
 */
data class FlatHierarchyItem(
    val project: Context,
    val level: Int,
    val isLinkedAppearance: Boolean = false,
)

data class OrientationHierarchyItem(
    val node: OrientationHierarchyNode,
    val level: Int,
)

sealed interface OrientationHierarchyNode {
    val id: String
    val title: String

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
        val linkedBeaconIds: Set<String>,
        val isLinkedAppearance: Boolean = false,
    ) : OrientationHierarchyNode {
        override val id: String = context.id
        override val title: String = context.name
    }
}

const val NO_GROUP_NODE_ID = "virtual:no-group"
const val NO_BEACON_NODE_ID = "virtual:no-beacon"
