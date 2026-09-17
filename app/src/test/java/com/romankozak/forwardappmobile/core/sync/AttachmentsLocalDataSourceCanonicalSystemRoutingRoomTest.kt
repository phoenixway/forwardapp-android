package com.romankozak.forwardappmobile.core.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.AttachmentsBackup
import com.romankozak.forwardappmobile.core.data.models.entities.Context as ContextEntity
import com.romankozak.forwardappmobile.core.data.models.entities.LinkType
import com.romankozak.forwardappmobile.core.data.models.entities.RelatedLink
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalCapabilityInstanceStore
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalConnectionsRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AttachmentsLocalDataSourceCanonicalSystemRoutingRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `shell-free strategic link is stored only as canonical WorkspaceConnection`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, SystemContexts.STRATEGIC.raw)
            val source = source(database)

            val attachmentId =
                source.createLinkAttachment(
                    contextId = SystemContexts.STRATEGIC.raw,
                    link = RelatedLink(type = LinkType.URL, target = "https://example.test", displayName = "Example"),
                )

            val connections = database.workspaceConnectionDao().getLive(SystemContexts.STRATEGIC.raw)
            assertEquals(1, connections.size)
            assertEquals(SystemContexts.STRATEGIC.raw, connections.single().workspaceId)
            assertEquals(attachmentId, connections.single().attachmentId)
            assertEquals(SystemContexts.STRATEGIC.raw, database.attachmentDao().getAttachmentById(attachmentId)?.ownerContextId)
            assertTrue(source.getAttachmentsForContext(SystemContexts.STRATEGIC.raw).first().map { it.attachment.id }.contains(attachmentId))
            assertTrue(database.attachmentDao().getAllContextAttachmentCrossRefs().isEmpty())
            assertNull(database.contextDao().getContextById(SystemContexts.STRATEGIC.raw))
        } finally {
            database.close()
        }
    }

    @Test
    fun `canonical System link reorder and unlink preserve canonical placement semantics`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, SystemContexts.STRATEGIC.raw)
            seedAttachment(database, "first")
            seedAttachment(database, "second")
            val source = source(database)

            source.linkAttachmentToContext("first", SystemContexts.STRATEGIC.raw)
            source.linkAttachmentToContext("second", SystemContexts.STRATEGIC.raw)
            source.linkAttachmentToContext("first", SystemContexts.STRATEGIC.raw)
            assertEquals(2, database.workspaceConnectionDao().getLive(SystemContexts.STRATEGIC.raw).size)

            source.updateAttachmentOrders(
                SystemContexts.STRATEGIC.raw,
                mapOf("second" to 0L, "first" to 1L),
            )
            assertEquals(
                listOf("second", "first"),
                database.workspaceConnectionDao().getLive(SystemContexts.STRATEGIC.raw).map { it.attachmentId },
            )

            source.unlinkAttachmentFromContext("second", SystemContexts.STRATEGIC.raw)
            assertTrue(database.workspaceConnectionDao().getLive(SystemContexts.STRATEGIC.raw).map { it.attachmentId } == listOf("first"))
            assertTrue(database.workspaceConnectionDao().getAll().single { it.attachmentId == "second" }.isDeleted)
        } finally {
            database.close()
        }
    }

    @Test
    fun `invalid canonical System owner fails before link content is created`() = runBlocking {
        val database = database()
        try {
            val source = source(database)

            assertTrue(
                runCatching {
                    source.createLinkAttachment(
                        contextId = SystemContexts.STRATEGIC.raw,
                        link = RelatedLink(type = LinkType.URL, target = "https://example.test", displayName = "Example"),
                    )
                }.isFailure,
            )
            assertTrue(database.attachmentDao().getAll().isEmpty())
            assertTrue(database.workspaceConnectionDao().getAll().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `inactive canonical System Connections capability fails before link content is created`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(
                database = database,
                workspaceId = SystemContexts.STRATEGIC.raw,
                capabilityState = "DISABLED",
            )
            val source = source(database)

            assertTrue(
                runCatching {
                    source.createLinkAttachment(
                        contextId = SystemContexts.STRATEGIC.raw,
                        link = RelatedLink(type = LinkType.URL, target = "https://example.test", displayName = "Example"),
                    )
                }.isFailure,
            )
            assertTrue(database.attachmentDao().getAll().isEmpty())
            assertTrue(database.workspaceConnectionDao().getAll().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `System document attachment bridge writes canonical placement without a Context shell`() = runBlocking {
        val database = database()
        try {
            seedWorkspace(database, SystemContexts.STRATEGIC.raw)
            val source = source(database)

            source.ensureAttachmentLinkedToContext(
                attachmentType = "NOTE_DOCUMENT",
                entityId = "document",
                contextId = SystemContexts.STRATEGIC.raw,
                ownerContextId = SystemContexts.STRATEGIC.raw,
                createdAt = 10L,
                roleCode = null,
                isSystem = false,
            )

            val attachment = requireNotNull(database.attachmentDao().findAttachmentByEntity("NOTE_DOCUMENT", "document"))
            assertEquals(
                listOf(attachment.id),
                database.workspaceConnectionDao().getLive(SystemContexts.STRATEGIC.raw).map { it.attachmentId },
            )
            assertNull(database.contextDao().getContextById(SystemContexts.STRATEGIC.raw))
        } finally {
            database.close()
        }
    }

    @Test
    fun `standalone attachment backup refuses canonical System ownership`() = runBlocking {
        val database = database()
        try {
            val source = source(database)
            val backup =
                AttachmentsBackup(
                    attachments =
                        listOf(
                            AttachmentEntity(
                                id = "system-attachment",
                                attachmentType = "LINK_ITEM",
                                entityId = "link",
                                ownerContextId = SystemContexts.STRATEGIC.raw,
                            ),
                        ),
                )

            assertTrue(runCatching { source.importAttachments(backup) }.isFailure)
            assertTrue(database.attachmentDao().getAll().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `ordinary Context backed attachment compatibility route remains unchanged`() = runBlocking {
        val database = database()
        try {
            val contextId = "ordinary"
            database.contextDao().insert(
                ContextEntity(
                    id = contextId,
                    name = "Ordinary",
                    description = null,
                    parentId = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                ),
            )
            seedWorkspace(
                database = database,
                workspaceId = contextId,
                provenance = WorkspaceProvenance.CONTEXT_BACKED,
                sourceContextId = contextId,
            )
            seedAttachment(database, "ordinary-attachment")
            val source = source(database)

            source.linkAttachmentToContext("ordinary-attachment", contextId)

            assertEquals(
                listOf("ordinary-attachment"),
                source.getAttachmentsForContext(contextId).first().map { it.attachment.id },
            )
            assertEquals(
                listOf(contextId),
                database.attachmentDao().getAllContextAttachmentCrossRefs().map { it.contextId },
            )
            assertFalse(database.workspaceConnectionDao().getLive(SystemContexts.STRATEGIC.raw).isNotEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun `arbitrary canonical only Workspace is not admitted by the System router`() = runBlocking {
        val database = database()
        try {
            val workspaceId = "custom-canonical"
            seedWorkspace(database, workspaceId)
            seedAttachment(database, "custom-attachment")
            val source = source(database)

            source.linkAttachmentToContext("custom-attachment", workspaceId)

            assertTrue(database.workspaceConnectionDao().getLive(workspaceId).isEmpty())
        } finally {
            database.close()
        }
    }

    private fun source(database: AppDatabase): AttachmentsLocalDataSourceImpl {
        val store = CanonicalCapabilityInstanceStore(database, database.workspaceDao(), database.orientationDao())
        return AttachmentsLocalDataSourceImpl(
            appDatabase = database,
            contextDao = database.contextDao(),
            noteDocumentDao = database.noteDocumentDao(),
            musicNoteDao = database.musicNoteDao(),
            checklistDao = database.checklistDao(),
            linkItemDao = database.linkItemDao(),
            attachmentDao = database.attachmentDao(),
            workspaceDao = database.workspaceDao(),
            canonicalConnectionsRepository = CanonicalConnectionsRepository(database, store, database.workspaceConnectionDao()),
        )
    }

    private suspend fun seedWorkspace(
        database: AppDatabase,
        workspaceId: String,
        provenance: WorkspaceProvenance = WorkspaceProvenance.CANONICAL_ONLY,
        sourceContextId: String? = null,
        capabilityState: String = "ACTIVE",
    ) {
        database.workspaceDao().upsert(
            listOf(
                WorkspaceEntity(
                    id = workspaceId,
                    nameOverride = "Workspace",
                    descriptionOverride = null,
                    parentWorkspaceId = null,
                    roleCode = null,
                    workspaceOrder = 0L,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                    provenance = provenance.name,
                    sourceContextId = sourceContextId,
                ),
            ),
        )
        database.orientationDao().upsertWorkspaceCapabilities(
            listOf(
                WorkspaceCapabilityInstanceEntity(
                    id = "connections-$workspaceId",
                    workspaceId = workspaceId,
                    capabilityType = "CONNECTIONS",
                    instanceKey = "default",
                    capabilityOrder = 0L,
                    state = capabilityState,
                    configurationVersion = 1,
                    configuration = "{}",
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                ),
            ),
        )
    }

    private suspend fun seedAttachment(database: AppDatabase, id: String) {
        database.attachmentDao().insertAttachment(
            AttachmentEntity(
                id = id,
                attachmentType = "LINK_ITEM",
                entityId = id,
                createdAt = 1L,
                updatedAt = 1L,
                version = 1L,
            ),
        )
    }

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
}
