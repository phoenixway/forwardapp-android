package com.romankozak.forwardappmobile.data.hierarchy

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.data.orientation.SemanticDeletionRoomFixture
import com.romankozak.forwardappmobile.data.workspace.CanonicalWorkspaceTagRepository
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspacePresentationContextProjector
import com.romankozak.forwardappmobile.data.workspace.SystemWorkspaceTagAuthority
import com.romankozak.forwardappmobile.data.workspace.workspaceDeletionRepository
import com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralParent
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.OrientationHierarchyNode
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType
import com.romankozak.forwardappmobile.shared.core.models.orientation.ManagedSubjectType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Real production reader + projector + lifecycle, including the background screen adapter. */
@RunWith(RobolectricTestRunner::class)
class CanonicalV2SourceDeletionReactiveReadRoomTest {
    private fun reader(f: SemanticDeletionRoomFixture): CanonicalV2ReactiveHierarchyReadSource {
        val db = f.db
        val tags = CanonicalWorkspaceTagRepository(db)
        val projector = SystemWorkspacePresentationContextProjector(db.workspaceDao(),
            SystemWorkspaceTagAuthority(db.workspaceDao(), db.workspaceTagRefDao(), db.systemWorkspaceTagSeedStateDao(), tags),
            tags, db.contextDao())
        return CanonicalV2ReactiveHierarchyReadSource(db, projector, CanonicalV2HierarchyReadSnapshotAssembler())
    }

    private fun render(read: CanonicalV2ProductionHierarchyRead) =
        CanonicalV2HierarchyScreenPresentationAdapter().adapt(read,
            CanonicalV2HierarchyScreenMetadataAssembler().assemble(read, read.workspacePresentationsByTargetId.values, emptyList()))

    @Test fun `Workspace PRIMARY and LINK source deletion keeps background reader coherent live and cold`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val workspace = f.db.workspaceDao().getById("workspace")!!
            f.db.workspaceDao().upsert(listOf(workspace.copy(id = "survivor")))
            listOf("workspace", "workspace", "survivor").forEach { id ->
                f.h1.addAppearance(HierarchyTargetRef(HierarchyTargetType.WORKSPACE, id), H1StructuralParent.Root, now = 20)
            }
            val source = reader(f)
            val started = CompletableDeferred<Unit>()
            val deleted = async(Dispatchers.Default) {
                withTimeout(10_000) { source.observe().onEach { frame ->
                    render(frame) // Old adapter crash occurred in this background collector.
                    frame.workspaceOccurrences().forEach { occurrence ->
                        assertThat(frame.workspacePresentationsByTargetId[occurrence.target.id]).isNotNull()
                    }
                    started.complete(Unit)
                }.first { it.workspaceOccurrences("workspace").isEmpty() } }
            }
            withTimeout(10_000) { started.await() }
            workspaceDeletionRepository(f.db).tombstone("workspace", now = 100, expectedVersion = 1)
            val frame = deleted.await()
            assertThat(frame.workspacePresentationsByTargetId.keys).containsExactly("survivor")
            assertThat(render(frame).mapNotNull { (it.node as? OrientationHierarchyNode.WorkspaceNode)?.id })
                .containsExactly("survivor")
            val beforeRead = f.rows()
            val cold = withTimeout(10_000) { source.observe().first() }
            assertThat(cold).isEqualTo(frame)
            assertThat(render(cold).mapNotNull { it.node.placementId }).hasSize(1)
            assertThat(f.rows()).isEqualTo(beforeRead)
        }
    }

    @Test fun `semantic source deletion cannot mix tombstoned identity with old live occurrences`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val orientation = f.orientation()
            val aspect = f.aspect()
            f.appearance(orientation)
            f.appearance(orientation)
            f.appearance(aspect)
            val source = reader(f)
            for ((id, type) in listOf(orientation to ManagedSubjectType.ORIENTATION, aspect to ManagedSubjectType.ASPECT)) {
                val started = CompletableDeferred<Unit>()
                val deleted = async(Dispatchers.Default) {
                    withTimeout(10_000) { source.observe().onEach {
                        render(it)
                        started.complete(Unit)
                    }.first { frame -> frame.hierarchy.occurrences.none { it.target.id == id } } }
                }
                withTimeout(10_000) { started.await() }
                f.delete(id, type)
                assertThat(deleted.await().hierarchy.occurrences.map { it.target.id }).doesNotContain(id)
            }
            assertThat(withTimeout(10_000) { source.observe().first() }.hierarchy.occurrences).isEmpty()
        }
    }

    @Test fun `Workspace rename updates frame metadata and target title together without changing old frame`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            f.h1.addAppearance(HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "workspace"), H1StructuralParent.Root, now = 20)
            val source = reader(f)
            val old = withTimeout(10_000) { source.observe().first() }
            val current = f.db.workspaceDao().getById("workspace")!!
            f.db.workspaceDao().upsert(listOf(current.copy(nameOverride = "Renamed", version = 2, updatedAt = 30)))
            val renamed = withTimeout(10_000) { source.observe().first { it.workspacePresentationsByTargetId["workspace"]?.name == "Renamed" } }
            assertThat(renamed.workspaceOccurrences("workspace").single().title).isEqualTo("Renamed")
            assertThat(render(renamed).mapNotNull { (it.node as? OrientationHierarchyNode.WorkspaceNode)?.presentation?.name })
                .containsExactly("Renamed")
            assertThat(old.workspacePresentationsByTargetId.getValue("workspace").name).isEqualTo("Workspace")
        }
    }

    @Test fun `persisted dangling Workspace target still fails closed instead of being hidden`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            f.h1.addAppearance(HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "workspace"), H1StructuralParent.Root, now = 20)
            val current = f.db.workspaceDao().getById("workspace")!!
            // Intentionally bypass lifecycle in the fixture to prove validation stays strict.
            f.db.workspaceDao().upsert(listOf(current.copy(isDeleted = true)))
            val failure = runCatching { withTimeout(10_000) { reader(f).observe().first() } }.exceptionOrNull()
            assertThat(failure).isInstanceOf(CanonicalV2HierarchyProjectionException::class.java)
        }
    }
}
