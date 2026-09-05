package com.romankozak.forwardappmobile.data.orientation

import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity

import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.core.data.models.sync.softDelete
import com.romankozak.forwardappmobile.data.workspace.ContextWorkspaceWriteThrough
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacyOrientationSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Explicit user-selected Context migration target.
 *
 * Context semantic retirement is intentionally bottom-up: a Context may cut
 * over only after its active legacy child Contexts have already retired. The
 * existing Workspace, including its operational hierarchy edge, survives that
 * cutover. Semantic targets add canonical retirement evidence; Workspace-only
 * cutover intentionally does not create semantic identity.
 *
 * Proven targets may create a new canonical Aspect, create a new canonical
 * Orientation with an explicit kind, or adopt one otherwise-unowned existing
 * canonical Aspect or Orientation while preserving the Context's operational
 * Workspace.
 * Additional target shapes belong here only after their ownership/cutover
 * contracts are proven.
 */
sealed interface ContextMigrationTarget {
    data class NewAspectWithExistingWorkspace(
        val titleOverride: String? = null,
        val descriptionOverride: String? = null,
        val parentAspectId: String? = null,
    ) : ContextMigrationTarget

    /**
     * Creates a new canonical Orientation while preserving this Context's
     * existing operational Workspace.
     *
     * The Orientation kind is explicit caller-selected intent. Classifier
     * output never reaches this command as authority.
     */
    data class NewOrientationWithExistingWorkspace(
        val kind: OrientationKind,
        val titleOverride: String? = null,
        val descriptionOverride: String? = null,
    ) : ContextMigrationTarget

    /**
     * Adopts one already-existing, otherwise-unowned canonical Aspect.
     *
     * This is deliberately not a merge operation. The target Aspect must not
     * already be owned by another legacy mapping or embodied by another live
     * Workspace.
     */
    data class ExistingAspectWithExistingWorkspace(
        val aspectId: String,
    ) : ContextMigrationTarget

    /**
     * Adopts one independently owned, complete canonical Orientation without
     * rewriting its semantic aggregate.
     */
    data class ExistingOrientationWithExistingWorkspace(
        val orientationId: String,
    ) : ContextMigrationTarget

    /**
     * Retires a legacy Context while retaining only its operational Workspace.
     * Workspace-only cutover intentionally creates no semantic identity.
     */
    data object WorkspaceOnly : ContextMigrationTarget
}

data class ContextMigrationResult(
    val contextId: String,
    val subjectId: String?,
    val workspaceId: String,
    val mappingId: String?,
    val changed: Boolean,
)

/**
 * Canonical command boundary for explicit legacy Context cutover.
 *
 * Classification is intentionally not consulted here. The caller supplies the
 * user-selected target. The migration preserves the existing Workspace id and
 * therefore does not move capability-owned data.
 */
@Singleton
class CanonicalContextMigrationRepository
    @Inject
    constructor(
        private val contextDao: ContextDao,
        private val workspaceDao: WorkspaceDao,
        private val orientationDao: OrientationDao,
        private val aspectRepository: CanonicalAspectRepository,
        private val aspectLinksRepository: CanonicalAspectLinksRepository,
        private val orientationRepository: CanonicalOrientationRepository,
        private val graphRepository: CanonicalOrientationGraphRepository,
        private val workspaceWriteThrough: ContextWorkspaceWriteThrough,
    ) {
        suspend fun migrateContext(
            contextId: String,
            target: ContextMigrationTarget,
            now: Long = System.currentTimeMillis(),
        ): ContextMigrationResult {
            require(!SystemContexts.isSystem(ContextId(contextId))) {
                "System Context cannot be migrated"
            }
            return when (target) {
                is ContextMigrationTarget.NewOrientationWithExistingWorkspace ->
                    migrateToNewOrientationWithExistingWorkspace(
                        contextId = contextId,
                        target = target,
                        now = now,
                    )

                is ContextMigrationTarget.NewAspectWithExistingWorkspace ->
                    migrateToNewAspectWithExistingWorkspace(
                        contextId = contextId,
                        target = target,
                        now = now,
                    )

                is ContextMigrationTarget.ExistingAspectWithExistingWorkspace ->
                    migrateToExistingAspectWithExistingWorkspace(
                        contextId = contextId,
                        target = target,
                        now = now,
                    )

                is ContextMigrationTarget.ExistingOrientationWithExistingWorkspace ->
                    migrateToExistingOrientationWithExistingWorkspace(
                        contextId = contextId,
                        target = target,
                        now = now,
                    )

                ContextMigrationTarget.WorkspaceOnly ->
                    migrateToWorkspaceOnly(
                        contextId = contextId,
                        now = now,
                    )
            }
        }

        private suspend fun migrateToWorkspaceOnly(
            contextId: String,
            now: Long,
        ): ContextMigrationResult =
            workspaceWriteThrough.mutate(now) {
                val existingMapping =
                    orientationDao.getLegacyMapping(
                        LegacyOrientationSourceType.CONTEXT.name,
                        contextId,
                    )
                require(existingMapping == null) {
                    "Workspace-only cutover cannot adopt a semantic Context mapping"
                }

                val context =
                    requireNotNull(contextDao.getContextById(contextId)) {
                        "Context does not exist"
                    }
                if (context.isDeleted) {
                    return@mutate validateWorkspaceOnlyCutOver(contextId)
                }

                require(contextDao.getActiveContextsByParentId(contextId).isEmpty()) {
                    "Context migration currently requires an active legacy leaf"
                }
                val workspace =
                    requireNotNull(workspaceDao.getById(contextId)) {
                        "Context-backed Workspace does not exist"
                    }
                require(
                    !workspace.isDeleted &&
                        workspace.provenance == WorkspaceProvenance.CONTEXT_BACKED.name &&
                        workspace.sourceContextId == contextId,
                ) {
                    "Workspace is not owned by this live legacy Context"
                }
                require(
                    orientationDao.getAllWorkspaceBindings().none {
                        !it.isDeleted &&
                            it.bindingType == WorkspaceBindingType.EMBODIES.name &&
                            it.workspaceId == contextId
                    },
                ) {
                    "Workspace-only cutover cannot remove an existing embodiment"
                }

                workspaceDao.upsert(
                    listOf(
                        workspace.materializeContextPresentation(context).copy(
                            updatedAt = now,
                            syncedAt = null,
                            version = nextVersion(workspace.version),
                            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                            sourceContextId = null,
                        ),
                    ),
                )
                contextDao.insert(context.softDelete(now))

                ContextMigrationResult(
                    contextId = contextId,
                    subjectId = null,
                    workspaceId = contextId,
                    mappingId = null,
                    changed = true,
                )
            }

        private suspend fun validateWorkspaceOnlyCutOver(
            contextId: String,
        ): ContextMigrationResult {
            require(
                orientationDao.getLegacyMapping(
                    LegacyOrientationSourceType.CONTEXT.name,
                    contextId,
                ) == null,
            ) {
                "Workspace-only cutover must not have a semantic Context mapping"
            }
            val workspace =
                requireNotNull(workspaceDao.getById(contextId)) {
                    "Workspace-only cut-over Workspace is missing"
                }
            require(
                !workspace.isDeleted &&
                    workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                    workspace.sourceContextId == null,
            ) {
                "Workspace-only cut-over Workspace does not match canonical ownership"
            }
            // Workspace bindings are intentionally not part of completed-state
            // retirement evidence. After Workspace-only cutover, the canonical
            // Workspace may independently acquire EMBODIES/REALIZES/SUPPORTS/
            // MONITORS relations. Semantic Context cutover remains
            // distinguishable by its durable CONTEXT LegacySubjectMapping.
            return ContextMigrationResult(
                contextId = contextId,
                subjectId = null,
                workspaceId = contextId,
                mappingId = null,
                changed = false,
            )
        }

        private suspend fun migrateToExistingOrientationWithExistingWorkspace(
            contextId: String,
            target: ContextMigrationTarget.ExistingOrientationWithExistingWorkspace,
            now: Long,
        ): ContextMigrationResult =
            workspaceWriteThrough.mutate(now) {
                require(target.orientationId.isNotBlank()) {
                    "Existing Orientation id must not be blank"
                }

                val existingSourceMapping =
                    orientationDao.getLegacyMapping(
                        LegacyOrientationSourceType.CONTEXT.name,
                        contextId,
                    )
                if (existingSourceMapping != null) {
                    return@mutate validateExistingOrientationAdoptionCutOver(
                        contextId = contextId,
                        target = target,
                        mapping = existingSourceMapping,
                    )
                }

                val context =
                    requireNotNull(contextDao.getContextById(contextId)) {
                        "Context does not exist"
                    }
                require(!context.isDeleted) { "Context is already deleted" }
                require(contextDao.getActiveContextsByParentId(contextId).isEmpty()) {
                    "Context migration currently requires an active legacy leaf"
                }

                val workspace =
                    requireNotNull(workspaceDao.getById(contextId)) {
                        "Context-backed Workspace does not exist"
                    }
                require(
                    !workspace.isDeleted &&
                        workspace.provenance == WorkspaceProvenance.CONTEXT_BACKED.name &&
                        workspace.sourceContextId == contextId,
                ) {
                    "Workspace is not owned by this live legacy Context"
                }

                orientationRepository.requireCompleteActiveOrientationAggregate(target.orientationId)

                // Any mapping, including a tombstone, reserves a canonical
                // subject id under one-source <-> one-subject provenance.
                val subjectMapping =
                    orientationDao.getAllLegacyMappings()
                        .firstOrNull { it.subjectId == target.orientationId }
                require(subjectMapping == null) {
                    "Existing Orientation is already reserved by legacy mapping " +
                        "${subjectMapping?.sourceType}:${subjectMapping?.sourceId}"
                }

                val liveEmbodiments =
                    orientationDao.getAllWorkspaceBindings()
                        .filter {
                            !it.isDeleted &&
                                it.bindingType == WorkspaceBindingType.EMBODIES.name
                        }
                require(
                    liveEmbodiments.none {
                        it.workspaceId == contextId &&
                            it.subjectId != target.orientationId
                    },
                ) {
                    "Workspace already embodies a different canonical subject"
                }
                require(
                    liveEmbodiments.none {
                        it.subjectId == target.orientationId &&
                            it.workspaceId != contextId
                    },
                ) {
                    "Existing Orientation is already embodied by another Workspace"
                }

                val bindingId =
                    graphRepository.bindExistingPrimaryEmbodiment(
                        subjectId = target.orientationId,
                        workspaceId = contextId,
                        now = now,
                    )

                workspaceDao.upsert(
                    listOf(
                        workspace.materializeContextPresentation(context).copy(
                            updatedAt = now,
                            syncedAt = null,
                            version = nextVersion(workspace.version),
                            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                            sourceContextId = null,
                        ),
                    ),
                )
                val mapping =
                    LegacySubjectMappingEntity(
                        id = stableMappingId(contextId),
                        sourceType = LegacyOrientationSourceType.CONTEXT.name,
                        sourceId = contextId,
                        subjectId = target.orientationId,
                        migrationVersion = CONTEXT_MIGRATION_VERSION,
                        state = LegacySubjectMappingState.CUT_OVER.name,
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    )
                orientationDao.upsertLegacyMappings(listOf(mapping))
                contextDao.insert(context.softDelete(now))

                ContextMigrationResult(
                    contextId = contextId,
                    subjectId = target.orientationId,
                    workspaceId = contextId,
                    mappingId = mapping.id,
                    changed = true,
                ).also {
                    check(bindingId.isNotBlank())
                }
            }

        private suspend fun validateExistingOrientationAdoptionCutOver(
            contextId: String,
            target: ContextMigrationTarget.ExistingOrientationWithExistingWorkspace,
            mapping: LegacySubjectMappingEntity,
        ): ContextMigrationResult {
            require(!mapping.isDeleted) { "Context migration mapping is deleted" }
            require(mapping.state == LegacySubjectMappingState.CUT_OVER.name) {
                "Context already has a non-cutover legacy mapping: ${mapping.state}"
            }
            require(mapping.migrationVersion == CONTEXT_MIGRATION_VERSION) {
                "Unsupported Context migration version: ${mapping.migrationVersion}"
            }
            require(mapping.subjectId == target.orientationId) {
                "Context is already migrated to a different canonical subject"
            }

            val context =
                requireNotNull(contextDao.getContextById(contextId)) {
                    "Cut-over Context tombstone is missing"
                }
            require(context.isDeleted) {
                "Context cutover mapping exists but legacy Context is still live"
            }
            orientationRepository.requireCompleteActiveOrientationAggregate(target.orientationId)

            val workspace =
                requireNotNull(workspaceDao.getById(contextId)) {
                    "Cut-over Workspace is missing"
                }
            require(
                !workspace.isDeleted &&
                    workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                    workspace.sourceContextId == null,
            ) {
                "Cut-over Workspace does not match canonical ownership"
            }
            val liveEmbodiments =
                orientationDao.getAllWorkspaceBindings()
                    .filter {
                        !it.isDeleted &&
                            it.bindingType == WorkspaceBindingType.EMBODIES.name &&
                            (it.workspaceId == contextId || it.subjectId == target.orientationId)
                    }
            require(liveEmbodiments.size == 1) {
                "Cut-over Orientation/Workspace embodiment is missing or conflicting"
            }
            val binding = liveEmbodiments.single()
            require(
                binding.workspaceId == contextId &&
                    binding.subjectId == target.orientationId &&
                    binding.isPrimary,
            ) {
                "Cut-over Orientation/Workspace embodiment does not match the migration mapping"
            }

            return ContextMigrationResult(
                contextId = contextId,
                subjectId = target.orientationId,
                workspaceId = contextId,
                mappingId = mapping.id,
                changed = false,
            )
        }

        private suspend fun migrateToNewOrientationWithExistingWorkspace(
            contextId: String,
            target: ContextMigrationTarget.NewOrientationWithExistingWorkspace,
            now: Long,
        ): ContextMigrationResult =
            workspaceWriteThrough.mutate(now) {
                val existingMapping =
                    orientationDao.getLegacyMapping(
                        LegacyOrientationSourceType.CONTEXT.name,
                        contextId,
                    )
                if (existingMapping != null) {
                    return@mutate validateExistingOrientationCutOver(
                        contextId = contextId,
                        target = target,
                        mapping = existingMapping,
                    )
                }

                val context =
                    requireNotNull(contextDao.getContextById(contextId)) {
                        "Context does not exist"
                    }
                require(!context.isDeleted) {
                    "Context is deleted"
                }
                require(
                    contextDao.getActiveContextsByParentId(contextId).isEmpty(),
                ) {
                    "Context migration requires an active legacy leaf"
                }

                val workspace =
                    requireNotNull(workspaceDao.getById(contextId)) {
                        "Context-backed Workspace does not exist"
                    }
                require(!workspace.isDeleted) {
                    "Context-backed Workspace is deleted"
                }
                require(
                    workspace.provenance == WorkspaceProvenance.CONTEXT_BACKED.name &&
                        workspace.sourceContextId == contextId,
                ) {
                    "Workspace is not owned by this legacy Context"
                }

                val orientationId = stableContextSubjectId(contextId)
                val expectedTitle =
                    (target.titleOverride ?: context.name).trim()
                require(expectedTitle.isNotEmpty()) {
                    "Orientation title must not be blank"
                }
                val expectedDescription =
                    (target.descriptionOverride ?: context.description)
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }

                // LegacySubjectMapping remains one-source <-> one-subject
                // provenance ownership. Any previous mapping, including a
                // tombstone, reserves the canonical subject id.
                val subjectMapping =
                    orientationDao.getAllLegacyMappings()
                        .firstOrNull { it.subjectId == orientationId }
                require(subjectMapping == null) {
                    "Canonical Orientation id is already reserved by another legacy mapping"
                }

                orientationRepository.ensureOrientationWithId(
                    id = orientationId,
                    kind = target.kind,
                    title = expectedTitle,
                    description = expectedDescription,
                    now = now,
                )

                val bindingId =
                    graphRepository.bindExistingPrimaryEmbodiment(
                        subjectId = orientationId,
                        workspaceId = contextId,
                        now = now,
                    )

                workspaceDao.upsert(
                    listOf(
                        workspace.materializeContextPresentation(context).copy(
                            updatedAt = now,
                            syncedAt = null,
                            version = nextVersion(workspace.version),
                            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                            sourceContextId = null,
                        ),
                    ),
                )

                val mapping =
                    LegacySubjectMappingEntity(
                        id = stableMappingId(contextId),
                        sourceType = LegacyOrientationSourceType.CONTEXT.name,
                        sourceId = contextId,
                        subjectId = orientationId,
                        migrationVersion = CONTEXT_MIGRATION_VERSION,
                        state = LegacySubjectMappingState.CUT_OVER.name,
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    )
                orientationDao.upsertLegacyMappings(listOf(mapping))

                contextDao.insert(context.softDelete(now))

                ContextMigrationResult(
                    contextId = contextId,
                    subjectId = orientationId,
                    workspaceId = contextId,
                    mappingId = mapping.id,
                    changed = true,
                ).also {
                    check(bindingId.isNotBlank())
                }
            }

        private suspend fun validateExistingOrientationCutOver(
            contextId: String,
            target: ContextMigrationTarget.NewOrientationWithExistingWorkspace,
            mapping: LegacySubjectMappingEntity,
        ): ContextMigrationResult {
            require(!mapping.isDeleted) {
                "Context migration mapping is deleted"
            }
            require(mapping.state == LegacySubjectMappingState.CUT_OVER.name) {
                "Context already has a non-cutover legacy mapping: ${mapping.state}"
            }
            require(mapping.migrationVersion == CONTEXT_MIGRATION_VERSION) {
                "Unsupported Context migration version: ${mapping.migrationVersion}"
            }

            val expectedOrientationId = stableContextSubjectId(contextId)
            require(mapping.subjectId == expectedOrientationId) {
                "Context is already migrated to a different canonical subject"
            }

            val context =
                requireNotNull(contextDao.getContextById(contextId)) {
                    "Cut-over Context tombstone is missing"
                }
            require(context.isDeleted) {
                "Context cutover mapping exists but legacy Context is still live"
            }

            val expectedTitle =
                (target.titleOverride ?: context.name).trim()
            val expectedDescription =
                (target.descriptionOverride ?: context.description)
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }

            val subject =
                requireNotNull(
                    orientationDao.getManagedSubject(mapping.subjectId),
                ) {
                    "Cut-over Orientation subject is missing"
                }
            require(
                subject.subjectType == ManagedSubjectType.ORIENTATION.name &&
                    !subject.isDeleted,
            ) {
                "Cut-over canonical subject is not an active Orientation"
            }
            require(
                subject.title == expectedTitle &&
                    subject.description == expectedDescription,
            ) {
                "Context is already migrated with different Orientation content"
            }

            val orientation =
                orientationDao.getAllOrientations()
                    .singleOrNull {
                        it.subjectId == mapping.subjectId
                    }
            requireNotNull(orientation) {
                "Cut-over Orientation node is missing"
            }
            require(orientation.kind == target.kind.name) {
                "Context is already migrated as a different Orientation kind"
            }

            val workspace =
                requireNotNull(workspaceDao.getById(contextId)) {
                    "Cut-over Workspace is missing"
                }
            require(!workspace.isDeleted) {
                "Cut-over Workspace is deleted"
            }
            require(
                workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name,
            ) {
                "Cut-over Workspace has not been promoted to canonical ownership"
            }
            require(workspace.sourceContextId == null) {
                "Cut-over Workspace still references the legacy Context"
            }

            val liveEmbodiments =
                orientationDao.getAllWorkspaceBindings()
                    .filter {
                        !it.isDeleted &&
                            it.bindingType == WorkspaceBindingType.EMBODIES.name &&
                            (
                                it.workspaceId == contextId ||
                                    it.subjectId == mapping.subjectId
                            )
                    }
            require(liveEmbodiments.size == 1) {
                "Cut-over Orientation/Workspace embodiment is missing or conflicting"
            }
            val binding = liveEmbodiments.single()
            require(
                binding.workspaceId == contextId &&
                    binding.subjectId == mapping.subjectId &&
                    binding.isPrimary,
            ) {
                "Cut-over Orientation/Workspace embodiment does not match the migration mapping"
            }

            return ContextMigrationResult(
                contextId = contextId,
                subjectId = mapping.subjectId,
                workspaceId = contextId,
                mappingId = mapping.id,
                changed = false,
            )
        }

        private suspend fun migrateToNewAspectWithExistingWorkspace(
            contextId: String,
            target: ContextMigrationTarget.NewAspectWithExistingWorkspace,
            now: Long,
        ): ContextMigrationResult =
            workspaceWriteThrough.mutate(now) {
                val existingMapping =
                    orientationDao.getLegacyMapping(
                        LegacyOrientationSourceType.CONTEXT.name,
                        contextId,
                    )
                if (existingMapping != null) {
                    return@mutate validateExistingCutOver(
                        contextId = contextId,
                        target = target,
                        mapping = existingMapping,
                    )
                }

                val context =
                    requireNotNull(contextDao.getContextById(contextId)) {
                        "Context does not exist"
                    }
                require(!context.isDeleted) { "Context is already deleted" }

                val activeChildren = contextDao.getActiveContextsByParentId(contextId)
                require(activeChildren.isEmpty()) {
                    "Context migration currently requires a leaf Context; " +
                        "active children: ${activeChildren.joinToString { it.id }}"
                }

                val workspace =
                    requireNotNull(workspaceDao.getContextBackedForContextId(contextId)) {
                        "Context-backed Workspace does not exist"
                    }
                require(!workspace.isDeleted) { "Context-backed Workspace is deleted" }

                val aspectId = stableContextSubjectId(contextId)
                val expectedTitle = target.resolvedTitle(context)
                val expectedDescription = target.resolvedDescription(context)

                val liveEmbodiments =
                    orientationDao.getAllWorkspaceBindings()
                        .filter {
                            !it.isDeleted &&
                                it.bindingType == WorkspaceBindingType.EMBODIES.name
                        }

                require(
                    liveEmbodiments.none {
                        it.workspaceId == contextId && it.subjectId != aspectId
                    },
                ) {
                    "Workspace already embodies a different canonical subject"
                }
                require(
                    liveEmbodiments.none {
                        it.subjectId == aspectId && it.workspaceId != contextId
                    },
                ) {
                    "Target Aspect already has a different embodied Workspace"
                }

                aspectRepository.createWithId(
                    id = aspectId,
                    title = expectedTitle,
                    description = expectedDescription,
                    parentAspectId = target.parentAspectId,
                    now = now,
                )

                val bindingId =
                    aspectLinksRepository.bindCompatibilityWorkspace(
                        aspectId = aspectId,
                        contextId = contextId,
                        now = now,
                    )

                workspaceDao.upsert(
                    listOf(
                        workspace.materializeContextPresentation(context).copy(
                            updatedAt = now,
                            syncedAt = null,
                            version = nextVersion(workspace.version),
                            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                            sourceContextId = null,
                        ),
                    ),
                )

                val mapping =
                    LegacySubjectMappingEntity(
                        id = stableMappingId(contextId),
                        sourceType = LegacyOrientationSourceType.CONTEXT.name,
                        sourceId = contextId,
                        subjectId = aspectId,
                        migrationVersion = CONTEXT_MIGRATION_VERSION,
                        state = LegacySubjectMappingState.CUT_OVER.name,
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    )
                orientationDao.upsertLegacyMappings(listOf(mapping))

                contextDao.insert(context.softDelete(now))

                ContextMigrationResult(
                    contextId = contextId,
                    subjectId = aspectId,
                    workspaceId = contextId,
                    mappingId = mapping.id,
                    changed = true,
                ).also {
                    // Keep the value used so an accidental future refactor cannot
                    // silently remove the canonical binding operation.
                    check(bindingId.isNotBlank())
                }
            }

        private suspend fun migrateToExistingAspectWithExistingWorkspace(
            contextId: String,
            target: ContextMigrationTarget.ExistingAspectWithExistingWorkspace,
            now: Long,
        ): ContextMigrationResult =
            workspaceWriteThrough.mutate(now) {
                require(target.aspectId.isNotBlank()) {
                    "Existing Aspect id must not be blank"
                }

                val existingSourceMapping =
                    orientationDao.getLegacyMapping(
                        LegacyOrientationSourceType.CONTEXT.name,
                        contextId,
                    )
                if (existingSourceMapping != null) {
                    return@mutate validateExistingAspectAdoptionCutOver(
                        contextId = contextId,
                        target = target,
                        mapping = existingSourceMapping,
                    )
                }

                val context =
                    requireNotNull(contextDao.getContextById(contextId)) {
                        "Context does not exist"
                    }
                require(!context.isDeleted) { "Context is already deleted" }

                val activeChildren = contextDao.getActiveContextsByParentId(contextId)
                require(activeChildren.isEmpty()) {
                    "Context migration currently requires a leaf Context; " +
                        "active children: ${activeChildren.joinToString { it.id }}"
                }

                val workspace =
                    requireNotNull(workspaceDao.getContextBackedForContextId(contextId)) {
                        "Context-backed Workspace does not exist"
                    }
                require(!workspace.isDeleted) { "Context-backed Workspace is deleted" }

                val aspect =
                    requireNotNull(aspectRepository.get(target.aspectId)) {
                        "Existing Aspect does not exist"
                    }
                require(
                    aspect.subject.subjectType == ManagedSubjectType.ASPECT.name &&
                        !aspect.subject.isDeleted
                ) {
                    "Existing Aspect is not active"
                }

                // LegacySubjectMapping is a one-source <-> one-subject identity
                // bridge. Room also enforces subjectId uniqueness across
                // tombstones, so any prior mapping reserves this canonical id.
                val subjectMapping =
                    orientationDao.getAllLegacyMappings()
                        .firstOrNull { it.subjectId == target.aspectId }
                require(subjectMapping == null) {
                    "Existing Aspect is already reserved by legacy mapping " +
                        "${subjectMapping?.sourceType}:${subjectMapping?.sourceId}"
                }

                val liveEmbodiments =
                    orientationDao.getAllWorkspaceBindings()
                        .filter {
                            !it.isDeleted &&
                                it.bindingType == WorkspaceBindingType.EMBODIES.name
                        }

                require(
                    liveEmbodiments.none {
                        it.workspaceId == contextId &&
                            it.subjectId != target.aspectId
                    },
                ) {
                    "Workspace already embodies a different canonical subject"
                }
                require(
                    liveEmbodiments.none {
                        it.subjectId == target.aspectId &&
                            it.workspaceId != contextId
                    },
                ) {
                    "Existing Aspect is already embodied by another Workspace"
                }

                // bindCompatibilityWorkspace() is allowed here only because the
                // preflight above proves there is no live binding to displace.
                val bindingId =
                    aspectLinksRepository.bindCompatibilityWorkspace(
                        aspectId = target.aspectId,
                        contextId = contextId,
                        now = now,
                    )

                workspaceDao.upsert(
                    listOf(
                        workspace.materializeContextPresentation(context).copy(
                            updatedAt = now,
                            syncedAt = null,
                            version = nextVersion(workspace.version),
                            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                            sourceContextId = null,
                        ),
                    ),
                )

                val mapping =
                    LegacySubjectMappingEntity(
                        id = stableMappingId(contextId),
                        sourceType = LegacyOrientationSourceType.CONTEXT.name,
                        sourceId = contextId,
                        subjectId = target.aspectId,
                        migrationVersion = CONTEXT_MIGRATION_VERSION,
                        state = LegacySubjectMappingState.CUT_OVER.name,
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    )
                orientationDao.upsertLegacyMappings(listOf(mapping))

                contextDao.insert(context.softDelete(now))

                ContextMigrationResult(
                    contextId = contextId,
                    subjectId = target.aspectId,
                    workspaceId = contextId,
                    mappingId = mapping.id,
                    changed = true,
                ).also {
                    check(bindingId.isNotBlank())
                }
            }

        private suspend fun validateExistingAspectAdoptionCutOver(
            contextId: String,
            target: ContextMigrationTarget.ExistingAspectWithExistingWorkspace,
            mapping: LegacySubjectMappingEntity,
        ): ContextMigrationResult {
            require(!mapping.isDeleted) {
                "Context migration mapping is deleted"
            }
            require(mapping.state == LegacySubjectMappingState.CUT_OVER.name) {
                "Context already has a non-cutover legacy mapping: ${mapping.state}"
            }
            require(mapping.migrationVersion == CONTEXT_MIGRATION_VERSION) {
                "Unsupported Context migration version: ${mapping.migrationVersion}"
            }
            require(mapping.subjectId == target.aspectId) {
                "Context is already migrated to a different canonical subject"
            }

            val context =
                requireNotNull(contextDao.getContextById(contextId)) {
                    "Cut-over Context tombstone is missing"
                }
            require(context.isDeleted) {
                "Context cutover mapping exists but legacy Context is still live"
            }

            val aspect =
                requireNotNull(aspectRepository.get(target.aspectId)) {
                    "Cut-over Aspect is missing"
                }
            require(
                aspect.subject.subjectType == ManagedSubjectType.ASPECT.name &&
                    !aspect.subject.isDeleted
            ) {
                "Cut-over subject is not an active Aspect"
            }

            val workspace =
                requireNotNull(workspaceDao.getById(contextId)) {
                    "Cut-over Workspace is missing"
                }
            require(!workspace.isDeleted) {
                "Cut-over Workspace is deleted"
            }
            require(workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name) {
                "Cut-over Workspace has not been promoted to canonical ownership"
            }
            require(workspace.sourceContextId == null) {
                "Cut-over Workspace still references the legacy Context"
            }

            val liveEmbodiments =
                orientationDao.getAllWorkspaceBindings()
                    .filter {
                        !it.isDeleted &&
                            it.bindingType == WorkspaceBindingType.EMBODIES.name &&
                            (
                                it.workspaceId == contextId ||
                                    it.subjectId == target.aspectId
                            )
                    }
            require(liveEmbodiments.size == 1) {
                "Cut-over Aspect/Workspace embodiment is missing or conflicting"
            }
            val binding = liveEmbodiments.single()
            require(
                binding.workspaceId == contextId &&
                    binding.subjectId == target.aspectId &&
                    binding.isPrimary
            ) {
                "Cut-over Aspect/Workspace embodiment does not match the migration mapping"
            }

            return ContextMigrationResult(
                contextId = contextId,
                subjectId = target.aspectId,
                workspaceId = contextId,
                mappingId = mapping.id,
                changed = false,
            )
        }

        private suspend fun validateExistingCutOver(
            contextId: String,
            target: ContextMigrationTarget.NewAspectWithExistingWorkspace,
            mapping: LegacySubjectMappingEntity,
        ): ContextMigrationResult {
            require(!mapping.isDeleted) { "Context migration mapping is deleted" }
            require(mapping.state == LegacySubjectMappingState.CUT_OVER.name) {
                "Context already has a non-cutover legacy mapping: ${mapping.state}"
            }
            require(mapping.migrationVersion == CONTEXT_MIGRATION_VERSION) {
                "Unsupported Context migration version: ${mapping.migrationVersion}"
            }

            val expectedAspectId = stableContextSubjectId(contextId)
            require(mapping.subjectId == expectedAspectId) {
                "Context cutover points to a different canonical subject"
            }

            val context =
                requireNotNull(contextDao.getContextById(contextId)) {
                    "Cut-over Context tombstone is missing"
                }
            require(context.isDeleted) {
                "Context cutover mapping exists but legacy Context is still live"
            }

            val subject =
                requireNotNull(orientationDao.getManagedSubject(mapping.subjectId)) {
                    "Cut-over Aspect subject is missing"
                }
            val node =
                requireNotNull(orientationDao.getAspect(mapping.subjectId)) {
                    "Cut-over Aspect node is missing"
                }
            require(
                subject.subjectType == ManagedSubjectType.ASPECT.name &&
                    !subject.isDeleted
            ) {
                "Cut-over subject is not an active Aspect"
            }

            require(subject.title == target.resolvedTitle(context)) {
                "Context is already migrated with a different Aspect title"
            }
            require(subject.description == target.resolvedDescription(context)) {
                "Context is already migrated with a different Aspect description"
            }
            require(node.parentAspectId == target.parentAspectId) {
                "Context is already migrated under a different Aspect parent"
            }

            val workspace =
                requireNotNull(workspaceDao.getById(contextId)) {
                    "Cut-over Workspace is missing"
                }
            require(!workspace.isDeleted) { "Cut-over Workspace is deleted" }
            require(workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name) {
                "Cut-over Workspace has not been promoted to canonical ownership"
            }
            require(workspace.sourceContextId == null) {
                "Cut-over Workspace still references the legacy Context"
            }

            val liveEmbodiments =
                orientationDao.getAllWorkspaceBindings()
                    .filter {
                        !it.isDeleted &&
                            it.bindingType == WorkspaceBindingType.EMBODIES.name &&
                            (it.workspaceId == contextId || it.subjectId == mapping.subjectId)
                    }
            require(liveEmbodiments.size == 1) {
                "Cut-over Aspect/Workspace embodiment is missing or conflicting"
            }
            val binding = liveEmbodiments.single()
            require(
                binding.workspaceId == contextId &&
                    binding.subjectId == mapping.subjectId &&
                    binding.isPrimary
            ) {
                "Cut-over Aspect/Workspace embodiment does not match the migration mapping"
            }

            return ContextMigrationResult(
                contextId = contextId,
                subjectId = mapping.subjectId,
                workspaceId = contextId,
                mappingId = mapping.id,
                changed = false,
            )
        }

        private fun ContextMigrationTarget.NewAspectWithExistingWorkspace.resolvedTitle(
            context: Context,
        ): String =
            titleOverride
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: context.name.trim().also {
                    require(it.isNotEmpty()) { "Context name cannot produce a blank Aspect title" }
                }

        private fun ContextMigrationTarget.NewAspectWithExistingWorkspace.resolvedDescription(
            context: Context,
        ): String? =
            descriptionOverride
                ?.trim()
                ?.ifEmpty { null }
                ?: context.description?.trim()?.ifEmpty { null }

        private fun stableMappingId(contextId: String): String =
            UUID.nameUUIDFromBytes(
                "CONTEXT-MIGRATION:$contextId".toByteArray(StandardCharsets.UTF_8),
            ).toString()

        private fun nextVersion(version: Long): Long =
            if (version == Long.MAX_VALUE) Long.MAX_VALUE else version + 1L

        private companion object {
            const val CONTEXT_MIGRATION_VERSION = 1
        }
    }

private fun WorkspaceEntity.materializeContextPresentation(context: Context): WorkspaceEntity =
    copy(
        nameOverride = nameOverride?.trim()?.takeIf { it.isNotEmpty() } ?: context.name,
        descriptionOverride = descriptionOverride ?: context.description,
    )
