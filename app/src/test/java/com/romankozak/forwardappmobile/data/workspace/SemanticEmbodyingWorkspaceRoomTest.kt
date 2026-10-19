package com.romankozak.forwardappmobile.data.workspace

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.orientation.SemanticDeletionRoomFixture
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.semanticeditor.SemanticEntityEditorUiState
import com.romankozak.forwardappmobile.features.semanticeditor.SemanticEntityEditorViewModel
import com.romankozak.forwardappmobile.features.semanticeditor.canCreateEmbodyingWorkspace
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SemanticEmbodyingWorkspaceRoomTest {
    @Before fun setup() { Dispatchers.setMain(Dispatchers.Unconfined) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun `Orientation and Aspect create distinct canonical Workspace identity plus primary EMBODIES only`() = runBlocking<Unit> {
        for (type in ManagedSubjectType.entries) SemanticDeletionRoomFixture().use { f ->
            val id = if (type == ManagedSubjectType.ORIENTATION) f.orientation() else f.aspect()
            val before = f.rows()
            val workspaceId = command(f).create(request(id, type), now = 30)
            val workspace = f.db.workspaceDao().getById(workspaceId)!!
            assertThat(workspace.id).isNotEqualTo(id)
            assertThat(workspace.nameOverride).isEqualTo("Operational name")
            assertThat(workspace.descriptionOverride).isEqualTo("**Markdown**")
            assertThat(workspace.sourceContextId).isNull()
            assertThat(workspace.provenance).isEqualTo("STANDALONE")
            assertThat(workspace.version).isEqualTo(1)
            assertThat(workspace.syncedAt).isNull()
            val binding = f.dao.getAllWorkspaceBindings().single()
            assertThat(binding.subjectId).isEqualTo(id)
            assertThat(binding.workspaceId).isEqualTo(workspaceId)
            assertThat(binding.bindingType).isEqualTo("EMBODIES")
            assertThat(binding.isPrimary).isTrue()
            assertThat(binding.version).isEqualTo(1)
            assertThat(binding.syncedAt).isNull()
            assertThat(f.rows().filterKeys { it !in setOf("workspaces", "workspace_bindings") })
                .isEqualTo(before.filterKeys { it !in setOf("workspaces", "workspace_bindings") })
            val catalog = CanonicalEntityCatalogRepository(f.db.canonicalEntityCatalogDao()).observeCatalog().first()
            assertThat(catalog.single { it.ref == CanonicalEntityRef.Workspace(workspaceId) }.title).isEqualTo("Operational name")
        }
    }

    @Test fun `existing EMBODIES and replay reject without new Workspace or displaced binding`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation()
            val command = command(f)
            command.create(request(id), now = 30)
            val before = f.rows()
            assertThat(runCatching { command.create(request(id), now = 40) }.exceptionOrNull()).hasMessageThat().contains("уже має")
            assertThat(f.rows()).isEqualTo(before)
        }
    }

    @Test fun `stale deleted wrong-class missing subject and blank name fail without partial writes`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation()
            val before = f.rows()
            for (invalid in listOf(request(id).copy(expectedVersion = 0), request(id).copy(subjectType = ManagedSubjectType.ASPECT),
                request("missing"), request(id).copy(name = "  "))) {
                assertThat(runCatching { command(f).create(invalid, now = 30) }.isFailure).isTrue()
                assertThat(f.rows()).isEqualTo(before)
            }
            f.dao.upsertManagedSubjects(listOf(f.dao.getManagedSubject(id)!!.copy(isDeleted = true)))
            val deleted = f.rows()
            assertThat(runCatching { command(f).create(request(id), now = 40) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(deleted)
        }
    }

    @Test fun `binding failure after Workspace insertion rolls back the whole command`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.aspect()
            f.db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_embodies BEFORE INSERT ON workspace_bindings BEGIN SELECT RAISE(ABORT, 'binding failure'); END")
            val before = f.rows()
            assertThat(runCatching { command(f).create(request(id, ManagedSubjectType.ASPECT), now = 30) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
        }
    }

    @Test fun `concurrent creation commits exactly one Workspace and binding`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation()
            val command = command(f)
            val results = List(2) { async(Dispatchers.Default) { runCatching { command.create(request(id), now = 30) } } }.awaitAll()
            assertThat(results.count { it.isSuccess }).isEqualTo(1)
            assertThat(f.db.workspaceDao().getAll()).hasSize(1)
            assertThat(f.dao.getAllWorkspaceBindings()).hasSize(1)
            assertThat(f.db.hierarchyPlacementDao().getAll()).isEmpty()
        }
    }

    @Test fun `returning to editor refreshes created binding without saving or losing semantic name draft`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation()
            val store = ViewModelStore()
            val editor = SemanticEntityEditorViewModel(SavedStateHandle(mapOf("subjectId" to id)), f.orientations,
                f.aspects, f.links, f.bindings, f.graph, CanonicalEntityCatalogRepository(f.db.canonicalEntityCatalogDao()))
            store.put("editor", editor)
            val job = editor.viewModelScope.coroutineContext[Job]!!
            try {
                withTimeout(10_000) { editor.uiState.first { it is SemanticEntityEditorUiState.Loaded } }
                editor.setName("Unsaved draft")
                val workspace = command(f).create(request(id), now = 30)
                editor.refreshWorkspaceBindings()
                val state = withTimeout(10_000) { editor.uiState.first { it is SemanticEntityEditorUiState.Loaded &&
                    it.model.workspaceBindings.any { binding -> binding.workspaceId == workspace } } } as SemanticEntityEditorUiState.Loaded
                assertThat(state.model.name).isEqualTo("Unsaved draft")
                assertThat(state.model.persistedName).isEqualTo("Meaning")
                assertThat(state.model.isDirty).isTrue()
                assertThat(f.dao.getManagedSubject(id)!!.title).isEqualTo("Meaning")
                assertThat(state.model.canCreateEmbodyingWorkspace).isFalse()
                // Return after unbinding in Workspace Properties: refresh only this relation family.
                f.bindings.unbind(state.model.workspaceBindings.single().bindingId, now = 40)
                editor.refreshWorkspaceBindings()
                val unbound = withTimeout(10_000) { editor.uiState.first { it is SemanticEntityEditorUiState.Loaded &&
                    it.model.workspaceBindings.isEmpty() } } as SemanticEntityEditorUiState.Loaded
                assertThat(unbound.model.canCreateEmbodyingWorkspace).isTrue()
                assertThat(unbound.model.name).isEqualTo("Unsaved draft")
            } finally { store.clear(); job.join() }
        }
    }

    @Test fun `new identity and EMBODIES roundtrip canonical JSON Restore and Merge without H1`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val subject = f.aspect()
            val id = command(f).create(request(subject, ManagedSubjectType.ASPECT), now = 30)
            val bundle = Gson().fromJson(Gson().toJson(f.snapshot()), SnapshotBundle::class.java)
            for (merge in listOf(false, true)) SemanticDeletionRoomFixture().use { target ->
                target.admit(bundle, merge)
                assertThat(target.db.workspaceDao().getById(id)!!.isDeleted).isFalse()
                assertThat(target.dao.getAllWorkspaceBindings().single().workspaceId).isEqualTo(id)
                assertThat(target.dao.getManagedSubject(subject)!!.isDeleted).isFalse()
                assertThat(target.db.hierarchyPlacementDao().getAll()).isEmpty()
                assertThat(target.db.contextDao().getAllRaw()).isEmpty()
            }
        }
    }

    private fun command(f: SemanticDeletionRoomFixture) = SemanticEmbodyingWorkspaceCoordinator(f.db, workspaceDeletionRepository(f.db), f.bindings)
    private fun request(id: String, type: ManagedSubjectType = ManagedSubjectType.ORIENTATION) =
        EmbodyingWorkspaceRequest(id, type, 1, " Operational name ", " **Markdown** ")
}
