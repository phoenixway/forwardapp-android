package com.romankozak.forwardappmobile.data.workspace.capability

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.ContextLog
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.bumpSync
import com.romankozak.forwardappmobile.core.data.models.sync.softDelete
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.data.dao.ContextManagementDao
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateCapabilityInstances
import com.romankozak.forwardappmobile.shared.core.domain.workspace.ExecutionLogCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityInstance
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Canonical command boundary for EXECUTION_LOG on CANONICAL_ONLY Workspaces.
 *
 * Capability-instance lifecycle is separate from execution-log content
 * lifecycle. Capability disable/archive/delete preserve log content, while
 * explicit content commands create, update, or tombstone canonical log rows.
 * CONTEXT_BACKED Workspaces remain owned by Context compatibility.
 */
@Singleton
class CanonicalExecutionLogRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val workspaceDao: WorkspaceDao,
        private val orientationDao: OrientationDao,
        private val contextManagementDao: ContextManagementDao,
    ) {
        suspend fun enable(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ): String =
            database.withTransaction {
                requireActiveCanonicalWorkspace(workspaceId)

                val all = orientationDao.getAllWorkspaceCapabilities()
                val existing = logicalInstance(all, workspaceId)

                if (
                    existing != null &&
                    !existing.isDeleted &&
                    existing.state == WorkspaceCapabilityState.ACTIVE.name
                ) {
                    validateMutableConfiguration(existing)
                    return@withTransaction existing.id
                }

                if (existing != null && !existing.isDeleted) {
                    require(existing.state == WorkspaceCapabilityState.DISABLED.name) {
                        "Archived EXECUTION_LOG capability must be restored before enable"
                    }
                }

                val next =
                    if (existing == null) {
                        WorkspaceCapabilityInstanceEntity(
                            id = UUID.randomUUID().toString(),
                            workspaceId = workspaceId,
                            capabilityType = WorkspaceCapabilityType.EXECUTION_LOG.name,
                            instanceKey = DEFAULT_INSTANCE_KEY,
                            capabilityOrder = nextOrder(all, workspaceId),
                            state = WorkspaceCapabilityState.ACTIVE.name,
                            configurationVersion = ExecutionLogCapabilityConfigurationCodec.CURRENT_VERSION,
                            configuration = ExecutionLogCapabilityConfigurationCodec.encode(),
                            createdAt = now,
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                        )
                    } else {
                        validateMutableConfiguration(existing)
                        existing.bump(now).copy(
                            state = WorkspaceCapabilityState.ACTIVE.name,
                            isDeleted = false,
                        )
                    }

                persistValidated(all, next)
                next.id
            }

        suspend fun disable(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ) = transition(
            workspaceId = workspaceId,
            allowedSources =
                setOf(
                    WorkspaceCapabilityState.ACTIVE,
                    WorkspaceCapabilityState.DISABLED,
                ),
            target = WorkspaceCapabilityState.DISABLED,
            now = now,
        )

        suspend fun archive(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ) = transition(
            workspaceId = workspaceId,
            allowedSources =
                setOf(
                    WorkspaceCapabilityState.ACTIVE,
                    WorkspaceCapabilityState.DISABLED,
                    WorkspaceCapabilityState.ARCHIVED,
                ),
            target = WorkspaceCapabilityState.ARCHIVED,
            now = now,
        )

        /**
         * Restore is deliberately non-activating.
         * Explicit enable is required to resume runtime behavior.
         */
        suspend fun restore(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ) =
            database.withTransaction {
                requireActiveCanonicalWorkspace(workspaceId)
                val all = orientationDao.getAllWorkspaceCapabilities()
                val current = requireLogicalInstance(all, workspaceId)

                require(!current.isDeleted) {
                    "Deleted EXECUTION_LOG capability must be enabled explicitly"
                }
                require(current.state == WorkspaceCapabilityState.ARCHIVED.name) {
                    "Only archived EXECUTION_LOG capability can be restored"
                }
                validateMutableConfiguration(current)

                persistValidated(
                    all,
                    current.bump(now).copy(
                        state = WorkspaceCapabilityState.DISABLED.name,
                    ),
                )
            }

        /**
         * Capability deletion tombstones only instance metadata.
         * Execution-log content is deliberately preserved.
         */
        suspend fun delete(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ) =
            database.withTransaction {
                requireActiveCanonicalWorkspace(workspaceId)
                val all = orientationDao.getAllWorkspaceCapabilities()
                val current = requireLogicalInstance(all, workspaceId)

                if (current.isDeleted) return@withTransaction
                validateMutableConfiguration(current)

                persistValidated(
                    all,
                    current.bump(now).copy(isDeleted = true),
                )
            }

        suspend fun getLiveLogs(
            workspaceId: String,
        ): List<ContextLog> {
            requireCanonicalWorkspace(workspaceId)
            return contextManagementDao.getLiveCanonicalExecutionLogsForWorkspace(workspaceId)
        }

        suspend fun createLog(
            workspaceId: String,
            type: String,
            description: String,
            details: String? = null,
            timestamp: Long = System.currentTimeMillis(),
            now: Long = timestamp,
        ): String =
            database.withTransaction {
                requireAuthoringEnabled(workspaceId)

                val id = UUID.randomUUID().toString()
                require(contextManagementDao.getLogById(id) == null) {
                    "Generated EXECUTION_LOG id already exists"
                }

                contextManagementDao.insertLog(
                    ContextLog(
                        id = id,
                        contextId = null,
                        timestamp = timestamp,
                        type = type,
                        description = description,
                        details = details,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                        workspaceId = workspaceId,
                    ),
                )
                id
            }

        suspend fun updateLog(
            workspaceId: String,
            logId: String,
            type: String,
            description: String,
            details: String? = null,
            timestamp: Long? = null,
            now: Long = System.currentTimeMillis(),
        ) =
            database.withTransaction {
                requireAuthoringEnabled(workspaceId)
                val current = requireCanonicalOwnedLog(workspaceId, logId)
                require(!current.isDeleted) {
                    "Deleted EXECUTION_LOG row cannot be updated"
                }

                contextManagementDao.insertLog(
                    current.bumpSync(now).copy(
                        timestamp = timestamp ?: current.timestamp,
                        type = type,
                        description = description,
                        details = details,
                    ),
                )
            }

        suspend fun deleteLog(
            workspaceId: String,
            logId: String,
            now: Long = System.currentTimeMillis(),
        ) =
            database.withTransaction {
                requireAuthoringEnabled(workspaceId)
                val current = requireCanonicalOwnedLog(workspaceId, logId)
                if (current.isDeleted) return@withTransaction

                contextManagementDao.insertLog(current.softDelete(now))
            }

        private suspend fun requireAuthoringEnabled(workspaceId: String) {
            requireActiveCanonicalWorkspace(workspaceId)

            val all = orientationDao.getAllWorkspaceCapabilities()
            val instance = requireLogicalInstance(all, workspaceId)

            require(!instance.isDeleted) {
                "EXECUTION_LOG capability is deleted"
            }
            validateMutableConfiguration(instance)
            require(instance.state == WorkspaceCapabilityState.ACTIVE.name) {
                "EXECUTION_LOG authoring requires an active capability"
            }
        }

        private suspend fun requireCanonicalOwnedLog(
            workspaceId: String,
            logId: String,
        ): ContextLog {
            val log =
                requireNotNull(contextManagementDao.getLogById(logId)) {
                    "EXECUTION_LOG row does not exist"
                }

            require(log.contextId == null && log.workspaceId != null) {
                "Canonical EXECUTION_LOG commands cannot mutate a legacy Context row"
            }
            require(log.workspaceId == workspaceId) {
                "EXECUTION_LOG row belongs to another Workspace"
            }
            return log
        }

        private suspend fun requireCanonicalWorkspace(workspaceId: String) {
            val workspace =
                requireNotNull(workspaceDao.getById(workspaceId)) {
                    "Workspace does not exist"
                }

            require(workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name) {
                "EXECUTION_LOG canonical commands require CANONICAL_ONLY Workspace ownership"
            }
        }

        private suspend fun transition(
            workspaceId: String,
            allowedSources: Set<WorkspaceCapabilityState>,
            target: WorkspaceCapabilityState,
            now: Long,
        ) =
            database.withTransaction {
                requireActiveCanonicalWorkspace(workspaceId)
                val all = orientationDao.getAllWorkspaceCapabilities()
                val current = requireLogicalInstance(all, workspaceId)

                require(!current.isDeleted) { "EXECUTION_LOG capability is deleted" }
                validateMutableConfiguration(current)

                val currentState = WorkspaceCapabilityState.valueOf(current.state)
                require(currentState in allowedSources) {
                    "Invalid EXECUTION_LOG lifecycle transition: $currentState -> $target"
                }
                if (currentState == target) return@withTransaction

                persistValidated(
                    all,
                    current.bump(now).copy(state = target.name),
                )
            }

        private suspend fun requireActiveCanonicalWorkspace(workspaceId: String) {
            val workspace =
                requireNotNull(workspaceDao.getById(workspaceId)) {
                    "Workspace does not exist"
                }

            require(
                workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                    !workspace.isDeleted,
            ) {
                "EXECUTION_LOG canonical commands require an active CANONICAL_ONLY Workspace"
            }
        }

        private fun logicalInstance(
            all: List<WorkspaceCapabilityInstanceEntity>,
            workspaceId: String,
        ): WorkspaceCapabilityInstanceEntity? {
            val matches =
                all.filter {
                    it.workspaceId == workspaceId &&
                        it.capabilityType == WorkspaceCapabilityType.EXECUTION_LOG.name &&
                        it.instanceKey == DEFAULT_INSTANCE_KEY
                }

            require(matches.size <= 1) {
                "Multiple persisted EXECUTION_LOG default instances violate logical identity"
            }

            return matches.singleOrNull()
        }

        private fun requireLogicalInstance(
            all: List<WorkspaceCapabilityInstanceEntity>,
            workspaceId: String,
        ): WorkspaceCapabilityInstanceEntity =
            requireNotNull(logicalInstance(all, workspaceId)) {
                "EXECUTION_LOG capability does not exist"
            }

        private fun validateMutableConfiguration(instance: WorkspaceCapabilityInstanceEntity) {
            ExecutionLogCapabilityConfigurationCodec.decode(
                version = instance.configurationVersion,
                raw = instance.configuration,
            )
        }

        private suspend fun persistValidated(
            all: List<WorkspaceCapabilityInstanceEntity>,
            changed: WorkspaceCapabilityInstanceEntity,
        ) {
            val final =
                (all.associateBy { it.id } + (changed.id to changed))
                    .values
                    .map { it.toModel() }

            require(validateCapabilityInstances(final).isEmpty()) {
                "Workspace capabilities violate DOMAIN-CONTRACT v1"
            }

            orientationDao.upsertWorkspaceCapabilities(listOf(changed))
        }

        private fun nextOrder(
            all: List<WorkspaceCapabilityInstanceEntity>,
            workspaceId: String,
        ): Long =
            (
                all.filter {
                    it.workspaceId == workspaceId &&
                        !it.isDeleted
                }.maxOfOrNull { it.capabilityOrder } ?: -1L
            ) + 1L

        private companion object {
            const val DEFAULT_INSTANCE_KEY = "default"
        }
    }

private fun WorkspaceCapabilityInstanceEntity.bump(now: Long) =
    copy(
        updatedAt = now,
        syncedAt = null,
        version = version + 1L,
    )

private fun WorkspaceCapabilityInstanceEntity.toModel() =
    WorkspaceCapabilityInstance(
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        isDeleted = isDeleted,
        version = version,
        workspaceId = workspaceId,
        capabilityType = WorkspaceCapabilityType.valueOf(capabilityType),
        instanceKey = instanceKey,
        order = capabilityOrder,
        state = WorkspaceCapabilityState.valueOf(state),
        configurationVersion = configurationVersion,
        configuration = configuration,
    )
