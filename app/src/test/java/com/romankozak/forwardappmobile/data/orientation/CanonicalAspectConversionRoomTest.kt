package com.romankozak.forwardappmobile.data.orientation

import com.google.common.truth.Truth.assertThat
import com.google.gson.GsonBuilder
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.*
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.*
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.data.models.sync.*
import com.romankozak.forwardappmobile.core.sync.*
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalAspectConversionRoomTest {
    @Test fun `promotion order overflow fails before any mutation`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.aspect()
            f.aspects.move(id, null, order = Long.MAX_VALUE, now = 20)
            f.aspects.create("Child", parentAspectId = id, now = 21)
            val before = f.rows()
            assertThat(runCatching { coordinator(f).preview(id) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
        }
    }
    @Test fun `recurrence source is an explicit unsupported dependency with no partial mutation`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.aspect()
            val template = com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskTemplate(
                "Recurring", null, null, emptyList(), emptyList(), com.romankozak.forwardappmobile.shared.core.models.day.TaskPriority.MEDIUM,
                null, 0, planningSource = com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskPlanningSource(
                    com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSourceKind.ASPECT, id))
            f.db.canonicalRecurringSeriesDao().insert(CanonicalRecurringSeriesEntity("series", "TASK", "DAILY", 1,
                startDayKey = "2026-10-25", templateJson = com.google.gson.Gson().toJson(template), createdAt = 1, updatedAt = 1, version = 1))
            val before = f.rows()
            assertThat(runCatching { coordinator(f).preview(id) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
        }
    }
    @Test fun `deleted child taxonomy history survives while live children are promoted`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.aspect()
            val deleted = f.aspects.create("Old child", parentAspectId = id, now = 20)
            f.delete(deleted, ManagedSubjectType.ASPECT)
            val service = coordinator(f)
            service.convert(service.preview(id), OrientationKind.PROJECT, 200)
            assertThat(f.dao.getAspect(deleted)!!.parentAspectId).isEqualTo(id)
            CanonicalSubjectConversionStore(f.db).validateCurrentClosure()
            SemanticDeletionRoomFixture().use { target -> target.admit(f.snapshot(), false) }
        }
    }

    @Test fun `converted Orientation remains editable and deletable without erasing conversion evidence`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = setup(f); val service = coordinator(f)
            service.convert(service.preview(id), OrientationKind.PROJECT, 100)
            val aspect = f.aspects.create("New classification", now = 101)
            f.links.linkOrientation(aspect, id, AspectOrientationRelationType.BELONGS_TO, now = 102)
            f.orientations.updateOrientationSemantics(id, "Renamed Project", "Plan", now = 103)
            val version = f.dao.getManagedSubject(id)!!.version
            f.delete(id, ManagedSubjectType.ORIENTATION, version)
            assertThat(f.dao.getManagedSubject(id)!!.isDeleted).isTrue()
            assertThat(f.db.managedSubjectConversionDao().getAll()).hasSize(1)
            assertThat(f.db.workspaceDao().getById("workspace")!!.isDeleted).isFalse()
            CanonicalSubjectConversionStore(f.db).validateCurrentClosure()
            SemanticDeletionRoomFixture().use { target -> target.admit(f.snapshot(), false) }
        }
    }
    private fun coordinator(f: SemanticDeletionRoomFixture) = CanonicalAspectConversionCoordinator(f.db, f.orientations, f.aspects,
        f.links, CanonicalSemanticTagRepository(f.db), CanonicalPlanningReferenceRepository(f.db))
    private suspend fun setup(f: SemanticDeletionRoomFixture, parent: Boolean = false): String {
        val id = f.aspects.create("Treatment", "Plan", now = 10)
        if (parent) f.aspects.move(id, f.aspects.create("Parent", now = 11), now = 12)
        f.aspects.create("First child", parentAspectId = id, now = 13)
        f.aspects.create("Second child", parentAspectId = id, now = 14)
        f.links.linkOrientation(id, f.orientation(), AspectOrientationRelationType.BELONGS_TO, now = 15)
        CanonicalSemanticTagRepository(f.db).addTag(ManagedSubjectRef(ManagedSubjectType.ASPECT, id), "health", 16)
        f.workspace(); f.bindings.bind(id, "workspace", WorkspaceBindingType.EMBODIES, 17)
        f.appearance(id); f.appearance(id)
        return id
    }
    private suspend fun planning(f: SemanticDeletionRoomFixture, id: String) {
        f.db.dayPlanDao().insert(DayPlan(id = "day", date = 1))
        f.db.dayTaskDao().insert(DayTask(id = "task", dayPlanId = "day", title = "Work"))
        val repo = CanonicalPlanningReferenceRepository(f.db)
        PlanningReferenceRole.entries.forEach { repo.attach(PlanningReferenceOwner(PlanningReferenceOwnerKind.DAY_TASK, "task"),
            PlanningReferenceTarget(PlanningReferenceTargetKind.ASPECT, id), it, 30) }
    }

    @Test fun `native conversion retains identity bindings tags and exact H1 while retiring Aspect edges and promoting children`() = runBlocking<Unit> {
        for (parent in listOf(false, true)) SemanticDeletionRoomFixture().use { f ->
            val id = setup(f, parent); planning(f, id)
            val service = coordinator(f); val plan = service.preview(id)
            val placements = f.db.hierarchyPlacementDao().getAll()
            val bindings = f.dao.getAllWorkspaceBindings(); val workspaces = f.db.workspaceDao().getAll()
            assertThat(service.convert(plan, OrientationKind.PROJECT, 100)).isEqualTo(id)
            val subject = f.dao.getManagedSubject(id)!!
            assertThat(subject.subjectType).isEqualTo("ORIENTATION")
            assertThat(subject.title).isEqualTo("Treatment"); assertThat(subject.description).isEqualTo("Plan")
            assertThat(subject.version).isEqualTo(plan.version + 1)
            assertThat(f.dao.getAspect(id)).isNull()
            assertThat(f.orientations.getOrientationSemantics(id)!!.kind).isEqualTo(OrientationKind.PROJECT)
            assertThat(f.dao.getAllAspectOrientationRefs().all { it.isDeleted }).isTrue()
            val children = plan.childIds.map { f.dao.getAspect(it)!! }
            assertThat(children.map { it.parentAspectId }.toSet()).containsExactly(plan.parentId)
            assertThat(children.map { it.aspectOrder }).isInOrder()
            assertThat(f.db.hierarchyPlacementDao().getAll()).isEqualTo(placements)
            assertThat(f.dao.getAllWorkspaceBindings()).isEqualTo(bindings)
            assertThat(f.db.workspaceDao().getAll()).isEqualTo(workspaces)
            val tags = CanonicalSemanticTagRepository(f.db)
            assertThat(tags.observeTags(ManagedSubjectRef(ManagedSubjectType.ORIENTATION, id)).first()).containsExactly("health")
            assertThat(f.db.managedSubjectTagDao().getForSubject("ASPECT", id).all { it.isDeleted }).isTrue()
            val refs = f.db.planningReferenceDao().getAll()
            assertThat(refs.filter { !it.isDeleted }.all { it.targetKind == "ORIENTATION" && it.targetId == id }).isTrue()
            assertThat(refs.filter { it.isDeleted }.all { it.targetKind == "ASPECT" }).isTrue()
            assertThat(f.db.planningDao().getAllCommitments()).isEmpty()
            assertThat(CanonicalEntityCatalogRepository(f.db.canonicalEntityCatalogDao()).observeCatalog().first().single { it.ref.id == id }.ref)
                .isEqualTo(CanonicalEntityRef.Orientation(id))
            val after = f.rows()
            service.convert(plan, OrientationKind.PROJECT, 101)
            assertThat(f.rows()).isEqualTo(after)
        }
    }

    @Test fun `every admitted ordinary target uses its own standard initial assessment`() = runBlocking<Unit> {
        for (kind in SUBJECT_CONVERSION_TARGET_KINDS) SemanticDeletionRoomFixture().use { f ->
            val id = f.aspect(); val service = coordinator(f)
            service.convert(service.preview(id), kind, 30)
            f.orientations.requireCompleteActiveOrientationAggregate(id)
            assertThat(f.dao.getOrientation(id)!!.kind).isEqualTo(kind.name)
            CanonicalSubjectConversionStore(f.db).validateCurrentClosure()
        }
    }

    @Test fun `changed dependencies and unsupported target leave all state unchanged`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = setup(f); val service = coordinator(f); val plan = service.preview(id)
            val before = f.rows()
            assertThat(runCatching { service.convert(plan, OrientationKind.GOAL, 100) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
            CanonicalSemanticTagRepository(f.db).addTag(ManagedSubjectRef(ManagedSubjectType.ASPECT, id), "late", 90)
            val changed = f.rows()
            assertThat(runCatching { service.convert(plan, OrientationKind.PROJECT, 100) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(changed)
        }
    }

    @Test fun `late conversion fact failure rolls back subtype children tags references assessment and H1 together`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = setup(f, true); planning(f, id)
            val service = coordinator(f); val plan = service.preview(id)
            f.db.openHelper.writableDatabase.execSQL("CREATE TEMP TRIGGER fail_conversion BEFORE INSERT ON managed_subject_conversions BEGIN SELECT RAISE(ABORT,'late failure'); END")
            val before = f.rows()
            assertThat(runCatching { service.convert(plan, OrientationKind.PROJECT, 100) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
        }
    }

    @Test fun `conversion snapshot production exact Restore and peer Merge retain former typed tombstones`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { source ->
            val id = setup(source, true); val old = source.snapshot()
            val service = coordinator(source); service.convert(service.preview(id), OrientationKind.PROJECT, 100)
            val codec = GsonBuilder().registerTypeAdapterFactory(SubjectConversionSnapshotAdapter()).create()
            val snapshot = codec.fromJson(codec.toJson(source.snapshot().copy(workspaceBacklogEntries = emptyList(), workspaceInboxRecords = emptyList())), SnapshotBundle::class.java)
            SemanticDeletionRoomFixture().use { target ->
                val restore = SnapshotRestoreLocalDataSourceImpl(database = target.db,
                    writer = CanonicalSnapshotTransactionWriter { target.dao.storeCanonicalPayload(it, false, target.db.workspaceDao(), false) },
                    dayManagementRuntimeRepository = mockk(relaxed = true), dayPlanningAuthorityActivator = mockk(relaxed = true),
                    tacticalPlanningAuthorityActivator = mockk(relaxed = true), strategicPlanningAuthorityActivator = mockk(relaxed = true),
                    canonicalScoringAuthorityActivator = mockk(relaxed = true))
                restore.replaceWith(snapshot)
                assertThat(target.dao.getManagedSubject(id)!!.subjectType).isEqualTo("ORIENTATION")
                assertThat(target.db.managedSubjectConversionDao().getAll()).isEqualTo(source.db.managedSubjectConversionDao().getAll())
                assertThat(target.db.managedSubjectTagDao().getAll()).isEqualTo(source.db.managedSubjectTagDao().getAll())
            }
            SemanticDeletionRoomFixture().use { target ->
                target.admit(old, false)
                target.admit(snapshot, true)
                CanonicalSubjectConversionStore(target.db).validateCurrentClosure()
                val before = target.rows()
                assertThat(runCatching { target.admit(old, true) }.isFailure).isTrue()
                assertThat(target.rows()).isEqualTo(before)
            }
        }
    }

    @Test fun `missing evidence or higher version old Aspect cannot revive former type`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = setup(f); val old = f.snapshot(); val service = coordinator(f)
            service.convert(service.preview(id), OrientationKind.PROJECT, 100)
            val converted = f.snapshot()
            SemanticDeletionRoomFixture().use { target ->
                val before = target.rows()
                assertThat(runCatching { target.admit(converted.copy(managedSubjectConversions = null), false) }.isFailure).isTrue()
                assertThat(target.rows()).isEqualTo(before)
            }
            val hostile = old.copy(managedSubjects = old.managedSubjects!!.map { if (it.id == id) it.copy(version = 999) else it })
            val before = f.rows()
            assertThat(runCatching { f.admit(hostile, true) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
        }
    }

    @Test fun `historical archived and already Orientation sources fail closed`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.aspect(); val service = coordinator(f)
            f.aspects.setArchived(id, true, 20)
            assertThat(service.canOffer(id)).isFalse()
            assertThat(runCatching { service.preview(id) }.isFailure).isTrue()
            f.aspects.setArchived(id, false, 21)
            f.dao.upsertLegacyMappings(listOf(LegacySubjectMappingEntity("legacy", "CONTEXT", "old", id, 1, "CUT_OVER", 1, 1, null, false, 1)))
            assertThat(service.canOffer(id)).isFalse()
            assertThat(runCatching { service.preview(id) }.isFailure).isTrue()
            assertThat(service.canOffer(f.orientation())).isFalse()
        }
    }
}
