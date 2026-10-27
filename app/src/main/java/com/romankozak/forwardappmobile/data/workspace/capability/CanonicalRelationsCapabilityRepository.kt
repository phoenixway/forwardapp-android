package com.romankozak.forwardappmobile.data.workspace.capability

import com.romankozak.forwardappmobile.shared.core.domain.workspace.RelationsCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

/** Capability metadata only. Relation lifecycle remains with bindings and Planning references. */
@Singleton
class CanonicalRelationsCapabilityRepository @Inject constructor(private val store: CanonicalCapabilityInstanceStore) {
    fun observeEnabled(workspaceId: String) = store.observeLiveWorkspace(workspaceId)
    suspend fun isEnabled(workspaceId: String): Boolean = store.hasLiveWorkspace(workspaceId)
    suspend fun setEnabled(workspaceId: String, enabled: Boolean, now: Long = System.currentTimeMillis()) =
        store.setEnabled(SPEC, workspaceId, enabled, now)
    suspend fun archive(workspaceId: String, now: Long = System.currentTimeMillis()) = store.archive(SPEC, workspaceId, now)
    suspend fun restore(workspaceId: String, now: Long = System.currentTimeMillis()) = store.restore(SPEC, workspaceId, now)
    suspend fun delete(workspaceId: String, now: Long = System.currentTimeMillis()) = store.delete(SPEC, workspaceId, now)
    private companion object {
        val SPEC = CanonicalCapabilityInstanceSpec(WorkspaceCapabilityType.RELATIONS, RelationsCapabilityConfigurationCodec,
            workspaceAuthority = CapabilityWorkspaceAuthority.ALL_ACTIVE_WORKSPACES_AFTER_CUTOVER)
    }
}
