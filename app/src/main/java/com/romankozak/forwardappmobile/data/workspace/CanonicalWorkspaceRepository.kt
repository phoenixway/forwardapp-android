package com.romankozak.forwardappmobile.data.workspace

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.softDelete
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationGraphRepository
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextManagementDao
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateSingleParentHierarchy
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBinding
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanonicalWorkspaceRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val workspaceDao: WorkspaceDao,
        private val orientationDao: OrientationDao,
        private val graphRepository: CanonicalOrientationGraphRepository,
        private val contextManagementDao: ContextManagementDao,
    ) {
        suspend fun create(
            nameOverride: String,
            descriptionOverride: String? = null,
            parentWorkspaceId: String? = null,
            roleCode: String? = null,
            now: Long = System.currentTimeMillis(),
        ): String =
            database.withTransaction {
                val name = nameOverride.trim()
                require(name.isNotEmpty()) { "Standalone Workspace name must not be blank" }
                val live = loadLive()
                requireActiveParent(parentWorkspaceId, live)

                val id = UUID.randomUUID().toString()
                val workspace =
                    WorkspaceEntity(
                        id = id,
                        nameOverride = name,
                        descriptionOverride = descriptionOverride.normalized(),
                        parentWorkspaceId = parentWorkspaceId,
                        roleCode = roleCode.normalized(),
                        workspaceOrder = nextOrder(live.values, parentWorkspaceId),
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                        provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                        sourceContextId = null,
                    )
                validateHierarchy(live.values + workspace)
                workspaceDao.upsert(listOf(workspace))
                id
            }

        suspend fun updateDetails(
            id: String,
            nameOverride: String?,
            descriptionOverride: String?,
            roleCode: String?,
            now: Long = System.currentTimeMillis(),
        ) = database.withTransaction {
            val current = requireActiveCanonical(id)
            workspaceDao.upsert(
                listOf(
                    current.bump(now).copy(
                        nameOverride = nameOverride.normalized(),
                        descriptionOverride = descriptionOverride.normalized(),
                        roleCode = roleCode.normalized(),
                    ),
                ),
            )
        }

        suspend fun move(
            id: String,
            newParentWorkspaceId: String?,
            order: Long? = null,
            now: Long = System.currentTimeMillis(),
        ) = database.withTransaction {
            val current = requireActiveCanonical(id)
            val live = loadLive()
            requireActiveParent(newParentWorkspaceId, live)
            val changed =
                current.bump(now).copy(
                    parentWorkspaceId = newParentWorkspaceId,
                    workspaceOrder =
                        order ?: nextOrder(
                            live.values.filterNot { it.id == id },
                            newParentWorkspaceId,
                        ),
                )
            validateHierarchy(live.values.filterNot { it.id == id } + changed)
            workspaceDao.upsert(listOf(changed))
        }

        suspend fun tombstone(
            id: String,
            now: Long = System.currentTimeMillis(),
        ) = database.withTransaction {
            val current = workspaceDao.getById(id) ?: error("Workspace does not exist")
            require(current.provenance == WorkspaceProvenance.CANONICAL_ONLY.name) {
                "Context-backed Workspace lifecycle remains owned by Context"
            }
            if (current.isDeleted) return@withTransaction

            val live = loadLive()
            val children =
                live.values
                    .filter { it.parentWorkspaceId == id }
                    .sortedBy { it.workspaceOrder }
            val protectedChildren =
                children.filter { it.provenance != WorkspaceProvenance.CANONICAL_ONLY.name }
            require(protectedChildren.isEmpty()) {
                "Cannot tombstone canonical-only Workspace while it has Context-backed or unknown-provenance children"
            }

            val rootStart = nextOrder(live.values.filterNot { it.id == id }, null)
            val movedChildren =
                children.mapIndexed { index, child ->
                    child.bump(now).copy(
                        parentWorkspaceId = null,
                        workspaceOrder = rootStart + index,
                    )
                }

            validateHierarchy(
                live.values.filterNot { it.id == id || it.parentWorkspaceId == id } +
                    movedChildren,
            )

            workspaceDao.upsert(movedChildren + current.bump(now).copy(isDeleted = true))

            orientationDao.upsertWorkspaceBindings(
                orientationDao.getAllWorkspaceBindings()
                    .filter { it.workspaceId == id && !it.isDeleted }
                    .map {
                        it.copy(
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = true,
                            version = it.version + 1L,
                        )
                    },
            )
            orientationDao.upsertWorkspaceCapabilities(
                orientationDao.getAllWorkspaceCapabilities()
                    .filter { it.workspaceId == id && !it.isDeleted }
                    .map {
                        it.copy(
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = true,
                            version = it.version + 1L,
                        )
                    },
            )

            val liveExecutionLogs =
                contextManagementDao.getLiveCanonicalExecutionLogsForWorkspace(id)
            if (liveExecutionLogs.isNotEmpty()) {
                contextManagementDao.insertLogs(
                    liveExecutionLogs.map { it.softDelete(now) },
                )
            }
        }

        suspend fun ensureEmbodiedWorkspace(
            subjectId: String,
            parentWorkspaceId: String? = null,
            now: Long = System.currentTimeMillis(),
        ): String =
            database.withTransaction {
                requireLiveSubject(subjectId)

                val bindings =
                    orientationDao.getAllWorkspaceBindings()
                        .filter {
                            !it.isDeleted &&
                                it.subjectId == subjectId &&
                                it.bindingType == WorkspaceBindingType.EMBODIES.name
                        }
                require(bindings.size <= 1) { "Subject has multiple embodied Workspaces" }

                bindings.singleOrNull()?.let { binding ->
                    val existing =
                        requireNotNull(workspaceDao.getById(binding.workspaceId)) {
                            "Embodied Workspace does not exist"
                        }
                    require(!existing.isDeleted) { "Embodied Workspace is deleted" }
                    return@withTransaction existing.id
                }

                val live = loadLive()
                requireActiveParent(parentWorkspaceId, live)
                val workspaceId = UUID.randomUUID().toString()
                workspaceDao.upsert(
                    listOf(
                        WorkspaceEntity(
                            id = workspaceId,
                            nameOverride = null,
                            descriptionOverride = null,
                            parentWorkspaceId = parentWorkspaceId,
                            roleCode = null,
                            workspaceOrder = nextOrder(live.values, parentWorkspaceId),
                            createdAt = now,
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                            provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                            sourceContextId = null,
                        ),
                    ),
                )

                graphRepository.saveWorkspaceBindings(
                    listOf(
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
                        ),
                    ),
                )
                workspaceId
            }

        private suspend fun requireLiveSubject(id: String) {
            val subject = requireNotNull(orientationDao.getManagedSubject(id)) {
                "ManagedSubject does not exist"
            }
            require(!subject.isDeleted) { "ManagedSubject is deleted" }
            when (ManagedSubjectType.valueOf(subject.subjectType)) {
                ManagedSubjectType.ASPECT ->
                    require(orientationDao.getAspect(id) != null) { "Aspect node does not exist" }

                ManagedSubjectType.ORIENTATION ->
                    require(orientationDao.getAllOrientations().any { it.subjectId == id }) {
                        "Orientation node does not exist"
                    }
            }
        }

        private suspend fun requireActiveCanonical(id: String): WorkspaceEntity {
            val workspace = requireNotNull(workspaceDao.getById(id)) { "Workspace does not exist" }
            require(
                workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                    !workspace.isDeleted,
            ) { "Workspace is not an active canonical-only Workspace" }
            return workspace
        }

        private suspend fun loadLive(): Map<String, WorkspaceEntity> =
            workspaceDao.getAll()
                .filterNot { it.isDeleted }
                .associateBy { it.id }

        private fun requireActiveParent(
            parentId: String?,
            live: Map<String, WorkspaceEntity>,
        ) {
            require(parentId == null || parentId in live) { "Workspace parent must be active" }
        }
    }

private fun validateHierarchy(workspaces: Collection<WorkspaceEntity>) {
    require(
        validateSingleParentHierarchy(
            workspaces.associate { it.id to it.parentWorkspaceId },
        ).isEmpty(),
    ) { "Workspace hierarchy violates DOMAIN-CONTRACT v1" }
}

private fun nextOrder(
    workspaces: Collection<WorkspaceEntity>,
    parentId: String?,
): Long =
    (workspaces.filter { it.parentWorkspaceId == parentId }
        .maxOfOrNull { it.workspaceOrder } ?: -1L) + 1L

private fun WorkspaceEntity.bump(now: Long) =
    copy(
        updatedAt = now,
        syncedAt = null,
        version = version + 1L,
    )

private fun String?.normalized(): String? = this?.trim()?.ifEmpty { null }
