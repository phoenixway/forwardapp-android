package com.romankozak.forwardappmobile.data.orientation

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectOrientationRefEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextDao
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateAspectOrientationRefs
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateWorkspaceBindings
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectOrientationRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.AspectOrientationRelationType
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBinding
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Canonical command boundary for Aspect memberships and compatibility-Workspace embodiment. */
@Singleton
class CanonicalAspectLinksRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val orientationDao: OrientationDao,
        private val contextDao: ContextDao,
    ) {
        fun observeMemberships(): Flow<List<AspectOrientationRefEntity>> =
            orientationDao.observeAspectOrientationRefs()

        fun observeWorkspaceBindings(): Flow<List<WorkspaceBindingEntity>> =
            orientationDao.observeWorkspaceBindings()

        suspend fun linkOrientation(
            aspectId: String,
            orientationId: String,
            relationType: AspectOrientationRelationType,
            makePrimary: Boolean = false,
            now: Long = System.currentTimeMillis(),
        ): String =
            database.withTransaction {
                requireLiveSubject(aspectId, ManagedSubjectType.ASPECT)
                requireLiveSubject(orientationId, ManagedSubjectType.ORIENTATION)
                require(!makePrimary || relationType == AspectOrientationRelationType.BELONGS_TO) {
                    "Only BELONGS_TO may be primary"
                }
                val all = orientationDao.getAllAspectOrientationRefs()
                val existing =
                    all.firstOrNull {
                        it.aspectId == aspectId &&
                            it.orientationId == orientationId &&
                            it.relationType == relationType.name
                    }
                val demotions =
                    if (makePrimary) {
                        all.filter {
                            !it.isDeleted &&
                                it.orientationId == orientationId &&
                                it.relationType == AspectOrientationRelationType.BELONGS_TO.name &&
                                it.isPrimary &&
                                it.id != existing?.id
                        }.map { it.changed(now, primary = false) }
                    } else {
                        emptyList()
                    }
                val nextOrder =
                    (all.filter { !it.isDeleted && it.aspectId == aspectId }.maxOfOrNull { it.refOrder } ?: -1L) + 1L
                val targetPrimary = makePrimary || (existing?.isPrimary == true && !existing.isDeleted)
                val target =
                    existing?.copy(
                        isPrimary = targetPrimary,
                        refOrder = if (existing.isDeleted) nextOrder else existing.refOrder,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = existing.version + 1L,
                    ) ?: AspectOrientationRefEntity(
                        id = UUID.randomUUID().toString(),
                        aspectId = aspectId,
                        orientationId = orientationId,
                        relationType = relationType.name,
                        isPrimary = targetPrimary,
                        refOrder = nextOrder,
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    )
                val changes = demotions + target
                validateMemberships(mergeRefs(all, changes))
                orientationDao.upsertAspectOrientationRefs(changes)
                target.id
            }

        suspend fun unlinkOrientation(refId: String, now: Long = System.currentTimeMillis()) =
            database.withTransaction {
                val ref = orientationDao.getAllAspectOrientationRefs().firstOrNull { it.id == refId }
                    ?: error("Aspect membership does not exist")
                if (!ref.isDeleted) orientationDao.upsertAspectOrientationRefs(listOf(ref.deleted(now)))
            }

        suspend fun reorderMemberships(
            aspectId: String,
            orderedRefIds: List<String>,
            now: Long = System.currentTimeMillis(),
        ) = database.withTransaction {
            requireLiveSubject(aspectId, ManagedSubjectType.ASPECT)
            val all = orientationDao.getAllAspectOrientationRefs()
            val live = all.filter { it.aspectId == aspectId && !it.isDeleted }
            require(orderedRefIds.size == orderedRefIds.distinct().size) { "Membership reorder contains duplicate ids" }
            require(orderedRefIds.toSet() == live.map { it.id }.toSet()) {
                "Membership reorder must contain every active Aspect reference exactly once"
            }
            val changes =
                orderedRefIds.mapIndexed { index, id ->
                    live.first { it.id == id }.changed(now, index.toLong())
                }
            validateMemberships(mergeRefs(all, changes))
            orientationDao.upsertAspectOrientationRefs(changes)
        }

        /**
         * Binds the current Context compatibility host as this Aspect's primary embodied Workspace.
         * Context fields and hierarchy remain untouched until the accepted Workspace cutover phase.
         */
        suspend fun bindCompatibilityWorkspace(
            aspectId: String,
            contextId: String,
            now: Long = System.currentTimeMillis(),
        ): String =
            database.withTransaction {
                requireLiveSubject(aspectId, ManagedSubjectType.ASPECT)
                val context =
                    requireNotNull(contextDao.getContextById(contextId)) {
                        "Compatibility Workspace does not exist"
                    }
                require(!context.isDeleted) { "Compatibility Workspace is deleted" }
                val all = orientationDao.getAllWorkspaceBindings()
                val existing =
                    all.firstOrNull {
                        it.workspaceId == contextId &&
                            it.subjectId == aspectId &&
                            it.bindingType == WorkspaceBindingType.EMBODIES.name
                    }
                val displaced =
                    all.filter {
                        !it.isDeleted &&
                            it.bindingType == WorkspaceBindingType.EMBODIES.name &&
                            it.id != existing?.id &&
                            (it.subjectId == aspectId || it.workspaceId == contextId)
                    }.map { it.deleted(now) }
                val target =
                    existing?.copy(
                        isPrimary = true,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = existing.version + 1L,
                    ) ?: WorkspaceBindingEntity(
                        id = UUID.randomUUID().toString(),
                        workspaceId = contextId,
                        subjectId = aspectId,
                        bindingType = WorkspaceBindingType.EMBODIES.name,
                        isPrimary = true,
                        bindingOrder = 0L,
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    )
                val changes = displaced + target
                validateBindings(mergeBindings(all, changes))
                orientationDao.upsertWorkspaceBindings(changes)
                target.id
            }

        suspend fun unbindCompatibilityWorkspace(aspectId: String, now: Long = System.currentTimeMillis()) =
            database.withTransaction {
                val changes =
                    orientationDao.getAllWorkspaceBindings()
                        .filter {
                            it.subjectId == aspectId &&
                                it.bindingType == WorkspaceBindingType.EMBODIES.name &&
                                !it.isDeleted
                        }.map { it.deleted(now) }
                orientationDao.upsertWorkspaceBindings(changes)
            }

        private suspend fun requireLiveSubject(id: String, type: ManagedSubjectType) {
            val subject = requireNotNull(orientationDao.getManagedSubject(id)) { "$type subject does not exist" }
            require(subject.subjectType == type.name && !subject.isDeleted) { "$type subject is not active" }
            val nodeExists =
                when (type) {
                    ManagedSubjectType.ASPECT -> orientationDao.getAspect(id) != null
                    ManagedSubjectType.ORIENTATION -> orientationDao.getAllOrientations().any { it.subjectId == id }
                }
            require(nodeExists) { "$type node does not exist" }
        }
    }

private fun AspectOrientationRefEntity.changed(now: Long, order: Long = refOrder, primary: Boolean = isPrimary) =
    copy(refOrder = order, isPrimary = primary, updatedAt = now, syncedAt = null, version = version + 1L)

private fun AspectOrientationRefEntity.deleted(now: Long) =
    copy(updatedAt = now, syncedAt = null, isDeleted = true, version = version + 1L)

private fun WorkspaceBindingEntity.deleted(now: Long) =
    copy(updatedAt = now, syncedAt = null, isDeleted = true, version = version + 1L)

private fun mergeRefs(all: List<AspectOrientationRefEntity>, changes: List<AspectOrientationRefEntity>) =
    (all.associateBy { it.id } + changes.associateBy { it.id }).values.toList()

private fun mergeBindings(all: List<WorkspaceBindingEntity>, changes: List<WorkspaceBindingEntity>) =
    (all.associateBy { it.id } + changes.associateBy { it.id }).values.toList()

private fun validateMemberships(refs: List<AspectOrientationRefEntity>) {
    require(validateAspectOrientationRefs(refs.map { it.toContract() }).isEmpty()) {
        "Aspect references violate DOMAIN-CONTRACT v1"
    }
}

private fun validateBindings(bindings: List<WorkspaceBindingEntity>) {
    require(validateWorkspaceBindings(bindings.map { it.toContract() }).isEmpty()) {
        "Workspace bindings violate DOMAIN-CONTRACT v1"
    }
}

private fun AspectOrientationRefEntity.toContract() =
    AspectOrientationRef(
        id, createdAt, updatedAt, syncedAt, isDeleted, version, aspectId, orientationId,
        AspectOrientationRelationType.valueOf(relationType), isPrimary, refOrder,
    )

private fun WorkspaceBindingEntity.toContract() =
    WorkspaceBinding(
        id, createdAt, updatedAt, syncedAt, isDeleted, version, workspaceId, subjectId,
        WorkspaceBindingType.valueOf(bindingType), isPrimary, bindingOrder,
    )
