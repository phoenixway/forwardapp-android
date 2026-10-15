package com.romankozak.forwardappmobile.data.orientation

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestSourceType
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestOrientationCutoverTest {
    private val quest =
        ArcQuestEntity(
            id = "quest-1",
            arcKey = "arc-1",
            title = "Canonical quest",
            description = "Do the hard thing",
            sourceType = ArcQuestSourceType.MANUAL.name,
            status = "ACTIVE",
            order = 7L,
            createdAt = 10L,
            updatedAt = 20L,
            syncedAt = 21L,
            version = 4L,
            isDeleted = false,
        )

    private val projection =
        (
            quest.toCompatibilityProjection(LegacySubjectUuid)
                as ArcQuestCompatibilityProjection.ManualOrientation
        ).value

    private val rows =
        projection.toCanonicalRows(
            Gson(),
            migrationVersion = 2,
        )

    @Test
    fun `materialized ARC_QUEST is promoted in place to QUEST and CUT_OVER`() {
        val plan = plan()

        assertTrue(plan.issues.isEmpty())
        assertEquals(rows.subject.id, plan.subjects.single().id)
        assertEquals(
            rows.subject.version + 1,
            plan.subjects.single().version,
        )
        assertEquals(
            OrientationKind.QUEST.name,
            plan.orientations.single().kind,
        )
        assertEquals(
            rows.orientation.subjectId,
            plan.orientations.single().subjectId,
        )
        assertEquals(
            null,
            plan.orientations.single().lifecycle,
        )
        assertEquals(
            ValueOrigin.UNSET.name,
            plan.orientations.single().lifecycleOrigin,
        )
        assertEquals(
            LegacySubjectMappingState.CUT_OVER.name,
            plan.mappings.single().state,
        )
        assertEquals(
            rows.mapping.subjectId,
            plan.mappings.single().subjectId,
        )
        assertEquals(
            rows.assessment.revisionId,
            rows.revision.id,
        )
    }

    @Test
    fun `already cut over QUEST is idempotent`() {
        val result =
            plan(
                subject =
                    rows.subject.copy(
                        version = rows.subject.version + 1,
                    ),
                orientation =
                    rows.orientation.copy(
                        kind = OrientationKind.QUEST.name,
                    ),
                mapping =
                    rows.mapping.copy(
                        migrationVersion = 3,
                        state = LegacySubjectMappingState.CUT_OVER.name,
                    ),
            )

        assertTrue(result.issues.isEmpty())
        assertTrue(result.subjects.isEmpty())
        assertTrue(result.orientations.isEmpty())
        assertTrue(result.mappings.isEmpty())
    }

    @Test
    fun `already cut over QUEST keeps canonical lifecycle and assessment authority`() {
        val canonicalOrientation =
            rows.orientation.copy(
                kind = OrientationKind.QUEST.name,
                lifecycle = "PAUSED",
                lifecycleOrigin = ValueOrigin.DERIVED.name,
            )
        val canonicalAssessment =
            rows.assessment.copy(
                importanceValue = "HIGH",
                importanceOrigin = ValueOrigin.DERIVED.name,
            )

        val result =
            planQuestOrientationCutover(
                projections = listOf(projection),
                mappings =
                    listOf(
                        rows.mapping.copy(
                            migrationVersion = 3,
                            state = LegacySubjectMappingState.CUT_OVER.name,
                        ),
                    ),
                subjects =
                    listOf(
                        rows.subject.copy(
                            title = "Canonical title",
                            description = "Canonical description",
                            version = rows.subject.version + 1,
                        ),
                    ),
                orientations = listOf(canonicalOrientation),
                assessments = listOf(canonicalAssessment),
                revisions = listOf(rows.revision),
                now = 100L,
                migrationVersion = 3,
            )

        assertTrue(result.issues.isEmpty())
        assertTrue(result.subjects.isEmpty())
        assertTrue(result.orientations.isEmpty())
        assertTrue(result.mappings.isEmpty())
    }

    @Test
    fun `identity mismatch fails closed`() {
        val result =
            plan(
                mapping =
                    rows.mapping.copy(
                        subjectId = "other",
                    ),
            )

        assertTrue(result.subjects.isEmpty())
        assertTrue(result.orientations.isEmpty())
        assertTrue(result.mappings.isEmpty())
        assertEquals(
            "QUEST_CUTOVER_IDENTITY_MISMATCH",
            result.issues.single().code,
        )
    }

    @Test
    fun `missing assessment revision fails closed`() {
        val result =
            plan(
                revisions = emptyList(),
            )

        assertTrue(result.subjects.isEmpty())
        assertTrue(result.orientations.isEmpty())
        assertTrue(result.mappings.isEmpty())
        assertEquals(
            "QUEST_CUTOVER_REVISION_INVALID",
            result.issues.single().code,
        )
    }

    @Test
    fun `mapping without ArcQuest extension fails closed`() {
        val result =
            planQuestOrientationCutover(
                projections = emptyList(),
                mappings = listOf(rows.mapping),
                subjects = listOf(rows.subject),
                orientations = listOf(rows.orientation),
                assessments = listOf(rows.assessment),
                revisions = listOf(rows.revision),
                now = 100L,
                migrationVersion = 3,
            )

        assertTrue(result.subjects.isEmpty())
        assertTrue(result.orientations.isEmpty())
        assertTrue(result.mappings.isEmpty())
        assertEquals(
            "QUEST_CUTOVER_EXTENSION_MISSING",
            result.issues.single().code,
        )
    }

    private fun plan(
        subject:
            com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity =
            rows.subject,
        orientation:
            com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity =
            rows.orientation,
        mapping:
            com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity =
            rows.mapping,
        revisions:
            List<com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentRevisionEntity> =
            listOf(rows.revision),
    ) =
        planQuestOrientationCutover(
            projections = listOf(projection),
            mappings = listOf(mapping),
            subjects = listOf(subject),
            orientations = listOf(orientation),
            assessments = listOf(rows.assessment),
            revisions = revisions,
            now = 100L,
            migrationVersion = 3,
        )
}
