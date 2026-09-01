package com.romankozak.forwardappmobile.data.workspace.capability

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceDirectionEntryEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceDirectionEntryProvenance
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDirectionEntryDao
import com.romankozak.forwardappmobile.database.AppDatabase

internal class CanonicalDirectionEntryLifecycle(
    private val database: AppDatabase,
    private val entryDao: WorkspaceDirectionEntryDao,
) {
    suspend fun tombstone(
        entryId: String,
        now: Long,
    ) {
        database.withTransaction {
            val entry = requireMutableEntry(entryId)
            entryDao.upsert(listOf(entry.bumpDirectionVersion(now).copy(isDeleted = true)))
        }
    }

    suspend fun tombstoneMany(
        entryIds: List<String>,
        now: Long,
    ) {
        if (entryIds.isEmpty()) return

        database.withTransaction {
            val requestedIds = entryIds.distinct()
            val entries = entryDao.getByIds(requestedIds).associateBy { it.id }
            val changes =
                requestedIds.mapNotNull { id ->
                    val entry = entries[id] ?: return@mapNotNull null
                    if (entry.isDeleted) {
                        null
                    } else {
                        require(entry.hasCanonicalDirectionProvenance()) {
                            "Direction entry has unsupported provenance"
                        }
                        entry.bumpDirectionVersion(now).copy(isDeleted = true)
                    }
                }

            if (changes.isNotEmpty()) entryDao.upsert(changes)
        }
    }

    suspend fun tombstoneOwnedEntriesForWorkspaces(
        workspaceIds: Collection<String>,
        now: Long,
    ): Int {
        val owners = workspaceIds.map(String::trim).filter(String::isNotEmpty).toSet()
        if (owners.isEmpty()) return 0

        return database.withTransaction {
            val entries = owners.flatMap { entryDao.getLiveForWorkspace(it) }
            if (entries.isNotEmpty()) {
                entryDao.upsert(entries.map { it.bumpDirectionVersion(now).copy(isDeleted = true) })
            }
            entries.size
        }
    }

    suspend fun tombstoneWorkspaceLinksTargeting(
        targetWorkspaceIds: Collection<String>,
        now: Long,
    ): Int {
        val targets =
            targetWorkspaceIds
                .asSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toSet()
        if (targets.isEmpty()) return 0

        return database.withTransaction {
            val matches =
                entryDao.getAll().filter { entry ->
                    !entry.isDeleted &&
                        entry.orientationId == null &&
                        entry.targetWorkspaceId in targets
                }
            require(matches.all { it.hasCanonicalDirectionProvenance() }) {
                "Direction entries contain unsupported provenance"
            }

            if (matches.isNotEmpty()) {
                entryDao.upsert(
                    matches.map { it.bumpDirectionVersion(now).copy(isDeleted = true) },
                )
            }
            matches.size
        }
    }

    private suspend fun requireMutableEntry(entryId: String): WorkspaceDirectionEntryEntity {
        val entry =
            requireNotNull(entryDao.getById(entryId)) {
                "Direction entry does not exist"
            }
        require(!entry.isDeleted) { "Direction entry is deleted" }
        require(entry.hasCanonicalDirectionProvenance()) {
            "Direction entry has unsupported provenance"
        }
        return entry
    }
}

internal fun WorkspaceDirectionEntryEntity.bumpDirectionVersion(now: Long) =
    copy(
        updatedAt = now,
        syncedAt = null,
        version = version + 1L,
    )

internal fun WorkspaceDirectionEntryEntity.hasCanonicalDirectionProvenance(): Boolean =
    provenance == WorkspaceDirectionEntryProvenance.LEGACY_DIRECTION_ITEM.name ||
        provenance == WorkspaceDirectionEntryProvenance.CANONICAL_ONLY.name
