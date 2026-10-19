package com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.hierarchy.BigPictureStructureRepository
import com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralParent
import com.romankozak.forwardappmobile.data.hierarchy.container.H1TypedNode
import com.romankozak.forwardappmobile.data.orientation.SemanticDeletionRoomFixture
import com.romankozak.forwardappmobile.data.workspace.workspaceDeletionRepository
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.*
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BigPictureOperationalEmbodimentsRoomTest {
    @Test fun `binding add and removal reactively decorate focused occurrence without any read side writes`() = runBlocking {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val store = ViewModelStore()
        try {
            SemanticDeletionRoomFixture().use { f ->
                f.workspace()
                val id = f.orientation()
                val section = f.h1.createContainer("Section", H1StructuralParent.Root, now = 15)
                val placement = f.appearance(id, H1StructuralParent.Node(H1TypedNode.Container(section)))
                val catalog = CanonicalEntityCatalogRepository(f.db.canonicalEntityCatalogDao())
                val saved = SavedStateHandle(mapOf("bigPictureFocus" to placement.value))
                val vm = BigPictureViewModel(BigPictureStructureRepository(f.db), catalog, saved, Dispatchers.Default)
                store.put("read", vm)
                val subscription = launch { vm.uiState.collect {} }
                try {
                    suspend fun ready(adaptive: Boolean): BigPictureUiState.Ready = withTimeout(10_000) {
                        vm.uiState.first { it is BigPictureUiState.Ready && it.activeNode != null &&
                            (it.activeNode.operationalEmbodiment != null) == adaptive } as BigPictureUiState.Ready
                    }
                    val before = f.rows()
                    val initial = ready(false)
                    assertThat(bigPictureDestination(initial.activeNode!!)).isEqualTo(NavTarget.SemanticEntityEditor(id))
                    assertThat(f.rows()).isEqualTo(before)
                    val binding = f.bindings.bind(id, "workspace", WorkspaceBindingType.EMBODIES, 30)
                    val afterBinding = f.rows()
                    val adaptive = ready(true)
                    assertThat(bigPictureDestination(adaptive.activeNode!!)).isEqualTo(NavTarget.Workspace("workspace"))
                    assertThat(bigPictureSemanticDestination(adaptive.activeNode)).isEqualTo(NavTarget.SemanticEntityEditor(id))
                    assertThat(adaptive.activeNode.placement).isEqualTo(initial.activeNode.placement)
                    assertThat(adaptive.navigationBreadcrumbs).isEqualTo(initial.navigationBreadcrumbs)
                    assertThat(adaptive.structure!!.children(initial.structuralDestination()!!)).isEqualTo(initial.structure!!.children(initial.structuralDestination()!!))
                    assertThat(f.rows()).isEqualTo(afterBinding)
                    f.bindings.unbind(binding, 40)
                    val afterUnbind = f.rows()
                    assertThat(ready(false).activeNode).isEqualTo(initial.activeNode)
                    assertThat(f.rows()).isEqualTo(afterUnbind)
                } finally { subscription.cancelAndJoin(); store.clear() }
            }
        } finally { store.clear(); Dispatchers.resetMain() }
    }

    @Test fun `deleting embodied Workspace falls back reactively without retiring semantic appearance`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val id = f.aspect()
            val placement = f.appearance(id)
            f.bindings.bind(id, "workspace", WorkspaceBindingType.EMBODIES, 30)
            val reader = BigPictureStructureRepository(f.db)
            val catalog = CanonicalEntityCatalogRepository(f.db.canonicalEntityCatalogDao())
            val nodes = combine(reader.observe(), catalog.observeCatalog()) { structure, entities ->
                withOperationalEmbodiments(projectBigPicture(structure.placements, entities), structure.embodimentBindings, entities)
            }
            assertThat(withTimeout(10_000) { nodes.first { it.single().operationalEmbodiment != null } }.single().placement.id).isEqualTo(placement)
            val started = CompletableDeferred<Unit>()
            val fallback = async(Dispatchers.Default) {
                withTimeout(10_000) { nodes.onEach { started.complete(Unit) }.first { it.single().operationalEmbodiment == null } }
            }
            started.await()
            val workspaces = workspaceDeletionRepository(f.db)
            workspaces.tombstone("workspace")
            val node = fallback.await().single()
            assertThat(node.placement.id).isEqualTo(placement)
            assertThat(bigPictureDestination(node)).isEqualTo(NavTarget.SemanticEntityEditor(id))
            assertThat(f.dao.getManagedSubject(id)!!.isDeleted).isFalse()
        }
    }

    @Test fun `adaptive LINK remove and source delete retain distinct canonical lifecycle owners`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val id = f.orientation()
            val primary = f.appearance(id)
            val link = f.appearance(id)
            f.bindings.bind(id, "workspace", WorkspaceBindingType.EMBODIES, 30)
            f.h1.removeOccurrence(link, now = 40)
            assertThat(f.dao.getManagedSubject(id)!!.isDeleted).isFalse()
            assertThat(f.db.hierarchyPlacementDao().getAll().single { it.id == primary.value }.isDeleted).isFalse()
            val catalog = CanonicalEntityCatalogRepository(f.db.canonicalEntityCatalogDao()).observeCatalog().first()
            val frame = BigPictureStructureRepository(f.db).observe().first()
            val node = withOperationalEmbodiments(projectBigPicture(frame.placements, catalog), frame.embodimentBindings, catalog).single()
            val actions = BigPictureTargetActions(f.db, f.command, workspaceDeletionRepository(f.db))
            val target = sourceDeleteTarget(node, actions.observeTargets().first())!!
            assertThat(target.ref).isEqualTo(CanonicalEntityRef.Orientation(id))
            actions.deleteSource(target)
            assertThat(f.dao.getManagedSubject(id)!!.isDeleted).isTrue()
            assertThat(f.db.workspaceDao().getById("workspace")!!.isDeleted).isFalse()
            assertThat(f.db.hierarchyPlacementDao().getAll().all { it.isDeleted }).isTrue()
        }
    }
}
