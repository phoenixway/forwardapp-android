package com.romankozak.forwardappmobile.data.hierarchy

import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId

/**
 * H4.0a clipboard carrier. Placement identity remains separate from Workspace,
 * Context compatibility ids, ManagedSubject ids, and presentation ids.
 *
 * No paste semantics are selected here. The mixed-domain planner decides which
 * structural, semantic, operational, and/or target operands each destination implies.
 */
enum class HierarchyClipboardOperation {
    COPY,
    CUT,
    LINK,
}

enum class HierarchyClipboardSourceKind {
    WORKSPACE,
    CONTEXT_COMPATIBILITY,
    BEACON,
}

data class HierarchyClipboardOccurrence(
    val occurrence: HierarchyOccurrenceRef,
    val sourceKind: HierarchyClipboardSourceKind,
    /**
     * Optional CURRENT compatibility id, for example a legacy Main Beacon id.
     * Never substitutes for occurrence.placementId or occurrence.target.
     */
    val legacySourceId: String? = null,
)

sealed interface HierarchyClipboardDestination {
    /**
     * A concrete canonical hierarchy parent occurrence.
     */
    data class ParentOccurrence(
        val parentPlacementId: PlacementId?,
    ) : HierarchyClipboardDestination

    /**
     * Concrete Beacon destination occurrence plus the CURRENT compatibility id
     * needed by the independently owned operational-owner semantic surface.
     *
     * The legacy id never substitutes for occurrence.placementId. A Beacon may
     * have multiple hierarchy appearances, so structural paste must identify
     * the exact destination occurrence.
     */
    data class BeaconOwner(
        val legacyBeaconId: String,
        val occurrence: HierarchyOccurrenceRef,
    ) : HierarchyClipboardDestination {
        init {
            require(legacyBeaconId.isNotBlank()) {
                "Legacy Beacon destination id must not be blank"
            }
        }
    }

    /**
     * Synthetic presentation destination, never a persisted hierarchy target.
     */
    data object NoBeacon : HierarchyClipboardDestination

    /**
     * Synthetic Group/NoGroup destination.
     *
     * [groupSubjectId] is the canonical MAIN_BEACON_GROUP ManagedSubject id.
     * null means explicit NoGroup. Legacy Group ids belong only to compatibility
     * boundaries and must never enter the occurrence-authority command plan.
     */
    data class Group(
        val groupSubjectId: String?,
    ) : HierarchyClipboardDestination {
        init {
            require(groupSubjectId == null || groupSubjectId.isNotBlank()) {
                "Canonical Group subject id must not be blank"
            }
        }
    }
}

data class HierarchyClipboardIntent(
    val operation: HierarchyClipboardOperation,
    val sources: List<HierarchyClipboardOccurrence>,
    val destination: HierarchyClipboardDestination? = null,
) {
    init {
        require(sources.isNotEmpty()) {
            "Hierarchy clipboard intent requires at least one concrete occurrence"
        }
        require(sources.map { it.occurrence.placementId }.distinct().size == sources.size) {
            "Hierarchy clipboard intent contains duplicate placement ids"
        }
    }
}
