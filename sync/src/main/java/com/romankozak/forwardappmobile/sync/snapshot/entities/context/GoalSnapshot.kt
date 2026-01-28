package com.romankozak.forwardappmobile.sync.snapshot

/**
 * Стабільна версія цілі для синхронізації.
 */
data class GoalSnapshot(
    val id: String,
    val text: String,
    val description: String?,
    val isCompleted: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val isDeleted: Boolean,
    val tags: List<String>,
    val scoringStatus: String,
    // Поля скорингу
    val valueImportance: Float,
    val valueImpact: Float,
    val effort: Float,
    val cost: Float,
    val risk: Float,
    // Зв'язки з батьківськими цілями (якщо є)
    val parentValueImportance: Float?,
    val impactOnParentGoal: Float?
)