package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceTagRefEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import javax.inject.Inject
import javax.inject.Singleton

/** Canonical transport merge boundary for independently-versioned tag refs. */
@Singleton
class CanonicalWorkspaceTagTransportStore
    @Inject
    constructor(
        private val database: AppDatabase,
    ) {
        suspend fun loadAll(): List<WorkspaceTagRefEntity> =
            database.workspaceTagRefDao().getAll()

        suspend fun loadChangedSince(timestamp: Long): List<WorkspaceTagRefEntity> =
            database.workspaceTagRefDao().getChangedSince(timestamp)

        suspend fun mergeIncoming(incoming: List<WorkspaceTagRefEntity>?) {
            if (incoming == null) return
            database.withTransaction {
                val keys = incoming.map { it.workspaceId to it.normalizedTag }
                require(keys.size == keys.distinct().size) {
                    "Canonical Workspace tag payload contains duplicate logical membership rows"
                }

                val workspaces = database.workspaceDao().getAll().associateBy { it.id }
                incoming.forEach { candidate ->
                    require(candidate.workspaceId.isNotBlank()) { "Workspace tag owner must not be blank" }
                    require(
                        candidate.normalizedTag ==
                            CanonicalWorkspaceTagRepository
                                .normalizeTags(listOf(candidate.normalizedTag))
                                .singleOrNull(),
                    ) {
                        "Workspace tag is not canonically normalized: ${candidate.normalizedTag}"
                    }
                    require(candidate.createdAt >= 0L && candidate.updatedAt >= candidate.createdAt) {
                        "Workspace tag timestamps are invalid for ${candidate.workspaceId}/${candidate.normalizedTag}"
                    }
                    require(candidate.version >= 1L) {
                        "Workspace tag version must be positive for ${candidate.workspaceId}/${candidate.normalizedTag}"
                    }
                    val workspace = requireNotNull(workspaces[candidate.workspaceId]) {
                        "Workspace tag references missing Workspace: ${candidate.workspaceId}"
                    }
                    require(!workspace.isDeleted || candidate.isDeleted) {
                        "Live Workspace tag cannot belong to deleted Workspace: ${candidate.workspaceId}"
                    }
                }

                val local = database.workspaceTagRefDao().getAll().associateBy { it.workspaceId to it.normalizedTag }
                val winners =
                    incoming.filter { candidate ->
                        val current = local[candidate.workspaceId to candidate.normalizedTag] ?: return@filter true
                        candidate.version > current.version ||
                            (candidate.version == current.version && candidate.updatedAt > current.updatedAt) ||
                            (candidate.version == current.version &&
                                candidate.updatedAt == current.updatedAt &&
                                candidate.isDeleted && !current.isDeleted)
                    }
                if (winners.isNotEmpty()) database.workspaceTagRefDao().upsert(winners)
            }
        }
    }
