package com.romankozak.forwardappmobile.data.orientation

import java.util.UUID
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectOrientationRefEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateAspectOrientationRefs
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateOrientationRelations
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateWorkspaceBindings
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectOrientationRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelation
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBinding
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Validated local write boundary for canonical semantic relations and Workspace bindings. */
@Singleton
class CanonicalOrientationGraphRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val dao: OrientationDao,
        private val workspaceDao: WorkspaceDao,
    ) {
        /**
         * Narrow canonical read for the Orientation editor.
         * Includes both incoming and outgoing live edges.
         */
        fun observeRelations(
            orientationId: String,
        ): Flow<List<OrientationRelationEntity>> =
            dao.observeLiveOrientationRelationsForOrientation(orientationId)

        /**
         * Product command for independently authorable Orientation graph edges.
         * PART_OF is excluded because Beacon Group membership has a separate
         * GroupScope / hierarchy mutation authority.
         */
        suspend fun addRelation(
            fromOrientationId: String,
            toOrientationId: String,
            relationType: OrientationRelationType,
            now: Long = System.currentTimeMillis(),
        ): String =
            database.withTransaction {
                require(relationType != OrientationRelationType.PART_OF) {
                    "PART_OF is managed by its canonical hierarchy authority"
                }
                require(fromOrientationId != toOrientationId) {
                    "Orientation self-relations are forbidden"
                }

                requireLiveOrientation(fromOrientationId)
                requireLiveOrientation(toOrientationId)

                val fromId: String
                val toId: String

                if (
                    relationType == OrientationRelationType.CONFLICTS_WITH &&
                    fromOrientationId > toOrientationId
                ) {
                    fromId = toOrientationId
                    toId = fromOrientationId
                } else {
                    fromId = fromOrientationId
                    toId = toOrientationId
                }

                val all = dao.getAllOrientationRelations()
                val existing =
                    all.firstOrNull {
                        it.fromOrientationId == fromId &&
                            it.toOrientationId == toId &&
                            it.relationType == relationType.name
                    }

                if (existing != null && !existing.isDeleted) {
                    return@withTransaction existing.id
                }

                val target =
                    existing?.copy(
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = existing.version + 1L,
                    ) ?: OrientationRelationEntity(
                        id = UUID.randomUUID().toString(),
                        fromOrientationId = fromId,
                        toOrientationId = toId,
                        relationType = relationType.name,
                        relationOrder = null,
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    )

                validateGraphChange(all, target)
                dao.upsertOrientationRelations(listOf(target))
                target.id
            }

        /**
         * Tombstones precisely one canonical edge using optimistic versioning.
         * Neither endpoint nor any other relation family is mutated.
         */
        suspend fun removeRelation(
            relationId: String,
            expectedVersion: Long,
            now: Long = System.currentTimeMillis(),
        ) {
            database.withTransaction {
                val all = dao.getAllOrientationRelations()
                val existing =
                    requireNotNull(all.firstOrNull { it.id == relationId }) {
                        "Orientation relation does not exist"
                    }

                require(existing.relationType != OrientationRelationType.PART_OF.name) {
                    "PART_OF is managed by its canonical hierarchy authority"
                }
                require(!existing.isDeleted) {
                    "Orientation relation is already deleted"
                }
                require(existing.version == expectedVersion) {
                    "Orientation relation version conflict"
                }

                requireLiveOrientation(existing.fromOrientationId)
                requireLiveOrientation(existing.toOrientationId)

                val tombstone =
                    existing.copy(
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = true,
                        version = existing.version + 1L,
                    )

                validateGraphChange(all, tombstone)
                dao.upsertOrientationRelations(listOf(tombstone))
            }
        }

        private suspend fun requireLiveOrientation(id: String) {
            val subject =
                requireNotNull(dao.getManagedSubject(id)) {
                    "Orientation ManagedSubject does not exist"
                }
            require(
                !subject.isDeleted &&
                    subject.subjectType == ManagedSubjectType.ORIENTATION.name
            ) {
                "Orientation ManagedSubject is not active"
            }
            require(dao.getOrientation(id) != null) {
                "Canonical Orientation node does not exist"
            }
        }

        private suspend fun validateGraphChange(
            existing: List<OrientationRelationEntity>,
            change: OrientationRelationEntity,
        ) {
            val final =
                (existing.associateBy { it.id } + (change.id to change))
                    .values.map { it.toModel() }
            val orientationIds =
                dao.getAllOrientations().mapTo(hashSetOf()) { it.subjectId }
            val violations = validateOrientationRelations(orientationIds, final)

            require(violations.isEmpty()) {
                "Orientation graph violates DOMAIN-CONTRACT v1: " +
                    violations.joinToString { it.code }
            }
        }

        suspend fun saveRelations(changes: List<OrientationRelation>) {
            database.withTransaction {
                val final = mergeById(dao.getAllOrientationRelations().map { it.toModel() }, changes) { it.id }
                val ids = dao.getAllOrientations().mapTo(hashSetOf()) { it.subjectId }
                require(validateOrientationRelations(ids, final).isEmpty()) {
                    "Orientation relation graph violates DOMAIN-CONTRACT v1"
                }
                dao.upsertOrientationRelations(changes.map { it.toEntity() })
            }
        }

        @Deprecated("Use CanonicalAspectLinksRepository membership commands")
        suspend fun saveAspectRefs(changes: List<AspectOrientationRef>) {
            database.withTransaction {
                val final = mergeById(dao.getAllAspectOrientationRefs().map { it.toModel() }, changes) { it.id }
                val aspectIds = dao.getAllAspects().mapTo(hashSetOf()) { it.subjectId }
                val orientationIds = dao.getAllOrientations().mapTo(hashSetOf()) { it.subjectId }
                require(final.all { it.aspectId in aspectIds && it.orientationId in orientationIds }) {
                    "Aspect references require existing endpoints"
                }
                require(validateAspectOrientationRefs(final).isEmpty()) {
                    "Aspect references violate DOMAIN-CONTRACT v1"
                }
                dao.upsertAspectOrientationRefs(changes.map { it.toEntity() })
            }
        }

        suspend fun saveWorkspaceBindings(changes: List<WorkspaceBinding>) {
            database.withTransaction {
                val final = mergeById(dao.getAllWorkspaceBindings().map { it.toModel() }, changes) { it.id }
                val subjectIds = dao.getAllManagedSubjects().mapTo(hashSetOf()) { it.id }
                val workspaceIds = workspaceDao.getAll().mapTo(hashSetOf()) { it.id }
                require(final.all { it.subjectId in subjectIds }) { "Workspace binding requires an existing subject" }
                require(final.all { it.workspaceId in workspaceIds }) {
                    "Workspace binding requires an existing Workspace"
                }
                require(validateWorkspaceBindings(final).isEmpty()) {
                    "Workspace bindings violate DOMAIN-CONTRACT v1"
                }
                dao.upsertWorkspaceBindings(changes.map { it.toEntity() })
            }
        }

        /**
         * Makes one already-existing live Workspace the primary EMBODIES host
         * for one already-existing live ManagedSubject.
         *
         * This command never displaces another embodiment. Exact existing
         * ownership is idempotent; any competing EMBODIES edge fails closed.
         */
        suspend fun bindExistingPrimaryEmbodiment(
            subjectId: String,
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ): String =
            database.withTransaction {
                val subject =
                    requireNotNull(dao.getManagedSubject(subjectId)) {
                        "ManagedSubject does not exist"
                    }
                require(!subject.isDeleted) {
                    "ManagedSubject is deleted"
                }

                val workspace =
                    requireNotNull(workspaceDao.getById(workspaceId)) {
                        "Workspace does not exist"
                    }
                require(!workspace.isDeleted) {
                    "Workspace is deleted"
                }

                val all = dao.getAllWorkspaceBindings().map { it.toModel() }
                val liveEmbodies =
                    all.filter {
                        !it.isDeleted &&
                            it.bindingType == WorkspaceBindingType.EMBODIES
                    }

                val exact =
                    liveEmbodies.filter {
                        it.subjectId == subjectId &&
                            it.workspaceId == workspaceId
                    }
                require(exact.size <= 1) {
                    "Exact EMBODIES binding is duplicated"
                }

                val existing = exact.singleOrNull()
                val conflicts =
                    liveEmbodies.filter {
                        it.id != existing?.id &&
                            (
                                it.subjectId == subjectId ||
                                    it.workspaceId == workspaceId
                            )
                    }
                require(conflicts.isEmpty()) {
                    "EMBODIES ownership conflicts with an existing binding"
                }

                existing?.let {
                    require(it.isPrimary) {
                        "Existing EMBODIES binding is not primary"
                    }
                    return@withTransaction it.id
                }

                val binding =
                    WorkspaceBinding(
                        id = UUID.randomUUID().toString(),
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                        workspaceId = workspaceId,
                        subjectId = subjectId,
                        bindingType = WorkspaceBindingType.EMBODIES,
                        isPrimary = true,
                        order = 0L,
                    )

                val final =
                    mergeById(
                        existing = all,
                        changes = listOf(binding),
                        id = { it.id },
                    )
                require(validateWorkspaceBindings(final).isEmpty()) {
                    "Workspace bindings violate DOMAIN-CONTRACT v1"
                }

                dao.upsertWorkspaceBindings(
                    listOf(binding.toEntity()),
                )
                binding.id
            }

    }

private fun <T> mergeById(existing: List<T>, changes: List<T>, id: (T) -> String): List<T> =
    (existing.associateBy(id) + changes.associateBy(id)).values.toList()

private fun OrientationRelationEntity.toModel() =
    OrientationRelation(
        id, createdAt, updatedAt, syncedAt, isDeleted, version,
        fromOrientationId, toOrientationId,
        com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationRelationType.valueOf(relationType),
        relationOrder,
    )

private fun OrientationRelation.toEntity() =
    OrientationRelationEntity(
        id, fromOrientationId, toOrientationId, relationType.name, order,
        createdAt, updatedAt, syncedAt, isDeleted, version,
    )

private fun AspectOrientationRefEntity.toModel() =
    AspectOrientationRef(
        id, createdAt, updatedAt, syncedAt, isDeleted, version,
        aspectId, orientationId,
        com.romankozak.forwardappmobile.shared.core.models.orientation.AspectOrientationRelationType.valueOf(relationType),
        isPrimary, refOrder,
    )

private fun AspectOrientationRef.toEntity() =
    AspectOrientationRefEntity(
        id, aspectId, orientationId, relationType.name, isPrimary, order,
        createdAt, updatedAt, syncedAt, isDeleted, version,
    )

private fun WorkspaceBindingEntity.toModel() =
    WorkspaceBinding(
        id, createdAt, updatedAt, syncedAt, isDeleted, version,
        workspaceId, subjectId,
        com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType.valueOf(bindingType),
        isPrimary, bindingOrder,
    )

private fun WorkspaceBinding.toEntity() =
    WorkspaceBindingEntity(
        id, workspaceId, subjectId, bindingType.name, isPrimary, order,
        createdAt, updatedAt, syncedAt, isDeleted, version,
    )
