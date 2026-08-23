package com.romankozak.forwardappmobile.core.data.models.sync

import com.romankozak.forwardappmobile.shared.core.models.day.canonicalDayThemeId

fun requireValidCanonicalDayThemePayload(bundle: SnapshotBundle) {
    val fields =
        listOf(
            bundle.themeDefinitions,
            bundle.dayThemes,
            bundle.dayThemeAssignmentDocuments,
        )
    val presentCount = fields.count { it != null }

    require(presentCount == 0 || presentCount == 3) {
        "Canonical Day Themes must contain either none or all of themeDefinitions/dayThemes/dayThemeAssignmentDocuments."
    }
    if (presentCount == 0) return

    val definitions = requireNotNull(bundle.themeDefinitions)
    val dayThemes = requireNotNull(bundle.dayThemes)
    val assignmentDocuments = requireNotNull(bundle.dayThemeAssignmentDocuments)

    definitions.forEach { definition ->
        require(definition.id.isNotBlank()) {
            "ThemeDefinition.id must not be blank."
        }
        require(definition.title.trim().isNotEmpty()) {
            "ThemeDefinition ${definition.id} must have a non-blank title."
        }
    }
    require(definitions.map { it.id }.distinct().size == definitions.size) {
        "Canonical Day Themes contain duplicate ThemeDefinition ids."
    }

    val definitionIds = definitions.mapTo(hashSetOf()) { it.id }
    val dayPlanIds = bundle.dayPlans.mapTo(hashSetOf()) { it.id }

    dayThemes.forEach { dayTheme ->
        require(dayTheme.themeId.isNotBlank()) {
            "DayTheme ${dayTheme.id} has a blank themeId."
        }
        require(dayTheme.dayPlanId.isNotBlank()) {
            "DayTheme ${dayTheme.id} has a blank dayPlanId."
        }
        require(dayTheme.themeId in definitionIds) {
            "DayTheme ${dayTheme.id} references missing ThemeDefinition ${dayTheme.themeId}."
        }
        require(dayTheme.dayPlanId in dayPlanIds) {
            "DayTheme ${dayTheme.id} references missing DayPlan ${dayTheme.dayPlanId}."
        }
        require(dayTheme.budgetPercent in 0..100) {
            "DayTheme ${dayTheme.id} budgetPercent must be in 0..100."
        }

        val expectedId = canonicalDayThemeId(dayTheme.dayPlanId, dayTheme.themeId)
        require(dayTheme.id == expectedId) {
            "DayTheme id ${dayTheme.id} does not match canonical identity $expectedId."
        }
    }

    require(dayThemes.map { it.id }.distinct().size == dayThemes.size) {
        "Canonical Day Themes contain duplicate DayTheme ids."
    }
    require(dayThemes.map { it.dayPlanId to it.themeId }.distinct().size == dayThemes.size) {
        "Canonical Day Themes contain duplicate (dayPlanId, themeId) pairs."
    }

    val dayThemesById = dayThemes.associateBy { it.id }

    assignmentDocuments.forEach { document ->
        require(document.dayPlanId.isNotBlank()) {
            "DayThemeAssignmentDocument.dayPlanId must not be blank."
        }
        require(document.dayPlanId in dayPlanIds) {
            "DayThemeAssignmentDocument references missing DayPlan ${document.dayPlanId}."
        }

        require(document.assignments.map { it.entityId }.distinct().size == document.assignments.size) {
            "DayThemeAssignmentDocument ${document.dayPlanId} contains duplicate entity assignments."
        }

        document.assignments.forEach { assignment ->
            require(assignment.entityId.isNotBlank()) {
                "DayTheme assignment entityId must not be blank."
            }
            require(assignment.dayThemeIds.distinct().size == assignment.dayThemeIds.size) {
                "DayTheme assignment ${assignment.entityId} contains duplicate dayThemeIds."
            }

            assignment.dayThemeIds.forEach { dayThemeId ->
                val referenced =
                    requireNotNull(dayThemesById[dayThemeId]) {
                        "DayTheme assignment ${assignment.entityId} references missing DayTheme $dayThemeId."
                    }
                require(referenced.dayPlanId == document.dayPlanId) {
                    "DayTheme assignment ${assignment.entityId} references DayTheme $dayThemeId from another day."
                }
            }
        }
    }

    require(assignmentDocuments.map { it.dayPlanId }.distinct().size == assignmentDocuments.size) {
        "Canonical Day Themes contain duplicate assignment documents for the same dayPlanId."
    }
}
