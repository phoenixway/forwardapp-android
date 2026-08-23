package com.romankozak.forwardappmobile.core.data.models.sync.mappers

import com.google.gson.Gson
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.CanonicalDayThemeEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.DayThemeAssignmentDocumentEntity
import com.romankozak.forwardappmobile.core.data.models.entities.day_management.ThemeDefinitionEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentDocumentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeAssignmentSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.DayThemeSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.day_management.ThemeDefinitionSnapshot

private val canonicalDayThemeGson = Gson()

fun ThemeDefinitionEntity.toCanonicalSnapshot(): ThemeDefinitionSnapshot =
    ThemeDefinitionSnapshot(
        id = id,
        title = title,
        colorArgb = colorArgb,
        iconKey = iconKey,
        description = description,
        carryForward = carryForward,
        archived = archived,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
    )

fun ThemeDefinitionSnapshot.toCanonicalEntity(): ThemeDefinitionEntity =
    ThemeDefinitionEntity(
        id = id,
        title = title,
        colorArgb = colorArgb,
        iconKey = iconKey,
        description = description,
        carryForward = carryForward,
        archived = archived,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
    )

fun CanonicalDayThemeEntity.toCanonicalSnapshot(): DayThemeSnapshot =
    DayThemeSnapshot(
        id = id,
        themeId = themeId,
        dayPlanId = dayPlanId,
        budgetPercent = budgetPercent,
        order = order,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
    )

fun DayThemeSnapshot.toCanonicalEntity(): CanonicalDayThemeEntity =
    CanonicalDayThemeEntity(
        id = id,
        themeId = themeId,
        dayPlanId = dayPlanId,
        budgetPercent = budgetPercent,
        order = order,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
    )

fun DayThemeAssignmentDocumentEntity.toCanonicalSnapshot(): DayThemeAssignmentDocumentSnapshot =
    DayThemeAssignmentDocumentSnapshot(
        dayPlanId = dayPlanId,
        assignments =
            canonicalDayThemeGson
                .fromJson(assignmentsJson, Array<DayThemeAssignmentSnapshot>::class.java)
                ?.toList()
                .orEmpty(),
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
    )

fun DayThemeAssignmentDocumentSnapshot.toCanonicalEntity(): DayThemeAssignmentDocumentEntity =
    DayThemeAssignmentDocumentEntity(
        dayPlanId = dayPlanId,
        assignmentsJson = canonicalDayThemeGson.toJson(assignments),
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncedAt = syncedAt,
        version = version,
        isDeleted = isDeleted,
    )
