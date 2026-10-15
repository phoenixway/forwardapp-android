package com.romankozak.forwardappmobile.data.orientation

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateWorkspaceBindings
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBinding
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

/** Canonical runtime command boundary for ManagedSubject-to-Workspace bindings. */
@Singleton
class CanonicalWorkspaceBindingRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val orientationDao: OrientationDao,
        private val workspaceDao: WorkspaceDao,
    ) {
        fun observeBindings(subjectId: String): Flow<List<WorkspaceBindingEntity>> =
            orientationDao.observeLiveWorkspaceBindingsForSubject(subjectId)

        suspend fun bind(
            subjectId: String,
            workspaceId: String,
            bindingType: WorkspaceBindingType,
            now: Long = System.currentTimeMillis(),
        ): String =
            database.withTransaction {
                val subject = requireNotNull(orientationDao.getManagedSubject(subjectId)) {
                    "ManagedSubject does not exist"
                }
                require(!subject.isDeleted) { "ManagedSubject is deleted" }
                val nodeExists =
                    when (ManagedSubjectType.valueOf(subject.subjectType)) {
                        ManagedSubjectType.ORIENTATION ->
                            orientationDao.getAllOrientations().any { it.subjectId == subjectId }
                        ManagedSubjectType.ASPECT -> orientationDao.getAspect(subjectId) != null
                    }
                require(nodeExists) { "ManagedSubject canonical node does not exist" }
                val workspace = requireNotNull(workspaceDao.getById(workspaceId)) {
                    "Workspace does not exist"
                }
                require(!workspace.isDeleted) { "Workspace is deleted" }

                val all = orientationDao.getAllWorkspaceBindings()
                val existing =
                    all.firstOrNull {
                        it.subjectId == subjectId &&
                            it.workspaceId == workspaceId &&
                            it.bindingType == bindingType.name
                    }
                val primary = bindingType == WorkspaceBindingType.EMBODIES
                if (existing != null && !existing.isDeleted && existing.isPrimary == primary) {
                    return@withTransaction existing.id
                }
                val nextOrder =
                    (all.filter { !it.isDeleted && it.workspaceId == workspaceId }
                        .maxOfOrNull { it.bindingOrder } ?: -1L) + 1L
                val target =
                    existing?.copy(
                        isPrimary = primary,
                        bindingOrder = if (existing.isDeleted) nextOrder else existing.bindingOrder,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = existing.version + 1L,
                    ) ?: WorkspaceBindingEntity(
                        id = UUID.randomUUID().toString(),
                        workspaceId = workspaceId,
                        subjectId = subjectId,
                        bindingType = bindingType.name,
                        isPrimary = primary,
                        bindingOrder = nextOrder,
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    )
                val final =
                    (all.associateBy { it.id } + (target.id to target)).values.map { it.toContract() }
                require(validateWorkspaceBindings(final).isEmpty()) {
                    "Workspace bindings violate DOMAIN-CONTRACT v1"
                }
                orientationDao.upsertWorkspaceBindings(listOf(target))
                target.id
            }

        suspend fun unbind(bindingId: String, now: Long = System.currentTimeMillis()) {
            database.withTransaction {
                val existing = orientationDao.getAllWorkspaceBindings().firstOrNull { it.id == bindingId }
                    ?: error("Workspace binding does not exist")
                if (!existing.isDeleted) {
                    orientationDao.upsertWorkspaceBindings(
                        listOf(
                            existing.copy(
                                updatedAt = now,
                                syncedAt = null,
                                isDeleted = true,
                                version = existing.version + 1L,
                            ),
                        ),
                    )
                }
            }
        }
    }

private fun WorkspaceBindingEntity.toContract() =
    WorkspaceBinding(
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        workspaceId = workspaceId,
        subjectId = subjectId,
        bindingType = WorkspaceBindingType.valueOf(bindingType),
        isPrimary = isPrimary,
        order = bindingOrder,
    )
