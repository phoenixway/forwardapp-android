package com.romankozak.forwardappmobile.core.data.models.sync

import com.google.gson.Gson
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.AxisAssessment
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueOrigin
import com.romankozak.forwardappmobile.shared.core.models.orientation.ValueProvenance
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationEvaluationState

/**
 * Decodes immutable Orientation assessment revisions across model versions.
 *
 * DOMAIN-CONTRACT v1 assessment JSON predates evaluation scoring axes. Missing
 * v2 fields therefore mean "not assessed yet", never null canonical state.
 */
@Suppress("UNUSED_PARAMETER") // Keep the existing caller API; canonical keys must not use reflective JVM field names.
fun decodeOrientationAssessmentJson(
    gson: Gson,
    json: String,
): OrientationAssessment {
    val root =
        JsonParser
            .parseString(json)
            .asJsonObject
            .deepCopy()

    root.ensureUnsetAxis("effort")
    root.ensureUnsetAxis("cost")
    root.ensureUnsetAxis("risk")

    if (!root.has("evaluationState") || root.get("evaluationState").isJsonNull) {
        root.addProperty("evaluationState", "UNASSESSED")
    }

    return OrientationAssessment(
        importance = root.axis("importance"),
        impact = root.axis("impact"),
        breadth = root.axis("breadth"),
        expectedSpan = root.axis("expectedSpan"),
        targetWindow = root.axis("targetWindow"),
        attentionTier = root.axis("attentionTier"),
        commitment = root.axis("commitment"),
        confidence = root.axis("confidence"),
        effort = root.axis("effort"),
        cost = root.axis("cost"),
        risk = root.axis("risk"),
        evaluationState = OrientationEvaluationState.valueOf(root.get("evaluationState").asString),
    )
}

/** Kotlin non-null constructor invariants cannot be bypassed by Gson Unsafe allocation. */
private fun JsonObject.axis(name: String): AxisAssessment {
    val value = get(name)
    require(value != null && value.isJsonObject) { "Missing or invalid assessment axis: $name" }
    val axis = value.asJsonObject
    val origin = axis.get("origin")
    require(origin != null && origin.isJsonPrimitive && origin.asJsonPrimitive.isString) {
        "Missing or invalid origin for assessment axis: $name"
    }
    val code = axis.get("valueCode")?.takeUnless { it.isJsonNull }?.let {
        require(it.isJsonPrimitive && it.asJsonPrimitive.isString) { "Invalid valueCode for assessment axis: $name" }
        it.asString
    }
    val provenance = axis.get("provenance")?.takeUnless { it.isJsonNull }?.asJsonObject?.let { source ->
        ValueProvenance(source.requiredString("sourceType"), source.requiredString("sourceId"),
            source.requiredString("fieldName"), source.get("rawValue")?.takeUnless { it.isJsonNull }?.asString)
    }
    return AxisAssessment(code, ValueOrigin.valueOf(origin.asString), provenance)
}

private fun JsonObject.requiredString(name: String): String {
    val value = get(name)
    require(value != null && value.isJsonPrimitive && value.asJsonPrimitive.isString) { "Missing or invalid provenance field: $name" }
    return value.asString
}

private fun JsonObject.ensureUnsetAxis(name: String) {
    if (has(name) && !get(name).isJsonNull) {
        return
    }

    add(
        name,
        JsonObject().apply {
            add("valueCode", JsonNull.INSTANCE)
            addProperty("origin", "UNSET")
        },
    )
}
