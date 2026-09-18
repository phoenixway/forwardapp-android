package com.romankozak.forwardappmobile.data.workspace.capability

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.context.ContextId
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.InboxRecord
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceInboxRecordEntity
import com.romankozak.forwardappmobile.data.orientation.LegacySubjectUuid
import com.romankozak.forwardappmobile.data.workspace.WorkspaceDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.workspace.InboxCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityState
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** One-way materializer for pre-canonical full-backup INBOX evidence. */
@Singleton
class LegacyInboxFullBackupAdapter
    @Inject
    constructor(
        private val database: AppDatabase,
        private val workspaceDao: WorkspaceDao,
        private val recordDao: WorkspaceInboxRecordDao,
    ) {
        suspend fun materializeLegacyFullBackup(sources: List<InboxRecord>) {
            if (sources.isEmpty()) return
            database.withTransaction {
                require(sources.map { it.id }.distinct().size == sources.size) {
                    "Legacy INBOX full-backup fallback has duplicate record ids"
                }
                val contextsById = database.contextDao().getAll().associateBy { it.id }
                val workspacesById = workspaceDao.getAll().associateBy { it.id }
                val ownerByContextId = sources.map { it.contextId }.distinct().associateWith { contextId ->
                    requireNotNull(resolveOwner(contextId, contextsById[contextId], workspacesById[contextId])) {
                        "Legacy INBOX full-backup fallback has no admitted Workspace owner for $contextId"
                    }
                }
                val capabilities = database.orientationDao().getAllWorkspaceCapabilities()
                val inboxByWorkspace =
                    capabilities
                        .filter {
                            it.capabilityType == WorkspaceCapabilityType.INBOX.name && it.instanceKey == DEFAULT_KEY
                        }.groupBy { it.workspaceId }
                require(inboxByWorkspace.values.none { it.size > 1 }) {
                    "Legacy INBOX full-backup fallback has duplicate INBOX capability anchors"
                }
                val now = System.currentTimeMillis()
                val missingCapabilities =
                    ownerByContextId.values.distinctBy { it.id }.mapNotNull { workspace ->
                        if (inboxByWorkspace.containsKey(workspace.id)) null
                        else WorkspaceCapabilityInstanceEntity(
                            id = stableInboxCapabilityId(workspace.id),
                            workspaceId = workspace.id,
                            capabilityType = WorkspaceCapabilityType.INBOX.name,
                            instanceKey = DEFAULT_KEY,
                            capabilityOrder = 0L,
                            state = WorkspaceCapabilityState.ACTIVE.name,
                            configurationVersion = InboxCapabilityConfigurationCodec.CURRENT_VERSION,
                            configuration = InboxCapabilityConfigurationCodec.encodeDefault(),
                            createdAt = workspace.createdAt,
                            updatedAt = now,
                            syncedAt = null,
                            isDeleted = false,
                            version = 1L,
                        )
                    }
                if (missingCapabilities.isNotEmpty()) database.orientationDao().upsertWorkspaceCapabilities(missingCapabilities)
                val capabilityIdByWorkspace =
                    (inboxByWorkspace.mapValues { it.value.single().id } +
                        missingCapabilities.associate { it.workspaceId to it.id })
                recordDao.upsert(
                    sources.map { source ->
                        val owner = ownerByContextId.getValue(source.contextId)
                        WorkspaceInboxRecordEntity(
                            id = source.id,
                            workspaceId = owner.id,
                            capabilityInstanceId = capabilityIdByWorkspace.getValue(owner.id),
                            text = source.text,
                            recordOrder = source.order,
                            createdAt = source.createdAt,
                            updatedAt = source.updatedAt ?: source.createdAt,
                            syncedAt = source.syncedAt,
                            isDeleted = source.isDeleted,
                            version = source.version,
                        )
                    },
                )
            }
        }

        private fun resolveOwner(contextId: String, context: Context?, workspace: WorkspaceEntity?): WorkspaceEntity? {
            val liveWorkspace = workspace?.takeUnless { it.isDeleted } ?: return null
            if (SystemContexts.isSystem(ContextId(contextId))) {
                return liveWorkspace.takeIf {
                    it.provenance == WorkspaceProvenance.CANONICAL_ONLY.name && it.sourceContextId == null
                }
            }
            return when {
                context?.isDeleted == false &&
                    liveWorkspace.provenance == WorkspaceProvenance.CONTEXT_BACKED.name &&
                    liveWorkspace.sourceContextId == contextId -> liveWorkspace
                context?.isDeleted == true &&
                    liveWorkspace.provenance == WorkspaceProvenance.CANONICAL_ONLY.name &&
                    liveWorkspace.sourceContextId == null -> liveWorkspace
                else -> null
            }
        }
    }

private fun stableInboxCapabilityId(workspaceId: String): String =
    LegacySubjectUuid.uuidV5(
        UUID.fromString(LegacySubjectUuid.NAMESPACE_UUID),
        "WORKSPACE:CAPABILITY:$workspaceId:${WorkspaceCapabilityType.INBOX.name}:default",
    ).toString()

private const val DEFAULT_KEY = "default"
