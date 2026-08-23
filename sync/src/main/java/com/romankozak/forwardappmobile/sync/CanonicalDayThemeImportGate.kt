package com.romankozak.forwardappmobile.sync

import com.google.gson.JsonObject
import com.romankozak.forwardappmobile.core.data.models.sync.DayThemeCanonicalPresence
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.classifyDayThemeCanonicalPresence
import com.romankozak.forwardappmobile.core.data.models.sync.requireValidCanonicalDayThemePayload

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
                requireValidCanonicalDayThemePayload(decodedBundle)
                decodedBundle
            }

            DayThemeCanonicalPresence.MALFORMED -> malformedCanonicalPayload()
        }

    private fun malformedCanonicalPayload(): Nothing =
        throw IllegalArgumentException(
            "Malformed canonical Day Themes payload: expected either none or all of " +
                "themeDefinitions, dayThemes, dayThemeAssignmentDocuments as arrays.",
        )
}
