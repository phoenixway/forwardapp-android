package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.data.orientation.CanonicalAspectRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Positive schema-200 roundtrip acceptance. UX activation remains a separate gate. */
@RunWith(RobolectricTestRunner::class)
class BigPictureMutationTransportBoundaryRoomTest {
    private fun database() = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java,
    ).allowMainThreadQueries().build()

    @Test fun `native semantic root Add roundtrips Restore and peer Merge without historical provenance`() = runBlocking {
        val db = database()
        try {
            val dao = db.orientationDao()
            val o = CanonicalOrientationRepository(db, dao).createOrientation(OrientationKind.INTENT, "O", now = 1)
            val a = CanonicalAspectRepository(db, dao, HierarchyPlacementLifecycleCoordinator(db)).create("A", now = 2)
            val writer = H1StructuralMutationCoordinator(db)
            listOf(o, a).forEach { target ->
                writer.addAppearance(HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, target), H1StructuralParent.Root)
            }
            val store = CanonicalHierarchyPlacementGroupScopeSyncStore(db)
            val exportedScopes = store.loadAll()
            assertThat(exportedScopes).isEmpty() // Correct native command: never manufactures historical provenance.
            val bundle = SnapshotBundle(managedSubjects = dao.getAllManagedSubjects(), orientations = dao.getAllOrientations(),
                aspects = dao.getAllAspects(), hierarchyPlacementGroupScopes = exportedScopes,
                groupScopeCoverage = GroupScopeCoverageStore(db).snapshot())
            val placements = db.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() }
            store.decodeAndValidateForRestore(bundle, exportedScopes, placements, requireComplete = true)
            assertThat(bundle.groupScopeCoverage!!.records.map { it.mode }).containsExactly("NATIVE", "NATIVE")
            roundtrip(db)
        } finally { db.close() }
    }

    @Test fun `moving historical occurrence retires scope and roundtrips nested and returned root state`() = runBlocking {
        val db = database()
        try {
            val o = CanonicalOrientationRepository(db, db.orientationDao()).createOrientation(OrientationKind.INTENT, "O", now = 1)
            db.workspaceDao().upsert(listOf(WorkspaceEntity("w", "W", null, null, 1, 1, null, false, 1,
                provenance = WorkspaceProvenance.CANONICAL_ONLY.name, sourceContextId = null)))
            val writer = H1StructuralMutationCoordinator(db)
            val source = writer.addAppearance(HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, o), H1StructuralParent.Root)
            val parent = writer.addAppearance(HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "w"), H1StructuralParent.Root)
            HierarchyPlacementGroupScopeMutationCoordinator(db).setRootScope(source, null, now = 10)
            val store = CanonicalHierarchyPlacementGroupScopeSyncStore(db)
            val before = store.loadAll()
            store.mergeIncoming(before) // Initially admissible provenance.
            writer.moveNode(H1TypedNode.Occurrence(source.value), H1StructuralParent.Node(H1TypedNode.Occurrence(parent.value)))
            assertThat(store.loadAll().single().isDeleted).isTrue()
            assertThat(store.loadAll().single().version).isGreaterThan(before.single().version)
            assertThat(GroupScopeCoverageStore(db).snapshot().records.single().mode).isEqualTo("HISTORICAL")
            roundtrip(db)
            writer.moveNode(H1TypedNode.Occurrence(source.value), H1StructuralParent.Root)
            assertThat(store.loadAll().single().isDeleted).isTrue()
            roundtrip(db)
        } finally { db.close() }
    }

    private suspend fun roundtrip(source: AppDatabase) {
        val scopes = CanonicalHierarchyPlacementGroupScopeSyncStore(source).loadAll()
        val coverage = GroupScopeCoverageStore(source).snapshot()
        val placementStore = CanonicalHierarchyPlacementSyncStore(source)
        val rows = placementStore.loadAll()
        val placements = source.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() }
        val o = source.orientationDao()
        val bundle = SnapshotBundle(managedSubjects = o.getAllManagedSubjects(), orientations = o.getAllOrientations(),
            aspects = o.getAllAspects(), hierarchyPlacementGroupScopes = scopes, groupScopeCoverage = coverage)
        val presentation = BigPicturePresentationRepository(source).snapshot()
        listOf(false, true).forEach { peerMerge ->
            val target = database()
            try {
                target.orientationDao().upsertManagedSubjects(o.getAllManagedSubjects())
                target.orientationDao().upsertOrientations(o.getAllOrientations())
                target.orientationDao().upsertAspects(o.getAllAspects())
                target.workspaceDao().upsert(source.workspaceDao().getAll())
                val gs = CanonicalHierarchyPlacementGroupScopeSyncStore(target)
                if (peerMerge) {
                    CanonicalHierarchyPlacementSyncStore(target).mergeIncoming(rows)
                    gs.mergeIncoming(scopes, coverage)
                    BigPicturePresentationRepository(target).merge(presentation)
                } else {
                    val decoded = gs.decodeAndValidateForRestore(bundle, scopes, placements, true)
                    CanonicalHierarchyPlacementSyncStore(target).restoreExactDecoded(placements)
                    GroupScopeCoverageStore(target).restoreExact(coverage)
                    gs.restoreExactDecoded(decoded, true)
                    BigPicturePresentationRepository(target).restoreExact(presentation)
                }
                GroupScopeCoverageStore(target).validateCurrent()
                assertThat(GroupScopeCoverageStore(target).snapshot().records.map { it.copy(syncedAt = null) })
                    .containsExactlyElementsIn(coverage.records.map { it.copy(syncedAt = null) })
                assertThat(gs.loadAll().map { it.copy(syncedAt = null) }).containsExactlyElementsIn(scopes.map { it.copy(syncedAt = null) })
            } finally { target.close() }
        }
    }
}
