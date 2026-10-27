package com.romankozak.forwardappmobile.data.workspace.capability

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkspaceCoreViewsRoomTest {
    @Test fun `new live Workspace has both core views without capability persistence writes`() = runBlocking {
        withDatabase { db ->
            db.workspaceDao().upsert(listOf(workspace()))
            val store = store(db)
            assertTrue(CanonicalDashboardCapabilityRepository(store).isEnabled("w"))
            assertTrue(CanonicalRelationsCapabilityRepository(store).isEnabled("w"))
            assertTrue(CanonicalRelationsCapabilityRepository(store).observeEnabled("w").first())
            assertTrue(db.orientationDao().getAllWorkspaceCapabilities().isEmpty())
        }
    }

    @Test fun `disabled archived or tombstoned metadata does not hide core views or get silently rewritten`() = runBlocking {
        withDatabase { db ->
            db.workspaceDao().upsert(listOf(workspace()))
            val dashboard = CanonicalDashboardCapabilityRepository(store(db))
            val semantics = CanonicalRelationsCapabilityRepository(store(db))
            dashboard.enable("w", 10)
            semantics.setEnabled("w", true, 10)
            dashboard.disable("w", 20)
            semantics.setEnabled("w", false, 20)
            val before = db.orientationDao().getAllWorkspaceCapabilities()
            assertTrue(dashboard.isEnabled("w"))
            assertTrue(semantics.isEnabled("w"))
            assertEquals(before, db.orientationDao().getAllWorkspaceCapabilities())
            dashboard.archive("w", 30)
            semantics.delete("w", 30)
            assertTrue(dashboard.isEnabled("w"))
            assertTrue(semantics.observeEnabled("w").first())
        }
    }

    @Test fun `missing or deleted Workspace never exposes live core views`() = runBlocking {
        withDatabase { db ->
            val dashboard = CanonicalDashboardCapabilityRepository(store(db))
            val semantics = CanonicalRelationsCapabilityRepository(store(db))
            assertFalse(dashboard.isEnabled("w"))
            assertFalse(semantics.observeEnabled("w").first())
            db.workspaceDao().upsert(listOf(workspace()))
            assertTrue(semantics.observeEnabled("w").first())
            db.workspaceDao().upsert(listOf(workspace().copy(isDeleted = true)))
            assertFalse(dashboard.isEnabled("w"))
            assertFalse(semantics.observeEnabled("w").first())
        }
    }

    @Test fun `Context provenance does not affect core view availability`() = runBlocking {
        withDatabase { db ->
            db.workspaceDao().upsert(listOf(workspace().copy(
                provenance = WorkspaceProvenance.CONTEXT_BACKED.name, sourceContextId = "historical")))
            assertTrue(CanonicalDashboardCapabilityRepository(store(db)).isEnabled("w"))
            assertTrue(CanonicalRelationsCapabilityRepository(store(db)).isEnabled("w"))
            assertTrue(db.orientationDao().getAllWorkspaceCapabilities().isEmpty())
        }
    }

    private fun workspace() = WorkspaceEntity(id = "w", nameOverride = "Workspace", descriptionOverride = null,
        roleCode = null, createdAt = 1, updatedAt = 1, syncedAt = null, isDeleted = false, version = 1,
        provenance = WorkspaceProvenance.CANONICAL_ONLY.name, sourceContextId = null)
    private fun store(db: AppDatabase) = CanonicalCapabilityInstanceStore(db, db.workspaceDao(), db.orientationDao())
    private suspend fun withDatabase(block: suspend (AppDatabase) -> Unit) {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
        try { block(db) } finally { db.close() }
    }
}
