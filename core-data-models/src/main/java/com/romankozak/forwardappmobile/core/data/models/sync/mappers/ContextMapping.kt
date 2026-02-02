package com.romankozak.forwardappmobile.core.data.models.sync.mappers

import com.romankozak.forwardappmobile.core.data.models.entities.Context
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.context.ContextSnapshot
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toEntity
import com.romankozak.forwardappmobile.core.data.models.sync.snapshots.toSnapshot

fun Context.toSnapshot(): ContextSnapshot = ContextSnapshot(
    id = this.id,
    name = this.name,
    parentId = this.parentId,
    description = this.description,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt ?: this.createdAt,
    isExpanded = this.isExpanded,
    isDeleted = this.isDeleted,
    version = this.version,
    tags = this.tags,
    relatedLinks = this.relatedLinks?.map { it.toSnapshot() },
    // Явне перетворення типів:
    order = this.order.toInt(), // Long -> Int
    isAttachmentsExpanded = this.isAttachmentsExpanded,
    defaultViewModeName = this.defaultViewModeName,
    isCompleted = this.isCompleted,
    isContextManagementEnabled = this.isContextManagementEnabled,
    contextStatus = this.contextStatus,
    contextStatusText = this.contextStatusText,
    contextLogLevel = this.contextLogLevel,
    totalTimeSpentMinutes = this.totalTimeSpentMinutes ?: 0L,

    // Перетворення Float в Int (цілі числа для скорингу в Snapshot)
    valueImportance = this.valueImportance.toInt(),
    valueImpact = this.valueImpact.toInt(),
    effort = this.effort.toInt(),
    cost = this.cost.toInt(),
    risk = this.risk.toInt(),

    weightEffort = this.weightEffort,
    weightCost = this.weightCost,
    weightRisk = this.weightRisk,

    // Перетворення для точності Double
    rawScore = this.rawScore.toDouble(),     // Float -> Double
    displayScore = this.displayScore.toDouble(), // Int -> Double

    scoringStatus = this.scoringStatus,
    showCheckboxes = this.showCheckboxes,
    roleCode = this.roleCode,
)

fun ContextSnapshot.toEntity(): Context = Context(
    id = this.id,
    name = this.name,
    parentId = this.parentId,
    description = this.description,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    isExpanded = this.isExpanded,
    isDeleted = this.isDeleted,
    version = this.version,
    tags = this.tags,
    relatedLinks = this.relatedLinks?.map { it.toEntity() },

    // Зворотне перетворення типів:
    order = this.order.toLong(), // Int -> Long
    isAttachmentsExpanded = this.isAttachmentsExpanded,
    defaultViewModeName = this.defaultViewModeName,
    isCompleted = this.isCompleted,
    isContextManagementEnabled = this.isContextManagementEnabled,
    contextStatus = this.contextStatus,
    contextStatusText = this.contextStatusText,
    contextLogLevel = this.contextLogLevel,
    totalTimeSpentMinutes = this.totalTimeSpentMinutes,

    // Повертаємо до Float (якщо Entity очікує Float)
    valueImportance = this.valueImportance.toFloat(),
    valueImpact = this.valueImpact.toFloat(),
    effort = this.effort.toFloat(),
    cost = this.cost.toFloat(),
    risk = this.risk.toFloat(),

    weightEffort = this.weightEffort,
    weightCost = this.weightCost,
    weightRisk = this.weightRisk,

    rawScore = this.rawScore.toFloat(),    // Double -> Float
    displayScore = this.displayScore.toInt(), // Double -> Int (увага! можлива втрата дробової частини)

    scoringStatus = this.scoringStatus,
    showCheckboxes = this.showCheckboxes,
    roleCode = this.roleCode,
)
