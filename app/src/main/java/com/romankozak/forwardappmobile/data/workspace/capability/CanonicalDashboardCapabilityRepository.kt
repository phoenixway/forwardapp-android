package com.romankozak.forwardappmobile.data.workspace.capability

import com.romankozak.forwardappmobile.shared.core.domain.workspace.DashboardCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Typed DASHBOARD command boundary.
 *
 * DASHBOARD is a PRESENTATION capability and owns no content in v1. Shared
 * instance metadata lifecycle is delegated to the capability kernel.
 */
@Singleton
class CanonicalDashboardCapabilityRepository
    @Inject
    constructor(
        private val instanceStore: CanonicalCapabilityInstanceStore,
    ) {
        suspend fun enable(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ): String = instanceStore.enable(SPEC, workspaceId, now)

        suspend fun disable(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ) = instanceStore.disable(SPEC, workspaceId, now)

        suspend fun isEnabled(workspaceId: String): Boolean {
            val current = instanceStore.findInstance(SPEC, workspaceId) ?: return false
            return !current.isDeleted &&
                current.state == WorkspaceCapabilityState.ACTIVE.name
        }

        suspend fun setEnabled(
            workspaceId: String,
            enabled: Boolean,
            now: Long = System.currentTimeMillis(),
        ) {
            val current = instanceStore.findInstance(SPEC, workspaceId)

            if (enabled) {
                if (
                    current != null &&
                    !current.isDeleted &&
                    current.state == WorkspaceCapabilityState.ACTIVE.name
                ) {
                    return
                }
                enable(workspaceId, now)
                return
            }

            if (
                current == null ||
                current.isDeleted ||
                current.state == WorkspaceCapabilityState.DISABLED.name
            ) {
                return
            }
            disable(workspaceId, now)
        }

        suspend fun archive(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ) = instanceStore.archive(SPEC, workspaceId, now)

        suspend fun restore(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ) = instanceStore.restore(SPEC, workspaceId, now)

        suspend fun delete(
            workspaceId: String,
            now: Long = System.currentTimeMillis(),
        ) = instanceStore.delete(SPEC, workspaceId, now)

        private companion object {
            val SPEC =
                CanonicalCapabilityInstanceSpec(
                    type = WorkspaceCapabilityType.DASHBOARD,
                    configurationCodec = DashboardCapabilityConfigurationCodec,
                    workspaceAuthority = CapabilityWorkspaceAuthority.ALL_ACTIVE_WORKSPACES_AFTER_CUTOVER,
                )
        }
    }
