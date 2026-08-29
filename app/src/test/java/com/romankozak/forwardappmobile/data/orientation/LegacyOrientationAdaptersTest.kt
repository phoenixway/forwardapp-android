package com.romankozak.forwardappmobile.data.orientation

import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestEntity
import com.romankozak.forwardappmobile.core.data.models.entities.ArcQuestSourceType
import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.entities.DirectionItemEntity
import com.romankozak.forwardappmobile.core.data.models.entities.Goal
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeacon
import com.romankozak.forwardappmobile.core.data.models.entities.MainBeaconGroup
import com.romankozak.forwardappmobile.core.data.models.entities.ScoringStatusValues
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.ThemeDefinitionEntity
import com.romankozak.forwardappmobile.shared.core.models.orientation.ImpactValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.ImportanceValue
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationKind
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyOrientationAdaptersTest {
    private val resolver = LegacySubjectIdResolver { source -> "resolved:${source.sourceType}:${source.sourceId}" }

    @Test
    fun goalProjectionMapsOnlyAcceptedCommonFields() {
        val goal =
            Goal(
                id = "g1",
                text = "Reliable sync",
                completed = false,
                createdAt = 10L,
                updatedAt = 20L,
                valueImportance = 10f,
                valueImpact = 5f,
                scoringStatus = ScoringStatusValues.ASSESSED,
                relativeSize = 5,
            )

        val result = goal.toEffectiveOrientation(resolver)

        assertEquals(OrientationKind.GOAL, result.orientation.kind)
        assertEquals(ImportanceValue.CRITICAL.name, result.orientation.assessment.importance.valueCode)
        assertEquals(ImpactValue.LARGE.name, result.orientation.assessment.impact.valueCode)
        assertEquals(ValueOrigin.DERIVED, result.orientation.assessment.impact.origin)
        assertTrue("relativeSize" in result.preservedSpecializedFields)
    }

    @Test
    fun groupGetsApplicableButUnsetIndependentAssessment() {
        val result = MainBeaconGroup(id = "group", title = "Core", createdAt = 1L, updatedAt = 2L).toEffectiveOrientation(resolver)

        assertEquals(OrientationKind.MAIN_BEACON_GROUP, result.orientation.kind)
        assertEquals(ValueOrigin.UNSET, result.orientation.assessment.importance.origin)
        assertNull(result.orientation.assessment.importance.valueCode)
    }

    @Test
    fun sourceBackedArcQuestDoesNotCreateDuplicateOrientation() {
        val arc =
            ArcQuestEntity(
                id = "arc-placement",
                arcKey = "2026-08",
                title = "Existing Beacon",
                sourceType = ArcQuestSourceType.BEACON.name,
                sourceId = "beacon-1",
            )

        val result = arc.toCompatibilityProjection(resolver)

        assertTrue(result is ArcQuestCompatibilityProjection.ExistingSourcePlacement)
    }

    @Test
    fun manualArcQuestCreatesOrientationProjection() {
        val arc = ArcQuestEntity(id = "manual", arcKey = "2026-08", title = "Manual")

        val result = arc.toCompatibilityProjection(resolver)

        assertTrue(result is ArcQuestCompatibilityProjection.ManualOrientation)
    }

    @Test
    fun mainBeaconPreservesReadinessWithoutLifecycleInference() {
        val result = MainBeacon(id = "beacon", title = "North", createdAt = 1L, updatedAt = 2L).toEffectiveOrientation(resolver)

        assertEquals(OrientationKind.MAIN_BEACON, result.orientation.kind)
        assertNull(result.orientation.lifecycle)
        assertTrue("readinessStatus" in result.preservedSpecializedFields)
    }

    @Test
    fun reviewedContextMapsAssessmentButKeepsWorkspaceFieldsSpecialized() {
        val context =
            Context(
                id = "context",
                name = "Engineering",
                description = null,
                parentId = null,
                createdAt = 1L,
                updatedAt = 2L,
                valueImportance = 7f,
                valueImpact = 3f,
                scoringStatus = ScoringStatusValues.ASSESSED,
                roleCode = "aspect",
            )

        val result = context.toEffectiveOrientation(OrientationKind.DIRECTION, resolver)

        assertEquals(ImportanceValue.HIGH.name, result.orientation.assessment.importance.valueCode)
        assertTrue("roleCode" in result.preservedSpecializedFields)
    }

    @Test
    fun directionAndThemeUseTheirAcceptedKinds() {
        val direction =
            DirectionItemEntity(
                id = "direction",
                contextId = "context",
                text = "Build carefully",
                itemOrder = 0,
                updatedAt = 2L,
            ).toEffectiveOrientation(resolver)
        val theme =
            ThemeDefinitionEntity(
                id = "theme",
                title = "Execution",
                colorArgb = 0L,
                iconKey = "",
                description = "",
                carryForward = true,
                archived = false,
                createdAt = 1L,
                updatedAt = 2L,
                syncedAt = null,
                version = 1L,
                isDeleted = false,
            ).toEffectiveOrientation(resolver)

        assertEquals(OrientationKind.DIRECTION, direction.orientation.kind)
        assertEquals(OrientationKind.DAY_THEME, theme.orientation.kind)
        assertEquals(ValueOrigin.NOT_APPLICABLE, theme.orientation.assessment.attentionTier.origin)
    }

    @Test
    fun aspectRoleProducesSuggestionNotAutomaticClassification() {
        val context =
            Context(
                id = "aspect",
                name = "Engineering",
                description = null,
                parentId = null,
                createdAt = 1L,
                updatedAt = 2L,
                roleCode = "aspect",
            )

        val suggestion = context.classificationSuggestion()

        assertEquals(ContextClassificationOutcome.ASPECT_AND_WORKSPACE, suggestion.suggestedOutcome)
        assertTrue(suggestion.requiresReview)
    }

    @Test
    fun contextClassificationPreviewIsStableAndKeepsAmbiguousContextsAsCompatibilityWorkspaces() {
        val aspect =
            Context(
                id = "engineering",
                name = "Engineering",
                description = null,
                parentId = null,
                createdAt = 1L,
                updatedAt = 2L,
                roleCode = "aspect",
            )
        val ambiguous = aspect.copy(id = "unclear", roleCode = null)

        val first = aspect.classificationPreview()
        val second = aspect.classificationPreview()
        val unresolved = ambiguous.classificationPreview()

        assertEquals(first.proposedAspectId, second.proposedAspectId)
        assertEquals(aspect.id, first.compatibilityWorkspaceId)
        assertEquals(ContextClassificationConfidence.HIGH, first.confidence)
        assertEquals(ContextClassificationOutcome.REVIEW_REQUIRED, unresolved.suggestedOutcome)
        assertEquals(ambiguous.id, unresolved.compatibilityWorkspaceId)
        assertTrue(unresolved.requiresReview)
    }
}
