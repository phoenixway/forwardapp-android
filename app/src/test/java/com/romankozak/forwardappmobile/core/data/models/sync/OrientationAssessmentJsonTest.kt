package com.romankozak.forwardappmobile.core.data.models.sync

import com.google.gson.Gson
import com.romankozak.forwardappmobile.shared.core.models.orientation.AxisAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationEvaluationState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrientationAssessmentJsonTest {
    private val gson = Gson()

    @Test
    fun `v1 assessment JSON receives v2 neutral defaults`() {
        val json =
            gson.toJson(
                mapOf(
                    "importance" to axis("HIGH"),
                    "impact" to axis("MEDIUM"),
                    "breadth" to unset(),
                    "expectedSpan" to unset(),
                    "targetWindow" to unset(),
                    "attentionTier" to unset(),
                    "commitment" to unset(),
                    "confidence" to unset(),
                ),
            )

        val decoded = decodeOrientationAssessmentJson(gson, json)

        assertEquals(ValueOrigin.UNSET, decoded.effort.origin)
        assertNull(decoded.effort.valueCode)
        assertEquals(ValueOrigin.UNSET, decoded.cost.origin)
        assertEquals(ValueOrigin.UNSET, decoded.risk.origin)
        assertEquals(OrientationEvaluationState.UNASSESSED, decoded.evaluationState)
        assertEquals("HIGH", decoded.importance.valueCode)
    }

    @Test
    fun `v2 assessment JSON preserves explicit evaluation fields`() {
        val assessment =
            OrientationAssessment(
                importance = AxisAssessment("HIGH", ValueOrigin.EXPLICIT),
                impact = AxisAssessment("MEDIUM", ValueOrigin.EXPLICIT),
                breadth = unset(),
                expectedSpan = unset(),
                targetWindow = unset(),
                attentionTier = unset(),
                commitment = unset(),
                confidence = unset(),
                effort = AxisAssessment("VERY_HIGH", ValueOrigin.EXPLICIT),
                cost = AxisAssessment("LOW", ValueOrigin.EXPLICIT),
                risk = AxisAssessment("MODERATE", ValueOrigin.EXPLICIT),
                evaluationState = OrientationEvaluationState.ASSESSED,
            )

        val decoded =
            decodeOrientationAssessmentJson(
                gson,
                gson.toJson(assessment),
            )

        assertEquals(assessment, decoded)
    }

    private fun axis(value: String) =
        AxisAssessment(
            valueCode = value,
            origin = ValueOrigin.EXPLICIT,
        )

    private fun unset() =
        AxisAssessment(
            valueCode = null,
            origin = ValueOrigin.UNSET,
        )
}
