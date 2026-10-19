package com.romankozak.forwardappmobile.sync

import com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningReferenceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.withoutEmbeddedMainBeaconTopology
import com.romankozak.forwardappmobile.core.data.models.sync.withoutEmbeddedWorkspaceTopology

/**
 * Generation one uses the already-loaded full dependency frame for reference-bearing pushes.
 * A reference-only delta must not assume the peer already has its Mission/Quest/Day owner,
 * canonical participant, scope extensions, recurring carriers or source endpoint.
 * ACK remains limited to exact dirty versions. Other deltas retain their existing narrow path.
 */
internal fun planningReferenceTransportClosure(full: SnapshotBundle, rows: List<PlanningReferenceEntity>): SnapshotBundle =
    full.copy(
        planningReferences = rows,
        contextParentLinks = emptyList(),
        mainBeaconParentLinks = emptyList(),
        mainBeacons = full.mainBeacons.map { it.withoutEmbeddedMainBeaconTopology() },
        workspaces = full.workspaces?.map { it.withoutEmbeddedWorkspaceTopology() },
        backlogItems = emptyList(),
        backlogOrders = emptyList(),
    )
