package com.romankozak.forwardappmobile.data.orientation

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.romankozak.forwardappmobile.core.data.models.entities.orientation.*
import com.romankozak.forwardappmobile.core.data.models.entities.planning.*
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.*
import com.romankozak.forwardappmobile.core.data.models.sync.*
import com.romankozak.forwardappmobile.data.entitybrowser.CanonicalEntityCatalogRepository
import com.romankozak.forwardappmobile.data.planning.CanonicalPlanningReferenceRepository
import com.romankozak.forwardappmobile.features.entitybrowser.CanonicalEntityRef
import com.romankozak.forwardappmobile.shared.core.domain.orientation.*
import com.romankozak.forwardappmobile.shared.core.models.orientation.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CanonicalOrientationKindRoomTest {
    private fun command(f: SemanticDeletionRoomFixture) = CanonicalOrientationKindCoordinator(f.db, f.orientations)

    @Test fun `independent scoring overrides survive and immutable history ACK never acknowledges the next change`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation()
            val policy = DEFAULT_ORIENTATION_SCORING_POLICY_V1
            val override = OrientationScoringOverrideEntity(id, policy.id, policy.version, policy.formulaVersion,
                0.5f, null, 1f, null, null, 10, 10, null, false, 1)
            f.dao.upsertScoringOverrides(listOf(override))
            val c = command(f); val plan = c.preview(id)
            c.transform(plan, OrientationKind.PROJECT, 100)
            val first = f.db.managedSubjectConversionDao().getAll().single()
            f.db.managedSubjectConversionDao().acknowledge(first.id, 0, 110)
            assertThat(f.db.managedSubjectConversionDao().getUnsynced()).hasSize(1)
            f.db.managedSubjectConversionDao().acknowledge(first.id, 1, 111)
            c.transform(c.preview(id), OrientationKind.ONGOING_STANDARD, 120)
            f.db.managedSubjectConversionDao().acknowledge(first.id, 1, 130)
            assertThat(f.db.managedSubjectConversionDao().getUnsynced()).hasSize(1)
            val before = f.rows()
            CanonicalSubjectConversionStore(f.db).merge(listOf(first))
            assertThat(f.rows()).isEqualTo(before)
            val divergent = first.copy(previousAssessmentRevisionIdsJson = "[\"other\"]")
            assertThat(runCatching { CanonicalSubjectConversionStore(f.db).merge(listOf(divergent)) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
            assertThat(f.dao.getAllScoringOverrides()).containsExactly(override)
        }
    }

    @Test fun `legacy mapped native-looking kind is not a generic transform source`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation()
            f.dao.upsertLegacyMappings(listOf(LegacySubjectMappingEntity(id, "CONTEXT", "old", id, 1, "ADOPTED", 1, 1, null, false, 1)))
            val before = f.rows(); val c = command(f)
            assertThat(c.canOffer(id)).isFalse()
            assertThat(runCatching { c.preview(id) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
        }
    }

    @Test fun `native kind changes preserve exact identity workspaces tags relations appearances and terminal lifecycle`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation(); f.workspace()
            f.bindings.bind(id, "workspace", WorkspaceBindingType.EMBODIES, 12)
            val aspect = f.aspect()
            f.links.linkOrientation(aspect, id, AspectOrientationRelationType.BELONGS_TO, now = 13)
            CanonicalSemanticTagRepository(f.db).addTag(ManagedSubjectRef(ManagedSubjectType.ORIENTATION, id), "health", 14)
            f.appearance(id); f.appearance(id)
            f.dao.upsertOrientations(listOf(f.dao.getOrientation(id)!!.copy(lifecycle = "REALIZED", lifecycleOrigin = "EXPLICIT")))
            val preserved = f.rows().filterKeys { it !in setOf("managed_subjects", "orientations", "orientation_assessments", "orientation_assessment_revisions", "managed_subject_conversions") }
            val c = command(f)
            listOf(OrientationKind.PROJECT, OrientationKind.ONGOING_STANDARD, OrientationKind.OPPORTUNITY, OrientationKind.MILESTONE, OrientationKind.INTENT).forEachIndexed { index, target ->
                c.transform(c.preview(id), target, 100L + index)
                assertThat(f.dao.getOrientation(id)!!.kind).isEqualTo(target.name)
                assertThat(f.dao.getOrientation(id)!!.lifecycle).isEqualTo("REALIZED")
                assertThat(f.rows().filterKeys { it in preserved }).isEqualTo(preserved)
                assertThat(CanonicalEntityCatalogRepository(f.db.canonicalEntityCatalogDao()).observeCatalog().first().single { it.ref == CanonicalEntityRef.Orientation(id) }.orientationKind).isEqualTo(target)
            }
            assertThat(f.dao.getAllManagedSubjects().count { it.id == id }).isEqualTo(1)
            assertThat(f.db.managedSubjectConversionDao().getAll()).hasSize(5)
            assertThat(f.db.workspaceDao().getById("workspace")!!.isDeleted).isFalse()
        }
    }

    @Test fun `ongoing applicability resets only changed axes and keeps old explicit assessments immutable`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation(OrientationKind.PROJECT)
            val subject = f.orientations.getOrientationSemantics(id)!!.subject
            val scored = initialOrientationAssessment(OrientationKind.PROJECT).copy(importance = AxisAssessment("HIGH", ValueOrigin.EXPLICIT),
                expectedSpan = AxisAssessment("MONTHS", ValueOrigin.EXPLICIT), targetWindow = AxisAssessment("THIS_YEAR", ValueOrigin.EXPLICIT))
            f.orientations.saveOrientation(subject.copy(version = 2), OrientationNode(id, OrientationKind.PROJECT, null, ValueOrigin.UNSET, scored),
                OrientationAssessmentRevision("scored", 11, 11, null, false, 1, id, 11, 11, AssessmentRevisionSource.USER, null, scored))
            val old = f.dao.getAllAssessmentRevisions()
            val c = command(f); c.transform(c.preview(id), OrientationKind.ONGOING_STANDARD, 100)
            val current = f.dao.getAllAssessments().single()
            assertThat(current.importanceValue).isEqualTo("HIGH")
            assertThat(current.expectedSpanValue).isEqualTo("ONGOING")
            assertThat(current.expectedSpanOrigin).isEqualTo("DERIVED")
            assertThat(current.targetWindowOrigin).isEqualTo("NOT_APPLICABLE")
            assertThat(f.dao.getAllAssessmentRevisions().filter { it.id in old.map { r -> r.id } }).containsExactlyElementsIn(old)
            c.transform(c.preview(id), OrientationKind.PROJECT, 101)
            assertThat(f.dao.getAllAssessments().single().expectedSpanOrigin).isEqualTo("UNSET")
            assertThat(f.dao.getAllAssessments().single().targetWindowOrigin).isEqualTo("UNSET")
            validateCanonicalPayloadReferences(f.snapshot(), false)
        }
    }

    @Test fun `converted Aspect can change kind repeatedly without rewriting its immutable conversion fact`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.aspect()
            val aspectCommand = CanonicalAspectConversionCoordinator(f.db, f.orientations, f.aspects, f.links,
                CanonicalSemanticTagRepository(f.db), CanonicalPlanningReferenceRepository(f.db))
            aspectCommand.convert(aspectCommand.preview(id), OrientationKind.INTENT, 50)
            val original = f.db.managedSubjectConversionDao().getAll().single()
            val c = command(f); c.transform(c.preview(id), OrientationKind.PROJECT, 100)
            c.transform(c.preview(id), OrientationKind.ONGOING_STANDARD, 101)
            assertThat(f.db.managedSubjectConversionDao().getAll().first { it.id == original.id }).isEqualTo(original)
            SemanticDeletionRoomFixture().use { restored -> restored.admit(f.snapshot(), false) }
        }
    }

    @Test fun `exact current generation Restore and peer Merge preserve history and stale kinds cannot resurrect`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { source ->
            val id = source.orientation(); val before = source.snapshot()
            val c = command(source); c.transform(c.preview(id), OrientationKind.PROJECT, 100)
            c.transform(c.preview(id), OrientationKind.ONGOING_STANDARD, 101)
            val gson = GsonBuilder().registerTypeAdapterFactory(SubjectConversionSnapshotAdapter()).create()
            val raw = gson.toJson(source.snapshot())
            assertThat(raw).contains(SubjectConversionSnapshotAdapter.KIND_GENERATION)
            assertThat(runCatching { Gson().fromJson(raw, SnapshotBundle::class.java) }.isFailure).isTrue()
            val snapshot = gson.fromJson(raw, SnapshotBundle::class.java)
            for (merge in listOf(false, true)) SemanticDeletionRoomFixture().use { target ->
                if (merge) target.admit(before, false)
                target.admit(snapshot, merge)
                assertThat(target.dao.getOrientation(id)!!.kind).isEqualTo("ONGOING_STANDARD")
                assertThat(target.db.managedSubjectConversionDao().getAll()).hasSize(2)
                val state = target.rows()
                // A stale complete payload is conservatively rejected because required history is absent.
                runCatching { target.admit(before, true) }
                assertThat(target.rows()).isEqualTo(state)
                val high = before.copy(managedSubjects = before.managedSubjects!!.map { it.copy(version = 1000) })
                assertThat(runCatching { target.admit(high, true) }.isFailure).isTrue()
                assertThat(target.rows()).isEqualTo(state)
            }
        }
    }

    @Test fun `late history write failure rolls back identity assessment and every dependent row`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation(); f.workspace(); f.bindings.bind(id, "workspace", WorkspaceBindingType.EMBODIES)
            f.db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_kind BEFORE INSERT ON managed_subject_conversions WHEN NEW.formatVersion=2 BEGIN SELECT RAISE(ABORT,'late failure'); END")
            val c = command(f); val plan = c.preview(id); val before = f.rows()
            assertThat(runCatching { c.transform(plan, OrientationKind.PROJECT, 100) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(before)
        }
    }

    @Test fun `replay is idempotent race rejects and unsupported source target or deleted identity fail closed`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation(); val c = command(f); val plan = c.preview(id)
            c.transform(plan, OrientationKind.PROJECT, 100); val after = f.rows()
            c.transform(plan, OrientationKind.PROJECT, 101); assertThat(f.rows()).isEqualTo(after)
            assertThat(runCatching { c.transform(plan, OrientationKind.MILESTONE, 102) }.isFailure).isTrue()
            assertThat(runCatching { c.transform(c.preview(id), OrientationKind.DAY_THEME, 103) }.isFailure).isTrue()
            val stale = c.preview(id)
            f.orientations.updateOrientationSemantics(id, "Changed", null, now = 110)
            val changed = f.rows()
            assertThat(runCatching { c.transform(stale, OrientationKind.ONGOING_STANDARD, 120) }.isFailure).isTrue()
            assertThat(f.rows()).isEqualTo(changed)
            for (kind in listOf(OrientationKind.GOAL, OrientationKind.DAY_THEME, OrientationKind.ARC_QUEST, OrientationKind.THEME, OrientationKind.QUEST, OrientationKind.DIRECTION)) {
                val specialized = f.orientation(kind)
                assertThat(c.canOffer(specialized)).isFalse()
            }
            f.delete(id, version = f.dao.getManagedSubject(id)!!.version)
            assertThat(c.canOffer(id)).isFalse()
            CanonicalSubjectConversionStore(f.db).validateCurrentClosure()
        }
    }

    @Test fun `Planning reference and Orientation recurrence source remain exact and unchanged`() = runBlocking<Unit> {
        SemanticDeletionRoomFixture().use { f ->
            val id = f.orientation()
            f.db.planningDao().upsertScope(PlanningScopeEntity("scope", "DAY", "ACTIVE", null, null, null, 1, 1, null, 1, false))
            f.db.planningDao().upsertCommitment(PlanningCommitmentEntity("commitment", "scope", id, "FOCUS", 0, null, "PLANNED", "MANUAL", null, null, 1, 1, null, 1, false))
            f.db.dayPlanDao().insert(DayPlan(id = "day", date = 1))
            f.db.dayTaskDao().insert(DayTask(id = "task", dayPlanId = "day", title = "Work"))
            val refs = CanonicalPlanningReferenceRepository(f.db)
            refs.attach(PlanningReferenceOwner(PlanningReferenceOwnerKind.DAY_TASK, "task"), PlanningReferenceTarget(PlanningReferenceTargetKind.ORIENTATION, id), PlanningReferenceRole.SOURCE, 20)
            val template = com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskTemplate("Recurring", null, null, emptyList(), emptyList(),
                com.romankozak.forwardappmobile.shared.core.models.day.TaskPriority.MEDIUM, null, 0,
                planningSource = com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskPlanningSource(
                    com.romankozak.forwardappmobile.shared.core.models.recurrence.RecurringTaskSourceKind.ORIENTATION, id))
            f.db.canonicalRecurringSeriesDao().insert(CanonicalRecurringSeriesEntity("series", "TASK", "DAILY", 1,
                startDayKey = "2026-10-25", templateJson = Gson().toJson(template), createdAt = 1, updatedAt = 1, version = 1))
            val rows = f.db.planningReferenceDao().getAll(); val series = f.db.canonicalRecurringSeriesDao().getAllSync()
            val c = command(f); val plan = c.preview(id)
            assertThat(plan.planningCount).isEqualTo(2); assertThat(plan.recurrenceCount).isEqualTo(1)
            c.transform(plan, OrientationKind.ONGOING_STANDARD, 100)
            assertThat(f.db.planningReferenceDao().getAll()).isEqualTo(rows)
            assertThat(f.db.canonicalRecurringSeriesDao().getAllSync()).isEqualTo(series)
            assertThat(f.db.planningDao().getAllCommitments().single().orientationId).isEqualTo(id)
            assertThat(f.db.planningDao().getAllCommitments().single().version).isEqualTo(1)
        }
    }
}
