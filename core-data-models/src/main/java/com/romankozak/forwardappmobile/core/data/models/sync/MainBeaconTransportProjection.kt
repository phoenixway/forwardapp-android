package com.romankozak.forwardappmobile.core.data.models.sync

import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.misc.MainBeaconSnapshot

/**
 * Removes obsolete embedded GENERAL-hierarchy metadata from current transport.
 * H1 owns placement; MainBeacon flat-list order remains live metadata and is
 * preserved in transport.
 */
fun MainBeaconSnapshot.withoutEmbeddedMainBeaconTopology(): MainBeaconSnapshot =
    copy(
        parentBeaconId = null,
    )
