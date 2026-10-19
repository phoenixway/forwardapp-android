package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.*
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.data.orientation.CanonicalAspectRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.*
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BigPictureMutationUxRoomTest {
    private fun database() = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java,
    ).allowMainThreadQueries().build()

    private suspend fun targets(db: AppDatabase): List<HierarchyTargetRef> {
        val dao = db.orientationDao()
        val o = CanonicalOrientationRepository(db, dao).createOrientation(OrientationKind.INTENT, "O", now = 1)
        val a = CanonicalAspectRepository(db, dao, HierarchyPlacementLifecycleCoordinator(db)).create("A", now = 2)
        db.workspaceDao().upsert(listOf(WorkspaceEntity("w", "W", null, null, 1, 1, null, false, 1,
            provenance = WorkspaceProvenance.CANONICAL_ONLY.name, sourceContextId = null)))
        return listOf(HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, o),
            HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, a), HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "w"))
    }

    private suspend fun graph(db: AppDatabase) = projectH1UnifiedStructuralGraph(
        db.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() }, BigPicturePresentationRepository(db).snapshot(),
    )

    @Test fun `Add admits all classes at root occurrence and container and atomically chooses PRIMARY LINK`() = runBlocking {
        val db = database()
        try {
            val writer = H1StructuralMutationCoordinator(db)
            val refs = targets(db)
            val root = writer.addAppearance(refs[0], H1StructuralParent.Root, now = 10)
            val section = writer.createContainer("Mixed", H1StructuralParent.Root, now = 11)
            val destinations = listOf(H1StructuralParent.Root,
                H1StructuralParent.Node(H1TypedNode.Occurrence(root.value)), H1StructuralParent.Node(H1TypedNode.Container(section)))
            refs.forEach { target ->
                destinations.forEach { destination ->
                    val id = writer.addAppearance(target, destination, now = 20)
                    assertThat(graph(db).parentOf(H1TypedNode.Occurrence(id.value)))
                        .isEqualTo((destination as? H1StructuralParent.Node)?.ref)
                }
                val appearances = db.hierarchyPlacementDao().getAll().filter { it.targetId == target.id }
                assertThat(appearances.count { it.placementKind == "PRIMARY" }).isEqualTo(1)
                assertThat(appearances.count { it.placementKind == "LINK" }).isAtLeast(2)
            }
            val rows = db.hierarchyPlacementDao().getAll()
            assertThat(rows.map { it.id }.distinct()).hasSize(rows.size)
        } finally { db.close() }
    }

    @Test fun `simultaneous Add cannot race two PRIMARY appearances`() = runBlocking {
        val db = database()
        try {
            val ref = targets(db).last()
            val writer = H1StructuralMutationCoordinator(db)
            (1..3).map { async { writer.addAppearance(ref, H1StructuralParent.Root) } }.awaitAll()
            val rows = db.hierarchyPlacementDao().getAll()
            assertThat(rows.count { it.placementKind == "PRIMARY" }).isEqualTo(1)
            assertThat(rows.count { it.placementKind == "LINK" }).isEqualTo(2)
        } finally { db.close() }
    }

    @Test fun `product mutations preserve semantics targets and other appearances while read write IDs converge`() = runBlocking {
        val db = database()
        try {
            val writer = H1StructuralMutationCoordinator(db)
            val refs = targets(db)
            val beforeSubjects = db.orientationDao().getAllManagedSubjects()
            val beforeWorkspaces = db.workspaceDao().getAll()
            val primary = writer.addAppearance(refs[0], H1StructuralParent.Root, now = 10)
            val other = writer.addAppearance(refs[0], H1StructuralParent.Root, now = 11)
            val outer = writer.createContainer("Outer", H1StructuralParent.Node(H1TypedNode.Occurrence(primary.value)), now = 12)
            val inner = writer.createContainer("Inner", H1StructuralParent.Node(H1TypedNode.Container(outer)), now = 13)
            val aspect = writer.addAppearance(refs[1], H1StructuralParent.Node(H1TypedNode.Container(outer)), now = 14)
            val workspace = writer.addAppearance(refs[2], H1StructuralParent.Node(H1TypedNode.Container(outer)), now = 15)
            val mixed = listOf(H1TypedNode.Occurrence(workspace.value), H1TypedNode.Container(inner), H1TypedNode.Occurrence(aspect.value))
            writer.reorderChildren(H1StructuralParent.Node(H1TypedNode.Container(outer)), mixed, now = 20)
            assertThat(runCatching { writer.reorderChildren(H1StructuralParent.Node(H1TypedNode.Container(outer)), mixed.take(1), now = 21) }.isFailure).isTrue()
            writer.deleteContainer(outer, now = 22)
            assertThat(graph(db).childrenOf(H1TypedNode.Occurrence(primary.value)).map { it.node }).containsExactlyElementsIn(mixed).inOrder()
            writer.moveNode(H1TypedNode.Occurrence(workspace.value), H1StructuralParent.Root, now = 23)
            writer.removeOccurrence(workspace, now = 24)
            writer.removeOccurrence(other, now = 25)
            assertThat(db.hierarchyPlacementDao().getById(primary.value)!!.isDeleted).isFalse()
            assertThat(db.hierarchyPlacementDao().getById(other.value)!!.isDeleted).isTrue()
            val structure = BigPictureStructureRepository(db).observe().first()
            val catalog = CanonicalEntityCatalogRepository(db.canonicalEntityCatalogDao()).observeCatalog().first()
            val nodes = projectBigPicture(structure.placements, catalog)
            val read = BigPictureReadComposition(nodes, emptyList(), structure.presentation)
            val index = BigPictureStructuralIndex(read)
            assertThat(read.occurrenceEntries(primary.value).map { it.structuralRef(index) })
                .containsExactly(H1TypedNode.Container(inner), H1TypedNode.Occurrence(aspect.value)).inOrder()
            assertThat(db.orientationDao().getAllManagedSubjects()).containsExactlyElementsIn(beforeSubjects)
            assertThat(db.workspaceDao().getAll()).containsExactlyElementsIn(beforeWorkspaces)
            assertThat(db.orientationDao().getAllOrientationRelations()).isEmpty()
            assertThat(db.orientationDao().getAllAspectOrientationRefs()).isEmpty()
            assertThat(db.orientationDao().getAllWorkspaceBindings()).isEmpty()
            assertThat(db.planningDao().getAllScopes()).isEmpty()
            assertThat(db.planningDao().getAllCommitments()).isEmpty()
            assertThat(db.contextDao().getAllRaw()).isEmpty()
            assertThat(db.mainBeaconDao().getAllGroupsSync()).isEmpty()
            assertThat(db.hierarchyPlacementGroupScopeDao().getAll()).isEmpty()
        } finally { db.close() }
    }

    @Test fun `Add Group validates title and supports all typed destinations`() = runBlocking {
        val db = database()
        try {
            val writer = H1StructuralMutationCoordinator(db)
            val p = writer.addAppearance(targets(db).last(), H1StructuralParent.Root, now = 10)
            val root = writer.createContainer(" Root ", H1StructuralParent.Root, now = 11)
            val underOccurrence = writer.createContainer("Inside", H1StructuralParent.Node(H1TypedNode.Occurrence(p.value)), now = 12)
            val nested = writer.createContainer("Nested", H1StructuralParent.Node(H1TypedNode.Container(root)), now = 13)
            assertThat(graph(db).parentOf(H1TypedNode.Container(nested))).isEqualTo(H1TypedNode.Container(root))
            assertThat(graph(db).parentOf(H1TypedNode.Container(underOccurrence))).isEqualTo(H1TypedNode.Occurrence(p.value))
            val before = BigPicturePresentationRepository(db).snapshot()
            assertThat(runCatching { writer.createContainer("   ", H1StructuralParent.Root) }.isFailure).isTrue()
            assertThat(BigPicturePresentationRepository(db).snapshot()).isEqualTo(before)
            assertThat(before.containers.single { it.id == root }.title).isEqualTo("Root")
        } finally { db.close() }
    }

    @Test fun `removing PRIMARY appearance leaves canonical entity and LINK live`() = runBlocking {
        val db = database()
        try {
            val writer = H1StructuralMutationCoordinator(db)
            val ref = targets(db).last()
            val primary = writer.addAppearance(ref, H1StructuralParent.Root, now = 10)
            val link = writer.addAppearance(ref, H1StructuralParent.Root, now = 11)
            writer.removeOccurrence(primary, now = 20)
            assertThat(db.hierarchyPlacementDao().getById(primary.value)!!.isDeleted).isTrue()
            assertThat(db.hierarchyPlacementDao().getById(link.value)!!.isDeleted).isFalse()
            assertThat(db.workspaceDao().getAll().single().isDeleted).isFalse()
        } finally { db.close() }
    }

    @Test fun `visible root children equal the complete canonical root reorder set`() = runBlocking {
        val db = database()
        try {
            val writer = H1StructuralMutationCoordinator(db)
            val refs = targets(db)
            val workspace = writer.addAppearance(refs.last(), H1StructuralParent.Root, now = 10)
            val section = writer.createContainer("WIP", H1StructuralParent.Root, now = 11)
            val structure = BigPictureStructureRepository(db).observe().first()
            val catalog = CanonicalEntityCatalogRepository(db.canonicalEntityCatalogDao()).observeCatalog().first()
            val nodes = projectBigPicture(structure.placements, catalog)
            val read = BigPictureReadComposition(nodes, emptyList(), structure.presentation)
            val index = BigPictureStructuralIndex(read)
            assertThat(read.rootEntries().filterIsInstance<BigPictureReadEntry.Occurrence>().map { it.node.placement.id })
                .containsExactly(workspace)
            assertThat(index.children(H1StructuralParent.Root).map { it.ref }).containsExactly(
                H1TypedNode.Occurrence(workspace.value), H1TypedNode.Container(section),
            ).inOrder()
            assertThat(read.rootEntries().map { it.structuralRef(index) })
                .containsExactlyElementsIn(index.children(H1StructuralParent.Root).map { it.ref }).inOrder()
            assertThat(index.sections["presentation:$section"]).isEqualTo(H1TypedNode.Container(section))
        } finally { db.close() }
    }

    @Test fun `activated product path dirty delta stale ACK and tombstones survive`() = runBlocking {
        val db = database()
        try {
            val writer = H1StructuralMutationCoordinator(db)
            val targets = targets(db)
            val p = writer.addAppearance(targets.last(), H1StructuralParent.Root, now = 10)
            val presentation = BigPicturePresentationRepository(db)
            val sync = CanonicalHierarchyPlacementSyncStore(db)
            val sent = sync.loadUnsynced().single()
            val parent = writer.addAppearance(targets.first(), H1StructuralParent.Root, now = 11)
            val c = writer.createContainer("Section", H1StructuralParent.Node(H1TypedNode.Occurrence(parent.value)), now = 12)
            val containerSent = presentation.dirty().containers.single()
            writer.moveNode(H1TypedNode.Occurrence(p.value), H1StructuralParent.Node(H1TypedNode.Container(c)), now = 20)
            sync.markSynced(listOf(HierarchyPlacementSyncVersion(p.value, sent.version)))
            presentation.markSynced(BigPicturePresentationAck(containers = listOf(BigPictureRecordVersion(c, containerSent.version))), now = 21)
            assertThat(sync.loadUnsynced().single { it.id == p.value }.version).isGreaterThan(sent.version)
            assertThat(presentation.dirty().memberships.single().placementId).isEqualTo(p.value)
            writer.deleteContainer(c, now = 30)
            presentation.markSynced(BigPicturePresentationAck(containers = listOf(BigPictureRecordVersion(c, containerSent.version))), now = 31)
            assertThat(presentation.dirty().containers.single().isDeleted).isTrue()
            assertThat(presentation.changedSince(20).containers.single().isDeleted).isTrue()
            writer.removeOccurrence(p, now = 40)
            assertThat(sync.loadUnsynced().single { it.id == p.value }.isDeleted).isTrue()
            assertThat(sync.loadChangedSince(30).single { it.id == p.value }.isDeleted).isTrue()
        } finally { db.close() }
    }
}
