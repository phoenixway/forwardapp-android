package com.romankozak.forwardappmobile.data.orientation

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.navigation.NavTarget
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.hierarchy.toHierarchyPlacementStrict
import com.romankozak.forwardappmobile.features.contexts.ui.context_hierarchy_screen.models.*
import com.romankozak.forwardappmobile.features.entitybrowser.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import com.romankozak.forwardappmobile.shared.core.domain.orientation.initialOrientationAssessment
import com.romankozak.forwardappmobile.shared.core.domain.orientation.calculateOrientationScore
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalProjectOrientationRoomTest {
    private fun catalog(f: SemanticDeletionRoomFixture) = CanonicalEntityCatalogRepository(f.db.canonicalEntityCatalogDao())

    @Test fun `new vocabulary never reclassifies old identities and unknown ingress still fails closed`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace()
            val existing = f.orientation(OrientationKind.INTENT)
            val oldWorkspaces = f.db.workspaceDao().getAll()
            f.orientation(OrientationKind.PROJECT)
            assertThat(f.dao.getOrientation(existing)!!.kind).isEqualTo("INTENT")
            assertThat(f.db.workspaceDao().getAll()).isEqualTo(oldWorkspaces)
            val payload = f.snapshot()
            val unknown = payload.copy(orientations = payload.orientations!!.map { it.copy(kind = "FUTURE_KIND") })
            for (merge in listOf(false, true)) SemanticDeletionRoomFixture().use { target ->
                val before = target.rows()
                assertThat(runCatching { target.admit(unknown, merge) }.isFailure).isTrue()
                assertThat(target.rows()).isEqualTo(before)
            }
        }
    }

    @Test fun `Project canonical assessment and REALIZED lifecycle persist through normal aggregate authority`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation(OrientationKind.PROJECT)
            val subject = f.orientations.getOrientationSemantics(id)!!.subject
            val assessment = initialOrientationAssessment(OrientationKind.PROJECT).copy(
                importance = AxisAssessment("HIGH", ValueOrigin.EXPLICIT), impact = AxisAssessment("LARGE", ValueOrigin.EXPLICIT),
                effort = AxisAssessment("LOW", ValueOrigin.EXPLICIT), cost = AxisAssessment("LOW", ValueOrigin.EXPLICIT),
                risk = AxisAssessment("LOW", ValueOrigin.EXPLICIT), evaluationState = OrientationEvaluationState.ASSESSED)
            f.orientations.saveOrientation(subject.copy(version = 2, updatedAt = 20),
                OrientationNode(id, OrientationKind.PROJECT, OrientationLifecycle.REALIZED, ValueOrigin.EXPLICIT, assessment),
                OrientationAssessmentRevision("project-revision", 20, 20, null, false, 1, id, 20, 20,
                    AssessmentRevisionSource.USER, "Project completed", assessment))
            assertThat(f.dao.getOrientation(id)!!.lifecycle).isEqualTo("REALIZED")
            assertThat(f.dao.getAllAssessments().single().importanceValue).isEqualTo("HIGH")
            assertThat(calculateOrientationScore(assessment)).isNotEqualTo(OrientationScore.ZERO)
        }
    }

    @Test fun `create rename and catalog preserve normal Orientation identity with no Context or Workspace creation`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientations.createOrientation(OrientationKind.PROJECT, " Equip clinic ", " Plan ", 10)
            f.orientations.updateOrientationSemantics(id, "Equipped clinic", "Updated plan", 1, 20)
            assertThat(f.orientations.getOrientationSemantics(id)!!.kind).isEqualTo(OrientationKind.PROJECT)
            val row = catalog(f).observeCatalog().first().single()
            assertThat(row.ref).isEqualTo(CanonicalEntityRef.Orientation(id))
            assertThat(row.orientationKind).isEqualTo(OrientationKind.PROJECT)
            assertThat(row.title).isEqualTo("Equipped clinic")
            assertThat(f.db.workspaceDao().getAll()).isEmpty()
            assertThat(f.dao.getAllLegacyMappings()).isEmpty()
            assertThat(f.dao.getAllManagedSubjects()).hasSize(1)
        }
    }

    @Test fun `Project uses canonical semantic tags Aspect memberships and graph relations`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation(OrientationKind.PROJECT); val aspect = f.aspect(); val other = f.orientation()
            val tags = CanonicalSemanticTagRepository(f.db)
            tags.addTag(ManagedSubjectRef(ManagedSubjectType.ORIENTATION, id), "Clinic", 20)
            f.links.linkOrientation(aspect, id, AspectOrientationRelationType.BELONGS_TO, now = 21)
            f.graph.addRelation(id, other, OrientationRelationType.SUPPORTS, 22)
            assertThat(tags.observeTags(ManagedSubjectRef(ManagedSubjectType.ORIENTATION, id)).first()).contains("clinic")
            assertThat(f.links.observeMemberships(id).first().single().aspectId).isEqualTo(aspect)
            assertThat(f.graph.observeRelations(id).first().single().toOrientationId).isEqualTo(other)
            assertThat(f.dao.getAllOrientations().single { it.subjectId == id }.kind).isEqualTo("PROJECT")
        }
    }

    @Test fun `Project EMBODIES and REALIZES reuse WorkspaceBinding and adaptive Navigation without target mutation`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace(); val id = f.orientation(OrientationKind.PROJECT)
            f.bindings.bind(id, "workspace", WorkspaceBindingType.EMBODIES, 20)
            f.bindings.bind(id, "workspace", WorkspaceBindingType.REALIZES, 21)
            val primary = f.appearance(id); val link = f.appearance(id)
            val entities = catalog(f).observeCatalog().first()
            val placements = f.db.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() }
            val nodes = withOperationalEmbodiments(projectBigPicture(placements, entities), f.dao.getAllWorkspaceBindings(), entities)
            assertThat(nodes.map { it.placement.id }).containsExactly(primary, link)
            nodes.forEach {
                assertThat(bigPictureDestination(it)).isEqualTo(NavTarget.Workspace("workspace"))
                assertThat(bigPictureSemanticDestination(it)).isEqualTo(NavTarget.SemanticEntityEditor(id))
            }
            assertThat(f.db.hierarchyPlacementDao().getAll().map { it.toHierarchyPlacementStrict() }).isEqualTo(placements)
        }
    }

    @Test fun `Project snapshot JSON Restore and peer Merge preserve exact canonical kind and source identity`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { source ->
            val id = source.orientation(OrientationKind.PROJECT)
            source.appearance(id)
            val snapshot = Gson().fromJson(Gson().toJson(source.snapshot()), SnapshotBundle::class.java)
            for (merge in listOf(false, true)) SemanticDeletionRoomFixture().use { target ->
                target.admit(snapshot, merge)
                assertThat(target.orientations.getOrientationSemantics(id)!!.kind).isEqualTo(OrientationKind.PROJECT)
                assertThat(target.dao.getAllManagedSubjects()).isEqualTo(source.dao.getAllManagedSubjects())
                assertThat(target.db.hierarchyPlacementDao().getAll()).isEqualTo(source.db.hierarchyPlacementDao().getAll())
            }
        }
    }

    @Test fun `Project canonical dirty sync payload preserves stable string code`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation(OrientationKind.PROJECT)
            val store = CanonicalOrientationSyncStore(f.db, f.dao, mockk(relaxed = true), f.db.workspaceDao(),
                f.db.workspaceTagRefDao(), mockk(relaxed = true))
            val payload = store.loadUnsynced()
            assertThat(payload.orientations.single { it.subjectId == id }.kind).isEqualTo("PROJECT")
            assertThat(f.dao.getAllLegacyMappings()).isEmpty()
        }
    }

    @Test fun `Project standard semantic deletion retires every appearance and preserves bound Workspace`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            f.workspace(); val id = f.orientation(OrientationKind.PROJECT)
            f.bindings.bind(id, "workspace", WorkspaceBindingType.EMBODIES, 20)
            f.appearance(id); f.appearance(id)
            f.delete(id)
            assertThat(f.dao.getManagedSubject(id)!!.isDeleted).isTrue()
            assertThat(f.db.hierarchyPlacementDao().getAll().all { it.isDeleted }).isTrue()
            assertThat(f.db.workspaceDao().getById("workspace")!!.isDeleted).isFalse()
            assertThat(catalog(f).observeCatalog().first().any { it.ref == CanonicalEntityRef.Orientation(id) }).isFalse()
        }
    }
}
