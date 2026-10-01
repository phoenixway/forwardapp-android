package com.romankozak.forwardappmobile.core.data.models.sync

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.workspace.WorkspaceSnapshot

/**
 * Current canonical transport carries GENERAL structure through H1.
 * Embedded Workspace topology remains accepted only as historical ingress.
 */
fun WorkspaceSnapshot.withoutEmbeddedWorkspaceTopology(): WorkspaceSnapshot {
    // Gson may deserialize historical JSON without provenance/sourceContextId by
    // allocating the object directly, bypassing Kotlin constructor defaults.
    // Normalize before any Kotlin data-class operation can forward that
    // runtime null into a non-null constructor parameter.
    val rawProvenance: String? = provenance
    val normalizedProvenance = rawProvenance ?: "CONTEXT_BACKED"
    val normalizedSourceContextId =
        if (normalizedProvenance == "CONTEXT_BACKED" && sourceContextId == null) {
            id
        } else {
            sourceContextId
        }

    return WorkspaceSnapshot(
        id = id,
        nameOverride = nameOverride,
        descriptionOverride = descriptionOverride,
        parentWorkspaceId = null,
        roleCode = roleCode,
        workspaceOrder = 0L,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        provenance = normalizedProvenance,
        sourceContextId = normalizedSourceContextId,
    )
}

fun WorkspaceEntity.toWorkspaceSnapshot(): WorkspaceSnapshot =
    WorkspaceSnapshot(
        id = id,
        nameOverride = nameOverride,
        descriptionOverride = descriptionOverride,
        parentWorkspaceId = null,
        roleCode = roleCode,
        workspaceOrder = 0L,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        provenance = provenance,
        sourceContextId = sourceContextId,
    )

fun WorkspaceSnapshot.toWorkspaceEntity(): WorkspaceEntity {
    // Gson does not invoke Kotlin constructor defaults when fields are absent.
    // Historical pre-provenance Workspace JSON can therefore carry a runtime
    // null here despite the non-null Kotlin transport declaration.
    val rawProvenance: String? = provenance
    val normalizedProvenance = rawProvenance ?: "CONTEXT_BACKED"
    val normalizedSourceContextId =
        if (normalizedProvenance == "CONTEXT_BACKED" && sourceContextId == null) {
            id
        } else {
            sourceContextId
        }

    return WorkspaceEntity(
        id = id,
        nameOverride = nameOverride,
        descriptionOverride = descriptionOverride,
        roleCode = roleCode,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        provenance = normalizedProvenance,
        sourceContextId = normalizedSourceContextId,
    )
}
