package com.romankozak.forwardappmobile.data.workspace

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.data.orientation.SemanticDeletionRoomFixture
import com.romankozak.forwardappmobile.data.workspace.capability.*
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalWorkspaceSourceDeletionRoomTest {
    @Test fun `Workspace delete retires owned Inbox bindings all typed appearances and memberships while identities survive`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val instances = CanonicalCapabilityInstanceStore(f.db, f.db.workspaceDao(), f.dao)
            val inbox = CanonicalInboxRepository(f.db, instances, f.db.workspaceInboxRecordDao())
            inbox.enable("workspace", 10)
            val record = inbox.createRecord("workspace", "Operational content", 11)
            val subject = f.orientation()
            f.bindings.bind(subject, "workspace", WorkspaceBindingType.EMBODIES, 12)
            val section = f.h1.createContainer("Section", H1StructuralParent.Root, now = 13)
            f.h1.addAppearance(target(), H1StructuralParent.Root, now = 20)
            f.h1.addAppearance(target(), H1StructuralParent.Node(H1TypedNode.Container(section)), now = 21)
            workspaceDeletionRepository(f.db).tombstone("workspace", 100, expectedVersion = 1)
            assertThat(f.db.workspaceInboxRecordDao().getById(record)!!.isDeleted).isTrue()
            assertThat(f.dao.getAllWorkspaceCapabilities().single().isDeleted).isTrue()
            assertThat(f.dao.getAllWorkspaceBindings().single().isDeleted).isTrue()
            assertThat(f.dao.getManagedSubject(subject)!!.isDeleted).isFalse()
            assertThat(f.db.hierarchyPlacementDao().getAll().all { it.isDeleted }).isTrue()
            assertThat(f.db.bigPicturePresentationDao().memberships().single().record.isDeleted).isTrue()
            assertThat(f.db.bigPicturePresentationDao().containers().single().record.isDeleted).isFalse()
        }
    }

    @Test fun `late Workspace failure rolls back capability content H1 memberships and bindings`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val inbox = CanonicalInboxRepository(f.db, CanonicalCapabilityInstanceStore(f.db, f.db.workspaceDao(), f.dao), f.db.workspaceInboxRecordDao())
            inbox.enable("workspace", 10)
            inbox.createRecord("workspace", "Keep", 11)
            f.bindings.bind(f.orientation(), "workspace", WorkspaceBindingType.SUPPORTS, 12)
            val section = f.h1.createContainer("Section", H1StructuralParent.Root, now = 13)
            f.h1.addAppearance(target(), H1StructuralParent.Node(H1TypedNode.Container(section)), now = 20)
            f.db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_workspace_delete BEFORE UPDATE ON workspaces WHEN NEW.isDeleted=1 BEGIN SELECT RAISE(ABORT, 'late Workspace failure'); END")
            val before = f.rows()
            assertThat(runCatching { workspaceDeletionRepository(f.db).tombstone("workspace", 100, 1) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
        }
    }

    @Test fun `stale confirmation and unrelated typed children reject without partial Workspace changes`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val parent = f.h1.addAppearance(target(), H1StructuralParent.Root, now = 20)
            f.h1.createContainer("Keep section", H1StructuralParent.Node(H1TypedNode.Occurrence(parent.value)), now = 21)
            val before = f.rows()
            assertThat(runCatching { workspaceDeletionRepository(f.db).tombstone("workspace", 100, 0) }.isFailure).isTrue()
            assertThat(runCatching { workspaceDeletionRepository(f.db).tombstone("workspace", 100, 1) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
        }
    }

    @Test fun `Workspace source tombstone survives normal JSON Restore Merge and stale peer replay`() = runBlocking {
        SemanticDeletionRoomFixture().use { source ->
            source.workspace()
            source.h1.addAppearance(target(), H1StructuralParent.Root, now = 20)
            val old = source.snapshot()
            workspaceDeletionRepository(source.db).tombstone("workspace", 100, 1)
            val deleted = Gson().fromJson(Gson().toJson(source.snapshot()), SnapshotBundle::class.java)
            for (merge in listOf(false, true)) SemanticDeletionRoomFixture().use { restored ->
                restored.admit(deleted, merge)
                restored.admit(old, merge = true)
                assertThat(restored.db.workspaceDao().getById("workspace")!!.isDeleted).isTrue()
                assertThat(restored.db.hierarchyPlacementDao().getAll().all { it.isDeleted }).isTrue()
            }
        }
    }

    private fun target() = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "workspace")
}
