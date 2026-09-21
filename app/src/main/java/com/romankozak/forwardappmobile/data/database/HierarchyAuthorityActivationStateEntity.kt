package com.romankozak.forwardappmobile.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local-only durable marker for the one-time GENERAL hierarchy authority cutover.
 *
 * Presence means the bounded CURRENT V1 -> Canonical V2 establishment transaction
 * completed successfully. It is not sync payload and must never be used as a
 * source for hierarchy topology.
 */
@Entity(tableName = "hierarchy_authority_activation_state")
data class HierarchyAuthorityActivationStateEntity(
    @PrimaryKey val hierarchyId: String,
    val version: Int,
    val activatedAt: Long,
)
