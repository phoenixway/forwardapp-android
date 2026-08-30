package com.romankozak.forwardappmobile.data.workspace.capability

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.orientation.orientationCapabilityRegistry
import com.romankozak.forwardappmobile.shared.core.domain.orientation.validateCapabilityInstances
import com.romankozak.forwardappmobile.shared.core.domain.workspace.CapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.CapabilityLifecycleCommand
import com.romankozak.forwardappmobile.shared.core.domain.workspace.CapabilityLifecycleProjection
import com.romankozak.forwardappmobile.shared.core.domain.workspace.transitionCapabilityLifecycle
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityInstance
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityAvailability
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class CanonicalCapabilityInstanceSpec(
    val type: WorkspaceCapabilityType,
    val configurationCodec: CapabilityConfigurationCodec,
    val instanceKey: String = DEFAULT_CAPABILITY_INSTANCE_KEY,
    val workspaceAuthority: CapabilityWorkspaceAuthority = CapabilityWorkspaceAuthority.CANONICAL_ONLY,
)

enum class CapabilityWorkspaceAuthority {
    CANONICAL_ONLY,
    ALL_ACTIVE_WORKSPACES_AFTER_CUTOVER,
}

/**
 * Shared persistence kernel for canonical capability-instance metadata.
 *
 * Typed capability repositories own content and invoke this store from their
 * own transactional command boundary. This class deliberately knows nothing
 * about capability content, placement targets, search, or navigation.
 */
@Singleton
class CanonicalCapabilityInstanceStore
    @Inject
    constructor(
        private val database: AppDatabase,
        private val workspaceDao: WorkspaceDao,
        private val orientationDao: OrientationDao,
    ) {
        suspend fun enable(
            spec: CanonicalCapabilityInstanceSpec,
            workspaceId: String,
            now: Long,
        ): String =
            database.withTransaction {
                requireAuthorizedWorkspace(workspaceId, spec)
                val all = orientationDao.getAllWorkspaceCapabilities()
                val current = logicalInstance(all, workspaceId, spec)

                current?.let { validateMutableConfiguration(it, spec) }
                val projection =
                    transitionCapabilityLifecycle(
                        current = current?.lifecycleProjection(),
                        command = CapabilityLifecycleCommand.ENABLE,
                    )

                if (current != null && current.lifecycleProjection() == projection) {
                    return@withTransaction current.id
                }

                val changed =
                    current?.bump(now)?.copy(
                        state = projection.state.name,
                        isDeleted = projection.isDeleted,
                    ) ?: newInstance(all, workspaceId, spec, now, projection)

                persistValidated(all, changed)
                changed.id
            }

        suspend fun disable(
            spec: CanonicalCapabilityInstanceSpec,
            workspaceId: String,
            now: Long,
        ) = mutate(spec, workspaceId, CapabilityLifecycleCommand.DISABLE, now)

        suspend fun archive(
            spec: CanonicalCapabilityInstanceSpec,
            workspaceId: String,
            now: Long,
        ) = mutate(spec, workspaceId, CapabilityLifecycleCommand.ARCHIVE, now)

        suspend fun restore(
            spec: CanonicalCapabilityInstanceSpec,
            workspaceId: String,
            now: Long,
        ) = mutate(spec, workspaceId, CapabilityLifecycleCommand.RESTORE, now)

        suspend fun delete(
            spec: CanonicalCapabilityInstanceSpec,
            workspaceId: String,
            now: Long,
        ) = mutate(spec, workspaceId, CapabilityLifecycleCommand.DELETE, now)

        suspend fun requireActiveInstance(
            spec: CanonicalCapabilityInstanceSpec,
            workspaceId: String,
        ): WorkspaceCapabilityInstanceEntity {
            requireAuthorizedWorkspace(workspaceId, spec)
            val current =
                requireNotNull(
                    logicalInstance(
                        orientationDao.getAllWorkspaceCapabilities(),
                        workspaceId,
                        spec,
                    ),
                ) {
                    "${spec.type} capability does not exist"
                }

            require(!current.isDeleted) { "${spec.type} capability is deleted" }
            validateMutableConfiguration(current, spec)
            require(current.state == WorkspaceCapabilityState.ACTIVE.name) {
                "${spec.type} authoring requires an active capability"
            }
            return current
        }

        private suspend fun mutate(
            spec: CanonicalCapabilityInstanceSpec,
            workspaceId: String,
            command: CapabilityLifecycleCommand,
            now: Long,
        ) =
            database.withTransaction {
                requireAuthorizedWorkspace(workspaceId, spec)
                val all = orientationDao.getAllWorkspaceCapabilities()
                val current =
                    requireNotNull(logicalInstance(all, workspaceId, spec)) {
                        "${spec.type} capability does not exist"
                    }

                if (command == CapabilityLifecycleCommand.DELETE && current.isDeleted) {
                    return@withTransaction
                }

                validateMutableConfiguration(current, spec)
                val projection =
                    transitionCapabilityLifecycle(
                        current = current.lifecycleProjection(),
                        command = command,
                    )
                if (current.lifecycleProjection() == projection) return@withTransaction

                persistValidated(
                    all,
                    current.bump(now).copy(
                        state = projection.state.name,
                        isDeleted = projection.isDeleted,
                    ),
                )
            }

        private suspend fun requireAuthorizedWorkspace(
            workspaceId: String,
            spec: CanonicalCapabilityInstanceSpec,
        ) {
            val definition = orientationCapabilityRegistry.single { it.type == spec.type }
            require(definition.availability == WorkspaceCapabilityAvailability.TARGET) {
                "${spec.type} is not an activatable target capability"
            }

            val workspace =
                requireNotNull(workspaceDao.getById(workspaceId)) {
                    "Workspace does not exist"
                }

            require(!workspace.isDeleted) { "Workspace is deleted" }
            require(
                spec.workspaceAuthority == CapabilityWorkspaceAuthority.ALL_ACTIVE_WORKSPACES_AFTER_CUTOVER ||
                    workspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name,
            ) {
                "${spec.type} canonical commands require a CANONICAL_ONLY Workspace before authority cutover"
            }
        }

        private fun logicalInstance(
            all: List<WorkspaceCapabilityInstanceEntity>,
            workspaceId: String,
            spec: CanonicalCapabilityInstanceSpec,
        ): WorkspaceCapabilityInstanceEntity? {
            val matches =
                all.filter {
                    it.workspaceId == workspaceId &&
                        it.capabilityType == spec.type.name &&
                        it.instanceKey == spec.instanceKey
                }

            require(matches.size <= 1) {
                "Multiple persisted ${spec.type} ${spec.instanceKey} instances violate logical identity"
            }
            return matches.singleOrNull()
        }

        private fun validateMutableConfiguration(
            instance: WorkspaceCapabilityInstanceEntity,
            spec: CanonicalCapabilityInstanceSpec,
        ) {
            spec.configurationCodec.validate(
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
                "Workspace capabilities violate the canonical capability contract"
            }
            orientationDao.upsertWorkspaceCapabilities(listOf(changed))
        }

        private fun newInstance(
            all: List<WorkspaceCapabilityInstanceEntity>,
            workspaceId: String,
            spec: CanonicalCapabilityInstanceSpec,
            now: Long,
            projection: CapabilityLifecycleProjection,
        ) =
            WorkspaceCapabilityInstanceEntity(
                id = UUID.randomUUID().toString(),
                workspaceId = workspaceId,
                capabilityType = spec.type.name,
                instanceKey = spec.instanceKey,
                capabilityOrder = nextOrder(all, workspaceId),
                state = projection.state.name,
                configurationVersion = spec.configurationCodec.currentVersion,
                configuration = spec.configurationCodec.encodeDefault(),
                createdAt = now,
                updatedAt = now,
                syncedAt = null,
                isDeleted = projection.isDeleted,
                version = 1L,
            )

        private fun nextOrder(
            all: List<WorkspaceCapabilityInstanceEntity>,
            workspaceId: String,
        ): Long =
            (
                all.filter { it.workspaceId == workspaceId && !it.isDeleted }
                    .maxOfOrNull { it.capabilityOrder } ?: -1L
            ) + 1L
    }

const val DEFAULT_CAPABILITY_INSTANCE_KEY = "default"

private fun WorkspaceCapabilityInstanceEntity.lifecycleProjection() =
    CapabilityLifecycleProjection(
        state = WorkspaceCapabilityState.valueOf(state),
        isDeleted = isDeleted,
    )

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
