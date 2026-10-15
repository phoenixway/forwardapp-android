package com.romankozak.forwardappmobile.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.LegacyNoteEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceCapabilityInstanceEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.workspace.capability.BacklogCanonicalTargetResolver
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogRepository
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalBacklogTargetValidator
import com.romankozak.forwardappmobile.data.workspace.capability.CanonicalCapabilityInstanceStore
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetKind
import com.romankozak.forwardappmobile.shared.core.models.workspace.WorkspaceBacklogTargetRef
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * FK.10B.1 canonical Note lifecycle acceptance on the actual Room 198 model.
 *
 * Note owns content and lifecycle. BACKLOG owns independent occurrences.
 * Historical contextId is neither mandatory nor Workspace ownership.
 */
@RunWith(RobolectricTestRunner::class)
class LegacyNoteCanonicalRuntimeRoomAcceptanceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `standalone create and edit work without Context or placements`() = runBlocking {
        val f = fixture()
        try {
            val note = note("standalone")
            f.notes.saveNote(note)

            val created = requireNotNull(f.db.legacyNoteDao().getNoteById(note.id))
            assertNull(created.contextId)
            assertEquals(note.title, created.title)
            assertFalse(created.isDeleted)
            assertTrue(f.backlog.findLivePlacements(target(note.id)).isEmpty())

            f.notes.saveNote(created.copy(title = "Edited", content = "New content"))

            val edited = requireNotNull(f.db.legacyNoteDao().getNoteById(note.id))
            assertEquals("Edited", edited.title)
            assertEquals("New content", edited.content)
            assertEquals(created.version + 1L, edited.version)
            assertNull(edited.contextId)
            assertTrue(f.backlog.findLivePlacements(target(note.id)).isEmpty())
        } finally {
            f.db.close()
        }
    }

    @Test
    fun `initial Workspace placement and Note creation succeed together`() = runBlocking {
        val f = fixture()
        try {
            seedWorkspace(f.db, "workspace-a", active = true)
            val note = note("placed")

            f.notes.saveNote(note, initialWorkspaceId = "workspace-a")

            val stored = requireNotNull(f.db.legacyNoteDao().getNoteById(note.id))
            assertNull(stored.contextId)

            val placements = f.backlog.findLivePlacements(target(note.id))
            assertEquals(1, placements.size)
            assertEquals("workspace-a", placements.single().workspaceId)
            assertEquals(note.id, placements.single().targetId)
            assertEquals(WorkspaceBacklogTargetKind.LEGACY_NOTE.name, placements.single().targetKind)
        } finally {
            f.db.close()
        }
    }

    @Test
    fun `failed initial placement rolls back new Note`() = runBlocking {
        val f = fixture()
        try {
            seedWorkspace(f.db, "workspace-disabled", active = false)
            val note = note("rollback-create")

            val result = runCatching {
                f.notes.saveNote(note, initialWorkspaceId = "workspace-disabled")
            }

            assertTrue(result.isFailure)
            assertNull(f.db.legacyNoteDao().getNoteById(note.id))
            assertTrue(f.backlog.findLivePlacements(target(note.id)).isEmpty())
        } finally {
            f.db.close()
        }
    }

    @Test
    fun `multiple placements and moving one preserve Note content`() = runBlocking {
        val f = fixture()
        try {
            seedWorkspace(f.db, "workspace-a", active = true)
            seedWorkspace(f.db, "workspace-b", active = true)
            seedWorkspace(f.db, "workspace-c", active = true)

            val note = note("multi")
            f.notes.saveNote(note, initialWorkspaceId = "workspace-a")

            val initial = requireNotNull(f.db.legacyNoteDao().getNoteById(note.id))
            val placementA = f.backlog.findLivePlacements(target(note.id)).single().id

            val placementB = f.commands.addNoteAtStart("workspace-b", note.id)
            assertEquals(
                setOf("workspace-a", "workspace-b"),
                f.backlog.findLivePlacements(target(note.id)).map { it.workspaceId }.toSet(),
            )

            assertEquals(1, f.backlog.moveEntries(listOf(placementA), "workspace-c"))

            val placements = f.backlog.findLivePlacements(target(note.id))
            assertEquals(2, placements.size)
            assertEquals(
                setOf("workspace-b", "workspace-c"),
                placements.map { it.workspaceId }.toSet(),
            )
            assertEquals("workspace-b", requireNotNull(f.backlog.getEntry(placementB)).workspaceId)

            val after = requireNotNull(f.db.legacyNoteDao().getNoteById(note.id))
            assertEquals(initial, after)
            assertNull(after.contextId)
        } finally {
            f.db.close()
        }
    }

    @Test
    fun `removing the final placement leaves a live standalone Note`() = runBlocking {
        val f = fixture()
        try {
            seedWorkspace(f.db, "workspace-a", active = true)
            val note = note("last-placement")
            f.notes.saveNote(note, initialWorkspaceId = "workspace-a")

            val before = requireNotNull(f.db.legacyNoteDao().getNoteById(note.id))
            val placement = f.backlog.findLivePlacements(target(note.id)).single()

            f.backlog.tombstoneEntry(placement.id)

            assertTrue(f.backlog.findLivePlacements(target(note.id)).isEmpty())
            assertTrue(requireNotNull(f.backlog.getEntry(placement.id)).isDeleted)
            assertEquals(before, f.db.legacyNoteDao().getNoteById(note.id))
            assertFalse(requireNotNull(f.db.legacyNoteDao().getNoteById(note.id)).isDeleted)
        } finally {
            f.db.close()
        }
    }

    @Test
    fun `deleting Note tombstones all placements even in disabled Backlog`() = runBlocking {
        val f = fixture()
        try {
            seedWorkspace(f.db, "workspace-a", active = true)
            seedWorkspace(f.db, "workspace-b", active = true)

            val note = note("delete-everywhere")
            f.notes.saveNote(note, initialWorkspaceId = "workspace-a")
            f.commands.addNoteAtStart("workspace-b", note.id)

            val before = requireNotNull(f.db.legacyNoteDao().getNoteById(note.id))
            val placementIds = f.backlog.findLivePlacements(target(note.id)).map { it.id }
            assertEquals(2, placementIds.size)

            f.backlog.disable("workspace-b")
            f.notes.deleteNote(note.id)

            val deleted = requireNotNull(f.db.legacyNoteDao().getNoteById(note.id))
            assertTrue(deleted.isDeleted)
            assertEquals(before.version + 1L, deleted.version)
            assertEquals(before.id, deleted.id)
            assertNull(deleted.contextId)
            assertTrue(f.backlog.findLivePlacements(target(note.id)).isEmpty())
            placementIds.forEach { id ->
                assertTrue(requireNotNull(f.backlog.getEntry(id)).isDeleted)
            }

            f.notes.deleteNote(note.id)
            assertEquals(deleted, f.db.legacyNoteDao().getNoteById(note.id))
        } finally {
            f.db.close()
        }
    }

    @Test
    fun `stale edits fail and historical locator cannot be overwritten by edit`() = runBlocking {
        val f = fixture()
        try {
            val historical = note("historical").copy(contextId = "missing-context")
            f.notes.saveNote(historical)

            val original = requireNotNull(f.db.legacyNoteDao().getNoteById(historical.id))
            f.notes.saveNote(
                original.copy(
                    title = "Current content",
                    contextId = "different-context",
                ),
            )

            val current = requireNotNull(f.db.legacyNoteDao().getNoteById(historical.id))
            assertEquals("missing-context", current.contextId)
            assertEquals("Current content", current.title)
            assertEquals(original.version + 1L, current.version)

            val staleResult = runCatching {
                f.notes.saveNote(original.copy(title = "Stale content"))
            }

            assertTrue(staleResult.isFailure)
            assertEquals(current, f.db.legacyNoteDao().getNoteById(historical.id))
            assertTrue(f.backlog.findLivePlacements(target(historical.id)).isEmpty())
        } finally {
            f.db.close()
        }
    }

    @Test
    fun `failed placement cleanup rolls back Note tombstone`() = runBlocking {
        val f = fixture()
        try {
            val note = note("rollback-delete")
            f.notes.saveNote(note)
            val before = requireNotNull(f.db.legacyNoteDao().getNoteById(note.id))

            val failingCanonical = mockk<CanonicalBacklogRepository>()
            coEvery {
                failingCanonical.tombstoneEntriesTargeting(any(), any())
            } throws IllegalStateException("Injected placement cleanup failure")

            val failingCommands = BacklogPlacementCommands(
                failingCanonical,
                BacklogCanonicalTargetResolver(
                    f.db.orientationDao(),
                    f.db.workspaceDao(),
                ),
            )
            val failingNotes = LegacyNoteRepository(
                f.db.legacyNoteDao(),
                f.db,
                mockk(relaxed = true),
                failingCommands,
            )

            val result = runCatching { failingNotes.deleteNote(note.id) }

            assertTrue(result.isFailure)
            assertEquals(before, f.db.legacyNoteDao().getNoteById(note.id))
            assertFalse(requireNotNull(f.db.legacyNoteDao().getNoteById(note.id)).isDeleted)
        } finally {
            f.db.close()
        }
    }

    @Test
    fun `historical System Context locator cannot veto Note content edits`() = runBlocking {
        val f = fixture()
        try {
            // Historical transport can legitimately carry this optional locator.
            val historical = note("historical-system").copy(contextId = SystemContexts.INBOX.raw)
            f.db.legacyNoteDao().insert(historical)

            // Incoming compatibility metadata is not authoritative either.
            f.notes.saveNote(
                historical.copy(
                    title = "Edited standalone content",
                    contextId = SystemContexts.TODAY.raw,
                ),
            )

            val edited = requireNotNull(f.db.legacyNoteDao().getNoteById(historical.id))
            assertEquals("Edited standalone content", edited.title)
            assertEquals(SystemContexts.INBOX.raw, edited.contextId)
            assertEquals(historical.version + 1L, edited.version)
            assertTrue(f.backlog.findLivePlacements(target(historical.id)).isEmpty())
        } finally {
            f.db.close()
        }
    }

    @Test
    fun `Workspace Note presentation follows live placements rather than historical Context`() = runBlocking {
        val f = fixture()
        try {
            seedWorkspace(f.db, "workspace-a", active = true)
            seedWorkspace(f.db, "workspace-b", active = true)
            seedWorkspace(f.db, "workspace-c", active = true)

            val shared = note("shared-note").copy(contextId = "retired-context")
            f.notes.saveNote(shared, initialWorkspaceId = "workspace-a")
            f.commands.addNoteAtStart("workspace-b", shared.id)
            f.notes.saveNote(note("unplaced-note"))

            assertEquals(
                listOf(shared.id),
                f.notes.observePlacedNotesForWorkspace("workspace-a").first().map { it.id },
            )
            assertEquals(
                listOf(shared.id),
                f.notes.observePlacedNotesForWorkspace("workspace-b").first().map { it.id },
            )
            assertTrue(f.notes.observePlacedNotesForWorkspace("workspace-c").first().isEmpty())

            f.notes.deleteNote(shared.id)
            assertTrue(f.notes.observePlacedNotesForWorkspace("workspace-a").first().isEmpty())
            assertTrue(f.notes.observePlacedNotesForWorkspace("workspace-b").first().isEmpty())
            assertFalse(requireNotNull(f.db.legacyNoteDao().getNoteById("unplaced-note")).isDeleted)
        } finally {
            f.db.close()
        }
    }

    private data class Fixture(
        val db: AppDatabase,
        val backlog: CanonicalBacklogRepository,
        val commands: BacklogPlacementCommands,
        val notes: LegacyNoteRepository,
    )

    private fun fixture(): Fixture {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val canonical = CanonicalBacklogRepository(
            database = database,
            instanceStore = CanonicalCapabilityInstanceStore(
                database = database,
                workspaceDao = database.workspaceDao(),
                orientationDao = database.orientationDao(),
            ),
            entryDao = database.workspaceBacklogEntryDao(),
            targetValidator = CanonicalBacklogTargetValidator(database),
        )

        val resolver = BacklogCanonicalTargetResolver(
            database.orientationDao(),
            database.workspaceDao(),
        )
        val commands = BacklogPlacementCommands(canonical, resolver)
        val notes = LegacyNoteRepository(
            database.legacyNoteDao(),
            database,
            mockk(relaxed = true),
            commands,
        )

        return Fixture(database, canonical, commands, notes)
    }

    private suspend fun seedWorkspace(
        database: AppDatabase,
        id: String,
        active: Boolean,
    ) {
        database.workspaceDao().upsert(
            listOf(
                WorkspaceEntity(
                    id = id,
                    nameOverride = id,
                    descriptionOverride = null,
                    roleCode = null,
                    createdAt = 1L,
                    updatedAt = 1L,
                    syncedAt = null,
                    isDeleted = false,
                    version = 1L,
                    provenance = WorkspaceProvenance.CANONICAL_ONLY.name,
                    sourceContextId = null,
                ),
            ),
        )

        if (active) {
            database.orientationDao().upsertWorkspaceCapabilities(
                listOf(
                    WorkspaceCapabilityInstanceEntity(
                        id = "backlog-$id",
                        workspaceId = id,
                        capabilityType = "BACKLOG",
                        instanceKey = "default",
                        capabilityOrder = 0L,
                        state = "ACTIVE",
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
    }

    private fun note(id: String) =
        LegacyNoteEntity(
            id = id,
            contextId = null,
            title = "Title $id",
            content = "Content $id",
        )

    private fun target(noteId: String) =
        WorkspaceBacklogTargetRef(
            kind = WorkspaceBacklogTargetKind.LEGACY_NOTE,
            id = noteId,
        )
}
