package com.romankozak.forwardappmobile.features.workspace

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.context.SystemContexts
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralMutationCoordinator
import com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralParent
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.workspace.WorkspaceCompatibilityProjectionRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WorkspaceSurfaceAuthorityRoomTest {
    private lateinit var db: AppDatabase
    private val store = ViewModelStore()
    @Before fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries().build()
    }
    @After fun cleanup() { store.clear(); db.close(); Dispatchers.resetMain() }

    @Test fun `opening Context-backed standalone and System destinations preserves every persisted authority`() = runBlocking<Unit> {
        db.contextDao().insert(Context(id = "backed", name = "Legacy title", description = null,
            parentId = null, createdAt = 1, updatedAt = 1))
        val ids = listOf("backed", "standalone", SystemContexts.INBOX.raw)
        db.workspaceDao().upsert(ids.map { id -> WorkspaceEntity(
            id = id, nameOverride = "Canonical $id", descriptionOverride = "Canonical description",
            roleCode = null, createdAt = 1, updatedAt = 1, syncedAt = null, isDeleted = false, version = 1,
            provenance = when (id) { "backed" -> "CONTEXT_BACKED"; "standalone" -> "STANDALONE"; else -> "CANONICAL_ONLY" },
            sourceContextId = id.takeIf { it == "backed" },
        ) })
        CanonicalOrientationRepository(db, db.orientationDao()).createOrientation(OrientationKind.INTENT, "Direction", now = 1)
        H1StructuralMutationCoordinator(db).createContainer("Existing section", H1StructuralParent.Root)
        val before = persistedRows()
        val repository = WorkspaceCompatibilityProjectionRepository(db.workspaceDao(), db.orientationDao(), db.contextDao())

        ids.forEach { id ->
            val vm = WorkspaceDestinationViewModel(SavedStateHandle(mapOf("workspaceId" to id, "listId" to "wrong")), repository)
                .also { store.put(id, it) }
            val ready = withTimeout(10_000) { vm.uiState.first { it !is WorkspaceDestinationUiState.Loading } }
                as WorkspaceDestinationUiState.Ready
            assertThat(ready.workspaceId).isEqualTo(id)
            assertThat(ready.title).isEqualTo("Canonical $id")
            assertThat(ready.toContextPresentation().id).isEqualTo(id)
            assertThat(ready.compatibilityContextId).isEqualTo(id.takeIf { it == "backed" })
        }
        assertThat(persistedRows()).isEqualTo(before)
        assertThat(db.contextDao().getContextById("standalone")).isNull()
        assertThat(db.contextDao().getContextById(SystemContexts.INBOX.raw)).isNull()
    }

    private fun persistedRows(): Map<String, List<List<String?>>> {
        val sql = db.openHelper.writableDatabase
        val names = sql.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' " +
            "AND name NOT IN ('android_metadata', 'room_master_table')").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
        }
        return names.associateWith { name ->
            sql.query("SELECT * FROM \"$name\"").use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add((0 until cursor.columnCount).map { column ->
                        if (cursor.isNull(column)) null else cursor.getString(column)
                    })
                }.sortedBy { it.toString() }
            }
        }
    }
}
