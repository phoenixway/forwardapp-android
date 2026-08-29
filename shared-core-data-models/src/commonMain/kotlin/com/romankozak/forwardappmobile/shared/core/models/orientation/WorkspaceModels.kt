@file:OptIn(kotlin.js.ExperimentalJsExport::class)

package com.romankozak.forwardappmobile.shared.core.models.orientation

import com.romankozak.forwardappmobile.shared.core.models.sync.SyncEntityMeta
import kotlin.js.JsExport

@JsExport
enum class WorkspaceProvenance {
    CONTEXT_BACKED,
    CANONICAL_ONLY,
}

@JsExport
data class Workspace(
    override val id: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val syncedAt: Long?,
    override val isDeleted: Boolean,
    override val version: Long,
    val nameOverride: String?,
    val descriptionOverride: String?,
    val parentWorkspaceId: String?,
    val roleCode: String?,
    val order: Long,
    val provenance: WorkspaceProvenance = WorkspaceProvenance.CONTEXT_BACKED,
    val sourceContextId: String? = if (provenance == WorkspaceProvenance.CONTEXT_BACKED) id else null,
) : SyncEntityMeta

@JsExport
enum class WorkspaceBindingType {
    EMBODIES,
    REALIZES,
    SUPPORTS,
    MONITORS,
}

@JsExport
data class WorkspaceBinding(
    override val id: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val syncedAt: Long?,
    override val isDeleted: Boolean,
    override val version: Long,
    val workspaceId: String,
    val subjectId: String,
    val bindingType: WorkspaceBindingType,
    val isPrimary: Boolean,
    val order: Long,
) : SyncEntityMeta

@JsExport
enum class WorkspaceCapabilityType {
    BACKLOG,
    INBOX,
    INBOX_SORTING,
    KEY_PROBLEMS,
    DIRECTION,
    ARTIFACT,
    DASHBOARD,
    JOURNAL,
    EXECUTION_LOG,
    CONNECTIONS,
    DOCUMENTS,
    NOTES,
    ATTACHMENTS,
}

@JsExport
enum class WorkspaceCapabilityState {
    ACTIVE,
    DISABLED,
    ARCHIVED,
}

@JsExport
data class WorkspaceCapabilityInstance(
    override val id: String,
    override val createdAt: Long,
    override val updatedAt: Long,
    override val syncedAt: Long?,
    override val isDeleted: Boolean,
    override val version: Long,
    val workspaceId: String,
    val capabilityType: WorkspaceCapabilityType,
    val instanceKey: String,
    val order: Long,
    val state: WorkspaceCapabilityState,
    val configurationVersion: Int,
    val configuration: String,
) : SyncEntityMeta

@JsExport
data class WorkspaceCapabilityDefinition(
    val type: WorkspaceCapabilityType,
    val maxActiveInstances: Int,
    val requiredTypes: List<WorkspaceCapabilityType>,
    val legacyIds: List<String>,
    val autoMigrateWhenEnabled: Boolean,
)
