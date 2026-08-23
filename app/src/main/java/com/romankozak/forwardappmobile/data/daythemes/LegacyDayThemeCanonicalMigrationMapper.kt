package com.romankozak.forwardappmobile.data.daythemes

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.ThemeDefinitionSnapshot
import com.romankozak.forwardappmobile.shared.core.models.day.canonicalDayThemeId

/** Pure legacy DayThemeDocument -> canonical Day Themes transformation. */
object LegacyDayThemeCanonicalMigrationMapper {
    private val gson = Gson()

    fun migrate(
        legacyDocuments: List<DayThemeDocumentSnapshot>,
        existingThemeDefinitions: List<ThemeDefinitionSnapshot> = emptyList(),
    ): LegacyDayThemeCanonicalMigrationResult {
        val diagnostics = mutableListOf<LegacyDayThemeMigrationDiagnostic>()
        val protectedDefinitionIds = existingThemeDefinitions.mapTo(mutableSetOf(), ThemeDefinitionSnapshot::id)
        val legacyDefinitionCandidates = linkedMapOf<String, DefinitionCandidate>()
        val dayThemesById = linkedMapOf<String, DayThemeSnapshot>()
        val assignmentDocumentsByDayPlanId = linkedMapOf<String, DayThemeAssignmentDocumentSnapshot>()

        legacyDocuments.forEach { document ->
            val payload =
                try {
                    gson.fromJson(document.contentJson, LegacyDayThemePayload::class.java)
                } catch (error: Exception) {
                    diagnostics +=
                        LegacyDayThemeMigrationDiagnostic(
                            code = LegacyDayThemeMigrationDiagnosticCode.MALFORMED_DOCUMENT,
                            dayPlanId = document.dayPlanId,
                            message = error.message ?: "Invalid legacy DayTheme JSON",
                        )
                    null
                }

            if (payload == null) return@forEach

            val remap = linkedMapOf<String, String>()

            payload.themes.orEmpty().forEach themeLoop@{ theme ->
                val legacyId = theme.id?.trim().orEmpty()
                val explicitThemeId = theme.themeId?.trim().orEmpty()
                val definitionId = explicitThemeId.ifEmpty { legacyId }
                val assignmentIdentity = legacyId.ifEmpty { definitionId }
                val title = theme.title?.trim().orEmpty()

                if (definitionId.isEmpty() || assignmentIdentity.isEmpty() || title.isEmpty()) {
                    diagnostics +=
                        LegacyDayThemeMigrationDiagnostic(
                            code = LegacyDayThemeMigrationDiagnosticCode.INVALID_THEME,
                            dayPlanId = document.dayPlanId,
                            legacyThemeId = legacyId.ifEmpty { explicitThemeId }.ifEmpty { null },
                            message = "Legacy Theme requires a stable id and non-blank title",
                        )
                    return@themeLoop
                }

                val createdAt = theme.createdAt ?: document.createdAt
                val updatedAt = theme.updatedAt ?: document.updatedAt
                val canonicalId = canonicalDayThemeId(document.dayPlanId, definitionId)

                remap[assignmentIdentity] = canonicalId

                val dayTheme =
                    DayThemeSnapshot(
                        id = canonicalId,
                        themeId = definitionId,
                        dayPlanId = document.dayPlanId,
                        budgetPercent = theme.budgetPercent ?: 0,
                        order = theme.order ?: 0,
                        isActive = theme.isActive ?: true,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        syncedAt = null,
                        version = 1,
                        isDeleted = document.isDeleted,
                    )

                val currentDayTheme = dayThemesById[canonicalId]
                if (currentDayTheme == null || isNewer(dayTheme, currentDayTheme)) {
                    dayThemesById[canonicalId] = dayTheme
                }

                if (definitionId !in protectedDefinitionIds) {
                    val candidate =
                        DefinitionCandidate(
                            sourceDayPlanId = document.dayPlanId,
                            snapshot =
                                ThemeDefinitionSnapshot(
                                    id = definitionId,
                                    title = title,
                                    colorArgb = theme.colorArgb ?: DEFAULT_COLOR_ARGB,
                                    iconKey = canonicalIconKey(theme.iconKey ?: DEFAULT_ICON_KEY),
                                    description = theme.comment?.trim().orEmpty(),
                                    carryForward = theme.carryForward ?: true,
                                    archived = false,
                                    createdAt = createdAt,
                                    updatedAt = updatedAt,
                                    syncedAt = null,
                                    version = 1,
                                    isDeleted = false,
                                ),
                        )

                    val currentCandidate = legacyDefinitionCandidates[definitionId]
                    if (currentCandidate == null || isBetterDefinitionCandidate(candidate, currentCandidate)) {
                        legacyDefinitionCandidates[definitionId] = candidate
                    }
                }
            }

            val assignmentsByEntity = linkedMapOf<String, MutableSet<String>>()

            payload.assignments.orEmpty().forEach assignmentLoop@{ assignment ->
                val entityId = assignment.entityId?.trim().orEmpty()
                if (entityId.isEmpty()) {
                    diagnostics +=
                        LegacyDayThemeMigrationDiagnostic(
                            code = LegacyDayThemeMigrationDiagnosticCode.INVALID_ASSIGNMENT,
                            dayPlanId = document.dayPlanId,
                            message = "Legacy DayTheme assignment requires entityId",
                        )
                    return@assignmentLoop
                }

                assignment.themeIds.orEmpty().forEach { legacyThemeId ->
                    val canonicalId = remap[legacyThemeId]
                    if (canonicalId == null) {
                        diagnostics +=
                            LegacyDayThemeMigrationDiagnostic(
                                code = LegacyDayThemeMigrationDiagnosticCode.ORPHAN_ASSIGNMENT_THEME,
                                dayPlanId = document.dayPlanId,
                                entityId = entityId,
                                legacyThemeId = legacyThemeId,
                                message = "Legacy assignment references no DayTheme in the same day",
                            )
                    } else {
                        assignmentsByEntity.getOrPut(entityId) { linkedSetOf() } += canonicalId
                    }
                }
            }

            val assignmentDocument =
                DayThemeAssignmentDocumentSnapshot(
                    dayPlanId = document.dayPlanId,
                    assignments =
                        assignmentsByEntity
                            .toSortedMap()
                            .map { (entityId, ids) ->
                                DayThemeAssignmentSnapshot(
                                    entityId = entityId,
                                    dayThemeIds = ids.sorted(),
                                )
                            },
                    createdAt = document.createdAt,
                    updatedAt = document.updatedAt,
                    syncedAt = null,
                    version = maxOf(1L, document.version),
                    isDeleted = document.isDeleted,
                )

            val currentAssignmentDocument = assignmentDocumentsByDayPlanId[document.dayPlanId]
            if (currentAssignmentDocument == null || isNewer(assignmentDocument, currentAssignmentDocument)) {
                assignmentDocumentsByDayPlanId[document.dayPlanId] = assignmentDocument
            }
        }

        val definitions =
            buildMap<String, ThemeDefinitionSnapshot> {
                existingThemeDefinitions.forEach { definition -> putIfAbsent(definition.id, definition) }
                legacyDefinitionCandidates.forEach { (id, candidate) -> putIfAbsent(id, candidate.snapshot) }
            }.values.sortedBy(ThemeDefinitionSnapshot::id)

        return LegacyDayThemeCanonicalMigrationResult(
            themeDefinitions = definitions,
            dayThemes =
                dayThemesById.values.sortedWith(
                    compareBy<DayThemeSnapshot>({ it.dayPlanId }, { it.order }, { it.id }),
                ),
            dayThemeAssignmentDocuments = assignmentDocumentsByDayPlanId.values.sortedBy { it.dayPlanId },
            diagnostics =
                diagnostics.sortedWith(
                    compareBy(
                        LegacyDayThemeMigrationDiagnostic::dayPlanId,
                        { it.entityId.orEmpty() },
                        { it.legacyThemeId.orEmpty() },
                        { it.code.name },
                    ),
                ),
        )
    }

    private fun isNewer(candidate: DayThemeSnapshot, current: DayThemeSnapshot): Boolean =
        candidate.updatedAt > current.updatedAt ||
            (candidate.updatedAt == current.updatedAt && candidate.createdAt > current.createdAt)

    private fun isNewer(
        candidate: DayThemeAssignmentDocumentSnapshot,
        current: DayThemeAssignmentDocumentSnapshot,
    ): Boolean =
        when {
            candidate.version != current.version -> candidate.version > current.version
            candidate.updatedAt != current.updatedAt -> candidate.updatedAt > current.updatedAt
            candidate.isDeleted != current.isDeleted -> candidate.isDeleted
            else -> false
        }

    private fun isBetterDefinitionCandidate(
        candidate: DefinitionCandidate,
        current: DefinitionCandidate,
    ): Boolean =
        when {
            candidate.snapshot.updatedAt != current.snapshot.updatedAt ->
                candidate.snapshot.updatedAt > current.snapshot.updatedAt
            candidate.snapshot.createdAt != current.snapshot.createdAt ->
                candidate.snapshot.createdAt > current.snapshot.createdAt
            else -> candidate.sourceDayPlanId < current.sourceDayPlanId
        }

    private fun canonicalIconKey(key: String): String =
        when (key) {
            "spark" -> "sparkles"
            "mind" -> "brain"
            "flag" -> "target"
            else -> key
        }

    private data class DefinitionCandidate(
        val sourceDayPlanId: String,
        val snapshot: ThemeDefinitionSnapshot,
    )

    private data class LegacyDayThemePayload(
        val themes: List<LegacyDayTheme>? = null,
        val assignments: List<LegacyDayThemeAssignment>? = null,
    )

    private data class LegacyDayTheme(
        val id: String? = null,
        val themeId: String? = null,
        val dayPlanId: String? = null,
        val title: String? = null,
        val colorArgb: Long? = null,
        val iconKey: String? = null,
        val comment: String? = null,
        val carryForward: Boolean? = null,
        val budgetPercent: Int? = null,
        val order: Long? = null,
        val isActive: Boolean? = null,
        val createdAt: Long? = null,
        val updatedAt: Long? = null,
    )

    private data class LegacyDayThemeAssignment(
        val dayPlanId: String? = null,
        val entityId: String? = null,
        val themeIds: List<String>? = null,
    )

    private const val DEFAULT_COLOR_ARGB: Long = 0xFF2563EB
    private const val DEFAULT_ICON_KEY: String = "target"
}

data class LegacyDayThemeCanonicalMigrationResult(
    val themeDefinitions: List<ThemeDefinitionSnapshot>,
    val dayThemes: List<DayThemeSnapshot>,
    val dayThemeAssignmentDocuments: List<DayThemeAssignmentDocumentSnapshot>,
    val diagnostics: List<LegacyDayThemeMigrationDiagnostic>,
)

data class LegacyDayThemeMigrationDiagnostic(
    val code: LegacyDayThemeMigrationDiagnosticCode,
    val dayPlanId: String,
    val entityId: String? = null,
    val legacyThemeId: String? = null,
    val message: String,
)

enum class LegacyDayThemeMigrationDiagnosticCode {
    MALFORMED_DOCUMENT,
    INVALID_THEME,
    INVALID_ASSIGNMENT,
    ORPHAN_ASSIGNMENT_THEME,
}
