package com.romankozak.forwardappmobile.data.workspace

import com.romankozak.forwardappmobile.StartupTrace
import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.context.SystemOperationalDefinitions
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.SystemWorkspaceTagSeedStateEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceTagRefEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bounded one-way cutover from optional historical System Context.tags
 * compatibility input into canonical Workspace tag membership.
 *
 * This is neither a runtime mirror nor a Context tag owner. A persisted marker
 * makes the initial import retry-safe, including for an authoritative empty
 * canonical collection. After successful establishment, stale reserved-System
 * context_tag_refs are retired in the same transaction; ordinary Context tag
 * refs remain untouched.
 */
@Singleton
class SystemWorkspaceTagSeed
    @Inject
    constructor(
        private val database: AppDatabase,
        private val contextDao: ContextDao,
    ) {
        suspend fun seedMissingCanonicalCollections(
            now: Long = System.currentTimeMillis(),
        ) {
            database.withTransaction {
                val definitions = SystemOperationalDefinitions.all
                val definitionIds = definitions.map { it.id }
                val workspaceById =
                    StartupTrace.measure("Application.systemWorkspaceOwnership.tagSeed.loadWorkspaces") {
                        database.workspaceDao()
                            .getByIds(definitionIds)
                            .associateBy { it.id }
                    }
                val seededIds =
                    StartupTrace.measure("Application.systemWorkspaceOwnership.tagSeed.loadStates") {
                        database.systemWorkspaceTagSeedStateDao().getAll()
                            .mapTo(hashSetOf()) { it.workspaceId }
                    }
                val unseededIds = definitionIds.filterNot(seededIds::contains)
                val contextById =
                    StartupTrace.measure("Application.systemWorkspaceOwnership.tagSeed.loadContexts") {
                        if (unseededIds.isEmpty()) {
                            emptyMap()
                        } else {
                            contextDao.getContextsByIds(unseededIds)
                                .associateBy { it.id }
                        }
                    }
                val tagDao = database.workspaceTagRefDao()

                StartupTrace.measure("Application.systemWorkspaceOwnership.tagSeed.validate") {
                    definitions.forEach { definition ->
                        val workspace = requireNotNull(workspaceById[definition.id]) {
                            "Missing reserved System Workspace for tag seed: ${definition.id}"
                        }
                        require(!workspace.isDeleted) {
                            "Deleted reserved System Workspace for tag seed: ${definition.id}"
                        }
                        require(
                            workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                                workspace.sourceContextId == null,
                        ) {
                            "Malformed reserved System Workspace for tag seed: ${definition.id}"
                        }
                        if (definition.id !in seededIds) {
                            require(tagDao.getAllForWorkspace(definition.id).isEmpty()) {
                                "Unmarked canonical Workspace tag state for ${definition.id} cannot be seeded safely"
                            }
                            val context = contextById[definition.id]
                            require(context == null || !context.isDeleted) {
                                "Deleted unseeded reserved System Context cannot supply tag ingress: ${definition.id}"
                            }
                        }
                    }
                }

                val states = mutableListOf<SystemWorkspaceTagSeedStateEntity>()
                val refs = mutableListOf<WorkspaceTagRefEntity>()
                definitions.forEach { definition ->
                    if (definition.id in seededIds) return@forEach
                    val legacyTags = contextById[definition.id]?.tags.orEmpty()
                    CanonicalWorkspaceTagRepository.normalizeTags(
                        legacyTags,
                    ).forEach { tag ->
                        refs +=
                            WorkspaceTagRefEntity(
                                workspaceId = definition.id,
                                normalizedTag = tag,
                                createdAt = now,
                                updatedAt = now,
                                syncedAt = null,
                                isDeleted = false,
                                version = 1L,
                            )
                    }
                    states +=
                        SystemWorkspaceTagSeedStateEntity(
                            workspaceId = definition.id,
                            seededAt = now,
                            legacyIngressClosedAt = null,
                        )
                }
                StartupTrace.measure("Application.systemWorkspaceOwnership.tagSeed.write") {
                    if (refs.isNotEmpty()) tagDao.upsert(refs)
                    if (states.isNotEmpty()) database.systemWorkspaceTagSeedStateDao().upsert(states)
                }

                // Step 9B relational retirement:
                // once every reserved System tag collection has passed the
                // fail-closed canonical ownership checks above, its legacy
                // Context tag index is no longer needed. Keep context_tag_refs
                // for ordinary Contexts, but deterministically remove residue
                // for the exact reserved System ids.
                StartupTrace.measure("Application.systemWorkspaceOwnership.tagSeed.retireLegacyTagRefs") {
                    definitions.forEach { definition ->
                        database.contextTagRefDao().deleteForContext(definition.id)
                    }
                }
            }
        }

        /**
         * A current SnapshotBundle field (including []) establishes canonical
         * ownership for the imported promoted System collections and closes
         * legacy tag transport ingress permanently.
         */
        suspend fun markImportedCanonicalCollections(
            workspaceIds: Collection<String>,
            now: Long = System.currentTimeMillis(),
        ) {
            database.withTransaction {
                val reservedIds = SystemOperationalDefinitions.all.mapTo(hashSetOf()) { it.id }
                val statesById =
                    database.systemWorkspaceTagSeedStateDao().getAll().associateBy { it.workspaceId }
                val livePromotedIds =
                    database.workspaceDao().getAll()
                        .asSequence()
                        .filter { workspace ->
                            workspace.id in workspaceIds &&
                                !workspace.isDeleted &&
                                workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                                workspace.sourceContextId == null &&
                                workspace.id in reservedIds
                        }
                        .map { it.id }
                        .toList()

                val updates =
                    livePromotedIds.mapNotNull { id ->
                        val current = statesById[id]
                        when {
                            current == null ->
                                SystemWorkspaceTagSeedStateEntity(
                                    workspaceId = id,
                                    seededAt = now,
                                    legacyIngressClosedAt = now,
                                )

                            current.legacyIngressClosedAt == null ->
                                current.copy(legacyIngressClosedAt = now)

                            else -> null
                        }
                    }

                if (updates.isNotEmpty()) {
                    database.systemWorkspaceTagSeedStateDao().upsert(updates)
                }
            }
        }

        /**
         * Explicit import-only compatibility boundary for a SnapshotBundle
         * produced before canonical Workspace tag transport existed.
         *
         * Normal runtime never reads Context.tags back into canonical storage.
         * A reserved System collection may consume this ingress at most once.
         * Direct canonical authoring or current canonical transport closes it
         * without importing legacy state.
         */
        suspend fun ingestLegacySystemTagProjection(
            legacyTagsByWorkspaceId: Map<String, List<String>>,
            now: Long = System.currentTimeMillis(),
        ) {
            if (legacyTagsByWorkspaceId.isEmpty()) return

            val canonicalTagRepository = CanonicalWorkspaceTagRepository(database)
            database.withTransaction {
                val reservedIds = SystemOperationalDefinitions.all.mapTo(hashSetOf()) { it.id }
                val requestedIds =
                    legacyTagsByWorkspaceId.keys
                        .asSequence()
                        .filter { it in reservedIds }
                        .sorted()
                        .toList()
                if (requestedIds.isEmpty()) return@withTransaction

                val workspacesById = database.workspaceDao().getAll().associateBy { it.id }
                val statesById =
                    database.systemWorkspaceTagSeedStateDao().getAll().associateBy { it.workspaceId }

                requestedIds.forEach { id ->
                    val workspace = requireNotNull(workspacesById[id]) {
                        "Missing reserved System Workspace for legacy tag import: $id"
                    }
                    require(
                        !workspace.isDeleted &&
                            workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                            workspace.sourceContextId == null,
                    ) {
                        "Malformed reserved System Workspace for legacy tag import: $id"
                    }

                    val state = requireNotNull(statesById[id]) {
                        "Reserved System Workspace tag collection is not established before legacy import: $id"
                    }
                    if (state.legacyIngressClosedAt != null) return@forEach

                    canonicalTagRepository.replaceTags(
                        workspaceId = id,
                        tags = legacyTagsByWorkspaceId.getValue(id),
                        now = now,
                    )
                }
            }
        }
    }
