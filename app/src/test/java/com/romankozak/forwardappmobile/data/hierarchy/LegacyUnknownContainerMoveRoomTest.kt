package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.WorkspaceEntity
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.sync.admitCanonicalHierarchyStreams
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.workspace.ContextPresentation
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceProvenance
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LegacyUnknownContainerMoveRoomTest {
    private fun database() = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),
        AppDatabase::class.java).allowMainThreadQueries().build()

    private data class Fixture(val parent: PlacementId, val primary: PlacementId, val link: PlacementId, val container: String)

    private suspend fun fixture(db: AppDatabase): Fixture {
        db.workspaceDao().upsert(listOf(WorkspaceEntity("workspace", "Workspace", null, null, 1, 1, null, false, 1,
            provenance = WorkspaceProvenance.CANONICAL_ONLY.name, sourceContextId = null)))
        val writer = H1StructuralMutationCoordinator(db)
        val parent = writer.addAppearance(HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "workspace"), H1StructuralParent.Root, now = 1)
        val subject = CanonicalOrientationRepository(db, db.orientationDao()).createOrientation(OrientationKind.INTENT, "Meaning", now = 2)
        val target = HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, subject)
        val underParent = H1StructuralParent.Node(H1TypedNode.Occurrence(parent.value))
        val primary = writer.createOccurrence(target, PlacementKind.PRIMARY, underParent, now = 3)
        val container = writer.createContainer("Group", H1StructuralParent.Root, now = 4)
        val nested = writer.createContainer("Nested", underParent, now = 5)
        val link = writer.createOccurrence(target, PlacementKind.LINK,
            H1StructuralParent.Node(H1TypedNode.Container(nested)), now = 6)
        // Simulate an admitted pre-coverage nested occurrence. No production lineage repair.
        val coverage = GroupScopeCoverageStore(db)
        coverage.restoreExact(coverage.snapshot().let { snapshot -> snapshot.copy(records = snapshot.records.map { it.copy(mode = "LEGACY_UNKNOWN") }) })
        coverage.validateCurrent()
        return Fixture(parent, primary, link, container)
    }

    @Test fun `PRIMARY and LINK move into root container without changing lineage or semantic state`() = runBlocking {
        val db = database()
        try {
            val f = fixture(db)
            val writer = H1StructuralMutationCoordinator(db)
            val coverage = GroupScopeCoverageStore(db)
            val beforeCoverage = coverage.snapshot()
            val before = db.hierarchyPlacementDao().getAll().associateBy { it.id }
            val subjects = db.orientationDao().getAllManagedSubjects()
            listOf(f.primary, f.link).forEach { id ->
                writer.moveNode(H1TypedNode.Occurrence(id.value), H1StructuralParent.Node(H1TypedNode.Container(f.container)), now = 10)
                val after = db.hierarchyPlacementDao().getById(id.value)!!
                assertThat(after.parentPlacementId).isNull()
                assertThat(after.targetId).isEqualTo(before.getValue(id.value).targetId)
                assertThat(after.placementKind).isEqualTo(before.getValue(id.value).placementKind)
                val graph = projectH1UnifiedStructuralGraph(db.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() },
                    BigPicturePresentationRepository(db).snapshot())
                assertThat(graph.parentOf(H1TypedNode.Occurrence(id.value))).isEqualTo(H1TypedNode.Container(f.container))
            }
            coverage.validateCurrent()
            assertThat(coverage.snapshot()).isEqualTo(beforeCoverage)
            assertThat(db.hierarchyPlacementGroupScopeDao().getAll()).isEmpty()
            assertThat(db.orientationDao().getAllManagedSubjects()).isEqualTo(subjects)
            assertThat(db.orientationDao().getAllOrientationRelations()).isEmpty()
            assertThat(db.orientationDao().getAllAspectOrientationRefs()).isEmpty()
            assertThat(db.orientationDao().getAllWorkspaceBindings()).isEmpty()
            assertThat(db.planningDao().getAllCommitments()).isEmpty()
        } finally { db.close() }
    }

    @Test fun `unknown real root stays blocked and complete move rolls back with exact diagnostic`() = runBlocking {
        val db = database()
        try {
            val f = fixture(db)
            val before = db.hierarchyPlacementDao().getAll()
            val presentation = BigPicturePresentationRepository(db).snapshot()
            val failure = runCatching { H1StructuralMutationCoordinator(db).moveNode(H1TypedNode.Occurrence(f.primary.value), H1StructuralParent.Root) }
            assertThat(failure.exceptionOrNull()?.message).contains("placementId=${f.primary.value}")
            assertThat(db.hierarchyPlacementDao().getAll()).isEqualTo(before)
            assertThat(BigPicturePresentationRepository(db).snapshot()).isEqualTo(presentation)
        } finally { db.close() }
    }

    private suspend fun snapshot(db: AppDatabase) = SnapshotBundle(
        managedSubjects = db.orientationDao().getAllManagedSubjects(), orientations = db.orientationDao().getAllOrientations(),
        hierarchyPlacements = CanonicalHierarchyPlacementSyncStore(db).loadAll(),
        hierarchyPlacementGroupScopes = emptyList(), hierarchyPlacementLinkedAppearances = emptyList(),
        groupScopeCoverage = GroupScopeCoverageStore(db).snapshot(), bigPicturePresentation = BigPicturePresentationRepository(db).snapshot())

    private suspend fun installTargets(db: AppDatabase, bundle: SnapshotBundle) {
        db.workspaceDao().upsert(listOf(WorkspaceEntity("workspace", "Workspace", null, null, 1, 1, null, false, 1,
            provenance = WorkspaceProvenance.CANONICAL_ONLY.name, sourceContextId = null)))
        db.orientationDao().upsertManagedSubjects(bundle.managedSubjects.orEmpty())
        db.orientationDao().upsertOrientations(bundle.orientations.orEmpty())
    }

    @Test fun `exact Restore validates incoming container evidence not destination old state`() = runBlocking {
        val source = database(); val restored = database()
        try {
            val f = fixture(source)
            H1StructuralMutationCoordinator(source).moveNode(H1TypedNode.Occurrence(f.primary.value),
                H1StructuralParent.Node(H1TypedNode.Container(f.container)))
            val bundle = snapshot(source)
            val placements = source.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() }
            val scopes = CanonicalHierarchyPlacementGroupScopeSyncStore(restored)
            val decoded = scopes.decodeAndValidateForRestore(bundle, emptyList(), placements, true)
            restored.withTransaction {
                installTargets(restored, bundle)
                CanonicalHierarchyPlacementSyncStore(restored).restoreExactDecoded(placements)
                GroupScopeCoverageStore(restored).restoreExact(bundle.groupScopeCoverage!!)
                BigPicturePresentationRepository(restored).restoreExact(bundle.bigPicturePresentation!!)
                scopes.restoreExactDecoded(decoded, true)
                GroupScopeCoverageStore(restored).validateCurrent()
            }
            assertThat(GroupScopeCoverageStore(restored).snapshot()).isEqualTo(bundle.groupScopeCoverage)
            assertThat(BigPicturePresentationRepository(restored).snapshot()).isEqualTo(bundle.bigPicturePresentation)
            val missingEvidence = bundle.copy(bigPicturePresentation = bundle.bigPicturePresentation!!.copy(memberships = emptyList()))
            assertThat(runCatching { scopes.decodeAndValidateForRestore(missingEvidence, emptyList(), placements, true) }.isFailure).isTrue()
            val deletedContainer = bundle.copy(bigPicturePresentation = bundle.bigPicturePresentation!!.let { p -> p.copy(
                containers = p.containers.map { if (it.id == f.container) it.copy(isDeleted = true) else it }) })
            assertThat(runCatching { scopes.decodeAndValidateForRestore(deletedContainer, emptyList(), placements, true) }.isFailure).isTrue()
        } finally { source.close(); restored.close() }
    }

    @Test fun `production full and selective Merge preserve unknown container placement idempotently`() = runBlocking {
        val source = database()
        try {
            val f = fixture(source)
            H1StructuralMutationCoordinator(source).moveNode(H1TypedNode.Occurrence(f.link.value),
                H1StructuralParent.Node(H1TypedNode.Container(f.container)))
            val bundle = snapshot(source)
            listOf(false, true).forEach { selective ->
                val target = database()
                try {
                    installTargets(target, bundle)
                    admitCanonicalHierarchyStreams(target, bundle, selective)
                    val before = target.hierarchyPlacementDao().getAll()
                    admitCanonicalHierarchyStreams(target, bundle, selective)
                    assertThat(target.hierarchyPlacementDao().getAll()).isEqualTo(before)
                    assertThat(GroupScopeCoverageStore(target).snapshot().records.map { it.mode }).containsExactly("LEGACY_UNKNOWN", "LEGACY_UNKNOWN")
                    assertThat(target.bigPicturePresentationDao().liveContainerMemberIds()).contains(f.link.value)
                } finally { target.close() }
            }
        } finally { source.close() }
    }

    @Test fun `invalid unknown root Merge rolls all structural streams back`() = runBlocking {
        val source = database(); val target = database()
        try {
            val f = fixture(source)
            H1StructuralMutationCoordinator(source).moveNode(H1TypedNode.Occurrence(f.primary.value),
                H1StructuralParent.Node(H1TypedNode.Container(f.container)))
            val bundle = snapshot(source).let { b -> b.copy(bigPicturePresentation = b.bigPicturePresentation!!.copy(memberships = emptyList())) }
            installTargets(target, bundle)
            assertThat(runCatching { admitCanonicalHierarchyStreams(target, bundle, false) }.isFailure).isTrue()
            assertThat(target.hierarchyPlacementDao().getAll()).isEmpty()
            assertThat(GroupScopeCoverageStore(target).snapshot().records).isEmpty()
            assertThat(BigPicturePresentationRepository(target).snapshot().containers).isEmpty()
        } finally { source.close(); target.close() }
    }

    @Test fun `production reader observes container move and cold reload without synthetic GroupScope`() = runBlocking {
        val db = database()
        try {
            val f = fixture(db)
            val projector = mockk<SystemWorkspacePresentationContextProjector>()
            coEvery { projector.projectPresentationUniverse() } returns listOf(ContextPresentation("workspace", "Workspace", null, null, null, 0, emptyList()))
            val reader = CanonicalV2ReactiveHierarchyReadSource(db, projector, CanonicalV2HierarchyReadSnapshotAssembler())
            val started = CompletableDeferred<Unit>()
            val moved = async(Dispatchers.Default) { withTimeout(10_000) { reader.observe().onEach { started.complete(Unit) }
                .first { frame -> frame.hierarchy.occurrences.any { it.placementId == f.primary && it.parentPlacementId == null } } } }
            started.await()
            H1StructuralMutationCoordinator(db).moveNode(H1TypedNode.Occurrence(f.primary.value),
                H1StructuralParent.Node(H1TypedNode.Container(f.container)))
            assertThat(moved.await().syntheticScopeFor(f.primary)).isNull()
            val cold = withTimeout(10_000) { reader.observe().first() }
            assertThat(cold.hierarchy.occurrences.map { it.placementId }).containsAtLeast(f.primary, f.link)
            assertThat(cold.syntheticScopeFor(f.primary)).isNull()
        } finally { db.close() }
    }
}
