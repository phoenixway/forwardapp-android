package com.romankozak.forwardappmobile.features.entitylibrary

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationRelationEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.AspectOrientationRefEntity
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceBindingEntity
import com.romankozak.forwardappmobile.data.hierarchy.GroupScopeCoverageStore
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.PlacementId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EntityLibraryPlacementRoomTest {
    @Before fun setup() { Dispatchers.setMain(Dispatchers.Unconfined) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun `Library adds all three canonical classes as first PRIMARY occurrences`() = runBlocking<Unit> {
        LibraryPlacementRoomFixture().use { f ->
            val refs = f.seed()
            refs.forEach { f.add(it, H1StructuralParent.Root) }
            val rows = f.db.hierarchyPlacementDao().getAll()
            assertThat(rows.map { it.targetId }).containsExactlyElementsIn(refs.map { it.id })
            assertThat(rows.map { it.placementKind }).containsExactly("PRIMARY", "PRIMARY", "PRIMARY")
            assertThat(f.db.hierarchyPlacementGroupScopeDao().getAll()).isEmpty()
            assertThat(GroupScopeCoverageStore(f.db).snapshot().records.map { it.mode }).containsExactly("NATIVE", "NATIVE")
        }
    }

    @Test fun `Library supports exact Root occurrence generic and materialized historical container destinations`() = runBlocking<Unit> {
        LibraryPlacementRoomFixture().use { f ->
            val refs = f.seed()
            f.add(refs.first(), H1StructuralParent.Root)
            val parent = f.db.hierarchyPlacementDao().getAll().single().id
            val generic = f.writer.createContainer("Generic", H1StructuralParent.Root)
            val historical = f.historicalContainer()
            val parents = listOf(H1StructuralParent.Root, H1StructuralParent.Node(H1TypedNode.Occurrence(parent)),
                H1StructuralParent.Node(H1TypedNode.Container(generic)), H1StructuralParent.Node(H1TypedNode.Container(historical)))
            refs.forEach { ref -> parents.forEach { destination ->
                val before = f.db.hierarchyPlacementDao().getAll().mapTo(hashSetOf()) { it.id }
                f.add(ref, destination)
                val row = f.db.hierarchyPlacementDao().getAll().single { it.id !in before }
                assertThat(row.targetId).isEqualTo(ref.id)
                assertThat(row.targetType).isEqualTo(if (ref is CanonicalEntityRef.Workspace) "WORKSPACE" else "MANAGED_SUBJECT")
                assertThat(f.graph().parentOf(H1TypedNode.Occurrence(row.id)))
                    .isEqualTo((destination as? H1StructuralParent.Node)?.ref)
            } }
            refs.forEach { ref ->
                val rows = f.db.hierarchyPlacementDao().getAll().filter { it.targetId == ref.id }
                assertThat(rows.count { it.placementKind == "PRIMARY" }).isEqualTo(1)
                assertThat(rows.count { it.placementKind == "LINK" }).isAtLeast(3)
            }
        }
    }

    @Test fun `multiple appearances keep one Library row and Unplaced reacts to the last occurrence only`() = runBlocking<Unit> {
        LibraryPlacementRoomFixture().use { f ->
            val ref = f.seed().first()
            f.awaitLibrary { it.items.size == 3 }
            f.library.setFilter(EntityLibraryFilter.UNPLACED)
            f.awaitLibrary { it.filter == EntityLibraryFilter.UNPLACED && it.items.size == 3 }
            f.add(ref, H1StructuralParent.Root)
            f.awaitLibrary { it.items.size == 2 && it.items.none { item -> item.ref == ref } }
            f.add(ref, H1StructuralParent.Root)
            f.library.setFilter(EntityLibraryFilter.ALL)
            assertThat(f.awaitLibrary { it.filter == EntityLibraryFilter.ALL }.items.count { it.ref == ref }).isEqualTo(1)
            val rows = f.db.hierarchyPlacementDao().getAll().filter { it.targetId == ref.id }
            f.writer.removeOccurrence(PlacementId(rows.single { it.placementKind == "LINK" }.id))
            f.library.setFilter(EntityLibraryFilter.UNPLACED)
            f.awaitLibrary { it.filter == EntityLibraryFilter.UNPLACED && it.items.size == 2 }
            f.writer.removeOccurrence(PlacementId(rows.single { it.placementKind == "PRIMARY" }.id))
            f.awaitLibrary { it.items.size == 3 && it.items.any { item -> item.ref == ref } }
            assertThat(f.db.orientationDao().getManagedSubject(ref.id)!!.isDeleted).isFalse()
        }
    }

    @Test fun `late failed Library Add rolls back placement coverage membership ordering and retains retry intent`() = runBlocking<Unit> {
        LibraryPlacementRoomFixture().use { f ->
            val ref = f.seed().first()
            val container = f.writer.createContainer("Blocked", H1StructuralParent.Root, id = "blocked")
            f.library.setFilter(EntityLibraryFilter.UNPLACED)
            f.awaitLibrary { it.filter == EntityLibraryFilter.UNPLACED && it.items.size == 3 }
            val before = BigPicturePresentationRepository(f.db).snapshot()
            f.db.openHelper.writableDatabase.execSQL("""
                CREATE TRIGGER fail_library_member BEFORE INSERT ON big_picture_memberships
                WHEN NEW.containerId = 'blocked' BEGIN SELECT RAISE(ABORT, 'late library failure'); END
            """.trimIndent())
            f.command.begin(ref)
            f.command.place(H1StructuralParent.Node(H1TypedNode.Container(container)))
            withTimeout(10_000) { f.command.state.first { !it.busy } }
            assertThat(f.command.state.value.error).isNotNull()
            assertThat(f.command.state.value.pendingRef).isEqualTo(ref)
            assertThat(f.db.hierarchyPlacementDao().getAll()).isEmpty()
            assertThat(GroupScopeCoverageStore(f.db).snapshot().records).isEmpty()
            assertThat(BigPicturePresentationRepository(f.db).snapshot()).isEqualTo(before)
            f.awaitLibrary { it.filter == EntityLibraryFilter.UNPLACED && it.items.size == 3 }
            f.command.place(H1StructuralParent.Root)
            withTimeout(10_000) { f.command.state.first { !it.busy } }
            assertThat(f.command.state.value.error).isNull()
            assertThat(f.db.hierarchyPlacementDao().getAll()).hasSize(1)
        }
    }

    @Test fun `Library placement into historical section leaves every semantic and MainBeacon authority unchanged`() = runBlocking<Unit> {
        LibraryPlacementRoomFixture().use { f ->
            val refs = f.seed()
            val container = f.historicalContainer()
            val dao = f.db.orientationDao()
            dao.upsertOrientationRelations(listOf(OrientationRelationEntity("relation", refs[0].id, "group", "SUPPORTS", null, 1, 1, null, false, 1)))
            dao.upsertAspectOrientationRefs(listOf(AspectOrientationRefEntity("aspect-ref", refs[1].id, refs[0].id,
                "BELONGS_TO", true, 0, 1, 1, null, false, 1)))
            dao.upsertWorkspaceBindings(listOf(WorkspaceBindingEntity("binding", "w", refs[0].id,
                "SUPPORTS", false, 0, 1, 1, null, false, 1)))
            val before = f.semanticRows()
            refs.forEach { f.add(it, H1StructuralParent.Node(H1TypedNode.Container(container))) }
            assertThat(f.semanticRows()).isEqualTo(before)
            assertThat(f.db.hierarchyPlacementGroupScopeDao().getAll()).isEmpty()
        }
    }

    @Test fun `Library-created PRIMARY and LINK occurrences roundtrip existing Restore and Merge transport`() = runBlocking<Unit> {
        LibraryPlacementRoomFixture().use { f ->
            val refs = f.seed()
            val generic = f.writer.createContainer("Section", H1StructuralParent.Root)
            refs.forEach { f.add(it, H1StructuralParent.Root) }
            refs.forEach { f.add(it, H1StructuralParent.Node(H1TypedNode.Container(generic))) }
            f.roundtrip()
        }
    }
}
