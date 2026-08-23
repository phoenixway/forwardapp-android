package com.romankozak.forwardappmobile.data.daythemes

import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.ThemeDefinitionSnapshot
import com.romankozak.forwardappmobile.shared.core.models.day.canonicalDayThemeId

internal data class CanonicalDayThemeMergePlan(
    val themeDefinitions: List<ThemeDefinitionSnapshot>,
    val dayThemes: List<DayThemeSnapshot>,
    val assignmentDocuments: List<DayThemeAssignmentDocumentSnapshot>,
)

internal fun planCanonicalDayThemeMerge(
    incomingThemeDefinitions: List<ThemeDefinitionSnapshot>,
    incomingDayThemes: List<DayThemeSnapshot>,
    incomingAssignmentDocuments: List<DayThemeAssignmentDocumentSnapshot>,
    incomingPlanIdRemap: Map<String, String>,
    validPlanIds: Set<String>,
    localThemeDefinitions: List<ThemeDefinitionSnapshot>,
    localDayThemes: List<DayThemeSnapshot>,
    localAssignmentDocuments: List<DayThemeAssignmentDocumentSnapshot>,
): CanonicalDayThemeMergePlan {
    val remappedDayThemeIds =
        incomingDayThemes.associate { incoming ->
            incoming.id to
                canonicalDayThemeId(
                    incomingPlanIdRemap[incoming.dayPlanId] ?: incoming.dayPlanId,
                    incoming.themeId,
                )
        }

    val remappedDayThemes =
        incomingDayThemes.map { incoming ->
            val remappedPlanId = incomingPlanIdRemap[incoming.dayPlanId] ?: incoming.dayPlanId
            incoming.copy(
                id = canonicalDayThemeId(remappedPlanId, incoming.themeId),
                dayPlanId = remappedPlanId,
            )
        }

    require(remappedDayThemes.all { it.dayPlanId in validPlanIds }) {
        "Canonical Day Theme merge references a DayPlan that does not exist after DayPlan remap."
    }
    require(remappedDayThemes.map { it.id }.distinct().size == remappedDayThemes.size) {
        "Canonical Day Theme merge produced duplicate DayTheme ids after DayPlan remap."
    }
    require(remappedDayThemes.map { it.dayPlanId to it.themeId }.distinct().size == remappedDayThemes.size) {
        "Canonical Day Theme merge produced duplicate (dayPlanId, themeId) pairs after DayPlan remap."
    }

    val remappedAssignmentDocuments =
        incomingAssignmentDocuments.map { document ->
            val remappedPlanId = incomingPlanIdRemap[document.dayPlanId] ?: document.dayPlanId
            document.copy(
                dayPlanId = remappedPlanId,
                assignments =
                    document.assignments.map { assignment ->
                        assignment.copy(
                            dayThemeIds =
                                assignment.dayThemeIds.map { incomingDayThemeId ->
                                    remappedDayThemeIds[incomingDayThemeId]
                                        ?: error(
                                            "Canonical Day Theme assignment references unknown incoming DayTheme " +
                                                incomingDayThemeId,
                                        )
                                },
                        )
                    },
            )
        }

    require(remappedAssignmentDocuments.all { it.dayPlanId in validPlanIds }) {
        "Canonical Day Theme assignment merge references a DayPlan that does not exist after DayPlan remap."
    }
    require(
        remappedAssignmentDocuments.map { it.dayPlanId }.distinct().size ==
            remappedAssignmentDocuments.size,
    ) {
        "Canonical Day Theme merge produced duplicate assignment documents after DayPlan remap."
    }

    val remappedDayThemesById = remappedDayThemes.associateBy { it.id }
    remappedAssignmentDocuments.forEach { document ->
        document.assignments.forEach { assignment ->
            assignment.dayThemeIds.forEach { dayThemeId ->
                val referenced =
                    requireNotNull(remappedDayThemesById[dayThemeId]) {
                        "Canonical Day Theme assignment references missing remapped DayTheme $dayThemeId."
                    }
                require(referenced.dayPlanId == document.dayPlanId) {
                    "Canonical Day Theme assignment references a DayTheme from another remapped day."
                }
            }
        }
    }

    val localDefinitionsById = localThemeDefinitions.associateBy { it.id }
    val localDayThemesById = localDayThemes.associateBy { it.id }
    val localAssignmentsByDayPlanId = localAssignmentDocuments.associateBy { it.dayPlanId }

    return CanonicalDayThemeMergePlan(
        themeDefinitions =
            incomingThemeDefinitions.filter { incoming ->
                val local = localDefinitionsById[incoming.id] ?: return@filter true
                incomingWins(
                    incomingVersion = incoming.version,
                    incomingUpdatedAt = incoming.updatedAt,
                    incomingDeleted = incoming.isDeleted,
                    localVersion = local.version,
                    localUpdatedAt = local.updatedAt,
                    localDeleted = local.isDeleted,
                )
            },
        dayThemes =
            remappedDayThemes.filter { incoming ->
                val local = localDayThemesById[incoming.id] ?: return@filter true
                incomingWins(
                    incomingVersion = incoming.version,
                    incomingUpdatedAt = incoming.updatedAt,
                    incomingDeleted = incoming.isDeleted,
                    localVersion = local.version,
                    localUpdatedAt = local.updatedAt,
                    localDeleted = local.isDeleted,
                )
            },
        assignmentDocuments =
            remappedAssignmentDocuments.filter { incoming ->
                val local = localAssignmentsByDayPlanId[incoming.dayPlanId] ?: return@filter true
                incomingWins(
                    incomingVersion = incoming.version,
                    incomingUpdatedAt = incoming.updatedAt,
                    incomingDeleted = incoming.isDeleted,
                    localVersion = local.version,
                    localUpdatedAt = local.updatedAt,
                    localDeleted = local.isDeleted,
                )
            },
    )
}

internal fun planLegacyDayThemeMerge(
    incomingLegacyDocuments: List<DayThemeDocumentSnapshot>,
    incomingPlanIdRemap: Map<String, String>,
    validPlanIds: Set<String>,
    localThemeDefinitions: List<ThemeDefinitionSnapshot>,
    localDayThemes: List<DayThemeSnapshot>,
    localAssignmentDocuments: List<DayThemeAssignmentDocumentSnapshot>,
): CanonicalDayThemeMergePlan {
    val migrated =
        LegacyDayThemeCanonicalMigrationMapper.migrate(
            legacyDocuments = incomingLegacyDocuments,
            existingThemeDefinitions = localThemeDefinitions,
        )

    return planCanonicalDayThemeMerge(
        incomingThemeDefinitions = migrated.themeDefinitions,
        incomingDayThemes = migrated.dayThemes,
        incomingAssignmentDocuments = migrated.dayThemeAssignmentDocuments,
        incomingPlanIdRemap = incomingPlanIdRemap,
        validPlanIds = validPlanIds,
        localThemeDefinitions = localThemeDefinitions,
        localDayThemes = localDayThemes,
        localAssignmentDocuments = localAssignmentDocuments,
    )
}

private fun incomingWins(
    incomingVersion: Long,
    incomingUpdatedAt: Long,
    incomingDeleted: Boolean,
    localVersion: Long,
    localUpdatedAt: Long,
    localDeleted: Boolean,
): Boolean =
    when {
        incomingVersion != localVersion -> incomingVersion > localVersion
        incomingUpdatedAt != localUpdatedAt -> incomingUpdatedAt > localUpdatedAt
        incomingDeleted != localDeleted -> incomingDeleted
        else -> false
    }
