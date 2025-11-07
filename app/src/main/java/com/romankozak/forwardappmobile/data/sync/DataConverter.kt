package com.romankozak.forwardappmobile.data.sync

import com.romankozak.forwardappmobile.core.database.models.Goal
import com.romankozak.forwardappmobile.shared.data.database.models.Project
import com.romankozak.forwardappmobile.shared.data.database.models.ScoringStatusValues
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

fun DesktopGoal.toGoal(): Goal {
    val updatedAtMillis = this.updatedAt?.let {
        try { OffsetDateTime.parse(it, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli() } catch (e: Exception) { null }
    }
    val createdAtMillis = try { OffsetDateTime.parse(this.createdAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli() } catch (e: Exception) { System.currentTimeMillis() }

    return Goal(
        id = this.id,
        text = this.text,
        description = this.description,
        completed = this.completed,
        createdAt = createdAtMillis,
        updatedAt = updatedAtMillis,
        tags = this.tags,
        relatedLinks = null, // Desktop backup does not contain related links for goals
        valueImportance = this.valueImportance,
        valueImpact = this.valueImpact,
        effort = this.effort,
        cost = this.cost,
        risk = this.risk,
        weightEffort = this.weightEffort,
        weightCost = this.weightCost,
        weightRisk = this.weightRisk,
        rawScore = this.rawScore,
        displayScore = this.displayScore,
        scoringStatus = this.scoringStatus ?: ScoringStatusValues.NOT_ASSESSED
    )
}

fun DesktopGoalList.toProject(): Project {
    val updatedAtMillis = this.updatedAt?.let {
        try { OffsetDateTime.parse(it, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli() } catch (e: Exception) { null }
    }
    val createdAtMillis = try { OffsetDateTime.parse(this.createdAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant().toEpochMilli() } catch (e: Exception) { System.currentTimeMillis() }

    return Project(
        id = this.id,
        name = this.name,
        description = this.description,
        parentId = this.parentId,
        createdAt = createdAtMillis,
        updatedAt = updatedAtMillis,
        isExpanded = this.isExpanded ?: true,
        order = this.order ?: 0,
        tags = this.tags,
        isCompleted = this.isCompleted,
        valueImportance = this.valueImportance,
        valueImpact = this.valueImpact,
        effort = this.effort,
        cost = this.cost,
        risk = this.risk,
        scoringStatus = this.scoringStatus ?: ScoringStatusValues.NOT_ASSESSED
    )
}
