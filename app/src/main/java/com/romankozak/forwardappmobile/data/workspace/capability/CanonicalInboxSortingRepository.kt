package com.romankozak.forwardappmobile.data.workspace.capability

import com.romankozak.forwardappmobile.data.orientation.OrientationDao
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxSortingCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxSortingCapabilityConfigurationV1
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class InboxSortingCapabilityState(
    val lifecycleState: WorkspaceCapabilityState,
    val isDeleted: Boolean,
    val configuration: InboxSortingCapabilityConfigurationV1,
)

@Singleton
class CanonicalInboxSortingRepository
    @Inject
    constructor(
        private val instanceStore: CanonicalCapabilityInstanceStore,
        private val orientationDao: OrientationDao,
    ) {
        suspend fun enable(workspaceId: String, now: Long = System.currentTimeMillis()): String =
            instanceStore.enable(SPEC, workspaceId, now)

        suspend fun disable(workspaceId: String, now: Long = System.currentTimeMillis()) =
            instanceStore.disable(SPEC, workspaceId, now)

        suspend fun setEnabled(
            workspaceId: String,
            enabled: Boolean,
            now: Long = System.currentTimeMillis(),
        ) = instanceStore.setEnabled(SPEC, workspaceId, enabled, now)

        suspend fun establishDisabledIfMissing(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ): Boolean = instanceStore.establishDisabledIfMissing(SPEC, workspaceId, now)

        suspend fun hasEstablishedInstance(workspaceId: String): Boolean =
            instanceStore.hasEstablishedInstance(SPEC, workspaceId)

        fun observeEstablishedInstance(workspaceId: String): Flow<Boolean> =
            instanceStore.observeEstablishedInstance(SPEC, workspaceId)

        suspend fun getState(workspaceId: String): InboxSortingCapabilityState? =
            instanceStore.findInstance(SPEC, workspaceId)?.toInboxSortingCapabilityState()

        internal fun getState(
            workspaceId: String,
            snapshot: CanonicalCapabilityReadSnapshot,
        ): InboxSortingCapabilityState? =
            instanceStore.findInstance(SPEC, workspaceId, snapshot)?.toInboxSortingCapabilityState()

        fun observeState(workspaceId: String): Flow<InboxSortingCapabilityState?> =
            instanceStore.observeInstance(SPEC, workspaceId).map { instance ->
                instance?.let { runCatching { it.toInboxSortingCapabilityState() }.getOrNull() }
            }

        suspend fun archive(workspaceId: String, now: Long = System.currentTimeMillis()) =
            instanceStore.archive(SPEC, workspaceId, now)

        suspend fun restore(workspaceId: String, now: Long = System.currentTimeMillis()) =
            instanceStore.restore(SPEC, workspaceId, now)

        suspend fun deleteCapability(workspaceId: String, now: Long = System.currentTimeMillis()) =
            instanceStore.delete(SPEC, workspaceId, now)

        suspend fun getConfiguration(workspaceId: String): InboxSortingCapabilityConfigurationV1 {
            val instance = requireNotNull(instanceStore.findInstance(SPEC, workspaceId)) {
                "INBOX_SORTING capability does not exist"
            }
            return InboxSortingCapabilityConfigurationCodec.decode(
                version = instance.configurationVersion,
                raw = instance.configuration,
            )
        }

        fun observeConfiguration(workspaceId: String): Flow<InboxSortingCapabilityConfigurationV1> =
            orientationDao.observeWorkspaceCapabilities(workspaceId).map { instances ->
                val matches =
                    instances.filter {
                        it.capabilityType == WorkspaceCapabilityType.INBOX_SORTING.name &&
                            it.instanceKey == DEFAULT_CAPABILITY_INSTANCE_KEY
                    }
                require(matches.size <= 1) {
                    "Multiple persisted INBOX_SORTING default instances violate logical identity"
                }
                val instance = requireNotNull(matches.singleOrNull()) {
                    "INBOX_SORTING capability does not exist"
                }
                InboxSortingCapabilityConfigurationCodec.decode(
                    version = instance.configurationVersion,
                    raw = instance.configuration,
                )
            }

        suspend fun updateConfiguration(
            workspaceId: String,
            configuration: InboxSortingCapabilityConfigurationV1,
            now: Long = System.currentTimeMillis(),
        ) {
            instanceStore.updateConfiguration(
                spec = SPEC,
                workspaceId = workspaceId,
                configurationVersion = InboxSortingCapabilityConfigurationCodec.CURRENT_VERSION,
                configuration = InboxSortingCapabilityConfigurationCodec.encode(configuration),
                now = now,
            )
        }

        suspend fun requireActive(workspaceId: String) {
            instanceStore.requireActiveInstance(SPEC, workspaceId)
        }

        private companion object {
            val SPEC =
                CanonicalCapabilityInstanceSpec(
                    type = WorkspaceCapabilityType.INBOX_SORTING,
                    configurationCodec = InboxSortingCapabilityConfigurationCodec,
                    workspaceAuthority = CapabilityWorkspaceAuthority.ALL_ACTIVE_WORKSPACES_AFTER_CUTOVER,
                )
        }
    }

private fun com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
    .toInboxSortingCapabilityState() =
    InboxSortingCapabilityState(
        lifecycleState = WorkspaceCapabilityState.valueOf(state),
        isDeleted = isDeleted,
        configuration = InboxSortingCapabilityConfigurationCodec.decode(configurationVersion, configuration),
    )
