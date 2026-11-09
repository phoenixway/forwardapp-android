package com.romankozak.forwardappmobile.core.data.models.sync

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueProvenance
import com.romankozak.forwardappmobile.shared.core.models.orientation.AxisAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationEvaluationState
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
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

    @Test fun `canonical read is independent of reflected field naming used by minified writers`() {
        val json = gson.toJson(assessment())
        val renamedFields = GsonBuilder().setFieldNamingStrategy { "renamed_${it.name}" }.create()
        assertEquals(assessment(), decodeOrientationAssessmentJson(renamedFields, json))
    }

    @Test fun `missing mandatory axis is rejected inside decoder instead of leaking a null axis`() {
        val root = JsonParser.parseString(gson.toJson(assessment())).asJsonObject
        root.remove("importance")
        assertThrows(IllegalArgumentException::class.java) { decodeOrientationAssessmentJson(gson, root.toString()) }
    }

    @Test fun `explicit null axis invalid origin and non-string value fail closed`() {
        listOf("null", "{\"valueCode\":\"HIGH\"}", "{\"origin\":\"UNKNOWN\"}", "{\"origin\":\"EXPLICIT\",\"valueCode\":7}").forEach { axis ->
            val root = JsonParser.parseString(gson.toJson(assessment())).asJsonObject
            root.add("importance", JsonParser.parseString(axis))
            assertThrows(IllegalArgumentException::class.java) { decodeOrientationAssessmentJson(gson, root.toString()) }
        }
    }

    @Test fun `provenance and future value codes survive exact read without defaulting`() {
        val value = assessment().copy(importance = AxisAssessment("FUTURE_CODE", ValueOrigin.DERIVED,
            ValueProvenance("IMPORT", "source", "importance", "original")))
        assertEquals(value, decodeOrientationAssessmentJson(gson, gson.toJson(value)))
    }

    @Test fun `unrecognized obfuscated JSON cannot masquerade as a valid assessment`() {
        assertThrows(IllegalArgumentException::class.java) {
            decodeOrientationAssessmentJson(gson, "{\"a\":{\"a\":\"HIGH\",\"b\":\"EXPLICIT\"}}")
        }
    }

    private fun assessment() = OrientationAssessment(axis("HIGH"), axis("MEDIUM"), unset(), unset(), unset(), unset(), unset(), unset())

    private fun unset() =
        AxisAssessment(
            valueCode = null,
            origin = ValueOrigin.UNSET,
        )
}
