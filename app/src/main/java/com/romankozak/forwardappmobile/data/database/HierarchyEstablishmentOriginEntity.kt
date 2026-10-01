package com.romankozak.forwardappmobile.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local-only lineage metadata for choosing a future GENERAL establishment source.
 *
 * This records no hierarchy topology and is never part of SnapshotBundle.
 */
@Entity(tableName = "hierarchy_establishment_origin")
data class HierarchyEstablishmentOriginEntity(
    @PrimaryKey val hierarchyId: String,
    val origin: String,
)

enum class HierarchyEstablishmentOrigin {
    FRESH_NATIVE,
    LEGACY_UPGRADE_REQUIRES_CAPTURE,
    ESTABLISHED,
}
