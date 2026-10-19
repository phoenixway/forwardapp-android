package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.hierarchy.*
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.data.orientation.SemanticDeletionRoomFixture
import com.romankozak.forwardappmobile.data.workspace.workspaceDeletionRepository
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.features.entitylibrary.*
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BigPictureSourceDeleteRoomTest {
    @Test fun `remove exact Workspace LINK preserves source and other appearance then source delete retires every appearance`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val actions = BigPictureTargetActions(f.db, f.command, workspaceDeletionRepository(f.db))
            val first = f.h1.addAppearance(HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "workspace"), H1StructuralParent.Root, now = 10)
            val second = f.h1.addAppearance(HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "workspace"), H1StructuralParent.Root, now = 11)
            f.h1.removeOccurrence(second, now = 20)
            assertThat(f.db.workspaceDao().getById("workspace")!!.isDeleted).isFalse()
            assertThat(f.db.hierarchyPlacementDao().getAll().single { it.id == first.value }.isDeleted).isFalse()
            f.h1.addAppearance(HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "workspace"), H1StructuralParent.Root, now = 30)
            val target = actions.observeTargets().first().getValue(CanonicalEntityRef.Workspace("workspace"))
            actions.deleteSource(target)
            assertThat(f.db.workspaceDao().getById("workspace")!!.isDeleted).isTrue()
            assertThat(f.db.hierarchyPlacementDao().getAll().all { it.isDeleted }).isTrue()
            assertThat(workspaceDeletionRepository(f.db).observeProperties("workspace").first()).isNull()
        }
    }

    @Test fun `Orientation and Aspect source actions from LINK and PRIMARY use canonical lifecycle only`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val orientation = f.orientation()
            val aspect = f.aspect()
            val survivor = f.orientation()
            f.bindings.bind(orientation, "workspace", WorkspaceBindingType.SUPPORTS, 20)
            f.appearance(orientation)
            val link = f.appearance(orientation)
            val primary = f.appearance(aspect)
            val actions = BigPictureTargetActions(f.db, f.command, workspaceDeletionRepository(f.db))
            val catalog = CanonicalEntityCatalogRepository(f.db.canonicalEntityCatalogDao())
            for ((placement, ref) in listOf(link to CanonicalEntityRef.Orientation(orientation), primary to CanonicalEntityRef.Aspect(aspect))) {
                val nodes = com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.projectBigPicture(
                    f.db.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() }, catalog.observeCatalog().first())
                val target = sourceDeleteTarget(nodes.single { it.placement.id == placement }, actions.observeTargets().first())!!
                actions.deleteSource(target)
            }
            assertThat(f.dao.getManagedSubject(orientation)!!.isDeleted).isTrue()
            assertThat(f.dao.getManagedSubject(aspect)!!.isDeleted).isTrue()
            assertThat(f.dao.getManagedSubject(survivor)!!.isDeleted).isFalse()
            assertThat(f.dao.getAllWorkspaceBindings().single().isDeleted).isTrue()
            assertThat(f.db.workspaceDao().getById("workspace")!!.isDeleted).isFalse()
            assertThat(f.db.hierarchyPlacementDao().getAll().all { it.isDeleted }).isTrue()
            assertThat(catalog.observeCatalog().first().map { it.ref }).doesNotContain(CanonicalEntityRef.Orientation(orientation))
        }
    }

    @Test fun `focused source deletion reactively recovers surviving container and persists valid breadcrumbs`() = runBlocking<Unit> {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val store = ViewModelStore()
        val jobs = mutableListOf<Job>()
        try {
            SemanticDeletionRoomFixture().use { f ->
                try {
                    val id = f.orientation()
                    val section = f.h1.createContainer("WIP", H1StructuralParent.Root, now = 15)
                    val occurrence = f.appearance(id, H1StructuralParent.Node(H1TypedNode.Container(section)))
                    val catalog = CanonicalEntityCatalogRepository(f.db.canonicalEntityCatalogDao())
                    val saved = SavedStateHandle()
                    val reader = BigPictureViewModel(BigPictureStructureRepository(f.db), catalog, saved, Dispatchers.Unconfined)
                        .also { store.put("read", it); jobs += it.viewModelScope.coroutineContext[Job]!! }
                    val library = EntityLibraryViewModel(catalog, CanonicalHierarchyPlacementPresenceRepository(f.db.hierarchyPlacementDao()),
                        SavedStateHandle(), Dispatchers.Unconfined).also { store.put("library", it); jobs += it.viewModelScope.coroutineContext[Job]!! }
                    suspend fun ready(predicate: (BigPictureUiState.Ready) -> Boolean): BigPictureUiState.Ready = withTimeout(10_000) {
                        reader.uiState.first { it is BigPictureUiState.Ready && predicate(it) } as BigPictureUiState.Ready
                    }
                    ready { it.entries.size == 1 }
                    reader.focusScope("presentation:$section")
                    ready { it.activeScope?.id == "presentation:$section" }
                    reader.focusPlacement(occurrence.value)
                    ready { it.activeNode?.placement?.id == occurrence }
                    reader.rememberCurrentLocation()
                    withTimeout(10_000) { library.uiState.first { it is EntityLibraryUiState.Ready && it.items.size == 1 } }
                    val actions = BigPictureTargetActions(f.db, f.command, workspaceDeletionRepository(f.db))
                    actions.deleteSource(actions.observeTargets().first().getValue(CanonicalEntityRef.Orientation(id)))
                    ready { it.structure?.items?.none { item -> item.ref == H1TypedNode.Occurrence(occurrence.value) } == true }
                    reader.recoverMissingFocus()
                    val recovered = ready { it.activeScope?.id == "presentation:$section" }
                    assertThat(saved.get<String>("bigPictureFocus")).isNull()
                    assertThat(recovered.navigationBreadcrumbs.map { it.name }).containsExactly("WIP")
                    withTimeout(10_000) { library.uiState.first { it is EntityLibraryUiState.Ready && it.items.isEmpty() } }
                    val restored = BigPictureViewModel(BigPictureStructureRepository(f.db), catalog, saved, Dispatchers.Unconfined)
                        .also { store.put("restored", it); jobs += it.viewModelScope.coroutineContext[Job]!! }
                    withTimeout(10_000) { restored.uiState.first { it is BigPictureUiState.Ready && it.activeScope?.id == "presentation:$section" } }
                } finally { store.clear(); jobs.forEach { it.join() } }
            }
        } finally { Dispatchers.resetMain() }
    }
}
