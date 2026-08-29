package com.romankozak.forwardappmobile.data.orientation

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
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBinding
import javax.inject.Inject
import javax.inject.Singleton

/** Validated local write boundary for canonical semantic relations and Workspace bindings. */
@Singleton
class CanonicalOrientationGraphRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val dao: OrientationDao,
        private val workspaceDao: WorkspaceDao,
    ) {
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
