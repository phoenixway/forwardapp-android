package com.romankozak.forwardappmobile.sync.snapshot

/**
 * Снапшот глобального пресету (шаблону) структури контексту.
 */
data class ContextRoleProfileSnapshot(
    val id: String,
    val code: String,
    val label: String,
    val description: String?,
    // Налаштування модулів за замовчуванням для пресету
    val enableInbox: Boolean?,
    val enableLog: Boolean?,
    val enableArtifact: Boolean?,
    val enableAdvanced: Boolean?,
    val enableDashboard: Boolean?,
    val enableBacklog: Boolean?,
    val enableAttachments: Boolean?,
    val enableAutoLinkSubprojects: Boolean?,
    val version: Long,
    val updatedAt: Long,
    val isDeleted: Boolean
)