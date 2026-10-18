package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.hierarchy.*
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GroupScopeCoverageRoomTest {
    private fun database() = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),
        AppDatabase::class.java).allowMainThreadQueries().build()
    private inline fun <T> using(block: (AppDatabase) -> T): T { val db = database(); return try { block(db) } finally { db.close() } }
    private suspend fun native(db: AppDatabase, name: String = "O"): PlacementId {
        val id = CanonicalOrientationRepository(db, db.orientationDao()).createOrientation(OrientationKind.INTENT, name, now = 1)
        return H1StructuralMutationCoordinator(db).addAppearance(HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, id), H1StructuralParent.Root, now = 2)
    }

    @Test fun `native creation explicit historical adoption and exact version coverage ACK`() = runBlocking { using { db ->
        val p = native(db)
        val coverage = GroupScopeCoverageStore(db)
        val initial = coverage.snapshot().records.single()
        assertThat(initial.mode).isEqualTo("NATIVE")
        assertThat(db.hierarchyPlacementGroupScopeDao().getAll()).isEmpty()
        HierarchyPlacementGroupScopeMutationCoordinator(db).setRootScope(p, null, 10)
        val adopted = coverage.snapshot().records.single()
        assertThat(adopted.mode).isEqualTo("HISTORICAL")
        assertThat(adopted.version).isEqualTo(initial.version + 1)
        coverage.markSynced(listOf(GroupScopeCoverageVersion(p.value, initial.version)), 11)
        assertThat(coverage.dirty().records).hasSize(1)
        coverage.markSynced(listOf(GroupScopeCoverageVersion(p.value, adopted.version)), 12)
        assertThat(coverage.dirty().records).isEmpty()
        assertThat(coverage.changedSince(2).records).hasSize(1)
    } }

    @Test fun `historical Move to root container retires GroupScope and generic return never resurrects`() = runBlocking { using { db ->
        val p = native(db)
        val explicit = HierarchyPlacementGroupScopeMutationCoordinator(db)
        explicit.setRootScope(p, null, 10)
        val gs = CanonicalHierarchyPlacementGroupScopeSyncStore(db)
        val sent = gs.loadAll().single()
        gs.markSynced(listOf(HierarchyPlacementGroupScopeSyncVersion(p.value, sent.version)))
        val writer = H1StructuralMutationCoordinator(db)
        val c = writer.createContainer("C", H1StructuralParent.Root, now = 11)
        writer.moveNode(H1TypedNode.Occurrence(p.value), H1StructuralParent.Node(H1TypedNode.Container(c)), now = 12)
        val retired = gs.loadAll().single()
        assertThat(retired.isDeleted).isTrue()
        val lineage = GroupScopeCoverageStore(db).snapshot()
        assertThat(runCatching { explicit.setRootScope(p, null, 12) }.isFailure).isTrue()
        assertThat(gs.loadAll().single()).isEqualTo(retired)
        assertThat(GroupScopeCoverageStore(db).snapshot()).isEqualTo(lineage)
        assertThat(retired.version).isEqualTo(sent.version + 1)
        assertThat(retired.updatedAt).isGreaterThan(sent.updatedAt)
        gs.markSynced(listOf(HierarchyPlacementGroupScopeSyncVersion(p.value, sent.version)))
        assertThat(gs.loadAll().single().syncedAt).isNull()
        writer.moveNode(H1TypedNode.Occurrence(p.value), H1StructuralParent.Root, now = 13)
        assertThat(gs.loadAll().single()).isEqualTo(retired)
        GroupScopeCoverageStore(db).validateCurrent()
        explicit.setRootScope(p, null, 14)
        assertThat(gs.loadAll().single().isDeleted).isFalse()
        assertThat(gs.loadAll().single().version).isEqualTo(retired.version + 1)
        explicit.validateAuthoritativeState()
    } }

    @Test fun `historical active and tombstone snapshots restore without losing applicability`() = runBlocking { using { db ->
        val p = native(db)
        HierarchyPlacementGroupScopeMutationCoordinator(db).setRootScope(p, null, 10)
        val gs = CanonicalHierarchyPlacementGroupScopeSyncStore(db)
        val cov = GroupScopeCoverageStore(db)
        val o = db.orientationDao()
        val placements = db.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() }
        val envelope = SnapshotBundle(managedSubjects = o.getAllManagedSubjects(), orientations = o.getAllOrientations(), groupScopeCoverage = cov.snapshot())
        gs.decodeAndValidateForRestore(envelope, gs.loadAll(), placements, true)
        gs.mergeIncoming(gs.loadAll(), cov.snapshot())
        val bad = runCatching { gs.decodeAndValidateForRestore(envelope, emptyList(), placements, true) }
        assertThat(bad.exceptionOrNull()?.message).contains("lost GroupScope history")
        assertThat(cov.snapshot().records.single().mode).isEqualTo("HISTORICAL")
    } }

    @Test fun `equal version divergent GroupScope fails full and selective merge identical replay is idempotent`() = runBlocking { using { db ->
        val p = native(db)
        HierarchyPlacementGroupScopeMutationCoordinator(db).setRootScope(p, null, 10)
        val gs = CanonicalHierarchyPlacementGroupScopeSyncStore(db)
        val coverage = GroupScopeCoverageStore(db).snapshot()
        val original = gs.loadAll()
        gs.mergeIncoming(original, coverage)
        gs.mergeIncomingSelective(original, setOf(p.value), coverage)
        val divergent = original.map { it.copy(updatedAt = it.updatedAt + 100) }
        assertThat(runCatching { gs.mergeIncoming(divergent, coverage) }.isFailure).isTrue()
        assertThat(runCatching { gs.mergeIncomingSelective(divergent, setOf(p.value), coverage) }.isFailure).isTrue()
        assertThat(gs.loadAll()).isEqualTo(original)
    } }

    @Test fun `LEGACY UNKNOWN root stays conservative for Restore Merge and current validation`() = runBlocking { using { db ->
        native(db)
        val cov = GroupScopeCoverageStore(db)
        val ambiguous = cov.snapshot().copy(records = cov.snapshot().records.map { it.copy(mode = "LEGACY_UNKNOWN") })
        cov.restoreExact(ambiguous) // Exact replacement preserves ambiguity, does not guess from kind/title.
        val gs = CanonicalHierarchyPlacementGroupScopeSyncStore(db)
        assertThat(runCatching { gs.mergeIncoming(emptyList(), ambiguous) }.isFailure).isTrue()
        val o = db.orientationDao()
        val bundle = SnapshotBundle(managedSubjects = o.getAllManagedSubjects(), orientations = o.getAllOrientations(), groupScopeCoverage = ambiguous)
        assertThat(runCatching { gs.decodeAndValidateForRestore(bundle, emptyList(), db.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() }, true) }.isFailure).isTrue()
        assertThat(runCatching { cov.validateCurrent() }.isFailure).isTrue()
        assertThat(cov.snapshot()).isEqualTo(ambiguous)
    } }

    @Test fun `coverage omission unknown mode and divergent equal version fail closed`() = runBlocking { using { db ->
        native(db)
        val cov = GroupScopeCoverageStore(db)
        val before = cov.snapshot()
        assertThat(runCatching { cov.merge(before.copy(records = emptyList())) }.isFailure).isTrue()
        assertThat(runCatching { cov.merge(before.copy(records = before.records.map { it.copy(mode = "BOGUS") })) }.isFailure).isTrue()
        assertThat(runCatching { cov.merge(before.copy(records = before.records.map { it.copy(updatedAt = 999) })) }.isFailure).isTrue()
        assertThat(cov.snapshot()).isEqualTo(before)
    } }

    @Test fun `pre coverage Orientation payload remains UNKNOWN and cannot silently become native`() = runBlocking { using { db ->
        native(db)
        val o = db.orientationDao()
        val placements = db.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() }
        val legacy = SnapshotBundle(managedSubjects = o.getAllManagedSubjects(), orientations = o.getAllOrientations())
        val classified = GroupScopeCoverageStore(db).decodeForRestore(legacy, placements, emptyList())
        assertThat(classified.records.single().mode).isEqualTo("LEGACY_UNKNOWN")
        assertThat(runCatching { CanonicalHierarchyPlacementGroupScopeSyncStore(db)
            .decodeAndValidateForRestore(legacy, emptyList(), placements, true) }.isFailure).isTrue()
    } }

    @Test fun `explicit merge envelope cannot hide an omitted occurrence by selecting its own keys`() = runBlocking { using { db ->
        native(db)
        val cov = GroupScopeCoverageStore(db)
        val before = cov.snapshot()
        assertThat(runCatching { CanonicalHierarchyPlacementGroupScopeSyncStore(db)
            .mergeIncoming(emptyList(), GroupScopeCoverageSnapshot()) }.isFailure).isTrue()
        assertThat(cov.snapshot()).isEqualTo(before)
    } }

    @Test fun `structural Move scope failure rolls back placement membership mixed order and all compatibility sidecars`() = runBlocking { using { db ->
        val p = native(db)
        val parent = native(db, "Parent")
        val writer = H1StructuralMutationCoordinator(db)
        val c = writer.createContainer("C", H1StructuralParent.Node(H1TypedNode.Occurrence(parent.value)), now = 10)
        HierarchyPlacementGroupScopeMutationCoordinator(db).setRootScope(p, null, 11)
        val placements = db.hierarchyPlacementDao().getAll()
        val presentation = BigPicturePresentationRepository(db).snapshot()
        val coverage = GroupScopeCoverageStore(db).snapshot()
        val scopes = db.hierarchyPlacementGroupScopeDao().getAll()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_scope_retire BEFORE UPDATE ON hierarchy_placement_group_scopes BEGIN SELECT RAISE(ABORT, 'scope-failure'); END")
        assertThat(runCatching { writer.moveNode(H1TypedNode.Occurrence(p.value), H1StructuralParent.Node(H1TypedNode.Container(c)), now = 12) }.isFailure).isTrue()
        assertThat(db.hierarchyPlacementDao().getAll()).isEqualTo(placements)
        assertThat(BigPicturePresentationRepository(db).snapshot()).isEqualTo(presentation)
        assertThat(GroupScopeCoverageStore(db).snapshot()).isEqualTo(coverage)
        assertThat(db.hierarchyPlacementGroupScopeDao().getAll()).isEqualTo(scopes)
    } }

    @Test fun `explicit adoption failure rolls back applicability version and scope atomically`() = runBlocking { using { db ->
        val p = native(db)
        val before = GroupScopeCoverageStore(db).snapshot()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_scope_create BEFORE INSERT ON hierarchy_placement_group_scopes BEGIN SELECT RAISE(ABORT, 'scope-failure'); END")
        assertThat(runCatching { HierarchyPlacementGroupScopeMutationCoordinator(db).setRootScope(p, null, 10) }.isFailure).isTrue()
        assertThat(GroupScopeCoverageStore(db).snapshot()).isEqualTo(before)
        assertThat(db.hierarchyPlacementGroupScopeDao().getAll()).isEmpty()
    } }

    @Test fun `final coverage validation failure rolls back H1 scope retirement ordering membership and coverage writes`() = runBlocking { using { db ->
        val p = native(db)
        val parent = native(db, "Parent")
        val writer = H1StructuralMutationCoordinator(db)
        writer.createContainer("Mixed sibling", H1StructuralParent.Root, now = 10)
        HierarchyPlacementGroupScopeMutationCoordinator(db).setRootScope(p, null, 11)
        val placements = db.hierarchyPlacementDao().getAll()
        val presentation = BigPicturePresentationRepository(db).snapshot()
        val coverage = GroupScopeCoverageStore(db).snapshot()
        val scopes = db.hierarchyPlacementGroupScopeDao().getAll()
        assertThat(runCatching {
            writer.moveOccurrences(listOf(HierarchyPlacementMove(p, parent)), now = 12, historicalCompanion = {
                val old = db.groupScopeApplicabilityDao().all().first { it.placementId == p.value }
                db.groupScopeApplicabilityDao().upsert(listOf(old.copy(mode = "INVALID", version = old.version + 1)))
            })
        }.isFailure).isTrue()
        assertThat(db.hierarchyPlacementDao().getAll()).isEqualTo(placements)
        assertThat(BigPicturePresentationRepository(db).snapshot()).isEqualTo(presentation)
        assertThat(GroupScopeCoverageStore(db).snapshot()).isEqualTo(coverage)
        assertThat(db.hierarchyPlacementGroupScopeDao().getAll()).isEqualTo(scopes)
    } }

    @Test fun `native mutations preserve every semantic authority`() = runBlocking { using { db ->
        val p = native(db)
        val subjects = db.orientationDao().getAllManagedSubjects()
        val writer = H1StructuralMutationCoordinator(db)
        val c = writer.createContainer("WIP", H1StructuralParent.Root, now = 10)
        writer.moveNode(H1TypedNode.Occurrence(p.value), H1StructuralParent.Node(H1TypedNode.Container(c)), now = 11)
        writer.moveNode(H1TypedNode.Occurrence(p.value), H1StructuralParent.Root, now = 12)
        writer.removeOccurrence(p, now = 13)
        assertThat(db.orientationDao().getAllManagedSubjects()).isEqualTo(subjects)
        assertThat(db.orientationDao().getAllOrientationRelations()).isEmpty()
        assertThat(db.orientationDao().getAllAspectOrientationRefs()).isEmpty()
        assertThat(db.orientationDao().getAllWorkspaceBindings()).isEmpty()
        assertThat(db.planningDao().getAllScopes()).isEmpty()
        assertThat(db.planningDao().getAllCommitments()).isEmpty()
        assertThat(db.contextDao().getAllRaw()).isEmpty()
    } }
}
