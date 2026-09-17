package com.romankozak.forwardappmobile.core.data.models.entities.orientation

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * One-time compatibility-cutover marker for a reserved System Workspace tag
 * collection.
 *
 * Tag membership itself remains solely in [WorkspaceTagRefEntity]. This marker
 * distinguishes an authoritative canonical empty collection from an
 * unseeded schema-168 collection.
 *
 * [legacyIngressClosedAt] separately records that legacy transport is no
 * longer allowed to establish this collection. Startup establishment alone
 * deliberately leaves that bounded compatibility ingress open.
 */
@Entity(
    tableName = "system_workspace_tag_seed_states",
    foreignKeys = [
        ForeignKey(
            entity = WorkspaceEntity::class,
            parentColumns = ["id"],
            childColumns = ["workspaceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SystemWorkspaceTagSeedStateEntity(
    @PrimaryKey val workspaceId: String,
    val seededAt: Long,
    val legacyIngressClosedAt: Long? = null,
)
