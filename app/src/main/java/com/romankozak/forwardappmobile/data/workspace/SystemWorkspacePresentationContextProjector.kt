package com.romankozak.forwardappmobile.data.workspace

import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao

import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Read-only Context-shaped presentation data.
 *
 * This is deliberately not a Room [Context] entity: a shell-free promoted
 * System Workspace has no safe canonical values for the legacy Context-only
 * persistence fields. Consumers must not use this value for Context writes.
 */
data class ContextPresentation(
    val id: String,
    val name: String,
    val description: String?,
    val parentId: String?,
    val roleCode: String?,
    val order: Long,
    val tags: List<String>?,
)

/**
 * Transitional read projection for UI surfaces that still consume [Context].
 *
 * Canonical Workspace rows remain the presentation owner. This adapter only
 * overlays that presentation onto the legacy shell shape and can disappear
 * when those consumers stop reading Context compatibility rows.
 */
@Singleton
class SystemWorkspacePresentationContextProjector
    @Inject
    constructor(
        private val workspaceDao: WorkspaceDao,
        private val systemWorkspaceTagAuthority: SystemWorkspaceTagAuthority,
        private val canonicalWorkspaceTagRepository: CanonicalWorkspaceTagRepository,
        private val contextDao: ContextDao,
    ) {
        /**
         * Projects the complete read-only project presentation universe.
         *
 * A retired ordinary project may be presented from its live
 * CANONICAL_ONLY Workspace when a deleted same-id Context proves the
 * historical project identity. A live standalone operational Workspace is
 * independently admitted without a Context shell. The tombstone contributes
 * no display fields. Reserved System ids remain independently shell-free.
         */
        suspend fun projectPresentationUniverse(
            contexts: List<Context>,
        ): List<ContextPresentation> {
            val workspaces = workspaceDao.getAll()
            val retiredOrdinaryContextIds =
                contextDao.getAllRaw()
                    .asSequence()
                    .filter { context ->
                        context.isDeleted &&
                            !SystemContexts.isSystem(ContextId(context.id))
                    }
                    .mapTo(hashSetOf()) { it.id }

            val canonicalTagsById =
                canonicalWorkspaceTagRepository
                    .getLiveTagsByWorkspace()
                    .filterKeys { id -> !SystemContexts.isSystem(ContextId(id)) }
                    .toMutableMap()

            workspaces
                .asSequence()
                .filter { workspace ->
                    !workspace.isDeleted &&
                        SystemContexts.isSystem(ContextId(workspace.id)) &&
                        workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                        workspace.sourceContextId == null
                }
                .map { it.id }
                .distinct()
                .forEach { id ->
                    when (val resolution = systemWorkspaceTagAuthority.resolve(id)) {
                        is SystemWorkspaceTagAuthority.Resolution.Canonical ->
                            canonicalTagsById[id] = resolution.tags

                        SystemWorkspaceTagAuthority.Resolution.NotSystem,
                        SystemWorkspaceTagAuthority.Resolution.Unavailable,
                        -> Unit
                    }
                }

            return projectPresentationUniverseFromState(
                contexts = contexts,
                workspaces = workspaces,
                canonicalTagsById = canonicalTagsById,
                retiredOrdinaryContextIds = retiredOrdinaryContextIds,
            )
        }

        /**
         * Resolves one read-only presentation value by stable id.
         *
         * Context persistence remains an implementation detail of this
         * transitional read boundary: active ordinary Contexts are accepted
         * as compatibility owners, while deleted ordinary rows contribute
         * retirement identity evidence only. Callers must not fetch raw
         * Context merely to resolve presentation.
         */
        suspend fun resolvePresentation(
            contextId: String,
        ): ContextPresentation? =
            resolvePresentation(
                contextId = contextId,
                context = contextDao.getContextById(contextId),
            )

        /**
         * Resolves one read-only presentation value when compatibility
         * Context evidence is already available. Ordinary shell-free
         * resolution requires deleted same-id Context retirement evidence;
         * exact System ids retain their independent canonical contract.
         */
        suspend fun resolvePresentation(
            contextId: String,
            context: Context?,
        ): ContextPresentation? {
            if (context != null && context.id != contextId) return null

            val isSystem = SystemContexts.isSystem(ContextId(contextId))
            val ordinaryRetirementEvidence =
                !isSystem &&
                    (
                        context?.isDeleted == true ||
                            (
                                context == null &&
                                    contextDao.getContextById(contextId)?.isDeleted == true
                            )
                    )

            val workspace = workspaceDao.getById(contextId)
            val canonicalTags =
                when {
                    isSystem &&
                        workspace != null &&
                        !workspace.isDeleted &&
                        workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                        workspace.sourceContextId == null ->
                        when (val resolution = systemWorkspaceTagAuthority.resolve(contextId)) {
                            is SystemWorkspaceTagAuthority.Resolution.Canonical -> resolution.tags

                            SystemWorkspaceTagAuthority.Resolution.NotSystem,
                            SystemWorkspaceTagAuthority.Resolution.Unavailable,
                            -> null
                        }

                    ordinaryRetirementEvidence ||
                        workspace?.isStandaloneOperationalWorkspace() == true ->
                        canonicalWorkspaceTagRepository.getTags(contextId)

                    else -> null
                }

            return resolvePresentationFromState(
                contextId = contextId,
                context = context,
                workspace = workspace,
                canonicalTags = canonicalTags,
                ordinaryRetirementEvidence = ordinaryRetirementEvidence,
            )
        }

        /**
         * Reactively projects the complete read-only presentation universe.
         *
         * This includes retired ordinary project Workspaces, standalone
         * operational Workspaces, and exact System Workspaces without live
         * Context shells. It never manufactures a Context entity and must not
         * feed Context mutation paths.
         */
        fun observePresentationUniverse(
            contexts: Flow<List<Context>>,
        ): Flow<List<ContextPresentation>> =
            combine(
                contexts,
                workspaceDao.observeAll(),
                canonicalWorkspaceTagRepository.observeLiveTagsByWorkspace(),
                systemWorkspaceTagAuthority.observeEffectiveOwners(contexts),
                contextDao.getAllContextsFlow(),
            ) { contextRows, workspaces, workspaceTagsById, tagOwners, allContextRows ->
                val retiredOrdinaryContextIds =
                    allContextRows
                        .asSequence()
                        .filter { context ->
                            context.isDeleted &&
                                !SystemContexts.isSystem(ContextId(context.id))
                        }
                        .mapTo(hashSetOf()) { it.id }

                val canonicalTagsById =
                    workspaceTagsById
                        .filterKeys { id -> !SystemContexts.isSystem(ContextId(id)) }
                        .toMutableMap()

                tagOwners
                    .asSequence()
                    .filter { owner -> SystemContexts.isSystem(ContextId(owner.id)) }
                    .forEach { owner ->
                        canonicalTagsById[owner.id] = owner.tags
                    }

                projectPresentationUniverseFromState(
                    contexts = contextRows,
                    workspaces = workspaces,
                    canonicalTagsById = canonicalTagsById,
                    retiredOrdinaryContextIds = retiredOrdinaryContextIds,
                )
            }

        /**
         * Resolves display labels for stable operational-owner ids without
         * manufacturing a Context shell for a canonical-only System Workspace.
         */
        fun observeOwnerLabels(contexts: Flow<List<Context>>): Flow<Map<String, String>> =
            combine(
                contexts,
                workspaceDao.observeAll(),
                contextDao.getAllContextsFlow(),
            ) { contextRows, workspaces, allContextRows ->
                val retiredOrdinaryContextIds =
                    allContextRows
                        .asSequence()
                        .filter { context ->
                            context.isDeleted &&
                                !SystemContexts.isSystem(ContextId(context.id))
                        }
                        .mapTo(hashSetOf()) { it.id }

                operationalOwnerLabelsFromState(
                    contexts = contextRows,
                    workspaces = workspaces,
                    retiredOrdinaryContextIds = retiredOrdinaryContextIds,
                )
            }


}

internal fun projectPresentationUniverseFromState(
    contexts: List<Context>,
    workspaces: List<WorkspaceEntity>,
    canonicalTagsById: Map<String, List<String>>,
    retiredOrdinaryContextIds: Set<String> = emptySet(),
): List<ContextPresentation> {
    val workspaceById = workspaces.associateBy { it.id }
    val contextIds = contexts.mapTo(hashSetOf()) { it.id }

    val projectedContexts =
        contexts.mapNotNull { context ->
            resolvePresentationFromState(
                contextId = context.id,
                context = context,
                workspace = workspaceById[context.id],
                canonicalTags = canonicalTagsById[context.id],
                ordinaryRetirementEvidence = context.id in retiredOrdinaryContextIds,
            )
        }

    val shellFreeProjectWorkspaces =
        workspaces
            .asSequence()
            .filter { workspace ->
                workspace.id !in contextIds &&
                    (
                        SystemContexts.isSystem(ContextId(workspace.id)) ||
                            workspace.id in retiredOrdinaryContextIds ||
                            workspace.isStandaloneOperationalWorkspace()
                    )
            }
            .sortedWith(
                compareBy<WorkspaceEntity> { it.parentWorkspaceId ?: "" }
                    .thenBy { it.workspaceOrder }
                    .thenBy { it.id },
            )
            .mapNotNull { workspace ->
                resolvePresentationFromState(
                    contextId = workspace.id,
                    context = null,
                    workspace = workspace,
                    canonicalTags = canonicalTagsById[workspace.id],
                    ordinaryRetirementEvidence =
                        workspace.id in retiredOrdinaryContextIds,
                )
            }
            .toList()

    return projectedContexts + shellFreeProjectWorkspaces
}

private fun resolvePresentationFromState(
    contextId: String,
    context: Context?,
    workspace: WorkspaceEntity?,
    canonicalTags: List<String>?,
    ordinaryRetirementEvidence: Boolean,
): ContextPresentation? {
    val isSystem = SystemContexts.isSystem(ContextId(contextId))

    // Live ordinary Contexts retain their legacy read ownership until their
    // explicit cutover. A same-id canonical Workspace must not silently hijack
    // an active Context presentation.
    if (!isSystem && context != null && !context.isDeleted) {
        return context.toPresentation()
    }

    if (workspace != null && !workspace.isDeleted && workspace.isCanonicalWorkspaceOwner()) {
        if (workspace.sourceContextId != null) return null

        if (
            !isSystem &&
            !ordinaryRetirementEvidence &&
            !workspace.isStandaloneOperationalWorkspace()
        ) {
            return null
        }

        // Reserved System ids retain their explicit seed/ingress authority:
        // null means canonical tag ownership is not yet safely available.
        if (isSystem && canonicalTags == null) {
            return null
        }

        val name = workspace.nameOverride?.takeIf { it.isNotBlank() } ?: return null
        return ContextPresentation(
            id = workspace.id,
            name = name,
            description = workspace.descriptionOverride,
            parentId = workspace.parentWorkspaceId,
            roleCode = workspace.roleCode,
            order = workspace.workspaceOrder,
            tags = canonicalTags.orEmpty(),
        )
    }

    if (isSystem) {
        if (workspace == null || workspace.isDeleted) return null
        return when (workspace.provenance) {
            WorkspaceProvenance.CONTEXT_BACKED.name ->
                context
                    ?.takeUnless { it.isDeleted }
                    ?.takeIf { workspace.sourceContextId == it.id }
                    ?.toPresentation()

            else -> null
        }
    }

    return null
}

private fun Context.toPresentation(): ContextPresentation =
    ContextPresentation(
        id = id,
        name = name,
        description = description,
        parentId = parentId,
        roleCode = roleCode,
        order = order,
        tags = tags,
    )

internal fun operationalOwnerLabelsFromState(
    contexts: List<Context>,
    workspaces: List<WorkspaceEntity>,
    retiredOrdinaryContextIds: Set<String> = emptySet(),
): Map<String, String> {
    val workspaceById = workspaces.associateBy { it.id }
    val labels = linkedMapOf<String, String>()

    contexts.forEach { context ->
        val isSystem = SystemContexts.isSystem(ContextId(context.id))
        if (!isSystem) {
            // Active ordinary Contexts retain read authority until retirement.
            labels[context.id] = context.name
            return@forEach
        }

        val workspace = workspaceById[context.id]
        if (workspace == null || workspace.isDeleted) return@forEach

        when (workspace.provenance) {
            WorkspaceProvenance.CONTEXT_BACKED.name -> {
                if (workspace.sourceContextId == context.id) {
                    labels[context.id] = context.name
                }
            }

            WorkspaceProvenance.CANONICAL_ONLY.name,
            WorkspaceProvenance.STANDALONE.name -> {
                if (
                    workspace.sourceContextId == null &&
                    (workspace.provenance != WorkspaceProvenance.STANDALONE.name || !isSystem)
                ) {
                    workspace.nameOverride
                        ?.takeIf { it.isNotBlank() }
                        ?.let { name -> labels[context.id] = name }
                }
            }
        }
    }

    workspaces.forEach { workspace ->
        if (
            workspace.isDeleted ||
            !workspace.isCanonicalWorkspaceOwner()
        ) {
            return@forEach
        }

        val isSystem = SystemContexts.isSystem(ContextId(workspace.id))
        val isRetiredOrdinary =
            !isSystem && workspace.id in retiredOrdinaryContextIds

        if (!isSystem && !isRetiredOrdinary && !workspace.isStandaloneOperationalWorkspace()) {
            return@forEach
        }

        workspace.nameOverride
            ?.takeIf { it.isNotBlank() }
            ?.let { name -> labels[workspace.id] = name }
    }

    return labels
}

private fun WorkspaceEntity.isCanonicalWorkspaceOwner(): Boolean =
    sourceContextId == null &&
        (
            provenance == WorkspaceProvenance.CANONICAL_ONLY.name ||
                (
                    provenance == WorkspaceProvenance.STANDALONE.name &&
                        !SystemContexts.isSystem(ContextId(id))
                )
        )

private fun WorkspaceEntity.isStandaloneOperationalWorkspace(): Boolean =
    !isDeleted &&
        !SystemContexts.isSystem(ContextId(id)) &&
        provenance == WorkspaceProvenance.STANDALONE.name &&
        sourceContextId == null
