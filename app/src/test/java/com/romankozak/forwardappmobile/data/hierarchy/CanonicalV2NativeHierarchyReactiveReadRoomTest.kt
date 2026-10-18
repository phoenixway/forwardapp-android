package com.romankozak.forwardappmobile.data.hierarchy

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.data.orientation.CanonicalAspectRepository
import com.romankozak.forwardappmobile.data.orientation.CanonicalOrientationRepository
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.database.AppDatabase
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Exercises the production reader that remains subscribed outside Big Picture. */
@RunWith(RobolectricTestRunner::class)
class CanonicalV2NativeHierarchyReactiveReadRoomTest {
    private fun database() = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java,
    ).allowMainThreadQueries().build()

    private fun reader(db: AppDatabase): CanonicalV2ReactiveHierarchyReadSource {
        val workspaces = mockk<SystemWorkspacePresentationContextProjector>()
        every { workspaces.observePresentationUniverse() } returns flowOf(emptyList())
        return CanonicalV2ReactiveHierarchyReadSource(db, workspaces, CanonicalV2HierarchyReadSnapshotAssembler())
    }

    @Test fun `native Orientation and Aspect Add updates subscribed reader and survives cold startup read`() = runBlocking<Unit> {
        val db = database()
        try {
            val o = CanonicalOrientationRepository(db, db.orientationDao())
                .createOrientation(OrientationKind.INTENT, "Native O", now = 1)
            val a = CanonicalAspectRepository(db, db.orientationDao(), HierarchyPlacementLifecycleCoordinator(db))
                .create("Native A", now = 2)
            val started = CompletableDeferred<Unit>()
            val updated = async(Dispatchers.Default) {
                withTimeout(10_000) {
                    reader(db).observe().onEach { started.complete(Unit) }
                        .first { it.hierarchy.occurrences.size == 2 }
                }
            }
            withTimeout(10_000) { started.await() }
            val h1 = H1StructuralMutationCoordinator(db)
            listOf(o, a).forEach {
                h1.addAppearance(HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT, it), H1StructuralParent.Root)
            }
            assertThat(updated.await().hierarchy.rootPlacementIds).hasSize(2)
            val cold = withTimeout(10_000) { reader(db).observe().first() }
            assertThat(cold.hierarchy.occurrences.map { it.target.id }).containsExactly(o, a)
            assertThat(CanonicalV2HierarchyConsumerReadiness().coreLevelOccurrences(cold)).isEmpty()
            assertThat(CanonicalV2HierarchyScreenPresentationAdapter().adapt(cold,
                CanonicalV2HierarchyScreenMetadata(workspacePresentationsByTargetId = emptyMap()))).isEmpty()
            cold.hierarchy.rootPlacementIds.forEach { id ->
                assertThat(cold.syntheticScopeFor(id)).isNull()
                assertThat(cold.breadcrumbsToOccurrence(id).map { it.placementId }).containsExactly(id)
            }
            assertThat(db.hierarchyPlacementGroupScopeDao().getAll()).isEmpty()
            assertThat(db.groupScopeApplicabilityDao().all().map { it.mode }).containsExactly("NATIVE", "NATIVE")
        } finally { db.close() }
    }

    @Test fun `retired historical scope is read after generic nesting container move and return to root`() = runBlocking {
        val db = database()
        try {
            val subjects = CanonicalOrientationRepository(db, db.orientationDao())
            val h1 = H1StructuralMutationCoordinator(db)
            val source = h1.addAppearance(HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT,
                subjects.createOrientation(OrientationKind.INTENT, "Historical", now = 1)), H1StructuralParent.Root)
            val parent = h1.addAppearance(HierarchyTargetRef(HierarchyTargetType.MANAGED_SUBJECT,
                subjects.createOrientation(OrientationKind.INTENT, "Native parent", now = 2)), H1StructuralParent.Root)
            HierarchyPlacementGroupScopeMutationCoordinator(db).setRootScope(source, null, now = 10)
            assertThat(withTimeout(10_000) { reader(db).observe().first() }.hierarchy.occurrences).hasSize(2)
            val container = h1.createContainer("WIP", H1StructuralParent.Root)
            val destinations = listOf(
                H1StructuralParent.Node(H1TypedNode.Occurrence(parent.value)),
                H1StructuralParent.Node(H1TypedNode.Container(container)), H1StructuralParent.Root,
            )
            val initiallyScoped = withTimeout(10_000) { reader(db).observe().first() }
            assertThat(initiallyScoped.syntheticScopeFor(source)).isNotNull()
            // The native root follows a historical NoGroup section in the flat DTO;
            // it must not accidentally inherit that section or its breadcrumbs.
            assertThat(initiallyScoped.syntheticScopeFor(parent)).isNull()
            destinations.forEach { destination ->
                h1.moveNode(H1TypedNode.Occurrence(source.value), destination)
                val frame = withTimeout(10_000) { reader(db).observe().first() }
                assertThat(frame.hierarchy.occurrences.map { it.placementId }).contains(source)
                assertThat(frame.syntheticScopeFor(source)).isNull()
                assertThat(CanonicalV2HierarchyConsumerReadiness().coreLevelOccurrences(frame)).isEmpty()
                assertThat(frame.breadcrumbsToOccurrence(source).mapNotNull { it.placementId }).contains(source)
                assertThat(db.hierarchyPlacementGroupScopeDao().getAll().single().isDeleted).isTrue()
            }
        } finally { db.close() }
    }
}
