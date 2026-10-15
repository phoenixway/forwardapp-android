package com.romankozak.forwardappmobile.data.orientation

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.ThemeDefinitionEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.LegacySubjectMappingState
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeOrientationCutoverTest {
    private val definition =
        ThemeDefinitionEntity(
            id = "theme-1",
            title = "Deep work",
            colorArgb = 42,
            iconKey = "target",
            description = "Protected focus",
            carryForward = true,
            archived = false,
            createdAt = 10,
            updatedAt = 20,
            syncedAt = 21,
            version = 4,
            isDeleted = false,
        )
    private val projection = definition.toEffectiveOrientation(LegacySubjectUuid)
    private val rows = projection.toCanonicalRows(Gson(), migrationVersion = 2)

    @Test
    fun `materialized DAY_THEME is promoted in place to THEME and CUT_OVER`() {
        val plan = plan()

        assertTrue(plan.issues.isEmpty())
        assertEquals(rows.subject.id, plan.subjects.single().id)
        assertEquals(rows.subject.version + 1, plan.subjects.single().version)
        assertEquals(OrientationKind.THEME.name, plan.orientations.single().kind)
        assertEquals(rows.orientation.subjectId, plan.orientations.single().subjectId)
        assertEquals(LegacySubjectMappingState.CUT_OVER.name, plan.mappings.single().state)
        assertEquals(rows.mapping.subjectId, plan.mappings.single().subjectId)
        assertEquals(rows.assessment.revisionId, rows.revision.id)
    }

    @Test
    fun `already cut over THEME is idempotent`() {
        val result =
            plan(
                subject = rows.subject.copy(version = rows.subject.version + 1),
                orientation = rows.orientation.copy(kind = OrientationKind.THEME.name),
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
    fun `identity mismatch fails closed`() {
        val result = plan(mapping = rows.mapping.copy(subjectId = "other"))

        assertTrue(result.subjects.isEmpty())
        assertTrue(result.orientations.isEmpty())
        assertTrue(result.mappings.isEmpty())
        assertEquals("THEME_CUTOVER_IDENTITY_MISMATCH", result.issues.single().code)
    }

    @Test
    fun `missing assessment revision fails closed`() {
        val result = plan(revisions = emptyList())

        assertEquals("THEME_CUTOVER_REVISION_INVALID", result.issues.single().code)
    }

    @Test
    fun `mapping without Theme extension fails closed`() {
        val result =
            planThemeOrientationCutover(
                projections = emptyList(),
                mappings = listOf(rows.mapping),
                subjects = listOf(rows.subject),
                orientations = listOf(rows.orientation),
                assessments = listOf(rows.assessment),
                revisions = listOf(rows.revision),
                now = 100,
                migrationVersion = 3,
            )

        assertEquals("THEME_CUTOVER_EXTENSION_MISSING", result.issues.single().code)
    }

    @Test
    fun `projection reads common fields from canonical subject and extension fields from definition`() {
        val mapping =
            rows.mapping.copy(state = LegacySubjectMappingState.CUT_OVER.name)
        val canonical =
            projectCanonicalThemeDefinitions(
                definitions = listOf(definition),
                subjects =
                    listOf(
                        rows.subject.copy(
                            title = "Canonical title",
                            description = "Canonical description",
                            isDeleted = true,
                        ),
                    ),
                mappings = listOf(mapping),
                orientations = listOf(rows.orientation.copy(kind = OrientationKind.THEME.name)),
            ).single()

        assertEquals("Canonical title", canonical.title)
        assertEquals("Canonical description", canonical.description)
        assertTrue(canonical.isDeleted)
        assertEquals(definition.colorArgb, canonical.colorArgb)
        assertEquals(definition.iconKey, canonical.iconKey)
        assertEquals(definition.carryForward, canonical.carryForward)
        assertEquals(definition.archived, canonical.archived)
    }

    @Test
    fun `new Theme is born with deterministic THEME identity and CUT_OVER mapping`() {
        val created = definition.toCutOverCanonicalThemeRows(Gson(), migrationVersion = 3)

        assertEquals(LegacySubjectUuid.resolve(projection.source), created.subject.id)
        assertEquals(created.subject.id, created.orientation.subjectId)
        assertEquals(OrientationKind.THEME.name, created.orientation.kind)
        assertEquals(created.subject.id, created.mapping.subjectId)
        assertEquals(LegacySubjectMappingState.CUT_OVER.name, created.mapping.state)
        assertEquals(rows.assessment.importanceOrigin, created.assessment.importanceOrigin)
        assertEquals(rows.assessment.expectedSpanOrigin, created.assessment.expectedSpanOrigin)
    }

    @Test
    fun `projection does not fall back when CUT_OVER mapping is absent`() {
        val failure =
            runCatching {
                projectCanonicalThemeDefinitions(
                    definitions = listOf(definition),
                    subjects = listOf(rows.subject),
                    mappings = emptyList(),
                    orientations = listOf(rows.orientation.copy(kind = OrientationKind.THEME.name)),
                )
            }

        assertTrue(failure.isFailure)
        assertFalse(failure.exceptionOrNull()?.message.isNullOrBlank())
    }

    private fun plan(
        subject: com.romankozak.forwardappmobile.core.data.models.entities.orientation.ManagedSubjectEntity = rows.subject,
        orientation: com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationEntity = rows.orientation,
        mapping: com.romankozak.forwardappmobile.core.data.models.entities.orientation.LegacySubjectMappingEntity = rows.mapping,
        revisions: List<com.romankozak.forwardappmobile.core.data.models.entities.orientation.OrientationAssessmentRevisionEntity> =
            listOf(rows.revision),
    ) =
        planThemeOrientationCutover(
            projections = listOf(projection),
            mappings = listOf(mapping),
            subjects = listOf(subject),
            orientations = listOf(orientation),
            assessments = listOf(rows.assessment),
            revisions = revisions,
            now = 100,
            migrationVersion = 3,
        )
}
