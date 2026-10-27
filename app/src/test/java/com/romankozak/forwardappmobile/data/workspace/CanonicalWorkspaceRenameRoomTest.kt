package com.romankozak.forwardappmobile.data.workspace

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.data.hierarchy.CanonicalHierarchyPlacementRepository
import com.romankozak.forwardappmobile.data.hierarchy.container.H1StructuralParent
import com.romankozak.forwardappmobile.data.orientation.SemanticDeletionRoomFixture
import com.romankozak.forwardappmobile.shared.core.domain.hierarchy.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.WorkspaceBindingType
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalWorkspaceRenameRoomTest {
    @Test fun `rename all admitted Workspace provenances preserves other fields H1 relations and Context`() = runBlocking<Unit> {
        for (provenance in listOf("STANDALONE", "CANONICAL_ONLY", "CONTEXT_BACKED")) {
            SemanticDeletionRoomFixture().use { f ->
                f.workspace()
                f.db.contextDao().insert(Context(id = "legacy", name = "Legacy title", description = null,
                    parentId = null, createdAt = 1, updatedAt = 1))
                val seed = f.db.workspaceDao().getById("workspace")!!.copy(descriptionOverride = "Description",
                    roleCode = "aspect", provenance = provenance, sourceContextId = if (provenance == "CONTEXT_BACKED") "legacy" else null,
                    syncedAt = 40)
                f.db.workspaceDao().upsert(listOf(seed))
                val target = HierarchyTargetRef(HierarchyTargetType.WORKSPACE, "workspace")
                f.h1.addAppearance(target, H1StructuralParent.Root)
                f.h1.addAppearance(target, H1StructuralParent.Root)
                val aspect = f.aspect()
                f.bindings.bind(aspect, "workspace", WorkspaceBindingType.EMBODIES)
                val old = f.db.workspaceDao().getById("workspace")!!
                val before = f.rows()
                val repository = repository(f)
                repository.rename("workspace", "  New title  ", now = 100)
                val renamed = f.db.workspaceDao().getById("workspace")!!
                assertThat(renamed.nameOverride).isEqualTo("New title")
                assertThat(renamed.version).isEqualTo(old.version + 1)
                assertThat(renamed.syncedAt).isNull()
                assertThat(renamed.copy(nameOverride = old.nameOverride, version = old.version,
                    updatedAt = old.updatedAt, syncedAt = old.syncedAt)).isEqualTo(old)
                assertThat(repository.observeProperties("workspace").first()!!.name).isEqualTo("New title")
                val after = f.rows()
                before.filterKeys { it != "workspaces" }.forEach { (table, rows) ->
                    assertWithMessage(table).that(after[table]).isEqualTo(rows)
                }
                repository.rename("workspace", " New title ", now = 101)
                assertThat(f.rows()).isEqualTo(after)
            }
        }
    }

    @Test fun `blank missing and deleted targets reject with no writes`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val repository = repository(f)
            var before = f.rows()
            assertThat(runCatching { repository.rename("workspace", "  ") }.isFailure).isTrue()
            assertThat(runCatching { repository.rename("missing", "Name") }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
            f.db.workspaceDao().upsert(listOf(f.db.workspaceDao().getById("workspace")!!.copy(isDeleted = true)))
            before = f.rows()
            assertThat(runCatching { repository.rename("workspace", "Name") }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
        }
    }

    private fun repository(f: SemanticDeletionRoomFixture) = CanonicalWorkspaceRepository(
        database = f.db, workspaceDao = f.db.workspaceDao(), orientationDao = f.dao,
        executionLogRepository = mockk(), keyProblemsRepository = mockk(), directionRepository = mockk(),
        inboxRepository = mockk(), connectionsRepository = mockk(), backlogRepository = mockk(),
        hierarchyPlacementLifecycleCoordinator = f.hierarchy,
        hierarchyPlacementRepository = CanonicalHierarchyPlacementRepository(f.db), h1StructuralMutationCoordinator = f.h1)
}
