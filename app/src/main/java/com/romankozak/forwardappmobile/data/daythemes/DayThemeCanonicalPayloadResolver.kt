package com.romankozak.forwardappmobile.data.daythemes

import com.google.gson.JsonObject
import com.romankozak.forwardappmobile.core.data.models.sync.DayThemeCanonicalPresence
import com.romankozak.forwardappmobile.core.data.models.sync.SnapshotBundle
import com.romankozak.forwardappmobile.core.data.models.sync.classifyDayThemeCanonicalPresence
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.ThemeDefinitionSnapshot

/**
 * Pure authority resolver for Day Themes inside one SnapshotBundle source layer.
 *
 * 000 -> legacy migration is allowed.
 * 111 -> canonical trio is authoritative, including three empty arrays.
 * Partial/invalid canonical presence -> reject. Never stitch canonical state from
 * another layer and never fall back to legacy when canonical presence exists.
 */
object DayThemeCanonicalPayloadResolver {
    fun resolve(
        rawSnapshotBundle: JsonObject,
        decodedBundle: SnapshotBundle,
    ): DayThemeCanonicalPayload =
        when (classifyDayThemeCanonicalPresence(rawSnapshotBundle)) {
            DayThemeCanonicalPresence.LEGACY -> {
                val migrated =
                    LegacyDayThemeCanonicalMigrationMapper.migrate(
                        legacyDocuments = decodedBundle.dayThemeDocuments,
                    )

                DayThemeCanonicalPayload(
                    source = DayThemeCanonicalPayloadSource.LEGACY_MIGRATION,
                    themeDefinitions = migrated.themeDefinitions,
                    dayThemes = migrated.dayThemes,
                    dayThemeAssignmentDocuments = migrated.dayThemeAssignmentDocuments,
                    diagnostics = migrated.diagnostics,
                )
            }

            DayThemeCanonicalPresence.CANONICAL ->
                DayThemeCanonicalPayload(
                    source = DayThemeCanonicalPayloadSource.CANONICAL,
                    themeDefinitions =
                        decodedBundle.themeDefinitions
                            ?: malformed("themeDefinitions was present as an array but decoded as null"),
                    dayThemes =
                        decodedBundle.dayThemes
                            ?: malformed("dayThemes was present as an array but decoded as null"),
                    dayThemeAssignmentDocuments =
                        decodedBundle.dayThemeAssignmentDocuments
                            ?: malformed("dayThemeAssignmentDocuments was present as an array but decoded as null"),
                    diagnostics = emptyList(),
                )

            DayThemeCanonicalPresence.MALFORMED ->
                malformed(
                    "Canonical Day Themes payload must contain either none or all of " +
                        "themeDefinitions, dayThemes, dayThemeAssignmentDocuments as arrays",
                )
        }

    private fun malformed(message: String): Nothing =
        throw MalformedDayThemeCanonicalPayloadException(message)
}

data class DayThemeCanonicalPayload(
    val source: DayThemeCanonicalPayloadSource,
    val themeDefinitions: List<ThemeDefinitionSnapshot>,
    val dayThemes: List<DayThemeSnapshot>,
    val dayThemeAssignmentDocuments: List<DayThemeAssignmentDocumentSnapshot>,
    val diagnostics: List<LegacyDayThemeMigrationDiagnostic>,
)

enum class DayThemeCanonicalPayloadSource {
    CANONICAL,
    LEGACY_MIGRATION,
}

class MalformedDayThemeCanonicalPayloadException(
    message: String,
) : IllegalArgumentException(message)
