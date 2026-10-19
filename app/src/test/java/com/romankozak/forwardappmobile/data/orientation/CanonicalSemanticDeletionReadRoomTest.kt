package com.romankozak.forwardappmobile.data.orientation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.GlobalSearchResultItem
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementPresenceRepository
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2HierarchyReadSnapshotAssembler
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalV2ProductionHierarchyReadAdapter
import com.romankozak.forwardappmobile.data.product.CanonicalProductReadRoomSource
import com.romankozak.forwardappmobile.data.product.CanonicalProductPersistenceFrame
import com.romankozak.forwardappmobile.data.repository.SearchRepository
import com.romankozak.forwardappmobile.features.entitylibrary.*
import com.romankozak.forwardappmobile.features.semanticeditor.SemanticEntityEditorUiState
import com.romankozak.forwardappmobile.features.semanticeditor.SemanticEntityEditorViewModel
import com.romankozak.forwardappmobile.shared.core.domain.product.projectCanonicalProductRead
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CanonicalSemanticDeletionReadRoomTest {
    @Test fun `live Library and Unplaced omit deleted Orientation and Aspect and old editor routes are unavailable`() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val store = ViewModelStore()
        val jobs = mutableListOf<Job>()
        try {
            SemanticDeletionRoomFixture().use { f ->
                try {
                    val orientation = f.orientation()
                    val aspect = f.aspect()
                    val catalog = CanonicalEntityCatalogRepository(f.db.canonicalEntityCatalogDao())
                    val library = EntityLibraryViewModel(catalog, CanonicalHierarchyPlacementPresenceRepository(f.db.hierarchyPlacementDao()),
                        SavedStateHandle(), Dispatchers.Unconfined).also { store.put("library", it); jobs += it.viewModelScope.coroutineContext[Job]!! }
                    suspend fun ready(predicate: (EntityLibraryUiState.Ready) -> Boolean) = withTimeout(10_000) {
                        library.uiState.first { it is EntityLibraryUiState.Ready && predicate(it) }
                    }
                    ready { it.items.size == 2 }
                    f.delete(orientation)
                    f.delete(aspect, ManagedSubjectType.ASPECT)
                    ready { it.items.isEmpty() }
                    library.setFilter(EntityLibraryFilter.UNPLACED)
                    ready { it.filter == EntityLibraryFilter.UNPLACED && it.items.isEmpty() }
                    for (id in listOf(orientation, aspect)) {
                        val editor = SemanticEntityEditorViewModel(SavedStateHandle(mapOf("subjectId" to id)), f.orientations,
                            f.aspects, f.links, f.bindings, f.graph, catalog)
                            .also { store.put(id, it); jobs += it.viewModelScope.coroutineContext[Job]!! }
                        withTimeout(10_000) { editor.uiState.first { it is SemanticEntityEditorUiState.Unavailable } }
                    }
                } finally {
                    store.clear()
                    jobs.forEach { it.join() }
                }
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test fun `Global Search canonical product path excludes deleted Orientation without a manual index refresh`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation()
            val attachments = mockk<com.romankozak.forwardappmobile.sync.AttachmentsRepository>()
            every { attachments.getAttachmentLibraryItems() } returns flowOf(emptyList())
            val projector = mockk<com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector>()
            coEvery { projector.projectPresentationUniverse(any()) } returns emptyList()
            val search = SearchRepository(f.db.contextDao(), mockk(relaxed = true), mockk(relaxed = true),
                mockk(relaxed = true), attachments, projector, f.db.workspaceDao(), f.db,
                CanonicalV2HierarchyReadSnapshotAssembler(), mockk()).also { repository ->
                repository.canonicalV2SearchReadProvider = {
                    CanonicalV2ProductionHierarchyReadAdapter().read(emptyList(), emptyList(), emptyList())
                }
                repository.canonicalProductSearchReadProvider = {
                    val frame = CanonicalProductReadRoomSource(f.db).observe().first()
                    check(frame is CanonicalProductPersistenceFrame.Ready) { "Product read integrity failure: $frame" }
                    projectCanonicalProductRead(frame.snapshot)
                }
            }
            assertThat(search.searchGlobal("Meaning").filterIsInstance<GlobalSearchResultItem.OrientationItem>().map { it.orientationId })
                .contains(id)
            f.delete(id)
            assertThat(search.searchGlobal("Meaning").filterIsInstance<GlobalSearchResultItem.OrientationItem>()).isEmpty()
        }
    }
}
