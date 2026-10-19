package com.romankozak.forwardappmobile.data.orientation

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementSyncStore
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalSemanticDeletionTransportRoomTest {
    @Test fun `Orientation and Aspect tombstones roundtrip JSON exact Restore and peer Merge with H1 closure`() = runBlocking {
        SemanticDeletionRoomFixture().use { source ->
            source.workspace()
            val orientation = source.orientation()
            val aspect = source.aspect()
            val section = source.h1.createContainer("Section", H1StructuralParent.Root, now = 15)
            source.appearance(orientation)
            source.appearance(orientation, H1StructuralParent.Node(H1TypedNode.Container(section)))
            source.appearance(aspect)
            source.bindings.bind(orientation, "workspace", WorkspaceBindingType.SUPPORTS, 25)
            source.links.linkOrientation(aspect, orientation, AspectOrientationRelationType.BELONGS_TO, true, 25)
            source.delete(orientation)
            source.delete(aspect, ManagedSubjectType.ASPECT)
            val bundle = Gson().fromJson(Gson().toJson(source.snapshot()), SnapshotBundle::class.java)
            for (merge in listOf(false, true)) SemanticDeletionRoomFixture().use { target ->
                target.admit(bundle, merge)
                assertThat(target.dao.getAllManagedSubjects().all { it.isDeleted }).isTrue()
                assertThat(target.dao.getAllWorkspaceBindings().single().isDeleted).isTrue()
                assertThat(target.dao.getAllAspectOrientationRefs().single().isDeleted).isTrue()
                assertThat(target.db.hierarchyPlacementDao().getAll().all { it.isDeleted }).isTrue()
                assertThat(target.db.bigPicturePresentationDao().containers().single().record.isDeleted).isFalse()
                assertThat(target.db.workspaceDao().getById("workspace")!!.isDeleted).isFalse()
            }
        }
    }

    @Test fun `older live peer cannot resurrect deleted identity or retired edges and appearances`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val id = f.orientation()
            f.appearance(id)
            f.bindings.bind(id, "workspace", WorkspaceBindingType.SUPPORTS, 25)
            val old = f.snapshot()
            f.delete(id)
            val before = f.rows()
            f.admit(old, merge = true)
            assertThat(f.rows()).isEqualTo(before)
        }
    }

    @Test fun `equal version live payload with future wall clock cannot resurrect semantic tombstones`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val id = f.orientation()
            val aspect = f.aspect()
            val other = f.orientation()
            f.bindings.bind(id, "workspace", WorkspaceBindingType.SUPPORTS, 25)
            f.links.linkOrientation(aspect, id, AspectOrientationRelationType.BELONGS_TO, true, 25)
            f.graph.addRelation(id, other, OrientationRelationType.SUPPORTS, 25)
            val live = f.snapshot()
            f.delete(id)
            val before = f.rows()
            f.admit(live.copy(
                managedSubjects = live.managedSubjects!!.map { if (it.id == id) it.copy(version = 2, updatedAt = 99999) else it },
                orientationAssessments = live.orientationAssessments!!.map { if (it.orientationId == id) it.copy(version = 2, updatedAt = 99999) else it },
                orientationRelations = live.orientationRelations!!.map { it.copy(version = 2, updatedAt = 99999) },
                aspectOrientationRefs = live.aspectOrientationRefs!!.map { it.copy(version = 2, updatedAt = 99999) },
                workspaceBindings = live.workspaceBindings!!.map { it.copy(version = 2, updatedAt = 99999) },
            ), merge = true)
            assertThat(f.rows()).isEqualTo(before)
        }
    }

    @Test fun `peer new binding to locally deleted subject fails prospective admission and rolls back`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val id = f.orientation()
            f.bindings.bind(id, "workspace", WorkspaceBindingType.SUPPORTS, 25)
            val old = f.snapshot()
            f.delete(id)
            val before = f.rows()
            val freshEdge = old.workspaceBindings!!.single().copy(id = "new-peer-edge", bindingType = "MONITORS")
            assertThat(runCatching { f.admit(old.copy(workspaceBindings = listOf(freshEdge)), merge = true) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
        }
    }

    @Test fun `payload and selective-style subset cannot admit live orphan or deleted endpoint relation`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val id = f.orientation()
            f.bindings.bind(id, "workspace", WorkspaceBindingType.SUPPORTS, 25)
            val live = f.snapshot()
            val deleted = live.copy(managedSubjects = live.managedSubjects!!.map { it.copy(isDeleted = true, version = 2) })
            val missing = live.copy(managedSubjects = emptyList())
            for (invalid in listOf(deleted, missing)) {
                val before = f.rows()
                assertThat(runCatching { f.admit(invalid, merge = true) }.isFailure).isTrue()
                assertThat(f.rows()).isEqualTo(before)
            }
        }
    }

    @Test fun `stale semantic ACK cannot acknowledge newer deletion or relation retirement`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val id = f.orientation()
            val binding = f.bindings.bind(id, "workspace", WorkspaceBindingType.SUPPORTS, 25)
            f.delete(id)
            f.dao.markSubjectSynced(id, 1, 200)
            f.dao.markAssessmentSynced(id, 1, 200)
            f.dao.markWorkspaceBindingSynced(binding, 1, 200)
            assertThat(f.dao.getManagedSubject(id)!!.syncedAt).isNull()
            assertThat(f.dao.getAllAssessments().single().syncedAt).isNull()
            assertThat(f.dao.getAllWorkspaceBindings().single().syncedAt).isNull()
            f.dao.markSubjectSynced(id, 2, 201)
            assertThat(f.dao.getManagedSubject(id)!!.syncedAt).isEqualTo(201)
            assertThat(CanonicalHierarchyPlacementSyncStore(f.db).loadUnsynced()).isEmpty()
        }
    }

    @Test fun `actual Workspace selective import preserves live root without reviving deleted semantic child`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { source ->
            source.workspace()
            val root = source.h1.addAppearance(
                com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef(
                    com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType.WORKSPACE, "workspace"),
                H1StructuralParent.Root, now = 20)
            val id = source.orientation()
            source.appearance(id, H1StructuralParent.Node(H1TypedNode.Occurrence(root.value)))
            source.delete(id)
            val selected = com.romankozak.forwardappmobile.sync.SnapshotBundleSelectiveImportFilter().filter(source.snapshot(),
                com.romankozak.forwardappmobile.shared.contracts.contexts.WorkspaceSelectiveImportSelection(
                    selectedContextIds = setOf("workspace")))
            SemanticDeletionRoomFixture().use { target ->
                target.admit(selected, merge = true)
                assertThat(target.dao.getManagedSubject(id)?.isDeleted ?: true).isTrue()
                assertThat(target.db.hierarchyPlacementDao().getAll().filterNot { it.isDeleted }.map { it.targetId })
                    .containsExactly("workspace")
            }
        }
    }
}
