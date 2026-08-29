package com.romankozak.forwardappmobile.data.workspace.capability

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateCapabilityInstances
import com.romankozak.forwardappmobile.shared.core.domain.workspace.DashboardCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityInstance
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Canonical command boundary for DASHBOARD on CANONICAL_ONLY Workspaces.
 *
 * DASHBOARD v1 owns no external content. Lifecycle mutations therefore change
 * only WorkspaceCapabilityInstance metadata. CONTEXT_BACKED Workspaces remain
 * owned by the Context compatibility boundary.
 */
@Singleton
class CanonicalDashboardCapabilityRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val workspaceDao: WorkspaceDao,
        private val orientationDao: OrientationDao,
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
                        "Archived DASHBOARD capability must be restored before enable"
                    }
                }

                val next =
                    if (existing == null) {
                        WorkspaceCapabilityInstanceEntity(
                            id = UUID.randomUUID().toString(),
                            workspaceId = workspaceId,
                            capabilityType = WorkspaceCapabilityType.DASHBOARD.name,
                            instanceKey = DEFAULT_INSTANCE_KEY,
                            capabilityOrder = nextOrder(all, workspaceId),
                            state = WorkspaceCapabilityState.ACTIVE.name,
                            configurationVersion = DashboardCapabilityConfigurationCodec.CURRENT_VERSION,
                            configuration = DashboardCapabilityConfigurationCodec.encode(),
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
                    "Deleted DASHBOARD capability must be enabled explicitly"
                }
                require(current.state == WorkspaceCapabilityState.ARCHIVED.name) {
                    "Only archived DASHBOARD capability can be restored"
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
         * DASHBOARD v1 owns no content, so delete tombstones only instance
         * metadata and has no content cascade.
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

                require(!current.isDeleted) { "DASHBOARD capability is deleted" }
                validateMutableConfiguration(current)

                val currentState = WorkspaceCapabilityState.valueOf(current.state)
                require(currentState in allowedSources) {
                    "Invalid DASHBOARD lifecycle transition: $currentState -> $target"
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
                "DASHBOARD canonical commands require an active CANONICAL_ONLY Workspace"
            }
        }

        private fun logicalInstance(
            all: List<WorkspaceCapabilityInstanceEntity>,
            workspaceId: String,
        ): WorkspaceCapabilityInstanceEntity? {
            val matches =
                all.filter {
                    it.workspaceId == workspaceId &&
                        it.capabilityType == WorkspaceCapabilityType.DASHBOARD.name &&
                        it.instanceKey == DEFAULT_INSTANCE_KEY
                }

            require(matches.size <= 1) {
                "Multiple persisted DASHBOARD default instances violate logical identity"
            }

            return matches.singleOrNull()
        }

        private fun requireLogicalInstance(
            all: List<WorkspaceCapabilityInstanceEntity>,
            workspaceId: String,
        ): WorkspaceCapabilityInstanceEntity =
            requireNotNull(logicalInstance(all, workspaceId)) {
                "DASHBOARD capability does not exist"
            }

        private fun validateMutableConfiguration(instance: WorkspaceCapabilityInstanceEntity) {
            DashboardCapabilityConfigurationCodec.decode(
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
