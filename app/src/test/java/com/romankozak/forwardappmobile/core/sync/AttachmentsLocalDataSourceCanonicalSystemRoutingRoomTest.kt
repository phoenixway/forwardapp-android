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
import kotlinx.coroutines.flow.stateIn
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
    @Test fun `native Connections content reads also include linked checklist and music note`() = runBlocking {
        val db = database()
        try {
            seedWorkspace(db, "native", WorkspaceProvenance.STANDALONE)
            val checklist = com.romankozak.forwardappmobile.core.data.models.entities.ChecklistEntity(name = "Checklist", contextId = "other")
            val music = com.romankozak.forwardappmobile.core.data.models.entities.MusicNoteEntity(name = "Music", contextId = "other")
            db.checklistDao().insertChecklist(checklist)
            db.musicNoteDao().insert(music)
            val source = source(db)
            source.ensureAttachmentLinkedToContext("CHECKLIST", checklist.id, "native", "other", 20, null, false)
            source.ensureAttachmentLinkedToContext("MUSIC_NOTE", music.id, "native", "other", 21, null, false)
            assertEquals(listOf(checklist.id), db.checklistDao().getChecklistsForContext("native", "CHECKLIST").first().map { it.id })
            assertEquals(listOf(music.id), db.musicNoteDao().getForContext("native", "MUSIC_NOTE").first().map { it.id })
            assertEquals(2, source.getAttachmentsForContext("native").first().size)
            assertNull(db.contextDao().getContextById("native"))
        } finally { db.close() }
    }

    @Test fun `Context-backed canonical Workspace ID differs from its proven legacy alias`() = runBlocking {
        val db = database()
        try {
            db.contextDao().insert(ContextEntity(id = "legacy-context", name = "Legacy", description = null,
                parentId = null, createdAt = 1, updatedAt = 1))
            seedWorkspace(db, "canonical-workspace", WorkspaceProvenance.CONTEXT_BACKED, "legacy-context")
            val source = source(db)
            val note = com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity(name = "Note", contextId = "canonical-workspace")
            db.noteDocumentDao().insertDocument(note)
            source.ensureAttachmentLinkedToContext("NOTE_DOCUMENT", note.id, "canonical-workspace", "canonical-workspace", 20, null, false)
            assertEquals(listOf(note.id), db.noteDocumentDao().getDocumentsForContext("canonical-workspace").first().map { it.id })
            assertEquals(listOf(note.id), db.noteDocumentDao().getDocumentsForContext("legacy-context").first().map { it.id })
            assertEquals(1, source.getAttachmentsForContext("canonical-workspace").first().size)
            assertEquals(1, source.getAttachmentsForContext("legacy-context").first().size)
            assertEquals("canonical-workspace", db.workspaceConnectionDao().getAll().single().workspaceId)
            assertNull(db.contextDao().getContextById("canonical-workspace"))
        } finally { db.close() }
    }

    @Test fun `ordinary canonical and standalone note creation bridge writes visible canonical Connections`() = runBlocking {
        for (provenance in listOf(WorkspaceProvenance.CANONICAL_ONLY, WorkspaceProvenance.STANDALONE)) {
            val db = database()
            try {
                val owner = "native-${provenance.name}"
                seedWorkspace(db, owner, provenance)
                val source = source(db)
                source.requireAttachmentPlacementAuthoring(owner)
                val note = com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity(name = "New note", contextId = owner)
                db.noteDocumentDao().insertDocument(note)
                source.ensureAttachmentLinkedToContext("NOTE_DOCUMENT", note.id, owner, owner, 20, null, false)
                val attachment = source.findAttachmentByEntity("NOTE_DOCUMENT", note.id)!!
                assertEquals(listOf(attachment.id), source.getAttachmentsForContext(owner).first().map { it.attachment.id })
                assertEquals(listOf(note.id), db.noteDocumentDao().getDocumentsForContext(owner).first().map { it.id })
                assertEquals(owner, db.workspaceConnectionDao().getAll().single().workspaceId)
                assertNull(db.contextDao().getContextById(owner))
            } finally { db.close() }
        }
    }

    @Test fun `existing shared note selection and unlink update both attachment and content streams`() = runBlocking {
        val db = database()
        val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)
        try {
            seedWorkspace(db, "native", WorkspaceProvenance.STANDALONE)
            val note = com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity(name = "Existing", contextId = "other-owner")
            db.noteDocumentDao().insertDocument(note)
            db.attachmentDao().insertAttachment(AttachmentEntity(id = "existing", attachmentType = "NOTE_DOCUMENT", entityId = note.id, ownerContextId = "other-owner"))
            val source = source(db)
            val stream = kotlinx.coroutines.flow.combine(source.getAttachmentsForContext("native"),
                db.noteDocumentDao().getDocumentsForContext("native")) { attachments, notes -> attachments.size to notes.size }
            val state = stream.stateIn(scope)
            assertEquals(0 to 0, state.value)
            source.linkAttachmentToContext("existing", "native")
            kotlinx.coroutines.withTimeout(10_000) { state.first { it == (1 to 1) } }
            source.unlinkAttachmentFromContext("existing", "native")
            kotlinx.coroutines.withTimeout(10_000) { state.first { it == (0 to 0) } }
            assertEquals("other-owner", db.noteDocumentDao().getDocumentByIdFlow(note.id).first()!!.contextId)
            assertFalse(db.attachmentDao().getAttachmentById("existing")!!.isDeleted)
            assertNull(db.contextDao().getContextById("native"))
        } finally { scope.coroutineContext[kotlinx.coroutines.Job]?.cancel(); db.close() }
    }

    @Test fun `ordinary native disabled or deleted owner fails before inserting link content`() = runBlocking {
        for (deleted in listOf(false, true)) {
            val db = database()
            try {
                seedWorkspace(db, "native", capabilityState = if (deleted) "ACTIVE" else "DISABLED")
                if (deleted) db.workspaceDao().upsert(listOf(db.workspaceDao().getById("native")!!.copy(isDeleted = true)))
                val source = source(db)
                assertTrue(runCatching { source.requireAttachmentPlacementAuthoring("native") }.isFailure)
                assertTrue(runCatching { source.createLinkAttachment("native", RelatedLink(LinkType.URL, "https://example.com"), null, false) }.isFailure)
                assertTrue(db.attachmentDao().getAll().isEmpty())
                assertTrue(db.workspaceConnectionDao().getAll().isEmpty())
            } finally { db.close() }
        }
    }

    @Test fun `native canonical owner takes precedence over legacy Context alias with same raw ID`() = runBlocking {
        val db = database()
        try {
            seedWorkspace(db, "native")
            db.contextDao().insert(ContextEntity(id = "native", name = "Legacy shell", description = null,
                parentId = null, createdAt = 1, updatedAt = 1))
            seedWorkspace(db, "legacy-owner", WorkspaceProvenance.CONTEXT_BACKED, "native")
            val native = com.romankozak.forwardappmobile.core.data.models.entities.NoteDocumentEntity(name = "Native", contextId = "native")
            val legacy = native.copy(id = "legacy-note", name = "Legacy")
            db.noteDocumentDao().insertAllDocuments(listOf(native, legacy))
            db.attachmentDao().insertAttachments(listOf(
                AttachmentEntity(id = "native-a", attachmentType = "NOTE_DOCUMENT", entityId = native.id),
                AttachmentEntity(id = "legacy-a", attachmentType = "NOTE_DOCUMENT", entityId = legacy.id)))
            val store = CanonicalCapabilityInstanceStore(db, db.workspaceDao(), db.orientationDao())
            val connections = CanonicalConnectionsRepository(db, store, db.workspaceConnectionDao())
            connections.linkAttachment("native", "native-a", 20)
            connections.linkAttachment("legacy-owner", "legacy-a", 21)
            assertEquals(listOf(native.id), db.noteDocumentDao().getDocumentsForContext("native").first().map { it.id })
            assertEquals(listOf("native-a"), source(db).getAttachmentsForContext("native").first().map { it.attachment.id })
        } finally { db.close() }
    }

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
    fun `ordinary canonical only Workspace now uses canonical Connections routing`() = runBlocking {
        val database = database()
        try {
            val workspaceId = "custom-canonical"
            seedWorkspace(database, workspaceId)
            seedAttachment(database, "custom-attachment")
            val source = source(database)

            source.linkAttachmentToContext("custom-attachment", workspaceId)

            assertEquals(listOf("custom-attachment"), database.workspaceConnectionDao().getLive(workspaceId).map { it.attachmentId })
            assertNull(database.contextDao().getContextById(workspaceId))
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
                    roleCode = null,
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
