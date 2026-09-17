package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.SystemWorkspaceTagSeedStateEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceTagRefEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Canonical authoring/read boundary for Workspace-owned tag membership.
 *
 * Context.tags remains a compatibility representation until its readers and
 * writers are explicitly cut over. This repository never requires a Context
 * row for Workspace ownership.
 */
@Singleton
class CanonicalWorkspaceTagRepository
    @Inject
    constructor(
        private val database: AppDatabase,
    ) {
        suspend fun getTags(workspaceId: String): List<String> =
            database.workspaceTagRefDao()
                .getLiveForWorkspace(workspaceId)
                .map { it.normalizedTag }

        /**
         * Complete canonical Workspace-owned tag membership for read-only
         * presentation consumers. Absence means an empty canonical collection.
         *
         * Reserved System authority is still gated separately by
         * [SystemWorkspaceTagAuthority]; callers must not use this API to bypass
         * its seed/ingress contract.
         */
        suspend fun getLiveTagsByWorkspace(): Map<String, List<String>> =
            database.workspaceTagRefDao()
                .getAll()
                .asSequence()
                .filterNot { it.isDeleted }
                .groupBy { it.workspaceId }
                .mapValues { (_, refs) ->
                    refs.map { it.normalizedTag }.sorted()
                }

        fun observeLiveTagsByWorkspace(): Flow<Map<String, List<String>>> =
            database.workspaceTagRefDao()
                .observeAll()
                .map { refs ->
                    refs
                        .asSequence()
                        .filterNot { it.isDeleted }
                        .groupBy { it.workspaceId }
                        .mapValues { (_, liveRefs) ->
                            liveRefs.map { it.normalizedTag }.sorted()
                        }
                }

        suspend fun findLiveTagRefs(tags: Iterable<String>): List<WorkspaceTagRefEntity> {
            val normalized = normalizeTags(tags)
            if (normalized.isEmpty()) return emptyList()
            return database.workspaceTagRefDao().findLiveByTags(normalized)
        }

        suspend fun replaceTags(
            workspaceId: String,
            tags: Iterable<String>,
            now: Long = System.currentTimeMillis(),
        ): List<WorkspaceTagRefEntity> =
            database.withTransaction {
                require(workspaceId.isNotBlank()) {
                    "Canonical Workspace tag owner must not be blank"
                }

                val workspace =
                    requireNotNull(database.workspaceDao().getById(workspaceId)) {
                        "Canonical Workspace tag owner does not exist: $workspaceId"
                    }
                require(!workspace.isDeleted) {
                    "Canonical Workspace tags cannot belong to a deleted Workspace: $workspaceId"
                }

                val desired = normalizeTags(tags).toSet()
                val existing =
                    database.workspaceTagRefDao()
                        .getAllForWorkspace(workspaceId)
                        .associateBy { it.normalizedTag }

                val changed = mutableListOf<WorkspaceTagRefEntity>()

                desired.sorted().forEach { normalizedTag ->
                    val current = existing[normalizedTag]
                    when {
                        current == null ->
                            changed +=
                                WorkspaceTagRefEntity(
                                    workspaceId = workspaceId,
                                    normalizedTag = normalizedTag,
                                    createdAt = now,
                                    updatedAt = now,
                                    syncedAt = null,
                                    isDeleted = false,
                                    version = 1L,
                                )

                        current.isDeleted ->
                            changed +=
                                current.copy(
                                    updatedAt = now,
                                    syncedAt = null,
                                    isDeleted = false,
                                    version = nextVersion(current.version),
                                )
                    }
                }

                existing.values
                    .asSequence()
                    .filter { current ->
                        !current.isDeleted && current.normalizedTag !in desired
                    }
                    .sortedBy { it.normalizedTag }
                    .forEach { current ->
                        changed +=
                            current.copy(
                                updatedAt = now,
                                syncedAt = null,
                                isDeleted = true,
                                version = nextVersion(current.version),
                            )
                    }

                if (changed.isNotEmpty()) {
                    database.workspaceTagRefDao().upsert(changed)
                }

                // A direct canonical write establishes canonical authority,
                // including for an intentionally empty collection. Startup
                // seeding may leave bounded legacy transport ingress open, but
                // an explicit canonical write closes it permanently.
                if (
                    SystemContexts.isSystem(ContextId(workspaceId)) &&
                        workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                        workspace.sourceContextId == null
                ) {
                    val stateDao = database.systemWorkspaceTagSeedStateDao()
                    val currentState = stateDao.getByWorkspaceId(workspaceId)
                    when {
                        currentState == null ->
                            stateDao.upsert(
                                listOf(
                                    SystemWorkspaceTagSeedStateEntity(
                                        workspaceId = workspaceId,
                                        seededAt = now,
                                        legacyIngressClosedAt = now,
                                    ),
                                ),
                            )

                        currentState.legacyIngressClosedAt == null ->
                            stateDao.upsert(
                                listOf(currentState.copy(legacyIngressClosedAt = now)),
                            )
                    }
                }

                changed.sortedBy { it.normalizedTag }
            }

        companion object {
            fun normalizeTags(tags: Iterable<String>): List<String> =
                tags
                    .mapNotNull { tag ->
                        tag.trim()
                            .removePrefix("#")
                            .lowercase()
                            .takeIf { it.isNotBlank() }
                    }
                    .distinct()
                    .sorted()

            private fun nextVersion(version: Long): Long =
                if (version == Long.MAX_VALUE) Long.MAX_VALUE else version + 1L
        }
    }
