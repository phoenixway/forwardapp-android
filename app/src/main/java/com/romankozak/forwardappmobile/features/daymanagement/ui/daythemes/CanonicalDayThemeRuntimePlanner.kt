package com.romankozak.forwardappmobile.features.daymanagement.ui.daythemes

import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.ThemeDefinitionSnapshot
import com.romankozak.forwardappmobile.shared.core.models.day.canonicalDayThemeId

internal data class CanonicalDayThemeRuntimeUpdatePlan(
    val themeDefinitions: List<ThemeDefinitionSnapshot>,
    val dayThemes: List<DayThemeSnapshot>,
    val assignmentDocument: DayThemeAssignmentDocumentSnapshot?,
)

internal fun projectCanonicalDayThemeDocument(
    dayPlanId: String,
    themeDefinitions: List<ThemeDefinitionSnapshot>,
    dayThemes: List<DayThemeSnapshot>,
    assignmentDocument: DayThemeAssignmentDocumentSnapshot?,
): DayThemeDocument {
    val definitionsById = themeDefinitions.associateBy { it.id }

    val themes =
        dayThemes
            .asSequence()
            .filter { it.dayPlanId == dayPlanId && !it.isDeleted }
            .mapNotNull { daily ->
                val definition = definitionsById[daily.themeId] ?: return@mapNotNull null
                if (definition.isDeleted) return@mapNotNull null

                DayTheme(
                    id = daily.id,
                    dayPlanId = dayPlanId,
                    title = definition.title,
                    colorArgb = definition.colorArgb,
                    iconKey = canonicalDayThemeIconKey(definition.iconKey),
                    comment = definition.description,
                    budgetPercent = daily.budgetPercent,
                    order = daily.order,
                    isActive = daily.isActive,
                    createdAt = daily.createdAt,
                    updatedAt = maxOf(daily.updatedAt, definition.updatedAt),
                )
            }.toList()

    val liveThemeIds = themes.mapTo(hashSetOf()) { it.id }
    val assignments =
        assignmentDocument
            ?.takeUnless { it.isDeleted }
            ?.assignments
            .orEmpty()
            .mapNotNull { assignment ->
                val ids = assignment.dayThemeIds.filterTo(linkedSetOf()) { it in liveThemeIds }
                if (ids.isEmpty()) {
                    null
                } else {
                    DayThemeAssignment(
                        dayPlanId = dayPlanId,
                        entityId = assignment.entityId,
                        themeIds = ids,
                    )
                }
            }

    return DayThemeDocument(
        themes = themes,
        assignments = assignments,
    )
}

internal fun planCanonicalDayThemeRuntimeUpdate(
    dayPlanId: String,
    now: Long,
    localThemeDefinitions: List<ThemeDefinitionSnapshot>,
    localDayThemes: List<DayThemeSnapshot>,
    localAssignmentDocument: DayThemeAssignmentDocumentSnapshot?,
    transformedDocument: DayThemeDocument,
): CanonicalDayThemeRuntimeUpdatePlan {
    val updated = transformedDocument.normalizedForDay(dayPlanId)

    require(updated.themes.map { it.id }.distinct().size == updated.themes.size) {
        "Day Theme runtime update contains duplicate UI theme ids."
    }
    require(updated.assignments.map { it.entityId }.distinct().size == updated.assignments.size) {
        "Day Theme runtime update contains duplicate assignment entity ids."
    }

    val localDefinitionsById = localThemeDefinitions.associateBy { it.id }
    val localDayThemesById = localDayThemes.associateBy { it.id }

    val definitionUpserts = linkedMapOf<String, ThemeDefinitionSnapshot>()
    val dayThemeUpserts = linkedMapOf<String, DayThemeSnapshot>()
    val uiIdToCanonicalDayThemeId = linkedMapOf<String, String>()
    val desiredCanonicalIds = linkedSetOf<String>()

    updated.themes.forEach { uiTheme ->
        require(uiTheme.title.isNotBlank()) {
            "Day Theme title must not be blank."
        }
        require(uiTheme.budgetPercent in 0..100) {
            "Day Theme budgetPercent must be in 0..100."
        }

        val directlyExistingDayTheme = localDayThemesById[uiTheme.id]
        val definitionId = directlyExistingDayTheme?.themeId ?: uiTheme.id
        val canonicalId = canonicalDayThemeId(dayPlanId, definitionId)

        if (directlyExistingDayTheme != null) {
            require(directlyExistingDayTheme.id == canonicalId) {
                "Persisted DayTheme ${directlyExistingDayTheme.id} does not match canonical identity $canonicalId."
            }
        }

        require(desiredCanonicalIds.add(canonicalId)) {
            "Day Theme runtime update produced duplicate canonical identity $canonicalId."
        }
        uiIdToCanonicalDayThemeId[uiTheme.id] = canonicalId

        val existingDefinition = localDefinitionsById[definitionId]
        val desiredTitle = uiTheme.title.trim()
        val desiredDescription = uiTheme.comment.trim()
        val desiredIconKey = canonicalDayThemeIconKey(uiTheme.iconKey)

        if (existingDefinition == null) {
            definitionUpserts[definitionId] =
                ThemeDefinitionSnapshot(
                    id = definitionId,
                    title = desiredTitle,
                    colorArgb = uiTheme.colorArgb,
                    iconKey = desiredIconKey,
                    description = desiredDescription,
                    carryForward = true,
                    archived = false,
                    createdAt = now,
                    updatedAt = now,
                    syncedAt = null,
                    version = 1,
                    isDeleted = false,
                )
        } else {
            val definitionChanged =
                existingDefinition.title != desiredTitle ||
                    existingDefinition.colorArgb != uiTheme.colorArgb ||
                    existingDefinition.iconKey != desiredIconKey ||
                    existingDefinition.description != desiredDescription ||
                    existingDefinition.isDeleted

            if (definitionChanged) {
                definitionUpserts[definitionId] =
                    existingDefinition.copy(
                        title = desiredTitle,
                        colorArgb = uiTheme.colorArgb,
                        iconKey = desiredIconKey,
                        description = desiredDescription,
                        updatedAt = now,
                        syncedAt = null,
                        version = existingDefinition.version + 1,
                        isDeleted = false,
                    )
            }
        }

        val existingDayTheme = localDayThemesById[canonicalId]
        if (existingDayTheme == null) {
            dayThemeUpserts[canonicalId] =
                DayThemeSnapshot(
                    id = canonicalId,
                    themeId = definitionId,
                    dayPlanId = dayPlanId,
                    budgetPercent = uiTheme.budgetPercent,
                    order = uiTheme.order,
                    isActive = uiTheme.isActive,
                    createdAt = now,
                    updatedAt = now,
                    syncedAt = null,
                    version = 1,
                    isDeleted = false,
                )
        } else {
            val dailyChanged =
                existingDayTheme.budgetPercent != uiTheme.budgetPercent ||
                    existingDayTheme.order != uiTheme.order ||
                    existingDayTheme.isActive != uiTheme.isActive ||
                    existingDayTheme.isDeleted

            if (dailyChanged) {
                dayThemeUpserts[canonicalId] =
                    existingDayTheme.copy(
                        budgetPercent = uiTheme.budgetPercent,
                        order = uiTheme.order,
                        isActive = uiTheme.isActive,
                        updatedAt = now,
                        syncedAt = null,
                        version = existingDayTheme.version + 1,
                        isDeleted = false,
                    )
            }
        }
    }

    localDayThemes
        .filter { it.dayPlanId == dayPlanId && !it.isDeleted && it.id !in desiredCanonicalIds }
        .forEach { removed ->
            dayThemeUpserts[removed.id] =
                removed.copy(
                    updatedAt = now,
                    syncedAt = null,
                    version = removed.version + 1,
                    isDeleted = true,
                )
        }

    val desiredAssignments =
        updated.assignments
            .map { assignment ->
                val canonicalIds =
                    assignment.themeIds
                        .map { uiThemeId ->
                            uiIdToCanonicalDayThemeId[uiThemeId]
                                ?: uiThemeId.takeIf { it in desiredCanonicalIds }
                                ?: throw IllegalArgumentException(
                                    "Assignment ${assignment.entityId} references unknown DayTheme $uiThemeId.",
                                )
                        }.distinct()
                        .sorted()

                DayThemeAssignmentSnapshot(
                    entityId = assignment.entityId,
                    dayThemeIds = canonicalIds,
                )
            }.filter { it.dayThemeIds.isNotEmpty() }
            .sortedBy { it.entityId }

    val localAssignments =
        if (localAssignmentDocument != null && !localAssignmentDocument.isDeleted) {
            normalizeCanonicalAssignments(localAssignmentDocument.assignments)
        } else {
            emptyList()
        }

    val assignmentUpsert =
        when {
            desiredAssignments == localAssignments -> null
            localAssignmentDocument == null && desiredAssignments.isEmpty() -> null
            else ->
                DayThemeAssignmentDocumentSnapshot(
                    dayPlanId = dayPlanId,
                    assignments = desiredAssignments,
                    createdAt = localAssignmentDocument?.createdAt ?: now,
                    updatedAt = now,
                    syncedAt = null,
                    version = (localAssignmentDocument?.version ?: 0) + 1,
                    isDeleted = false,
                )
        }

    return CanonicalDayThemeRuntimeUpdatePlan(
        themeDefinitions = definitionUpserts.values.toList(),
        dayThemes = dayThemeUpserts.values.toList(),
        assignmentDocument = assignmentUpsert,
    )
}

private fun normalizeCanonicalAssignments(
    assignments: List<DayThemeAssignmentSnapshot>,
): List<DayThemeAssignmentSnapshot> =
    assignments
        .map { assignment ->
            assignment.copy(
                dayThemeIds = assignment.dayThemeIds.distinct().sorted(),
            )
        }.filter { it.dayThemeIds.isNotEmpty() }
        .sortedBy { it.entityId }
