package com.romankozak.forwardappmobile.sync

import com.google.gson.JsonObject
import com.romankozak.forwardappmobile.core.data.models.sync.DayThemeCanonicalPresence
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.classifyDayThemeCanonicalPresence
import com.romankozak.forwardappmobile.core.data.models.sync.requireValidCanonicalDayThemePayload

private const val MISSING_ASSIGNMENTS_MESSAGE =
    "У резервній копії відсутні дані призначень тем. Оновіть додаток і експортуйте нову копію."

/**
 * Canonical Day Theme wire-boundary policy.
 *
 * Legacy 000 remains accepted for compatibility. Canonical 111 is authoritative
 * and is accepted by both merge and full-restore persistence paths. Partial or
 * explicitly-null canonical payloads remain malformed and fail closed.
 */
object CanonicalDayThemeImportGate {
    fun requireImportable(
        rawSnapshotBundle: JsonObject,
        decodedBundle: SnapshotBundle,
    ): SnapshotBundle =
        requireByPresence(rawSnapshotBundle, decodedBundle)

    fun requireFullRestoreImportable(
        rawSnapshotBundle: JsonObject,
        decodedBundle: SnapshotBundle,
    ): SnapshotBundle =
        requireByPresence(rawSnapshotBundle, decodedBundle)

    private fun requireByPresence(
        rawSnapshotBundle: JsonObject,
        decodedBundle: SnapshotBundle,
    ): SnapshotBundle =
        when (classifyDayThemeCanonicalPresence(rawSnapshotBundle)) {
            DayThemeCanonicalPresence.LEGACY -> decodedBundle

            DayThemeCanonicalPresence.CANONICAL -> {
                requireCompleteAssignments(rawSnapshotBundle)
                requireValidCanonicalDayThemePayload(decodedBundle)
                decodedBundle
            }

            DayThemeCanonicalPresence.MALFORMED -> malformedCanonicalPayload()
        }

    fun requireCompleteAssignments(rawSnapshotBundle: JsonObject) {
        val documents = rawSnapshotBundle.get("dayThemeAssignmentDocuments")
            ?.takeIf { it.isJsonArray }
            ?.asJsonArray
            ?: return
        documents.forEach { documentValue ->
            val document = documentValue.takeIf { it.isJsonObject }?.asJsonObject
            val assignments = document?.get("assignments") ?: document?.get("b")
            require(assignments != null && assignments.isJsonArray) { MISSING_ASSIGNMENTS_MESSAGE }
            assignments.asJsonArray.forEach { assignmentValue ->
                val assignment = assignmentValue.takeIf { it.isJsonObject }?.asJsonObject
                val entityId = assignment?.get("entityId") ?: assignment?.get("a")
                val themeIds = assignment?.get("dayThemeIds") ?: assignment?.get("b")
                require(entityId != null && entityId.isJsonPrimitive && entityId.asString.isNotBlank() && themeIds?.isJsonArray == true) {
                    MISSING_ASSIGNMENTS_MESSAGE
                }
            }
        }
    }

    private fun malformedCanonicalPayload(): Nothing =
        throw IllegalArgumentException(
            "Malformed canonical Day Themes payload: expected either none or all of " +
                "themeDefinitions, dayThemes, dayThemeAssignmentDocuments as arrays.",
        )
}
