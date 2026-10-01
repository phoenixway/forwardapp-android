package com.romankozak.forwardappmobile.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local-only durable marker for the one-time GENERAL hierarchy authority cutover.
 *
 * Presence means the bounded establishment transaction completed successfully.
 * The version records the strongest local post-establishment storage invariant;
 * older supported versions converge transactionally without recapturing V1.
 * This is not sync payload and must never be used as a hierarchy source.
 */
@Entity(tableName = "hierarchy_authority_activation_state")
data class HierarchyAuthorityActivationStateEntity(
    @PrimaryKey val hierarchyId: String,
    val version: Int,
    val activatedAt: Long,
)
