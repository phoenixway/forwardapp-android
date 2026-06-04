package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconReadinessStatus

/**
 * Represents a single project inside the flattened hierarchy list along with its depth level.
 */
data class FlatHierarchyItem(
    val project: Context,
    val level: Int,
)

data class BeaconRootedHierarchyItem(
    val node: BeaconRootedHierarchyNode,
    val level: Int,
)

sealed interface BeaconRootedHierarchyNode {
    val id: String
    val title: String

    data class Group(
        override val id: String,
        override val title: String,
        val beaconCount: Int,
    ) : BeaconRootedHierarchyNode

    data class Beacon(
        override val id: String,
        override val title: String,
        val readinessStatus: MainBeaconReadinessStatus,
        val relatedContextCount: Int,
    ) : BeaconRootedHierarchyNode

    data object NoBeacon : BeaconRootedHierarchyNode {
        override val id: String = NO_BEACON_NODE_ID
        override val title: String = "No beacon"
    }

    data object NoGroup : BeaconRootedHierarchyNode {
        override val id: String = NO_GROUP_NODE_ID
        override val title: String = "No group"
    }

    data class ContextNode(
        val context: Context,
        val linkedBeaconIds: Set<String>,
    ) : BeaconRootedHierarchyNode {
        override val id: String = context.id
        override val title: String = context.name
    }
}

const val NO_GROUP_NODE_ID = "virtual:no-group"
const val NO_BEACON_NODE_ID = "virtual:no-beacon"
