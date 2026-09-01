package com.romankozak.forwardappmobile.data.workspace.capability

import androidx.room.withTransaction
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceConnectionEntity
import com.romankozak.forwardappmobile.data.workspace.WorkspaceConnectionDao
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.workspace.ConnectionsCapabilityConfigurationCodec
import com.romankozak.forwardappmobile.shared.core.domain.workspace.canonicalWorkspaceConnectionId
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceCapabilityType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CanonicalConnectionsRepository
    @Inject
    constructor(
        private val database: AppDatabase,
        private val instanceStore: CanonicalCapabilityInstanceStore,
        private val connectionDao: WorkspaceConnectionDao,
    ) {
        suspend fun enable(workspaceId: String, now: Long = System.currentTimeMillis()): String =
            instanceStore.enable(SPEC, workspaceId, now)

        suspend fun disable(workspaceId: String, now: Long = System.currentTimeMillis()) =
            instanceStore.disable(SPEC, workspaceId, now)

        suspend fun archive(workspaceId: String, now: Long = System.currentTimeMillis()) =
            instanceStore.archive(SPEC, workspaceId, now)

        suspend fun restore(workspaceId: String, now: Long = System.currentTimeMillis()) =
            instanceStore.restore(SPEC, workspaceId, now)

        /** Capability metadata deletion preserves placements and Attachment content. */
        suspend fun deleteCapability(workspaceId: String, now: Long = System.currentTimeMillis()) =
            instanceStore.delete(SPEC, workspaceId, now)

        suspend fun requireActive(workspaceId: String) {
            instanceStore.requireActiveInstance(SPEC, workspaceId)
        }

        suspend fun linkAttachment(
            workspaceId: String,
            attachmentId: String,
            now: Long = System.currentTimeMillis(),
        ): String =
            database.withTransaction {
                val capability = instanceStore.requireActiveInstance(SPEC, workspaceId)
                val id = canonicalWorkspaceConnectionId(capability.id, attachmentId)
                val current = connectionDao.getById(id)
                val next =
                    current?.copy(
                        connectionOrder = nextOrder(workspaceId),
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = current.version + 1L,
                    ) ?: WorkspaceConnectionEntity(
                        id = id,
                        workspaceId = workspaceId,
                        capabilityInstanceId = capability.id,
                        attachmentId = attachmentId,
                        connectionOrder = nextOrder(workspaceId),
                        createdAt = now,
                        updatedAt = now,
                        syncedAt = null,
                        isDeleted = false,
                        version = 1L,
                    )
                connectionDao.upsert(listOf(next))
                id
            }

        suspend fun unlinkAttachment(
            workspaceId: String,
            attachmentId: String,
            now: Long = System.currentTimeMillis(),
        ) {
            database.withTransaction {
                val capability = instanceStore.requireActiveInstance(SPEC, workspaceId)
                val id = canonicalWorkspaceConnectionId(capability.id, attachmentId)
                val current = connectionDao.getById(id)?.takeUnless { it.isDeleted } ?: return@withTransaction
                connectionDao.upsert(listOf(current.bump(now).copy(isDeleted = true)))
                compactOrder(workspaceId, now)
            }
        }

        suspend fun reorder(
            workspaceId: String,
            orderedAttachmentIds: List<String>,
            now: Long = System.currentTimeMillis(),
        ) {
            database.withTransaction {
                instanceStore.requireActiveInstance(SPEC, workspaceId)
                require(orderedAttachmentIds.size == orderedAttachmentIds.distinct().size) {
                    "Connections reorder contains duplicate attachment ids"
                }
                val current = connectionDao.getLive(workspaceId)
                require(orderedAttachmentIds.toSet() == current.map { it.attachmentId }.toSet()) {
                    "Connections reorder must contain every active placement exactly once"
                }
                val byAttachmentId = current.associateBy { it.attachmentId }
                val changes =
                    orderedAttachmentIds.mapIndexedNotNull { index, attachmentId ->
                        val connection = byAttachmentId.getValue(attachmentId)
                        val order = index.toLong()
                        connection.takeIf { it.connectionOrder != order }
                            ?.bump(now)
                            ?.copy(connectionOrder = order)
                    }
                if (changes.isNotEmpty()) connectionDao.upsert(changes)
            }
        }

        suspend fun tombstoneOwnedContentForWorkspaces(
            workspaceIds: Collection<String>,
            now: Long = System.currentTimeMillis(),
        ): Int {
            val owners = workspaceIds.map(String::trim).filter(String::isNotEmpty).toSet()
            if (owners.isEmpty()) return 0
            return database.withTransaction {
                val records = owners.flatMap { connectionDao.getLive(it) }
                if (records.isNotEmpty()) {
                    connectionDao.upsert(records.map { it.bump(now).copy(isDeleted = true) })
                }
                records.size
            }
        }

        private suspend fun nextOrder(workspaceId: String): Long =
            (connectionDao.getLive(workspaceId).maxOfOrNull { it.connectionOrder } ?: -1L) + 1L

        private suspend fun compactOrder(workspaceId: String, now: Long) {
            val changes =
                connectionDao.getLive(workspaceId).mapIndexedNotNull { index, record ->
                    val order = index.toLong()
                    record.takeIf { it.connectionOrder != order }?.bump(now)?.copy(connectionOrder = order)
                }
            if (changes.isNotEmpty()) connectionDao.upsert(changes)
        }

        private companion object {
            val SPEC =
                CanonicalCapabilityInstanceSpec(
                    type = WorkspaceCapabilityType.CONNECTIONS,
                    configurationCodec = ConnectionsCapabilityConfigurationCodec,
                    workspaceAuthority = CapabilityWorkspaceAuthority.ALL_ACTIVE_WORKSPACES_AFTER_CUTOVER,
                )
        }
    }

private fun WorkspaceConnectionEntity.bump(now: Long) =
    copy(updatedAt = now, syncedAt = null, version = version + 1L)
