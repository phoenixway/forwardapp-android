package com.romankozak.forwardappmobile.core.data.models.sync

import com.google.gson.Gson
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.romankozak.forwardappmobile.shared.core.models.orientation.OrientationAssessment

/**
 * Decodes immutable Orientation assessment revisions across model versions.
 *
 * DOMAIN-CONTRACT v1 assessment JSON predates evaluation scoring axes. Missing
 * v2 fields therefore mean "not assessed yet", never null canonical state.
 */
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

    return gson.fromJson(root, OrientationAssessment::class.java)
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
