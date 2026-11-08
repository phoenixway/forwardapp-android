package com.romankozak.forwardappmobile.data.workspace

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkspacePresentationPreferencesRepositoryRoomTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun `automatic view remembering skips missing and deleted owners without changing existing preferences`() = runBlocking {
        val db = database()
        try {
            val repository = WorkspacePresentationPreferencesRepository(db, db.workspaceDao(), db.workspacePresentationPreferencesDao())
            assertFalse(repository.rememberDefaultViewModeIfLive("missing", "DASHBOARD", 10))
            assertNull(db.workspacePresentationPreferencesDao().getRaw("missing"))
            val owner = WorkspaceEntity(id = "workspace", nameOverride = "Workspace", descriptionOverride = null,
                roleCode = null, createdAt = 1, updatedAt = 1, syncedAt = null, isDeleted = false,
                version = 1, provenance = WorkspaceProvenance.STANDALONE.name, sourceContextId = null)
            db.workspaceDao().upsert(listOf(owner))
            assertTrue(repository.rememberDefaultViewModeIfLive(owner.id, "BACKLOG", 10))
            val before = db.workspacePresentationPreferencesDao().getRaw(owner.id)
            db.workspaceDao().upsert(listOf(owner.copy(isDeleted = true, version = 2)))
            assertFalse(repository.rememberDefaultViewModeIfLive(owner.id, "DASHBOARD", 20))
            assertEquals(before, db.workspacePresentationPreferencesDao().getRaw(owner.id))
            assertTrue(db.workspaceDao().getById(owner.id)!!.isDeleted)
            assertTrue(runCatching { repository.setDefaultViewMode(owner.id, "DASHBOARD", 30) }.isFailure)
            assertEquals(before, db.workspacePresentationPreferencesDao().getRaw(owner.id))
            assertNull(db.contextDao().getContextById(owner.id))
        } finally { db.close() }
    }

    @Test fun `automatic view remembering on live owner preserves normal versioned preference behavior`() = runBlocking {
        val db = database()
        try {
            val repository = WorkspacePresentationPreferencesRepository(db, db.workspaceDao(), db.workspacePresentationPreferencesDao())
            db.workspaceDao().upsert(listOf(WorkspaceEntity(id = "live", nameOverride = "Live", descriptionOverride = null,
                roleCode = null, createdAt = 1, updatedAt = 1, syncedAt = null, isDeleted = false,
                version = 1, provenance = WorkspaceProvenance.CANONICAL_ONLY.name, sourceContextId = null)))
            assertTrue(repository.rememberDefaultViewModeIfLive("live", "DASHBOARD", 10))
            assertTrue(repository.rememberDefaultViewModeIfLive("live", "BACKLOG", 20))
            val row = db.workspacePresentationPreferencesDao().getRaw("live")!!
            assertEquals("BACKLOG", row.defaultViewModeName)
            assertEquals(2L, row.version)
            assertNull(row.syncedAt)
            assertNull(db.contextDao().getContextById("live"))
        } finally { db.close() }
    }

    @Test
    fun `shell-free Workspace owns presentation preferences without creating Context`() = runBlocking {
        val database = database()
        try {
            val workspaceId = "shell-free"
            database.workspaceDao().upsert(
                listOf(
                    WorkspaceEntity(
                        id = workspaceId,
                        nameOverride = "Shell free",
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

            assertNull(database.contextDao().getContextById(workspaceId))

            val repository =
                WorkspacePresentationPreferencesRepository(
                    database = database,
                    workspaceDao = database.workspaceDao(),
                    dao = database.workspacePresentationPreferencesDao(),
                )

            assertNull(repository.getCanonicalOrNull(workspaceId))
            assertEquals(null, repository.observeCanonicalOrNull(workspaceId).first())

            repository.setDefaultViewMode(
                workspaceId = workspaceId,
                defaultViewModeName = "BACKLOG",
                now = 10L,
            )
            assertTrue(repository.toggleAttachmentsExpanded(workspaceId, now = 20L))
            repository.setShowCheckboxes(
                workspaceId = workspaceId,
                showCheckboxes = true,
                now = 30L,
            )

            assertEquals(
                WorkspacePresentationPreferences(
                    defaultViewModeName = "BACKLOG",
                    isAttachmentsExpanded = true,
                    showCheckboxes = true,
                ),
                repository.getCanonicalOrNull(workspaceId),
            )
            assertEquals(
                WorkspacePresentationPreferences(
                    defaultViewModeName = "BACKLOG",
                    isAttachmentsExpanded = true,
                    showCheckboxes = true,
                ),
                repository.observeCanonicalOrNull(workspaceId).first(),
            )

            assertNull(database.contextDao().getContextById(workspaceId))

            val raw =
                requireNotNull(
                    database.workspacePresentationPreferencesDao().getRaw(workspaceId),
                )
            assertEquals(10L, raw.createdAt)
            assertEquals(30L, raw.updatedAt)
            assertNull(raw.syncedAt)
            assertEquals(3L, raw.version)
        } finally {
            database.close()
        }
    }

    @Test
    fun `missing preference row resolves deterministic defaults without materializing storage`() = runBlocking {
        val database = database()
        try {
            val workspaceId = "defaults"
            database.workspaceDao().upsert(
                listOf(
                    WorkspaceEntity(
                        id = workspaceId,
                        nameOverride = "Defaults",
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

            val repository =
                WorkspacePresentationPreferencesRepository(
                    database = database,
                    workspaceDao = database.workspaceDao(),
                    dao = database.workspacePresentationPreferencesDao(),
                )

            assertNull(repository.getCanonicalOrNull(workspaceId))
            assertNull(database.workspacePresentationPreferencesDao().getRaw(workspaceId))
            assertNull(database.contextDao().getContextById(workspaceId))

            assertEquals(
                WorkspacePresentationPreferences(),
                repository.getCanonicalOrNull(workspaceId) ?: WorkspacePresentationPreferences(),
            )

            assertNull(database.workspacePresentationPreferencesDao().getRaw(workspaceId))
        } finally {
            database.close()
        }
    }

    private fun database(): AppDatabase =
        Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java,
        ).allowMainThreadQueries()
            .build()
}
