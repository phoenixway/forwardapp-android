package com.romankozak.forwardappmobile.core.sync

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance

/**
 * Transport boundary for legacy Context persistence after canonical Workspace cut-over.
 *
 * CANONICAL_ONLY + no legacy source means the same-id Context has permanently
 * surrendered operational ownership. A later backup/sync payload may preserve
 * a Context tombstone as historical evidence, but it must never make that
 * Context live again.
 *
 * Workspace tombstones remain retirement authority: deleting a canonical
 * Workspace does not hand ownership back to the legacy Context.
 */
internal fun WorkspaceEntity.isCanonicalContextRetirementAuthority(): Boolean =
    provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
        sourceContextId == null

internal fun canonicalRetiredContextIds(
    localWorkspaces: Iterable<WorkspaceEntity>,
    incomingWorkspaces: Iterable<WorkspaceEntity> = emptyList(),
): Set<String> =
    buildSet {
        localWorkspaces
            .filter { it.isCanonicalContextRetirementAuthority() }
            .mapTo(this) { it.id }

        incomingWorkspaces
            .filter { it.isCanonicalContextRetirementAuthority() }
            .mapTo(this) { it.id }
    }

internal suspend fun AppDatabase.canonicalRetiredContextIds(
    incomingWorkspaces: Iterable<WorkspaceEntity> = emptyList(),
): Set<String> =
    canonicalRetiredContextIds(
        localWorkspaces = workspaceDao().getAll(),
        incomingWorkspaces = incomingWorkspaces,
    )

internal suspend fun AppDatabase.isCanonicalRetiredContextId(id: String): Boolean =
    workspaceDao()
        .getById(id)
        ?.isCanonicalContextRetirementAuthority() == true

internal fun <T> Iterable<T>.withoutLiveRetiredContexts(
    retiredContextIds: Set<String>,
    id: (T) -> String,
    isDeleted: (T) -> Boolean,
): List<T> =
    filterNot { item ->
        !isDeleted(item) && id(item) in retiredContextIds
    }
