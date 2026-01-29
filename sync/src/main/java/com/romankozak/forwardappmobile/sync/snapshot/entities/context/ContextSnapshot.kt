package com.romankozak.forwardappmobile.sync.snapshot

/**
 * Снапшот списку (проекту/контексту).
 */
data class ContextSnapshot(
    val id: String,
    val name: String,
    val description: String?,
    val parentId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val version: Long,
    val isDeleted: Boolean,
    val isCompleted: Boolean,
    val order: Long,
    val tags: List<String>,
    val contextStatus: String,
    val totalTimeSpentMinutes: Long,
    val scoringStatus: String,
    // Візуальні налаштування
    val defaultViewModeName: String?,
    val isExpanded: Boolean,
    val showCheckboxes: Boolean,
    val roleCode: String?
)