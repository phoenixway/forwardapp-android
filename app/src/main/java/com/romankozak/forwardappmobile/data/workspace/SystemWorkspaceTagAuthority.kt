package com.romankozak.forwardappmobile.data.workspace

import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceTagRefEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Transitional System-only tag ownership boundary.
 *
 * Reserved System ids never use Context.tags as runtime authority after their
 * tag cutover. Canonical membership is available only for a live promoted
 * CANONICAL_ONLY Workspace whose one-time seed/import marker is present.
 *
 * Ordinary non-System Contexts continue to own their existing Context.tags
 * representation until their later migration.
 */
@Singleton
class SystemWorkspaceTagAuthority
    @Inject
    constructor(
        private val workspaceDao: WorkspaceDao,
        private val workspaceTagRefDao: WorkspaceTagRefDao,
        private val systemWorkspaceTagSeedStateDao: SystemWorkspaceTagSeedStateDao,
        private val canonicalWorkspaceTagRepository: CanonicalWorkspaceTagRepository,
    ) {
        sealed interface Resolution {
            data object NotSystem : Resolution

            data class Canonical(
                val tags: List<String>,
            ) : Resolution

            data object Unavailable : Resolution
        }

        data class TagOwner(
            val id: String,
            val tags: List<String>,
        )

        data class TagMatch(
            val contextId: String,
            val normalizedTag: String,
        )

        suspend fun resolve(contextId: String): Resolution {
            if (!SystemContexts.isSystem(ContextId(contextId))) return Resolution.NotSystem
            val workspace = workspaceDao.getById(contextId)
            val seeded = systemWorkspaceTagSeedStateDao.getByWorkspaceId(contextId) != null
            return resolve(
                contextId = contextId,
                workspace = workspace,
                tagRefs = workspaceTagRefDao.getAllForWorkspace(contextId),
                seeded = seeded,
            )
        }

        suspend fun project(context: Context): Context? {
            if (!SystemContexts.isSystem(ContextId(context.id))) return context
            val workspace = workspaceDao.getById(context.id)
            val seeded = systemWorkspaceTagSeedStateDao.getByWorkspaceId(context.id) != null
            return project(
                context = context,
                workspace = workspace,
                tagRefs = workspaceTagRefDao.getAllForWorkspace(context.id),
                seeded = seeded,
            )
        }

        suspend fun project(contexts: List<Context>): List<Context> =
            project(
                contexts = contexts,
                workspaces = workspaceDao.getAll(),
                tagRefs = workspaceTagRefDao.getAll(),
                seededIds = systemWorkspaceTagSeedStateDao.getAll().mapTo(hashSetOf()) { it.workspaceId },
            )

        fun observe(contexts: Flow<List<Context>>): Flow<List<Context>> =
            combine(
                contexts,
                workspaceDao.observeAll(),
                workspaceTagRefDao.observeAll(),
                systemWorkspaceTagSeedStateDao.observeAll(),
            ) { contextRows, workspaces, tagRefs, seedStates ->
                project(
                    contexts = contextRows,
                    workspaces = workspaces,
                    tagRefs = tagRefs,
                    seededIds = seedStates.mapTo(hashSetOf()) { it.workspaceId },
                )
            }

        fun observeEffectiveOwners(contexts: Flow<List<Context>>): Flow<List<TagOwner>> =
            combine(
                contexts,
                workspaceDao.observeAll(),
                workspaceTagRefDao.observeAll(),
                systemWorkspaceTagSeedStateDao.observeAll(),
            ) { contextRows, workspaces, tagRefs, seedStates ->
                effectiveOwners(
                    contexts = contextRows,
                    workspaces = workspaces,
                    tagRefs = tagRefs,
                    seededIds = seedStates.mapTo(hashSetOf()) { it.workspaceId },
                )
            }

        /**
         * Includes canonical System owners even when no temporary Context shell
         * exists. Stale System Context.tags never participate.
         */
        suspend fun effectiveOwners(contexts: List<Context>): List<TagOwner> =
            effectiveOwners(
                contexts = contexts,
                workspaces = workspaceDao.getAll(),
                tagRefs = workspaceTagRefDao.getAll(),
                seededIds = systemWorkspaceTagSeedStateDao.getAll().mapTo(hashSetOf()) { it.workspaceId },
            )

        suspend fun findCanonicalSystemOwnersByTags(
            normalizedTags: List<String>,
        ): List<TagMatch> {
            if (normalizedTags.isEmpty()) return emptyList()

            val workspaces = workspaceDao.getAll().associateBy { it.id }
            val seededIds =
                systemWorkspaceTagSeedStateDao.getAll()
                    .mapTo(hashSetOf()) { it.workspaceId }

            return workspaceTagRefDao.findLiveByTags(normalizedTags)
                .asSequence()
                .filter { ref ->
                    isCanonicalSystemOwner(
                        contextId = ref.workspaceId,
                        workspace = workspaces[ref.workspaceId],
                        seeded = ref.workspaceId in seededIds,
                    )
                }
                .map { ref -> TagMatch(ref.workspaceId, ref.normalizedTag) }
                .distinct()
                .sortedWith(compareBy(TagMatch::contextId, TagMatch::normalizedTag))
                .toList()
        }

        suspend fun isCanonicalSystemOwner(contextId: String): Boolean {
            if (!SystemContexts.isSystem(ContextId(contextId))) return false
            return isCanonicalSystemOwner(
                contextId = contextId,
                workspace = workspaceDao.getById(contextId),
                seeded = systemWorkspaceTagSeedStateDao.getByWorkspaceId(contextId) != null,
            )
        }

        /**
         * System writes author canonical membership. The returned Context is only
         * the bounded one-way compatibility projection for callers that still
         * persist the temporary System Context shell.
         */
        suspend fun reconcileBeforeContextWrite(
            context: Context,
            tagsWereExplicitlyChanged: Boolean,
            now: Long,
        ): Context =
            when (val resolution = resolve(context.id)) {
                Resolution.NotSystem -> context
                Resolution.Unavailable ->
                    throw IllegalStateException(
                        "Canonical System Workspace tags are unavailable: ${context.id}",
                    )
                is Resolution.Canonical -> {
                    val tags =
                        if (tagsWereExplicitlyChanged) {
                            canonicalWorkspaceTagRepository.replaceTags(
                                workspaceId = context.id,
                                tags = context.tags.orEmpty(),
                                now = now,
                            )
                            canonicalWorkspaceTagRepository.getTags(context.id)
                        } else {
                            resolution.tags
                        }
                    context.copy(tags = tags)
                }
            }

        private fun effectiveOwners(
            contexts: List<Context>,
            workspaces: List<WorkspaceEntity>,
            tagRefs: List<WorkspaceTagRefEntity>,
            seededIds: Set<String>,
        ): List<TagOwner> {
            val workspaceById = workspaces.associateBy { it.id }
            val refsByWorkspace =
                tagRefs
                    .asSequence()
                    .filterNot { it.isDeleted }
                    .groupBy({ it.workspaceId }, { it.normalizedTag })

            val ordinaryOwners =
                contexts
                    .asSequence()
                    .filterNot { it.isDeleted }
                    .filterNot { SystemContexts.isSystem(ContextId(it.id)) }
                    .map { context -> TagOwner(context.id, context.tags.orEmpty()) }

            val canonicalSystemOwners =
                workspaces
                    .asSequence()
                    .filter { workspace ->
                        isCanonicalSystemOwner(
                            contextId = workspace.id,
                            workspace = workspace,
                            seeded = workspace.id in seededIds,
                        )
                    }
                    .map { workspace ->
                        TagOwner(
                            id = workspace.id,
                            tags = refsByWorkspace[workspace.id].orEmpty().sorted(),
                        )
                    }

            return (ordinaryOwners + canonicalSystemOwners).toList()
        }

        private fun project(
            contexts: List<Context>,
            workspaces: List<WorkspaceEntity>,
            tagRefs: List<WorkspaceTagRefEntity>,
            seededIds: Set<String>,
        ): List<Context> {
            val workspaceById = workspaces.associateBy { it.id }
            return contexts.mapNotNull { context ->
                project(
                    context = context,
                    workspace = workspaceById[context.id],
                    tagRefs = tagRefs,
                    seeded = context.id in seededIds,
                )
            }
        }

        private fun project(
            context: Context,
            workspace: WorkspaceEntity?,
            tagRefs: List<WorkspaceTagRefEntity>,
            seeded: Boolean,
        ): Context? =
            when (
                val resolution =
                    resolve(
                        contextId = context.id,
                        workspace = workspace,
                        tagRefs = tagRefs,
                        seeded = seeded,
                    )
            ) {
                Resolution.NotSystem -> context
                Resolution.Unavailable -> null
                is Resolution.Canonical -> context.copy(tags = resolution.tags)
            }

        private fun resolve(
            contextId: String,
            workspace: WorkspaceEntity?,
            tagRefs: List<WorkspaceTagRefEntity>,
            seeded: Boolean,
        ): Resolution {
            if (!SystemContexts.isSystem(ContextId(contextId))) {
                return Resolution.NotSystem
            }

            if (
                !isCanonicalSystemOwner(
                    contextId = contextId,
                    workspace = workspace,
                    seeded = seeded,
                )
            ) {
                return Resolution.Unavailable
            }

            return Resolution.Canonical(
                tagRefs
                    .asSequence()
                    .filter { it.workspaceId == contextId && !it.isDeleted }
                    .map { it.normalizedTag }
                    .distinct()
                    .sorted()
                    .toList(),
            )
        }

        private fun isCanonicalSystemOwner(
            contextId: String,
            workspace: WorkspaceEntity?,
            seeded: Boolean,
        ): Boolean =
            SystemContexts.isSystem(ContextId(contextId)) &&
                workspace != null &&
                !workspace.isDeleted &&
                workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                workspace.sourceContextId == null &&
                seeded
    }
