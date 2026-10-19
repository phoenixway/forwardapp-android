package com.romankozak.forwardappmobile.data.orientation

import com.google.common.truth.Truth.assertThat
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity
import com.romankozak.forwardappmobile.data.hierarchy.container.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalSemanticEntityLifecycleRoomTest {
    @Test fun `ordinary Orientation tombstones identity current assessment and every exact appearance`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation()
            val container = f.h1.createContainer("Section", H1StructuralParent.Root, now = 15)
            val first = f.appearance(id)
            f.appearance(id, H1StructuralParent.Node(H1TypedNode.Container(container)))
            f.appearance(id, H1StructuralParent.Node(H1TypedNode.Occurrence(first.value)))
            val containers = f.db.bigPicturePresentationDao().containers()
            f.delete(id)
            val subject = f.dao.getManagedSubject(id)!!
            assertThat(subject.isDeleted).isTrue()
            assertThat(subject.version).isEqualTo(2)
            assertThat(subject.syncedAt).isNull()
            assertThat(f.dao.getAllOrientations()).hasSize(1)
            assertThat(f.dao.getAllAssessments().single().isDeleted).isTrue()
            assertThat(f.dao.getAllAssessmentRevisions().single().isDeleted).isFalse()
            assertThat(f.db.hierarchyPlacementDao().getAll()).hasSize(3)
            assertThat(f.db.hierarchyPlacementDao().getAll().all { it.isDeleted && it.syncedAt == null && it.version >= 2 }).isTrue()
            assertThat(f.db.bigPicturePresentationDao().memberships().all { it.record.isDeleted }).isTrue()
            assertThat(f.db.bigPicturePresentationDao().containers()).isEqualTo(containers)
        }
    }

    @Test fun `Aspect deletion preserves subtype promotes taxonomy children and preserves other targets`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.aspect()
            val child = f.aspects.create("Child", parentAspectId = id, now = 15)
            f.appearance(id)
            val other = f.orientation()
            f.delete(id, ManagedSubjectType.ASPECT)
            assertThat(f.dao.getManagedSubject(id)!!.isDeleted).isTrue()
            assertThat(f.dao.getAspect(id)).isNotNull()
            assertThat(f.dao.getAspect(child)!!.parentAspectId).isNull()
            assertThat(f.dao.getManagedSubject(child)!!.isDeleted).isFalse()
            assertThat(f.dao.getManagedSubject(other)!!.isDeleted).isFalse()
        }
    }

    @Test fun `incident graph associations and bindings tombstone while Workspace and unrelated edges survive`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val id = f.orientation()
            val other = f.orientation()
            val third = f.orientation()
            val aspect = f.aspect()
            f.graph.addRelation(id, other, OrientationRelationType.SUPPORTS, 20)
            f.graph.addRelation(other, id, OrientationRelationType.DEPENDS_ON, 20)
            val surviving = f.graph.addRelation(other, third, OrientationRelationType.SUPPORTS, 20)
            f.links.linkOrientation(aspect, id, AspectOrientationRelationType.BELONGS_TO, true, 20)
            f.bindings.bind(id, "workspace", WorkspaceBindingType.EMBODIES, 20)
            val workspace = f.db.workspaceDao().getById("workspace")
            f.delete(id)
            assertThat(f.dao.getAllOrientationRelations().filterNot { it.isDeleted }.map { it.id }).containsExactly(surviving)
            assertThat(f.dao.getAllAspectOrientationRefs().single().isDeleted).isTrue()
            assertThat(f.dao.getAllWorkspaceBindings().single().isDeleted).isTrue()
            assertThat(f.db.workspaceDao().getById("workspace")).isEqualTo(workspace)
        }
    }

    @Test fun `Aspect incident bindings and associations retire through same generic entry`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val aspect = f.aspect()
            val id = f.orientation()
            f.links.linkOrientation(aspect, id, AspectOrientationRelationType.RELEVANT_TO, false, 20)
            f.bindings.bind(aspect, "workspace", WorkspaceBindingType.SUPPORTS, 20)
            f.delete(aspect, ManagedSubjectType.ASPECT)
            assertThat(f.dao.getAllAspectOrientationRefs().all { it.isDeleted }).isTrue()
            assertThat(f.dao.getAllWorkspaceBindings().all { it.isDeleted }).isTrue()
            assertThat(f.dao.getManagedSubject(id)!!.isDeleted).isFalse()
        }
    }

    @Test fun `every specialized or unknown Orientation kind fails closed`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            OrientationKind.entries.filterNot { it in genericDeletableOrientationKinds }.forEach { kind ->
                val id = f.orientation(kind)
                val before = f.rows()
                assertThat(runCatching { f.delete(id) }.isFailure).isTrue()
                assertThat(f.rows()).isEqualTo(before)
            }
            val id = f.orientation()
            f.dao.upsertOrientations(listOf(f.dao.getAllOrientations().single { it.subjectId == id }.copy(kind = "FUTURE_KIND")))
            val before = f.rows()
            assertThat(runCatching { f.delete(id) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
        }
    }

    @Test fun `all admitted ordinary kinds use generic lifecycle`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            genericDeletableOrientationKinds.forEach { kind ->
                val id = f.orientation(kind)
                f.delete(id)
                assertThat(f.dao.getManagedSubject(id)!!.isDeleted).isTrue()
            }
        }
    }

    @Test fun `legacy provenance class mismatch stale version and missing subject reject without writes`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation()
            assertThat(runCatching { f.delete(id, version = 0) }.isFailure).isTrue()
            assertThat(runCatching { f.delete(id, ManagedSubjectType.ASPECT) }.isFailure).isTrue()
            assertThat(runCatching { f.delete("missing") }.isFailure).isTrue()
            f.dao.upsertLegacyMappings(listOf(LegacySubjectMappingEntity("mapping", "CONTEXT", "legacy", id, 1,
                "CUT_OVER", 1, 1, null, false, 1)))
            val before = f.rows()
            assertThat(runCatching { f.delete(id) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
        }
    }

    @Test fun `non-leaf appearance including container child rejects entire target deletion`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation()
            val first = f.appearance(id)
            val link = f.appearance(id)
            f.h1.createContainer("Keep me", H1StructuralParent.Node(H1TypedNode.Occurrence(link.value)), now = 30)
            val before = f.rows()
            assertThat(runCatching { f.delete(id) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
            assertThat(f.db.hierarchyPlacementDao().getAll().single { it.id == first.value }.isDeleted).isFalse()
        }
    }

    @Test fun `late identity write failure rolls back placements memberships order edges and bindings`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val id = f.orientation()
            val aspect = f.aspect()
            val container = f.h1.createContainer("Section", H1StructuralParent.Root, now = 15)
            f.appearance(id, H1StructuralParent.Node(H1TypedNode.Container(container)))
            f.links.linkOrientation(aspect, id, AspectOrientationRelationType.BELONGS_TO, true, 20)
            f.bindings.bind(id, "workspace", WorkspaceBindingType.SUPPORTS, 20)
            f.db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_semantic_delete BEFORE UPDATE ON managed_subjects WHEN NEW.isDeleted=1 BEGIN SELECT RAISE(ABORT, 'late delete failure'); END")
            val before = f.rows()
            assertThat(runCatching { f.delete(id) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
        }
    }

    @Test fun `cancellation during relation cleanup rolls back structural changes`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val id = f.orientation()
            f.appearance(id)
            f.bindings.bind(id, "workspace", WorkspaceBindingType.SUPPORTS, 20)
            val interrupted = mockk<CanonicalWorkspaceBindingRepository>()
            coEvery { interrupted.unbind(any(), any()) } throws CancellationException("cancelled")
            val command = CanonicalSemanticEntityLifecycleCoordinator(f.db, f.orientations, f.aspects, f.graph,
                f.links, interrupted, f.hierarchy)
            val before = f.rows()
            assertThat(runCatching { command.tombstone(id, ManagedSubjectType.ORIENTATION, 1, 100) }.exceptionOrNull())
                .isInstanceOf(CancellationException::class.java)
            assertThat(f.rows()).isEqualTo(before)
        }
    }

    @Test fun `current tombstone replay is idempotent and stale caller cannot mutate it`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation()
            f.delete(id)
            val before = f.rows()
            f.delete(id, version = 2)
            assertThat(f.rows()).isEqualTo(before)
            assertThat(runCatching { f.delete(id) }.isFailure).isTrue()
        }
    }

    @Test fun `live Planning participation is a precondition failure not a semantic cascade`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation()
            f.appearance(id)
            f.db.planningDao().upsertScope(com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningScopeEntity(
                "scope", "DAY", "ACTIVE", null, null, null, 1, 1, null, 1, false))
            f.db.planningDao().upsertCommitment(com.romankozak.forwardappmobile.core.data.models.entities.planning.PlanningCommitmentEntity(
                "commitment", "scope", id, "FOCUS", 0, null, "PLANNED", "MANUAL", null, null, 1, 1, null, 1, false))
            val before = f.rows()
            assertThat(runCatching { f.delete(id) }.exceptionOrNull()!!.message).contains("Planning")
            assertThat(f.rows()).isEqualTo(before)
        }
    }

    @Test fun `same raw Workspace id is not a semantic target or affected appearance`() = runBlocking {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val id = f.orientation()
            f.db.workspaceDao().upsert(listOf(f.db.workspaceDao().getById("workspace")!!.copy(id = id)))
            val workspaceAppearance = f.h1.addAppearance(
                com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetRef(
                    com.romankozak.forwardappmobile.shared.core.domain.hierarchy.HierarchyTargetType.WORKSPACE, id),
                H1StructuralParent.Root, now = 20)
            f.appearance(id)
            f.delete(id)
            assertThat(f.db.workspaceDao().getById(id)!!.isDeleted).isFalse()
            assertThat(f.db.hierarchyPlacementDao().getAll().single { it.id == workspaceAppearance.value }.isDeleted).isFalse()
        }
    }
}
